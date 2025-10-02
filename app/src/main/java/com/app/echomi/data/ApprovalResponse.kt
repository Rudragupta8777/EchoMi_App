package com.app.echomi.data

import com.google.gson.annotations.SerializedName

data class ApprovalResponse(
    @SerializedName("approvalId") val approvalId: String,
    @SerializedName("approved") val approved: Boolean,
    @SerializedName("userId") val userId: String? = null
)
