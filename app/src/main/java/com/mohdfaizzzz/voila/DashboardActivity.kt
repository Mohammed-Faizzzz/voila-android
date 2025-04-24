package com.mohdfaizzzz.voila

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.android.identity.util.UUID
import com.mohdfaizzzz.voila.ui.theme.VoilaTheme

class DashboardActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            VoilaTheme {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    topBar = {
                        TopAppBar(title = { Text("Your Dashboard") })
                    }
                ) { innerPadding ->
                    DashboardScreen(Modifier.padding(innerPadding))
                }
            }
        }
    }
}

@Composable
fun DashboardScreen(modifier: Modifier = Modifier) {

    val subscriptions = remember {
        mutableStateListOf(
            Subscription("1", "Netflix", 15.99, "2025-05-01"),
            Subscription("2", "Spotify", 9.99, "2025-04-25")
        )
    }

    Scaffold { innerPadding ->
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
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    elevation = CardDefaults.cardElevation(4.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(text = sub.serviceName, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        Text(text = "RM ${sub.amount}", fontSize = 16.sp)
                        Text(text = "Renews on ${sub.renewalDate}", fontSize = 14.sp, color = Color.Gray)
                        Spacer(Modifier.height(8.dp))
                        OutlinedButton(onClick = {
                            subscriptions.remove(sub)
                        }) {
                            Text("Cancel")
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = {
                    // Just add a fake one for now
                    subscriptions.add(
                        Subscription(
                            id = UUID.randomUUID().toString(),
                            serviceName = "Disney+",
                            amount = 12.99,
                            renewalDate = "2025-06-10"
                        )
                    )
                },
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                Text("Add Subscription")
            }
        }
    }
}

