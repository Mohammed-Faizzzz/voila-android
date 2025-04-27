package com.mohdfaizzzz.voila

import android.app.Activity
import android.content.Intent
import android.content.IntentSender
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.auth.api.identity.AuthorizationRequest
import com.google.android.gms.auth.api.identity.AuthorizationResult
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.common.api.Scope
import com.google.api.services.gmail.GmailScopes
import com.google.firebase.auth.FirebaseAuth
import com.mohdfaizzzz.voila.ui.theme.VoilaTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController


private const val REQUEST_CODE_AUTH = 1001

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val googleAuthClient = GoogleAuthClient(applicationContext)

        setContent {
            VoilaTheme {
                var isSignedIn by rememberSaveable { mutableStateOf(googleAuthClient.isSignedIn()) }

                if (isSignedIn) {
                    LaunchedEffect(Unit) {
                        startActivity(Intent(this@MainActivity, DashboardActivity::class.java))
                        finish()
                    }
                } else {
                    SignInScreen(
                        isSignedIn = false,
                        onSignInClick = {
                            lifecycleScope.launch {
                                val success = googleAuthClient.signIn()
                                if (success) {
                                    requestGmailAuthorization(this@MainActivity)
                                    startActivity(Intent(this@MainActivity, DashboardActivity::class.java))
                                    finish()
                                }
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun SignInScreen(
    isSignedIn: Boolean,
    onSignInClick: () -> Unit
) {
    Scaffold(
        containerColor = Color.White,
        modifier = Modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Voila", fontSize = 44.sp, fontWeight = FontWeight.Bold, color = Color(0xFFA855F7))
            Spacer(modifier = Modifier.height(8.dp))
            Text("Your subscriptions, simplified.", color = Color(0xFF666666), fontSize = 16.sp)
            Spacer(modifier = Modifier.height(40.dp))
            Button(
                onClick = onSignInClick,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFA855F7), contentColor = Color.White)
            ) {
                Text("Sign in with Google", fontSize = 16.sp)
            }
        }
    }
}

@Composable
fun LoadingScreen(onFinishLoading: () -> Unit) {
    LaunchedEffect(Unit) {
        delay(20)
        onFinishLoading()
    }
    Box(
        modifier = Modifier.fillMaxSize().background(Color.White),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(color = Color(0xFFA855F7))
    }
}

private fun requestGmailAuthorization(activity: Activity) {
    val requestedScopes = listOf(Scope(GmailScopes.GMAIL_READONLY))
    val authorizationRequest = AuthorizationRequest.builder().setRequestedScopes(requestedScopes).build()
    Identity.getAuthorizationClient(activity).authorize(authorizationRequest)
        .addOnSuccessListener { result ->
            result.pendingIntent?.let {
                try {
                    activity.startIntentSenderForResult(it.intentSender, REQUEST_CODE_AUTH, null, 0, 0, 0, null)
                } catch (e: IntentSender.SendIntentException) {
                    Log.e("Auth", "Failed to start auth UI", e)
                }
            }
        }
        .addOnFailureListener { e -> Log.e("Auth", "Authorization failed", e) }
}