package com.echomi.app.data

data class CallLog(
    val _id: String,
    val callerNumber: String,
    val summary: String?,
    val startTime: String,
    val duration: Int,
    val recordingUrl: String?,
    val transcript: List<TranscriptEntry>?
)
