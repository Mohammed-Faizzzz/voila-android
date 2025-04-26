package com.mohdfaizzzz.voila

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.android.identity.util.UUID
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.mohdfaizzzz.voila.ui.theme.VoilaTheme
import java.text.SimpleDateFormat
import java.util.Locale

class DashboardActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            VoilaTheme {
                val userId = FirebaseAuth.getInstance().currentUser?.uid.orEmpty()
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Column(modifier = Modifier.padding(innerPadding)) {
                        DashboardScreen(userId = userId)
                    }
                }
            }
        }
    }
}

@Composable
fun DashboardScreen(userId: String) {
    val subscriptions = remember { mutableStateListOf<Subscription>() }
    var showDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        getSubscriptions(userId) {
            subscriptions.clear()
            subscriptions.addAll(it)
        }
    }

    Scaffold(containerColor = Color.White) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp)
        ) {
            Text(
                text = "Your Subscriptions",
                fontSize = 28.sp,
                color = Color(0xFFC084FC),
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 24.dp)
            )

            subscriptions.forEach { sub ->
                SubscriptionCard(sub) {
                    // delete logic here if needed
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = { showDialog = true },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFC084FC),
                    contentColor = Color.White
                ),
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                Text("Add Subscription")
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

@Composable
fun SubscriptionCard(sub: Subscription, onCancel: () -> Unit) {
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
                onClick = onCancel,
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


@Composable
fun AddSubscriptionDialog(onDismiss: () -> Unit, onAdd: (Subscription) -> Unit) {
    var serviceName by remember { mutableStateOf("") }
    var currency by remember { mutableStateOf("GBP") }
    var amount by remember { mutableStateOf("") }
    var renewalDateText by remember { mutableStateOf("") } // user inputs date as text
    var renewalFreq by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Subscription") },
        text = {
            Column {
                OutlinedTextField(
                    value = serviceName,
                    onValueChange = { serviceName = it },
                    label = { Text("Service Name") }
                )
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = { Text("Amount") }
                )
                OutlinedTextField(
                    value = renewalDateText,
                    onValueChange = { renewalDateText = it },
                    label = { Text("Renewal Date (yyyy-MM-dd)") }
                )
                OutlinedTextField(
                    value = renewalFreq,
                    onValueChange = { renewalFreq = it },
                    label = { Text("Renewal Frequency") }
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                if (serviceName.isNotBlank() && amount.isNotBlank() && renewalDateText.isNotBlank() && renewalFreq.isNotBlank()) {
                    try {
                        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                        val parsedDate = dateFormat.parse(renewalDateText)
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
                    } catch (e: Exception) {
                        Log.e("AddDialog", "Date parsing error", e)
                    }
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


