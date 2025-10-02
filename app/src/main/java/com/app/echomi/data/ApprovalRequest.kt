package com.app.echomi.data

import com.google.gson.annotations.SerializedName

data class ApprovalRequest(
    @SerializedName("approvalId") val approvalId: String,
    @SerializedName("company") val company: String,
    @SerializedName("callerNumber") val callerNumber: String,
    @SerializedName("callSid") val callSid: String,
    @SerializedName("timestamp") val timestamp: Long
)


