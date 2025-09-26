package com.app.echomi.Services

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.util.Log
import com.app.echomi.Network.RetrofitInstance
import com.app.echomi.data.OtpRequest
import com.app.echomi.data.OtpResponse
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import retrofit2.Response
import java.util.regex.Pattern
import kotlin.collections.any
import kotlin.collections.isNullOrEmpty
import kotlin.text.contains
import kotlin.text.isEmpty
import kotlin.text.uppercase
import kotlin.to

class SmsReceiver : BroadcastReceiver() {
    companion object {
        private const val TAG = "SmsReceiver"
        private val OTP_PATTERN = Pattern.compile("\\b\\d{4,6}\\b") // 4-6 digit OTP
        private val KNOWN_SENDERS = mapOf(
            "AMAZON" to listOf("AMAZON", "AMZN", "Amazon"),
            "SWIGGY" to listOf("SWIGGY", "Swiggy"),
            // Add more senders as needed, e.g., "ZOMATO" to listOf("ZOMATO", "Zomato")
        )
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Telephony.Sms.Intents.SMS_RECEIVED_ACTION) {
            val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
            if (messages.isNullOrEmpty()) return

            // Combine multi-part SMS
            val smsBody = StringBuilder()
            var originatingAddress = ""
            for (message in messages) {
                smsBody.append(message.messageBody)
                if (originatingAddress.isEmpty()) {
                    originatingAddress = message.originatingAddress ?: ""
                }
            }

            val fullBody = smsBody.toString()
            Log.d(TAG, "Received SMS from $originatingAddress: $fullBody")

            // Extract OTP
            val matcher = OTP_PATTERN.matcher(fullBody)
            if (!matcher.find()) {
                Log.d(TAG, "No OTP found in message")
                return
            }
            val otp = matcher.group()

            // Determine sender
            val sender = determineSender(originatingAddress, fullBody)
            if (sender == null) {
                Log.d(TAG, "Unknown sender - skipping")
                return
            }

            // Extract orderId if present (optional - customize regex as needed)
            val orderId = extractOrderId(fullBody) // Implement if needed, else null

            // Get Firebase UID
            val auth = FirebaseAuth.getInstance()
            val firebaseUid = auth.currentUser?.uid
            if (firebaseUid == null) {
                Log.e(TAG, "User not authenticated - cannot send OTP")
                return
            }

            // Send to backend
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val request = OtpRequest(
                        firebaseUid = firebaseUid,
                        sender = sender,
                        otp = otp,
                        orderId = orderId
                    )
                    val response: Response<OtpResponse> = RetrofitInstance.api.sendOtp(request)

                    if (response.isSuccessful) {
                        Log.d(TAG, "✅ OTP sent to backend successfully: ${response.body()}")
                    } else {
                        Log.e(TAG, "❌ Failed to send OTP: ${response.code()} - ${response.errorBody()?.string()}")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error sending OTP to backend", e)
                }
            }
        }
    }

    private fun determineSender(address: String, body: String): String? {
        val upperAddress = address.uppercase()
        val upperBody = body.uppercase()

        for ((sender, keywords) in KNOWN_SENDERS) {
            if (keywords.any { upperAddress.contains(it.uppercase()) } ||
                keywords.any { upperBody.contains(it.uppercase()) }) {
                return sender
            }
        }
        return null
    }

    private fun extractOrderId(body: String): String? {
        // Optional: Implement regex to extract order ID from body
        // Example: Pattern.compile("Order ID: (\\w+)")
        // For now, return null if not needed
        return null
    }
}