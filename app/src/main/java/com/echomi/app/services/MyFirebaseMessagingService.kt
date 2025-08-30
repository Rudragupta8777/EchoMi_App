package com.echomi.app.services

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.media.RingtoneManager
import android.os.Build
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.echomi.app.MainActivity
import com.echomi.app.R
import android.app.ActivityManager
import android.content.pm.PackageManager
import com.echomi.app.network.FcmTokenRequest
import com.echomi.app.network.RetrofitInstance
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import android.os.Handler
import android.os.Looper
import androidx.core.content.ContextCompat
import kotlinx.coroutines.tasks.await

class MyFirebaseMessagingService : FirebaseMessagingService() {

    companion object {
        private const val TAG = "MyFirebaseMsgService"
        private const val CHANNEL_ID = "emergency_channel"
        private const val NOTIFICATION_ID = 1001
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        Log.d(TAG, "From: ${remoteMessage.from}")
        Log.d(TAG, "Message data: ${remoteMessage.data}")
        Log.d(TAG, "Notification: ${remoteMessage.notification}")

        // Validate data payload
        if (remoteMessage.data.getOrElse("type") { "" } == "emergency") {
            Log.d(TAG, "🚨 Emergency notification received!")
            sendEmergencyNotification(remoteMessage)
            triggerEmergencyAlarm()
            if (isAppInForeground()) {
                showInAppEmergencyAlert(remoteMessage)
            }
        } else {
            Log.w(TAG, "Invalid or missing 'type' in FCM payload")
        }
    }

    private fun sendEmergencyNotification(remoteMessage: RemoteMessage) {
        // Check notification permission (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            Log.w(TAG, "Notification permission not granted")
            return
        }

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("emergency", true)
            putExtra("callSid", remoteMessage.data["callSid"])
            putExtra("callerNumber", remoteMessage.data["callerNumber"])
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_emergency ?: android.R.drawable.ic_dialog_alert) // Fallback icon
            .setContentTitle(remoteMessage.data.getOrElse("title") { "🚨 EMERGENCY ALERT" })
            .setContentText(remoteMessage.data.getOrElse("body") { "Urgent call detected!" })
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setSound(alarmSound)
            .setVibrate(longArrayOf(1000, 1000, 1000, 1000))
            .setLights(0xFFFF0000.toInt(), 1000, 1000)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setOngoing(true)
            .build()

        with(NotificationManagerCompat.from(this)) {
            notify(NOTIFICATION_ID, notification)
        }

        Log.d(TAG, "Emergency notification displayed")
    }

    private fun triggerEmergencyAlarm() {
        try {
            val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
            val wl = pm.newWakeLock(
                PowerManager.SCREEN_BRIGHT_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP,
                "echomi:EmergencyWakeLock"
            )
            wl.acquire(60 * 1000L) // 1 minute

            val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
            val maxVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_ALARM)
            audioManager.setStreamVolume(AudioManager.STREAM_ALARM, maxVol, 0)

            val alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            val ringtone = RingtoneManager.getRingtone(applicationContext, alarmUri)
            ringtone.play()

            // Release wake lock and stop ringtone after 1 minute
            Handler(Looper.getMainLooper()).postDelayed({
                if (wl.isHeld) {
                    wl.release()
                    ringtone.stop()
                    Log.d(TAG, "Wake lock released and alarm stopped")
                }
            }, 60 * 1000L)

            Log.d(TAG, "Emergency alarm triggered")
        } catch (e: Exception) {
            Log.e(TAG, "Error triggering emergency alarm", e)
        }
    }

    private fun showInAppEmergencyAlert(remoteMessage: RemoteMessage) {
        Log.d(TAG, "App is in foreground - should show in-app emergency dialog")
        val intent = Intent("EMERGENCY_ALERT_RECEIVED").apply {
            putExtra("callSid", remoteMessage.data["callSid"])
            putExtra("callerNumber", remoteMessage.data["callerNumber"])
            putExtra("message", remoteMessage.data["body"])
        }
        sendBroadcast(intent)
    }

    private fun isAppInForeground(): Boolean {
        val activityManager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val appProcesses = activityManager.runningAppProcesses ?: return false
        val packageName = packageName

        for (appProcess in appProcesses) {
            if (appProcess.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND &&
                appProcess.processName == packageName) {
                return true
            }
        }
        return false
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Emergency Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Emergency call notifications"
                enableLights(true)
                lightColor = android.graphics.Color.RED
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 1000, 500, 1000)
                setSound(
                    RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM),
                    android.media.AudioAttributes.Builder()
                        .setUsage(android.media.AudioAttributes.USAGE_ALARM)
                        .build()
                )
            }

            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
            Log.d(TAG, "Emergency notification channel created")
        }
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "Refreshed token: $token")

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val idToken = getGoogleIdToken()
                if (idToken.isEmpty()) {
                    Log.e(TAG, "Cannot send FCM token: No Google ID token found")
                    return@launch
                }

                val authHeader = "Bearer $idToken"
                val request = FcmTokenRequest(fcmToken = token)
                val response = RetrofitInstance.api.updateFcmToken(request, authHeader)

                if (response.isSuccessful) {
                    Log.d(TAG, "✅ Token sent successfully to backend")
                } else {
                    Log.e(TAG, "❌ Failed to send token: ${response.code()} - ${response.message()}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to send token", e)
            }
        }
    }

    private suspend fun getGoogleIdToken(): String {
        return try {
            val auth = FirebaseAuth.getInstance()
            val user = auth.currentUser
            if (user != null) {
                val tokenResult = user.getIdToken(false).await()
                tokenResult.token ?: ""
            } else {
                ""
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting Google ID token", e)
            ""
        }
    }
}