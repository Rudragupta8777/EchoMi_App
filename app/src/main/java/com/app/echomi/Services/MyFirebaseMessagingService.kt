package com.app.echomi.Services

import android.Manifest
import com.app.echomi.R
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.Ringtone
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.app.echomi.Network.RetrofitInstance
import com.app.echomi.SplashScreen
import com.app.echomi.data.OtpResponse
import com.echomi.app.network.FcmTokenRequest
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import retrofit2.Response
import kotlin.apply
import kotlin.jvm.java
import kotlin.let
import kotlin.run
import kotlin.text.isEmpty

class MyFirebaseMessagingService : FirebaseMessagingService() {

    companion object {
        private const val TAG = "MyFirebaseService"
        private const val EMERGENCY_CHANNEL_ID = "emergency_channel"
        private const val DELIVERY_CHANNEL_ID = "delivery_channel"
        private const val EMERGENCY_NOTIFICATION_ID = 1001
        private const val DELIVERY_NOTIFICATION_ID = 1002

        private var emergencyRingtone: Ringtone? = null

        fun stopEmergencyAlarm() {
            emergencyRingtone?.stop()
            emergencyRingtone = null
            Log.d(TAG, "⏹️ Emergency alarm stopped by user")
        }
    }

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        Log.d(TAG, "📩 FCM Data Payload: ${remoteMessage.data}")

        val type = remoteMessage.data["type"] ?: ""
        when (type) {
            "emergency_alert" -> {
                Log.d(TAG, "🚨 Emergency notification received!")
                increaseVolume()
                sendEmergencyNotification(remoteMessage)
                triggerEmergencyAlarm()
            }
            "delivery_otp" -> {
                Log.d(TAG, "📦 Delivery OTP notification received!")
                handleDeliveryOtpNotification(remoteMessage)
            }
            else -> {
                Log.w(TAG, "Invalid or missing 'type' in FCM payload: $type")
            }
        }
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val emergencyChannel = NotificationChannel(
                EMERGENCY_CHANNEL_ID,
                "Emergency Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Emergency call notifications"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 1000, 500, 1000)
                setSound(
                    Uri.parse("android.resource://${packageName}/raw/buzzer"),
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .build()
                )
            }

            val deliveryChannel = NotificationChannel(
                DELIVERY_CHANNEL_ID,
                "Delivery OTPs",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notifications for delivery OTPs"
                enableVibration(true)
            }

            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(emergencyChannel)
            notificationManager.createNotificationChannel(deliveryChannel)
            Log.d(TAG, "Notification channels created")
        }
    }

    private fun increaseVolume() {
        val audioManager = getSystemService(AUDIO_SERVICE) as AudioManager
        if (audioManager.ringerMode != AudioManager.RINGER_MODE_NORMAL) {
            audioManager.ringerMode = AudioManager.RINGER_MODE_NORMAL
            Log.d(TAG, "🔊 Ringer mode changed to NORMAL")
        }

        val maxRing = audioManager.getStreamMaxVolume(AudioManager.STREAM_RING)
        val maxAlarm = audioManager.getStreamMaxVolume(AudioManager.STREAM_ALARM)
        audioManager.setStreamVolume(AudioManager.STREAM_RING, maxRing, 0)
        audioManager.setStreamVolume(AudioManager.STREAM_ALARM, maxAlarm, 0)
        Log.d(TAG, "🔊 Ring and Alarm volumes set to maximum")
    }

    private fun sendEmergencyNotification(remoteMessage: RemoteMessage) {
        val title = remoteMessage.data["title"] ?: "🚨 Emergency Alert"
        val body = remoteMessage.data["body"] ?: "Urgent situation detected!"
        val callerNumber = remoteMessage.data["callerNumber"] ?: "Unknown Caller"

        val intent = Intent(this, SplashScreen::class.java).apply {
            setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            putExtra("STOP_ALARM", true)
        }

        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val soundUri = Uri.parse("android.resource://${packageName}/raw/buzzer")

        val notificationBuilder = NotificationCompat.Builder(this, EMERGENCY_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_emergency ?: R.drawable.alert)
            .setContentTitle("$title - Call Back Needed")
            .setContentText("Caller: $callerNumber\n$body")
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setSound(soundUri)
            .setContentIntent(pendingIntent)
            .setFullScreenIntent(pendingIntent, true)

        try {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
            ) {
                with(NotificationManagerCompat.from(this)) {
                    notify(EMERGENCY_NOTIFICATION_ID, notificationBuilder.build())
                }
                Log.d(TAG, "Emergency notification displayed")
            } else {
                Log.w(TAG, "Notification permission not granted")
            }
        } catch (se: SecurityException) {
            Log.e(TAG, "❌ SecurityException while showing notification: ${se.message}")
        }
    }

    private fun handleDeliveryOtpNotification(remoteMessage: RemoteMessage) {
        val sender = remoteMessage.data["sender"]
        val orderId = remoteMessage.data["orderId"]

        fetchLatestOtp(sender, orderId) { otpResponse ->
            if (otpResponse != null && otpResponse.otp != null) {
                sendDeliveryOtpNotification(otpResponse.otp, otpResponse.sender, otpResponse.orderId)
            } else {
                Log.w(TAG, "No OTP found for sender: $sender, orderId: $orderId")
                sendDeliveryOtpNotification(null, sender, orderId)
            }
        }
    }

    private fun sendDeliveryOtpNotification(otp: String?, sender: String?, orderId: String?) {
        val title = "📦 Delivery OTP"
        val body = when {
            otp != null -> "OTP for ${sender ?: "Delivery"}${orderId?.let { " (Order $it)" } ?: ""}: $otp"
            else -> "No OTP found for ${sender ?: "Delivery"}${orderId?.let { " (Order $it)" } ?: ""}"
        }

        val intent = Intent(this, SplashScreen::class.java).apply {
            setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            putExtra("OTP_NOTIFICATION", true)
            putExtra("otp", otp)
            putExtra("sender", sender)
            putExtra("orderId", orderId)
        }

        val pendingIntent = PendingIntent.getActivity(
            this, DELIVERY_NOTIFICATION_ID, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notificationBuilder = NotificationCompat.Builder(this, DELIVERY_CHANNEL_ID)
            .setSmallIcon(R.drawable.delivery ?: R.drawable.info)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        try {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
            ) {
                with(NotificationManagerCompat.from(this)) {
                    notify(DELIVERY_NOTIFICATION_ID, notificationBuilder.build())
                }
                Log.d(TAG, "Delivery OTP notification displayed: $body")
            } else {
                Log.w(TAG, "Notification permission not granted")
            }
        } catch (se: SecurityException) {
            Log.e(TAG, "❌ SecurityException while showing OTP notification: ${se.message}")
            // Optional: Log to analytics or notify backend
        }
    }

    private fun fetchLatestOtp(sender: String? = null, orderId: String? = null, callback: (OtpResponse?) -> Unit) {
        serviceScope.launch {
            repeat(3) { attempt ->
                try {
                    val firebaseUid = FirebaseAuth.getInstance().currentUser?.uid ?: run {
                        Log.e(TAG, "User not authenticated - cannot fetch OTP")
                        callback(null)
                        return@launch
                    }
                    val response: Response<OtpResponse> =
                        RetrofitInstance.api.getLatestOtp(firebaseUid, sender, orderId)

                    if (response.isSuccessful) {
                        val otpData = response.body()
                        Log.d(TAG, "Fetched OTP: ${otpData?.otp}")
                        callback(otpData)
                        return@launch
                    } else {
                        Log.e(
                            TAG,
                            "Failed to fetch OTP (attempt ${attempt + 1}): ${response.code()} - ${
                                response.errorBody()?.string()
                            }"
                        )
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error fetching OTP (attempt ${attempt + 1})", e)
                }
                if (attempt < 2) delay(2000) // Wait 2s before retry
            }
            Log.e(TAG, "Failed to fetch OTP after 3 attempts")
            callback(null)
        }
    }

    private fun triggerEmergencyAlarm() {
        try {
            val pm = getSystemService(POWER_SERVICE) as PowerManager
            val wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "echomi:EmergencyWakeLock")
            wl.acquire(60 * 1000L) // 1 minute

            val alarmUri = Uri.parse("android.resource://${packageName}/raw/buzzer")
            emergencyRingtone = RingtoneManager.getRingtone(applicationContext, alarmUri).apply {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    isLooping = true // Loop the ringtone
                } else {
                    // For older versions, we can use a Handler to replay (already partially handled by default)
                }
                play()
            }

            Handler(Looper.getMainLooper()).postDelayed({
                if (wl.isHeld) {
                    wl.release()
                    stopEmergencyAlarm()
                    Log.d(TAG, "Wake lock released and alarm stopped automatically")
                }
            }, 2 * 60 * 1000L)

            Log.d(TAG, "Emergency alarm triggered")
        } catch (e: Exception) {
            Log.e(TAG, "Error triggering emergency alarm", e)
        }
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        serviceScope.launch {
            try {
                val idToken = getGoogleIdToken()
                if (idToken.isEmpty()) {
                    Log.e(TAG, "Cannot send FCM token: No Google ID token found")
                    return@launch
                }
                val authHeader = "Bearer $idToken"
                val request = FcmTokenRequest(fcmToken = token)
                val response = RetrofitInstance.api.updateFcmToken(request, authHeader)
                Log.d(TAG, "FCM token sent: ${response.isSuccessful}")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to send token", e)
            }
        }
    }

    private suspend fun getGoogleIdToken(): String {
        return try {
            val auth = FirebaseAuth.getInstance()
            val user = auth.currentUser
            user?.getIdToken(false)?.await()?.token ?: ""
        } catch (e: Exception) {
            Log.e(TAG, "Error getting Google ID token", e)
            ""
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel() // Clean up coroutines
        Log.d(TAG, "Service destroyed, coroutines cancelled")
    }
}