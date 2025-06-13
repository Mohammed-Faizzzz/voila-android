# Voila: Subscription Management App

## Product Overview

Voila is a mobile application designed to help users manage their subscription-based services more effectively. It aims to solve common problems like forgetting about free trials, unexpected renewals, and lacking a centralized view of all subscriptions.

### Core Problem

People often:
* Forget about subscriptions after a free trial.
* Lose money to renewals they didn’t anticipate.
* Have no centralized view of all their subscriptions.

### Solution

Voila automatically scrapes your Gmail inbox for subscription-related emails (e.g., welcome emails, renewal reminders, receipts), and helps you:
* View all subscriptions in one clean dashboard.
* Get push reminders before your renewal dates.
* See key details like amount, currency, billing frequency, and cancellation links.

### Status

This project was built over a few days in a hackathon-style manner. The current focus is on polishing the Android version for initial release, with plans for an iOS version based on strong early user interest.

---

## Technical Architecture & Core Concepts (Android)

Voila is a modern Android application built primarily with **Kotlin** and **Jetpack Compose**, leveraging **Firebase** for backend services.

### 1. Project Structure

* **`MainActivity.kt`**: The entry point of the application, handling user sign-in and initial navigation.
* **`DashboardActivity.kt`**: Manages the main subscription dashboard, displays subscriptions, allows adding new ones, and handles notifications.
* **`Subscription.kt`**: Defines the data model for a subscription and includes utility functions for interacting with Firestore.
* **`GoogleAuthClient.kt` (Implicit):** A separate class that encapsulates Google Sign-In logic.
* **`MyFirebaseMessagingService.kt`**: Handles incoming Firebase Cloud Messages (FCM) to display push notifications.
* **`AndroidManifest.xml`**: Declares app components, permissions, and features.
* **`build.gradle` (Module: app)**: Manages dependencies and build configurations.

### 2. Android Core Concepts in Voila

#### a. Activity: The Screen Manager

* **Concept:** An `Activity` represents a single screen with a user interface. It's the entry point for a specific part of your user's journey within the app.
* **Voila Implementation:**
    * `MainActivity`: This Activity is responsible for the initial "Voila" splash screen and the "Sign in with Google" button. It decides whether to show the sign-in UI or immediately navigate to the dashboard based on the user's logged-in state.
    * `DashboardActivity`: Once signed in, this Activity displays the user's list of subscriptions, allows adding new ones, and manages renewal notifications. It's the core interaction screen.

#### b. Composable: The UI Element

* **Concept:** In Jetpack Compose (Android's modern UI toolkit), a `Composable` is a function that describes a piece of UI. We define *what* our UI should look like, and Compose efficiently renders and updates it when the underlying data changes.
* **Voila Implementation:**
    * `SignInScreen(@Composable)`: This function defines the layout and content for the sign-in screen, including the app title, tagline, and Google Sign-In button.
    * `DashboardScreen(@Composable)`: This is the main UI for displaying subscriptions. It uses other Composables like `SubscriptionCard`.
    * `SubscriptionCard(@Composable)`: A reusable UI component that displays the details of a single subscription (name, amount, renewal date, frequency) and includes a "Cancel" button.
    * `AddSubscriptionDialog(@Composable)`: A composable that renders an `AlertDialog` for manually adding new subscription details.
    * `LoadingScreen(@Composable)`: A simple composable to show a loading spinner.

#### c. Manifest File (`AndroidManifest.xml`): The App's Passport

* **Concept:** This XML file is our app's central configuration. It declares all our app's components (Activities, Services, Broadcast Receivers), the permissions it needs, and other vital information for the Android system.
* **Voila Relevance (Implicit in code, but crucial):**
    * We'll declare `MainActivity` and `DashboardActivity` in this file, marking `MainActivity` as our app's main launcher.
    * We'll declare permissions like `<uses-permission android:name="android.permission.INTERNET"/>` for network access, and for Android 13+ devices, `<uses-permission android:name="android.permission.POST_NOTIFICATIONS"/>` for push notifications (as seen in `DashboardActivity`).
    * Our `MyFirebaseMessagingService` will also be declared here to ensure it can receive FCM messages.

#### d. Permissions: The Bouncer at the Door

* **Concept:** Android applications must explicitly ask the user for permission to access sensitive data or system features. These are declared in the manifest and sometimes requested at runtime.
* **Voila Implementation:**
    * `android.Manifest.permission.POST_NOTIFICATIONS`: In `DashboardActivity.onCreate`, we request this permission on Android 13 (TIRAMISU) and above, which is necessary for showing push notifications.
    * Gmail Read-Only Authorization (`GmailScopes.GMAIL_READONLY`): In `MainActivity`, after Google Sign-In, you initiate an `AuthorizationRequest` to get user consent for accessing their Gmail data. This is crucial for our email scraping feature.

#### e. Lifecycle: The App Component's Life Story

* **Concept:** Android components go through various states (created, started, resumed, paused, stopped, destroyed). We manage actions in these states to ensure proper resource handling.
* **Voila Implementation:**
    * `MainActivity.onCreate()`: This is where `MainActivity` is first created. We initialize `GoogleAuthClient` and determine whether to show the sign-in screen or immediately navigate.
    * `DashboardActivity.onCreate()`: Similar to `MainActivity`, this is where `DashboardActivity` is created. We also handle the `POST_NOTIFICATIONS` permission request here.
    * `finish()` calls: In both `MainActivity` and `DashboardActivity` (after sign-out), `finish()` is called to remove the current activity from the back stack, preventing users from going back to it.

#### f. Coroutine: The Smart Task Runner

* **Concept:** Coroutines are lightweight "tasks" that can pause and resume execution without blocking the main (UI) thread. This keeps our app responsive during long operations (like network calls).
* **Voila Implementation:**
    * `lifecycleScope.launch { ... }`: Used in `MainActivity` when signing in with Google. `googleAuthClient.signIn()` likely performs network operations, and running it in a coroutine prevents the UI from freezing.
    * `LaunchedEffect(Unit) { ... }`: Used in `DashboardActivity` to trigger `getSubscriptions` and `showRenewalNotificationIfDueSoon` when the `DashboardScreen` composable first enters the composition. This ensures data fetching happens asynchronously without blocking the UI.
    * `delay(20)` in `LoadingScreen`: A simple use of coroutine `delay` for a very short pause.

#### g. CoroutineScope: The Task Manager

* **Concept:** A `CoroutineScope` defines the lifetime and context for a group of coroutines. When the scope is cancelled, all coroutines within it are also cancelled, preventing leaks.
* **Voila Implementation:**
    * `lifecycleScope`: This is the primary scope used in our Activities. It's automatically tied to the `Activity`'s lifecycle. Any coroutine launched using `lifecycleScope.launch` will be cancelled if the `Activity` is destroyed. This is crucial for safely managing asynchronous tasks related to our UI.

#### h. Firebase Integration: Your Backend as a Service

* **Concept:** Firebase provides backend services for mobile apps, simplifying common tasks like authentication, databases, and messaging.
* **Voila Implementation:**
    * **Firebase Authentication (`FirebaseAuth`):** Used in `GoogleAuthClient` to manage user sign-in with Google. In `DashboardActivity`, `FirebaseAuth.getInstance().currentUser?.uid` retrieves the current user's ID for data access.
    * **Firestore (`Firebase.firestore`):** Our NoSQL cloud database.
        * `getSubscriptions`: Fetches all subscriptions for a specific user from Firestore.
        * `addSubscription`: Adds a new subscription document to Firestore.
    * **Firebase Cloud Messaging (FCM):** Used for push notifications.
        * `MyFirebaseMessagingService`: This `Service` receives push notifications sent from Firebase and displays them to the user.
        * `showRenewalNotificationIfDueSoon`: While this *sends* a local notification (triggered by app logic), FCM is the general infrastructure for sending *remote* notifications. `MyFirebaseMessagingService` specifically handles *remote* notifications.

---

## How Voila Works (Code Flow)

### 1. Initial Launch (`MainActivity.kt`)

* When the app starts, `MainActivity` checks if the user is already signed in using `googleAuthClient.isSignedIn()`.
* **If signed in:** It immediately navigates to `DashboardActivity` and finishes itself.
* **If not signed in:** It displays the `SignInScreen` Composable.
    * When the "Sign in with Google" button is tapped, `lifecycleScope.launch` initiates the `googleAuthClient.signIn()` process.
    * Upon successful Google Sign-In, it immediately triggers `requestGmailAuthorization` (which prompts the user for Gmail access).
    * *Note:* The current code navigates to `DashboardActivity` *before* the Gmail authorization process completes. This is a point for future refinement to ensure authorization is complete before moving to the dashboard.

### 2. Dashboard (`DashboardActivity.kt`)

* `DashboardActivity` retrieves the current user's ID from Firebase Auth.
* It displays a `LoadingScreen` initially.
* Once loading is complete, `DashboardScreen` Composable is shown.
* `LaunchedEffect` in `DashboardScreen` calls `getSubscriptions` to fetch the user's subscriptions from Firestore.
* Fetched subscriptions are displayed using `SubscriptionCard` Composables.
* `showRenewalNotificationIfDueSoon` is called to check for and display local renewal notifications.
* **Add Subscription:** Tapping "Add Subscription" opens an `AddSubscriptionDialog`.
    * Users can input details (service name, amount, date, frequency, cancellation URL).
    * Upon "Add," `addSubscription` saves the new data to Firestore, and the dashboard updates.
* **Sign Out:** Tapping "Sign Out" uses `googleAuthClient.signOut()`, then navigates back to `MainActivity`.
* **Cancel Subscription:** Tapping "Cancel" on a `SubscriptionCard` currently opens a hardcoded Netflix cancellation URL. This needs to be dynamic, using `sub.cancellationLink`.

### 3. Notifications (`MyFirebaseMessagingService.kt` and `showRenewalNotificationIfDueSoon`)

* **Local Notifications:** The `showRenewalNotificationIfDueSoon` function (in `DashboardActivity`) checks subscriptions due tomorrow and generates a local notification using `NotificationCompat.Builder` and `NotificationManager`. This notification is triggered by the app itself.
* **Remote Notifications (FCM):** `MyFirebaseMessagingService` extends `FirebaseMessagingService`.
    * When a push notification is sent from the Firebase Console (or your server) to the app, `onMessageReceived` is triggered.
    * It extracts the title and body from the remote message and uses `NotificationCompat.Builder` and `NotificationManager` to display it to the user.
    * `onNewToken` logs the FCM device token, which is important if you were to send targeted notifications from your own backend.

### 4. Data Management (`Subscription.kt`)

* The `Subscription` data class defines the structure of a subscription object.
* `getSubscriptions`: Asynchronously fetches all subscription documents for a given `userId` from the `users/{userId}/subscriptions` collection in Firestore.
* `addSubscription`: Asynchronously adds a new `Subscription` object to the user's subscriptions collection in Firestore.

---

## Future Enhancements & TODOs

* **Refine Gmail Authorization Flow:** Ensure navigation to `DashboardActivity` only occurs *after* Gmail authorization is completed or correctly handled.
* **Dynamic Cancellation Link:** Update `SubscriptionCard` to use `sub.cancellationLink` for the "Cancel" button, not a hardcoded Netflix URL.
* **"Automatic Cancellation/Renewal with the click of a button":** Clarify its exact implementation. For an MVP, it should streamline *linking* to cancellation, not performing it.
* **Email Scraping (Core Feature):** Implement the actual parsing logic to extract subscription details from Gmail. The current code structure fetches data but the parsing isn't explicitly shown.
* **Mandatory Fields in Add Subscription:** Implement robust validation to ensure all required fields are filled before adding a subscription.
