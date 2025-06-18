package com.mohdfaizzzz.voila

import android.content.Context
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import com.google.api.client.http.HttpRequestInitializer
import com.google.api.services.gmail.Gmail
import com.google.api.services.gmail.model.ListMessagesResponse
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.tasks.await
import java.security.MessageDigest
import java.util.UUID
import java.util.concurrent.CancellationException
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.mohdfaizzzz.voila.BuildConfig

class GoogleAuthClient (private val context: Context,){
    private val tag = "GoogleAuthClient: "

    private val credentialManager = CredentialManager.create(context)
    private val firebaseAuth = FirebaseAuth.getInstance()

    var currentGmailAccessToken: String? = null // To store the access token
    var currentGmailAccountId: String? = null

    fun isSignedIn(): Boolean {
        if (firebaseAuth.currentUser != null) {
            println(tag + "User already signed in")
            return true
        }
        return false
    }

    suspend fun signIn(): Boolean {
        if (isSignedIn()) {
            return true
        }
        try {
            val result = getCredentialReq()
            return handleSignIn(result)
        } catch (e: Exception) {
            e.printStackTrace()
            if (e is CancellationException) throw e

            println(tag + "signIn error: + ${e.message}")
            return false
        }
    }

    private suspend fun handleSignIn(result: GetCredentialResponse): Boolean {
        val credential = result.credential

        if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
            try {
                val tokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                println(tag + "Name: ${tokenCredential.displayName}")
                println(tag + "Email: ${tokenCredential.id}")
                println(tag + "Pic: ${tokenCredential.profilePictureUri}")
                currentGmailAccountId = tokenCredential.id

                val authCredential = GoogleAuthProvider.getCredential(
                    tokenCredential.idToken, null
                )
                val authResult = firebaseAuth.signInWithCredential(authCredential).await()
                return authResult.user != null
            } catch (e: GoogleIdTokenParsingException) {
                println(tag + "GoogleIdTokenParsingException: ${e.message}")
                return false
            }
        } else {
            println(tag + "credential is not GoogleIdTokenCredential")
            return false
        }
    }

    private suspend fun getCredentialReq(): GetCredentialResponse {
        val rawNonce = UUID.randomUUID().toString()
        val bytes = rawNonce.toByteArray()
        val md = MessageDigest.getInstance("SHA-256")
        val digest = md.digest(bytes)
        val hashedNonce = digest.fold("") { str, it -> str + "%02x".format(it) }

        val req = GetCredentialRequest.Builder()
            .addCredentialOption(
                GetGoogleIdOption.Builder()
                    .setFilterByAuthorizedAccounts(false)
                    .setServerClientId(BuildConfig.GOOGLE_SERVER_CLIENT_ID)
                    .setAutoSelectEnabled(false)
                    .setNonce(hashedNonce)
                    .build()
            )
            .build()
        return credentialManager.getCredential(
            request = req, context = context
        )
    }

    suspend fun signOut() {
        credentialManager.clearCredentialState(ClearCredentialStateRequest())
        firebaseAuth.signOut()
    }

    suspend fun getFirstInboxTitle(): String {
        val accessToken = currentGmailAccessToken // Accessing the class property
        val userIdForGmail = currentGmailAccountId // Accessing the class property

        if (accessToken.isNullOrBlank()) {
            println(tag + "Access token is null or blank for Gmail API. Cannot fetch email.")
            return "Access token is missing for Gmail API."
        }
        if (userIdForGmail.isNullOrBlank()) {
            println(tag + "User ID for Gmail API is null or blank. Cannot fetch email.")
            return "User account for Gmail API is missing."
        }

        try {
            // Build an HttpRequestInitializer using the accessToken for direct bearer token authentication
            val requestInitializer = HttpRequestInitializer { httpRequest ->
                httpRequest.headers.authorization = "Bearer $accessToken"
            }

            // Build the Gmail service client
            val service = Gmail.Builder(
//                AndroidHttp.newCompatibleTransport(),
                NetHttpTransport(),
                GsonFactory.getDefaultInstance(),     // Uses Gson for JSON parsing
                requestInitializer                    // Provides our custom initializer with the access token
            )
                .setApplicationName("Voila") // Replace "Voila" with your actual app name
                .build()

            // Fetch the list of messages for the specified user (use "me" for the authorized user, or their email)
            val response: ListMessagesResponse = service.users().messages().list(userIdForGmail)
                .setMaxResults(1L) // Request only the first message
                .setQ("in:inbox") // Query for messages specifically in the inbox folder
                .execute() // Execute the API call (this is a network call, hence 'suspend')

            val messages = response.messages
            if (messages.isNullOrEmpty()) {
                return "Inbox is empty."
            }

            // Get the full details of the first message to extract its subject
            val firstMessageId = messages[0].id
            val message = service.users().messages().get(userIdForGmail, firstMessageId)
                .setFormat("full") // Request full message details, including headers
                .execute() // Execute the API call

            // Extract the subject from the message's payload headers
            val subjectHeader = message.payload?.headers?.find { it.name == "Subject" }
            return subjectHeader?.value ?: "No subject found for the first email."

        } catch (e: Exception) {
            // Log the detailed error
            println(tag + "Error fetching inbox title: ${e.message}" + e)
            // Return a generic error message to the UI
            return "Error fetching inbox title: ${e.message}"
        }
    }
}