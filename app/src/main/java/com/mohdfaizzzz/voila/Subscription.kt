package com.mohdfaizzzz.voila

import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

data class Subscription(
    val id: String = "",
    val serviceName: String = "",
    val currency: String = "",
    val amount: Double = 0.0,
    val renewalDate: com.google.firebase.Timestamp = com.google.firebase.Timestamp.now(),
    val renewalFreq: String = ""
)

fun getSubscriptions(userId: String, onResult: (List<Subscription>) -> Unit) {
    val db = Firebase.firestore
    db.collection("users")
        .document(userId)
        .collection("subscriptions")
        .get()
        .addOnSuccessListener { result ->
            val subs = result.map { it.toObject(Subscription::class.java).copy(id = it.id) }
            onResult(subs)
        }
}

fun addSubscription(userId: String, subscription: Subscription, onComplete: () -> Unit) {
    val db = Firebase.firestore
    db.collection("users")
        .document(userId)
        .collection("subscriptions")
        .add(subscription)
        .addOnSuccessListener { onComplete() }
}

