# Voila: Subscription Management (Android)

## Project Overview

Voila is a mobile application designed to empower users to effectively manage their subscription-based services. In today's digital age, it's easy to lose track of numerous subscriptions, leading to forgotten free trials, unexpected renewals, and financial leakage. Voila provides a centralized, intelligent solution to these common challenges.

### The Problem Voila Solves

People often:

  * Forget about subscriptions after a free trial.
  * Lose money to renewals they didn’t anticipate.
  * Lack a centralized view of all their subscriptions.

### Our Solution

Voila tackles these issues by automatically scanning your Gmail inbox for subscription-related emails (welcome emails, renewal reminders, receipts). It then helps you:

  * **View All Subscriptions:** A clean, intuitive dashboard provides a consolidated view of all your services.
  * **Timely Reminders:** Get push notifications before renewal dates, giving you ample time to decide whether to continue or cancel.
  * **Key Details at a Glance:** See essential information like service name, amount, currency, billing frequency, and direct cancellation links.

### Project Status

This project was developed rapidly in a hackathon-style sprint. The Android version is currently in its polishing phase, nearing completion for initial public release. There are strong indications of demand for an iOS version, which is planned for future development. The app maintains a lightweight architecture with minimal server or AI costs, leveraging Firebase for its backend.

-----

## Technical Architecture

Voila is a modern Android application built using **Kotlin** and **Jetpack Compose**, integrated seamlessly with **Firebase** for backend services and Google APIs for user data.

### Core Technologies

The application's foundation rests on:

  * **Kotlin:** As the primary programming language, Kotlin offers conciseness, safety, and excellent interoperability with the Android ecosystem.
  * **Jetpack Compose:** This is Android's modern toolkit for building native UI. Its declarative approach simplifies UI development and ensures a responsive user experience.
  * **Firebase:** Utilized for robust backend services, specifically:
      * **Firebase Authentication:** Handles secure user sign-in via Google accounts.
      * **Cloud Firestore:** Serves as a flexible, scalable NoSQL cloud database for storing user subscription data.
      * **Firebase Cloud Messaging (FCM):** Facilitates the delivery of push notifications to users.
  * **Google Identity Services API:** Streamlines the complex processes of Google Sign-In and Gmail authorization.
  * **Gmail API:** Allows Voila to programmatically read user emails (with explicit user permission) to identify and extract subscription details.

### Key Application Components and Their Roles

Voila's functionality is organized across several key Kotlin files, each playing a distinct role:

  * **`MainActivity.kt`**: This is the application's initial entry point. It manages the Google Sign-In flow, determines if a user is already authenticated, and routes them accordingly. If a sign-in is required, it displays the dedicated `SignInScreen` and initiates the crucial Gmail read-only authorization after successful authentication.
  * **`DashboardActivity.kt`**: Serving as the primary user interface, this activity displays the user's managed subscriptions. It includes logic for requesting necessary notification permissions (Android 13+), fetching subscription data from Firestore, and triggering local renewal reminders. Users can also manually add new subscriptions through a dialog presented by this activity.
  * **`Subscription.kt`**: This file defines the `Subscription` data class, which models the attributes of each subscription (e.g., service name, amount, renewal date). It also contains helper functions (`getSubscriptions`, `addSubscription`) for interacting with the Cloud Firestore database to retrieve and store subscription data.
  * **`MyFirebaseMessagingService.kt`**: This Android Service is essential for handling remote push notifications sent via Firebase Cloud Messaging. It listens for incoming messages and is responsible for displaying these notifications to the user, ensuring timely reminders.
  * **`GoogleAuthClient.kt` (Inferred):** Although not explicitly provided in the snippets, the functionality implies a dedicated `GoogleAuthClient` class. This class likely encapsulates the detailed logic for Google Sign-In, managing the authentication flow and user session.

### Android Concepts in Practice Within Voila

Voila's codebase demonstrates several core Android development concepts:

  * **Activities:** `MainActivity` and `DashboardActivity` exemplify how different screens and distinct user flows are managed within an Android application.
  * **Jetpack Compose:** The entire user interface, including `SignInScreen`, `DashboardScreen`, `SubscriptionCard`, `AddSubscriptionDialog`, and `LoadingScreen`, is built declaratively using Composables. This approach simplifies UI construction and ensures a highly responsive and modern user experience.
  * **Permissions:** The app correctly requests runtime permissions, such as `POST_NOTIFICATIONS` for Android 13+ devices, and manages the explicit Gmail API authorization process, highlighting Android's focus on user privacy and control.
  * **Lifecycles:** The use of `onCreate` methods in Activities for initialization, and crucially, `lifecycleScope` for coroutine management, demonstrates awareness of Android component lifecycles. This ensures that asynchronous tasks are automatically cancelled when an Activity is destroyed, preventing memory leaks and crashes.
  * **Coroutines:** Asynchronous operations, such as Google Sign-In, data fetching from Firestore, and background email scanning (a planned feature), are handled efficiently using Kotlin Coroutines. This prevents the UI from freezing and keeps the application responsive.
  * **Firebase Integration:** The code showcases practical application of Firebase services, from authenticating users via `FirebaseAuth` and organizing user-specific data in `Cloud Firestore` (using `users/{userId}/subscriptions` collections) to delivering notifications with `Firebase Cloud Messaging`.

-----

## Features

### Implemented Functionality:

  * **Google Sign-In:** Seamless user authentication using their existing Google account.
  * **Gmail Authorization:** Securely obtains user consent for read-only access to their Gmail inbox, a prerequisite for the auto-parsing feature.
  * **Subscription Data Storage:** Captured (currently via manual entry) and parsed subscription information is securely stored and managed in Cloud Firestore.
  * **Intuitive Dashboard:** A clear and well-organized UI displays all managed subscriptions at a glance.
  * **Manual Subscription Entry:** Users can easily add new subscriptions with detailed fields including service name, currency, amount, renewal date, renewal frequency, and a dedicated cancellation link.
  * **Subscription Management (CRUD):** Core functionality for adding, viewing, and interacting with subscriptions is established.
  * **Timely Push Notifications:** Users receive automated push notifications via FCM, reminding them 24 hours before a subscription's renewal date, empowering informed decisions.
  * **Streamlined Cancellation:** A dedicated "Cancel" button on each subscription card (currently using a placeholder, but designed for dynamic links) simplifies the process by directing users to the service's cancellation page.
  * **Resource Efficiency:** The app is designed with a lightweight architecture to minimize server-side costs.

-----

## Getting Started (for Developers)

To set up and run Voila on your local development environment:

1.  **Clone the Repository:**
    ```bash
    git clone [your-repo-url-here]
    cd voila-android
    ```
2.  **Open in Android Studio:** Launch Android Studio and open the `voila-android` project.
3.  **Configure Firebase Project:**
      * Navigate to the [Firebase Console](https://console.firebase.google.com/).
      * Create a new Firebase project (or select an existing one).
      * **Add an Android App** to your Firebase project, following the console's instructions.
      * **Download `google-services.json`** and place it into your Android Studio project's `app/` directory.
      * Within the Firebase Console:
          * **Enable Firestore:** Go to "Build" \> "Firestore Database" and create a new database (test mode is fine for development).
          * **Enable Authentication:** Go to "Build" \> "Authentication" and enable "Google" as a sign-in provider.
          * **Enable Cloud Messaging:** Go to "Engage" \> "Cloud Messaging".
4.  **Configure Google Cloud Project (for Gmail API Access):**
      * Visit the [Google Cloud Console](https://console.cloud.google.com/) and ensure you're in the project linked to your Firebase setup.
      * **Enable Gmail API:** Go to "APIs & Services" \> "Enabled APIs & Services" and confirm "Gmail API" is enabled.
      * **Configure OAuth Consent Screen:** Set "User type" to "External" and add the `https://www.googleapis.com/auth/gmail.readonly` scope.
      * **Create Android OAuth 2.0 Client ID:** Under "APIs & Services" \> "Credentials", create an OAuth client ID for "Android". Provide your app's `Package name` (e.g., `com.mohdfaizzzz.voila`) and your SHA-1 certificate fingerprint. (You can obtain the SHA-1 fingerprint via Android Studio's Gradle tab: `app` \> `Tasks` \> `android` \> `signingReport`).
5.  **Sync Gradle:** After placing `google-services.json` and configuring the Google Cloud Project, sync your Gradle files in Android Studio (File \> Sync Project with Gradle Files).
6.  **Run the App:** Connect an Android physical device or launch an emulator. Click the "Run" button in Android Studio to deploy and test the application.

## Contribution

Voila is currently a personal project, but contributions are welcome. Feel free to open issues for bug reports or feature requests, or submit pull requests.
