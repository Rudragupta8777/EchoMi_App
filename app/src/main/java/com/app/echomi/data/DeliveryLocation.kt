package com.app.echomi.data

data class DeliveryLocation(
    val latitude: Double,
    val longitude: Double,
    val address: String? = null
)