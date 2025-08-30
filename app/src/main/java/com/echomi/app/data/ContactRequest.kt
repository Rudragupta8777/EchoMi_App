package com.echomi.app.data

// This class models the JSON object we will send to the backend.
data class ContactRequest(
    val contacts: List<CategorizedContact>
)
