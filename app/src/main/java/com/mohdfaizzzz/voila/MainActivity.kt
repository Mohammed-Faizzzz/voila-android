package com.mohdfaizzzz.voila

import android.app.Activity
import android.content.ContentValues.TAG
import android.content.Context
import android.content.IntentSender
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat.startIntentSenderForResult
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.auth.api.identity.AuthorizationRequest
import com.google.android.gms.auth.api.identity.AuthorizationResult
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.common.Scopes
import com.google.android.gms.common.api.Scope
import com.mohdfaizzzz.voila.ui.theme.VoilaTheme
import kotlinx.coroutines.launch
import java.util.Arrays
import com.google.api.services.gmail.GmailScopes



private const val REQUEST_CODE_AUTH = 1001

//class MainActivity : ComponentActivity() {
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        enableEdgeToEdge()
//        val googleAuthClient = GoogleAuthClient(applicationContext)
//        setContent {
//            VoilaTheme {
//                var isSignedIn by rememberSaveable {
//                    mutableStateOf(googleAuthClient.isSignedIn())
//                }
//                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
//                    Column(
//                        modifier = Modifier
//                            .fillMaxSize()
//                            .padding(innerPadding),
//                        verticalArrangement = Arrangement.Center,
//                        horizontalAlignment = Alignment.CenterHorizontally
//                    ) {
//                        if (isSignedIn) {
////                            requestGmailAuthorization(this@MainActivity)
//                            OutlinedButton(onClick = {
//                                lifecycleScope.launch {
//                                    googleAuthClient.signOut()
//                                    isSignedIn = false
//                                }
//                            }) {
//                                Text(
//                                    text = "Sign Out",
//                                    fontSize = 16.sp,
//                                    modifier = Modifier.padding(
//                                        horizontal = 24.dp, vertical = 4.dp
//                                    )
//                                )
//                            }
//                        } else {
//                            OutlinedButton(onClick = {
//                                lifecycleScope.launch {
//                                    isSignedIn = googleAuthClient.signIn()
//                                }
//                            }) {
//                                Text(
//                                    text = "Sign In",
//                                    fontSize = 16.sp,
//                                    modifier = Modifier.padding(
//                                        horizontal = 24.dp, vertical = 4.dp
//                                    )
//                                )
//                            }
//                        }
//                    }
//                }
//            }
//        }
//    }
//}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val googleAuthClient = GoogleAuthClient(applicationContext)

        setContent {
            VoilaTheme {
                var isSignedIn by rememberSaveable { mutableStateOf(googleAuthClient.isSignedIn()) }

                SignInScreen(
                    isSignedIn = isSignedIn,
                    onSignInClick = {
                        lifecycleScope.launch {
                            isSignedIn = googleAuthClient.signIn()
                            if (isSignedIn) {
//                                requestGmailAuthorization(this@MainActivity)
                                println("Sign in Successful!")
                            }
                        }
                    },
                    onSignOutClick = {
                        lifecycleScope.launch {
                            googleAuthClient.signOut()
                            isSignedIn = false
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun SignInScreen(
    isSignedIn: Boolean,
    onSignInClick: () -> Unit,
    onSignOutClick: () -> Unit
) {
    Scaffold(
        containerColor = Color.White,
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Voila",
                fontSize = 44.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFA855F7) // Updated purple
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Your subscriptions, simplified.",
                color = Color(0xFF666666),
                fontSize = 16.sp
            )

            Spacer(modifier = Modifier.height(40.dp))

            if (isSignedIn) {
                OutlinedButton(
                    onClick = onSignOutClick,
                    border = BorderStroke(1.dp, Color(0xFFA855F7))
                ) {
                    Text("Sign Out", fontSize = 16.sp, color = Color(0xFFA855F7))
                }
            } else {
                Button(
                    onClick = onSignInClick,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFA855F7),
                        contentColor = Color.White
                    )
                ) {
                    Text("Sign in with Google", fontSize = 16.sp)
                }
            }
        }
    }
}




private fun requestGmailAuthorization(activity: Activity) {
    val requestedScopes = listOf(Scope(GmailScopes.GMAIL_READONLY))
    val authorizationRequest =
        AuthorizationRequest.builder().setRequestedScopes(requestedScopes).build()
    Identity.getAuthorizationClient(activity)
        .authorize(authorizationRequest)
        .addOnSuccessListener { authorizationResult: AuthorizationResult ->
            if (authorizationResult.hasResolution()) {
                val pendingIntent = authorizationResult.pendingIntent
                try {
                    activity.startIntentSenderForResult( // ✅ Use activity
                        pendingIntent!!.intentSender,
                        REQUEST_CODE_AUTH,
                        null,
                        0,
                        0,
                        0,
                        null
                    )
                } catch (e: IntentSender.SendIntentException) {
                    Log.e(TAG, "Couldn't start Authorization UI: ${e.localizedMessage}")
                }
            } else {
                println("Access Granted")
                // TODO: Read emails here
            }
        }
        .addOnFailureListener { e ->
            Log.e(TAG, "Failed to authorize", e)
        }
}



