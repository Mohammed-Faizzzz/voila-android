package com.mohdfaizzzz.voila

import android.app.ComponentCaller
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.mohdfaizzzz.voila.ui.theme.VoilaTheme
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit
import androidx.core.net.toUri
import android.app.DatePickerDialog
import android.util.Log
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.clickable


class DashboardActivity : ComponentActivity() {

    private val googleAuthClient: GoogleAuthClient by lazy { GoogleAuthClient(applicationContext) }
    private val TAG = "DashboardActivity" // Define TAG for logging

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val userId = FirebaseAuth.getInstance().currentUser?.uid.orEmpty()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 101)
        }

        val hasGmailAccess = intent.getBooleanExtra("HAS_GMAIL_ACCESS", false)
        Log.d(TAG, "DashboardActivity launched. Has Gmail Access: $hasGmailAccess")

        setContent {
            VoilaTheme {
                var showLoading by remember { mutableStateOf(true) }

                if (showLoading) {
                    LoadingScreen {
                        showLoading = false
                    }
                } else {
                    DashboardScreen(
                        userId = userId,
                        hasGmailAccess = hasGmailAccess, // Pass the flag to your Composable
                        googleAuthClient = googleAuthClient, // Pass the client instance
                        onSignOut = {
                            lifecycleScope.launch {
                                googleAuthClient.signOut()
                                startActivity(Intent(this@DashboardActivity, MainActivity::class.java))
                                finish()
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun DashboardScreen(
    userId: String,
    hasGmailAccess: Boolean,
    googleAuthClient: GoogleAuthClient,
    onSignOut: () -> Unit) {
    val subscriptions = remember { mutableStateListOf<Subscription>() }
    var showDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val inboxTitle = remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        getSubscriptions(userId) {
            subscriptions.clear()
            subscriptions.addAll(it)
            showRenewalNotificationIfDueSoon(context, it)
        }

        if (hasGmailAccess) {
            Log.d("DashboardScreen", "Attempting to fetch first email title...")
            inboxTitle.value = googleAuthClient.getFirstInboxTitle()
            Log.d("DashboardScreen", "Fetched inbox title: ${inboxTitle.value}")
        } else {
            Log.d("DashboardScreen", "Gmail access not granted, not fetching email title.")
            inboxTitle.value = "Gmail access not granted." // Inform the user
        }
    }

    Scaffold(containerColor = Color.White) { innerPadding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(innerPadding).padding(24.dp)
        ) {
            Text(
                "Your Subscriptions",
                fontSize = 28.sp,
                color = Color(0xFFC084FC),
                fontWeight = FontWeight.Bold
            )

            inboxTitle.value?.let { title ->
                Text(
                    text = "First Email Subject: $title",
                    fontSize = 16.sp,
                    color = Color.Blue, // Use a distinct color
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }

            subscriptions.forEach { sub ->
                SubscriptionCard(sub) { }
            }

            Spacer(modifier = Modifier.weight(1f))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                OutlinedButton(
                    onClick = onSignOut,
                    border = BorderStroke(1.dp, Color(0xFFC084FC)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFC084FC)),
                    modifier = Modifier.weight(1f).padding(end = 8.dp)
                ) {
                    Text("Sign Out")
                }

                Button(
                    onClick = { showDialog = true },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFC084FC),
                        contentColor = Color.White
                    ),
                    modifier = Modifier.weight(1f).padding(start = 8.dp)
                ) {
                    Text("Add Subscription")
                }
            }
        }

        if (showDialog) {
            AddSubscriptionDialog(
                onDismiss = { showDialog = false },
                onAdd = { newSub ->
                    addSubscription(userId, newSub) {
                        subscriptions.add(newSub)
                        showDialog = false
                    }
                }
            )
        }
    }
}

fun showRenewalNotificationIfDueSoon(context: Context, subs: List<Subscription>) {
    val now = Timestamp.now().toDate()
    val dayInMillis = TimeUnit.DAYS.toMillis(1)

    subs.forEach { sub ->
        val diff = sub.renewalDate.toDate().time - now.time
        if (diff in 0..dayInMillis) {
            val manager = ContextCompat.getSystemService(context, NotificationManager::class.java)
            val channelId = "voila_renewal_reminders"

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(channelId, "Renewal Reminders", NotificationManager.IMPORTANCE_DEFAULT)
                manager?.createNotificationChannel(channel)
            }

            val notification = NotificationCompat.Builder(context, channelId)
                .setContentTitle("Upcoming Renewal")
                .setContentText("${sub.serviceName} renews tomorrow!")
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .build()

            manager?.notify(sub.id.hashCode(), notification)
        }
    }
}

@Composable
fun SubscriptionCard(sub: Subscription, onCancel: () -> Unit = {}) {
    val context = LocalContext.current

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFF8F8F8)
        ),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = sub.serviceName,
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.Black
            )
            Text(
                text = "${sub.currency} ${sub.amount}",
                fontSize = 16.sp,
                color = Color.DarkGray
            )
            Text(
                text = "Renews on ${sub.renewalDate.toDate().toString().substring(0, 10)}",
                fontSize = 14.sp,
                color = Color.Gray
            )
            Text(
                text = "Every ${sub.renewalFreq}",
                fontSize = 14.sp,
                color = Color.Gray
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedButton(
                onClick = {
                    val intent = Intent(Intent.ACTION_VIEW).apply {
                        data = "https://www.netflix.com/cancelplan".toUri()
                    }
                    context.startActivity(intent)
                    onCancel()
                },
                border = BorderStroke(1.dp, Color(0xFFC084FC)),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = Color(0xFFC084FC)
                )
            ) {
                Text("Cancel")
            }
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddSubscriptionDialog(onDismiss: () -> Unit, onAdd: (Subscription) -> Unit) {
    var serviceName by remember { mutableStateOf("") }
    var currency by remember { mutableStateOf("GBP") }
    var amount by remember { mutableStateOf("") }
    var renewalDate by remember { mutableStateOf("") }
    var renewalFreq by remember { mutableStateOf("Monthly") }
    var cancelURL by remember { mutableStateOf("") }

    val currencyOptions = listOf("USD", "EUR", "GBP", "SGD", "INR", "JPY", "AUD", "CAD") // add more if needed
    val renewalOptions = listOf("Monthly", "Annual")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Subscription") },
        text = {
            Column {
                OutlinedTextField(
                    value = serviceName,
                    onValueChange = { serviceName = it },
                    label = { Text("Service Name") },
                    modifier = Modifier.fillMaxWidth()
                )

                Row(modifier = Modifier.fillMaxWidth()) {
                    // Currency Dropdown
                    var expandedCurrency by remember { mutableStateOf(false) }
                    Box(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                        OutlinedTextField(
                            value = currency,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Currency") },
                            modifier = Modifier.fillMaxWidth(),
                            trailingIcon = {
                                IconButton(onClick = { expandedCurrency = true }) {
                                    Icon(Icons.Default.ArrowDropDown, contentDescription = "Currency")
                                }
                            }
                        )
                        DropdownMenu(
                            expanded = expandedCurrency,
                            onDismissRequest = { expandedCurrency = false }
                        ) {
                            currencyOptions.forEach {
                                DropdownMenuItem(
                                    text = { Text(it) },
                                    onClick = {
                                        currency = it
                                        expandedCurrency = false
                                    }
                                )
                            }
                        }
                    }

                    // Amount Field
                    OutlinedTextField(
                        value = amount,
                        onValueChange = { amount = it },
                        label = { Text("Amount") },
                        modifier = Modifier.weight(1f)
                    )
                }

                // Renewal Date Picker
                OutlinedTextField(
                    value = renewalDate,
                    onValueChange = { renewalDate = it },
                    label = { Text("Renewal Date (yyyy-MM-dd)") },
                )

                // Renewal Frequency Dropdown
                var expandedFreq by remember { mutableStateOf(false) }
                Box(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                    OutlinedTextField(
                        value = renewalFreq,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Renewal Frequency") },
                        trailingIcon = {
                            IconButton(onClick = { expandedFreq = true }) {
                                Icon(Icons.Default.ArrowDropDown, contentDescription = "Frequency")
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                    DropdownMenu(
                        expanded = expandedFreq,
                        onDismissRequest = { expandedFreq = false }
                    ) {
                        renewalOptions.forEach {
                            DropdownMenuItem(
                                text = { Text(it) },
                                onClick = {
                                    renewalFreq = it
                                    expandedFreq = false
                                }
                            )
                        }
                    }
                }
                OutlinedTextField(
                    value = cancelURL,
                    onValueChange = { cancelURL = it },
                    label = { Text("Cancellation Link") },
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                if (serviceName.isNotBlank() && amount.isNotBlank() && renewalDate.isNotBlank()) {
                    val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                    val parsedDate = dateFormat.parse(renewalDate)
                    val timestamp = Timestamp(parsedDate!!)

                    val sub = Subscription(
                        id = UUID.randomUUID().toString(),
                        serviceName = serviceName,
                        currency = currency,
                        amount = amount.toDoubleOrNull() ?: 0.0,
                        renewalDate = timestamp,
                        renewalFreq = renewalFreq
                    )
                    onAdd(sub)
                }
            }) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}


