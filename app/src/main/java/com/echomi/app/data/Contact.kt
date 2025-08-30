package com.echomi.app.data

data class Contact(
    val id: String,
    val name: String,
    val phoneNumber: String,
    var role: String = "default" // 'default' or 'family'
)
