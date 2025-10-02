package com.app.echomi.data

import com.google.gson.annotations.SerializedName

data class ApprovalStatus(
    @SerializedName("success") val success: Boolean,
    @SerializedName("message") val message: String? = null,
    @SerializedName("action") val action: String? = null
)