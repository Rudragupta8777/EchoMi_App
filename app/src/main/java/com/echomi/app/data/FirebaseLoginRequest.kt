package com.echomi.app.data

data class FirebaseLoginRequest(
    val email: String,
    val name: String,
    val firebaseUid: String
)