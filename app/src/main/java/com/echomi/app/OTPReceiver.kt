package com.echomi.app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

class OTPReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent?.action == Telephony.Sms.Intents.SMS_RECEIVED_ACTION) {
            val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
            for (sms in messages) {
                val sender = sms.displayOriginatingAddress
                val message = sms.messageBody
                Log.d("OTPReceiver", "SMS from: $sender - $message")

                // Only process if it’s a known delivery sender
                if (sender.contains("AMAZON") || sender.contains("SWIGGY")) {
                    val otpRegex = "\\b\\d{4,6}\\b".toRegex()
                    val otpMatch = otpRegex.find(message)
                    otpMatch?.let {
                        val otp = it.value
                        Log.d("OTPReceiver", "Found OTP: $otp")

                        // Send OTP to backend
                        CoroutineScope(Dispatchers.IO).launch {
                            sendOtpToBackend(otp, sender)
                        }
                    }
                }
            }
        }
    }

    private fun sendOtpToBackend(otp: String, sender: String) {
        try {
            val client = OkHttpClient()
            val json = JSONObject()
            json.put("otp", otp)
            json.put("sender", sender)

            val body = json.toString().toRequestBody("application/json".toMediaTypeOrNull())
            val request = Request.Builder()
                .url("http://your-backend-url.com/api/delivery/otp") // Replace with your backend endpoint
                .post(body)
                .build()

            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                Log.d("OTPReceiver", "OTP sent successfully")
            } else {
                Log.e("OTPReceiver", "Failed to send OTP: ${response.message}")
            }
        } catch (e: Exception) {
            Log.e("OTPReceiver", "Exception sending OTP: ${e.message}")
        }
    }
}
