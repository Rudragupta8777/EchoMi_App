package com.echomi.app.data

data class TranscriptMessage(
    val speaker: String, // "caller" or "ai"
    val text: String,
    val timestamp: String
)