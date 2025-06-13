package com.mohdfaizzzz.voila

import android.app.Activity
import android.content.Intent
import android.content.IntentSender
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import com.mohdfaizzzz.voila.ui.theme.VoilaTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


private const val REQUEST_CODE_AUTH = 1001
private const val TAG = "MainActivity: "

// MainActivity is the entry point of the Android application
class MainActivity : ComponentActivity() {
    private lateinit var googleAuthClient: GoogleAuthClient
    private lateinit var gmailAuthLauncher: ActivityResultLauncher<IntentSenderRequest>
    override fun onCreate(savedInstanceState: Bundle?) { // called when the app is launched
        super.onCreate(savedInstanceState) // restore the state of the UI as it was before it was previously closed, if applicable
        enableEdgeToEdge() // UI feature

        googleAuthClient = GoogleAuthClient(context = applicationContext)

        gmailAuthLauncher = registerForActivityResult(
            ActivityResultContracts.StartIntentSenderForResult()
        ) { result ->
            val authResult: AuthorizationResult? = result.data?.let { intentData ->
                Identity.getAuthorizationClient(this).getAuthorizationResultFromIntent(intentData)
            }

            if (authResult != null) {
                val requestedScopesObjects: List<Scope> = listOf(Scope(GmailScopes.GMAIL_READONLY))
                val requestedScopes: List<String> = requestedScopesObjects.map { it.scopeUri }
                val actualGrantedScopes: MutableList<String> = authResult.grantedScopes
                val hasGmailAccess = actualGrantedScopes.containsAll(requestedScopes)

                if (hasGmailAccess) {
                    println(TAG + "Gmail Authorization Successful! Navigating to Dashboard.")
                } else {
                    println(TAG + "Gmail Authorization Failed or Denied by user. Navigating to Dashboard without full access.")
                }
                navigateToDashboard(hasGmailAccess)
            } else {
                // This case would happen if result.data is null, meaning no result intent was provided
                println(TAG + "Authorization result intent was null. Navigating to Dashboard without full access.")
                navigateToDashboard(false)
            }
        }

        // UI Content of the App
        setContent {
            VoilaTheme {
                var isSignedIn by rememberSaveable { mutableStateOf(googleAuthClient.isSignedIn()) }

                if (isSignedIn) { // If signed in, show dashboard
                    LaunchedEffect(Unit) {
                        navigateToDashboard(true)
                    }
                } else {
                    SignInScreen(
                        isSignedIn = false,
                        onSignInClick = {
                            lifecycleScope.launch {
                                val success = googleAuthClient.signIn()
                                if (success) {
                                    isSignedIn = true
                                    requestGmailAuthorization()
                                } else {
                                    Log.e(TAG, "Google Sign-In failed.")
                                }
                            }
                        }
                    )
                }
            }
        }
    }

    private fun navigateToDashboard(hasGmailAccess: Boolean) {
        val intent = Intent(this@MainActivity, DashboardActivity::class.java).apply {
            putExtra("HAS_GMAIL_ACCESS", hasGmailAccess)
        }
        startActivity(intent)
        finish()
    }

    private fun requestGmailAuthorization() { // No longer needs `activity: Activity` parameter
        val requestedScopes = listOf(Scope(GmailScopes.GMAIL_READONLY))
        val authorizationRequest = AuthorizationRequest.builder().setRequestedScopes(requestedScopes).build()
        Identity.getAuthorizationClient(this).authorize(authorizationRequest)
            .addOnSuccessListener { result ->
                result.pendingIntent?.let {
                    try {
                        // Use the new launcher to start the intent
                        gmailAuthLauncher.launch(IntentSenderRequest.Builder(it).build())
                    } catch (e: IntentSender.SendIntentException) {
                        Log.e(TAG, "Failed to create IntentSenderRequest", e)
                    }
                }
            }
            .addOnFailureListener { e -> Log.e(TAG, "Authorization failed before launching UI", e) }
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
