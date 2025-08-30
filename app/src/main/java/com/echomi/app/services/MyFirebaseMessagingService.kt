package com.echomi.app.services

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
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
import com.echomi.app.R
import com.echomi.app.SplashActivity
import com.echomi.app.network.FcmTokenRequest
import com.echomi.app.network.RetrofitInstance
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class MyFirebaseMessagingService : FirebaseMessagingService() {

    companion object {
        private const val TAG = "MyFirebaseService"
        private const val CHANNEL_ID = "emergency_channel"
        private const val NOTIFICATION_ID = 1001

        private var emergencyRingtone: Ringtone? = null

        fun stopEmergencyAlarm() {
            emergencyRingtone?.stop()
            emergencyRingtone = null
            Log.d(TAG, "⏹️ Emergency alarm stopped by user")
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        Log.d(TAG, "📩 FCM Data Payload: ${remoteMessage.data}")

        val type = remoteMessage.data["type"] ?: ""
        if (type == "emergency_alert") {
            Log.d(TAG, "🚨 Emergency notification received!")
            increaseVolume()
            sendEmergencyNotification(remoteMessage)
            triggerEmergencyAlarm()
        } else {
            Log.w(TAG, "Invalid or missing 'type' in FCM payload: $type")
        }
    }

    private fun increaseVolume() {
        val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager

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

        // Intent → open SplashActivity and stop alarm
        val intent = Intent(this, SplashActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("STOP_ALARM", true)
        }

        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val soundUri = Uri.parse("android.resource://${packageName}/raw/buzzer")

        val notificationBuilder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_emergency)
            .setContentTitle("$title - Call Back Needed")
            .setContentText("Caller: $callerNumber\n$body")
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setSound(soundUri)  // Personal buzzer
            .setContentIntent(pendingIntent)
            .setFullScreenIntent(pendingIntent, true)

        try {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
            ) {
                with(NotificationManagerCompat.from(this)) {
                    notify(NOTIFICATION_ID, notificationBuilder.build())
                }
            }
        } catch (se: SecurityException) {
            Log.e(TAG, "❌ SecurityException while showing notification: ${se.message}")
        }
    }


    private fun triggerEmergencyAlarm() {
        try {
            val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
            val wl = pm.newWakeLock(
                PowerManager.SCREEN_BRIGHT_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP,
                "echomi:EmergencyWakeLock"
            )
            wl.acquire(60 * 1000L) // 1 minute

            val alarmUri = Uri.parse("android.resource://${packageName}/raw/buzzer")
            val ringtone = RingtoneManager.getRingtone(applicationContext, alarmUri)
            ringtone.play()

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

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Emergency Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Emergency call notifications"
            }
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val idToken = getGoogleIdToken()
                if (idToken.isEmpty()) return@launch
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
            ""
        }
    }
}
