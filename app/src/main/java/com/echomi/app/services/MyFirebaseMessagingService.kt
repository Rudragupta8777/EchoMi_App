package com.echomi.app.services

import android.content.Context
import android.media.AudioManager
import android.media.RingtoneManager
import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class MyFirebaseMessagingService : FirebaseMessagingService() {

    companion object {
        private const val TAG = "MyFirebaseMsgService"
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        Log.d(TAG, "From: ${remoteMessage.from}")

        // Check if message contains a data payload with our emergency flag
        remoteMessage.data.isNotEmpty().let {
            Log.d(TAG, "Message data payload: " + remoteMessage.data)
            if (remoteMessage.data["isEmergency"] == "true") {
                triggerEmergencyAlarm()
            }
        }
    }

    private fun triggerEmergencyAlarm() {
        Log.d(TAG, "Emergency alarm triggered!")

        // Get the AudioManager to control volume
        val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager

        // Set the ringer and notification volume to maximum
        val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_ALARM)
        audioManager.setStreamVolume(AudioManager.STREAM_ALARM, maxVolume, 0)

        // Play the default alarm sound
        try {
            val alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            val ringtone = RingtoneManager.getRingtone(applicationContext, alarmUri)
            ringtone.play()
        } catch (e: Exception) {
            Log.e(TAG, "Error playing alarm sound", e)
        }
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "Refreshed token: $token")
        // TODO: Send this new token to your backend server
        // sendTokenToServer(token)
    }
}
