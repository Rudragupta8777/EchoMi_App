package com.app.echomi

import android.Manifest
import android.app.NotificationManager
import android.content.ContentValues.TAG
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.app.echomi.Network.RetrofitInstance
import com.app.echomi.Services.MyFirebaseMessagingService
import kotlinx.coroutines.launch

class SplashScreen : AppCompatActivity() {
    private lateinit var progressBar: ProgressBar
    private lateinit var statusTextView: TextView
    private lateinit var retryButton: Button

    private val REQUEST_CODE_NOTIFICATIONS = 1001

    private fun requestDoNotDisturbPermission() {
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
            !notificationManager.isNotificationPolicyAccessGranted) {
            val intent = Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)
            startActivity(intent)
        }
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    REQUEST_CODE_NOTIFICATIONS
                )
            }
        }
    }

    private fun requestSmsPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val permissions = arrayOf(
                Manifest.permission.RECEIVE_SMS,
                Manifest.permission.READ_SMS,
                Manifest.permission.POST_NOTIFICATIONS
            )
            if (permissions.any { ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED }) {
                ActivityCompat.requestPermissions(this, permissions, 101)
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_NOTIFICATIONS) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d("MainActivity", "✅ Notification permission granted")
            } else {
                Log.w("MainActivity", "❌ Notification permission denied")
            }
        }
    }



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        requestNotificationPermission()
        requestDoNotDisturbPermission()
        requestSmsPermissions() // Add this line
        setContentView(R.layout.activity_splash_screen)


        MyFirebaseMessagingService.stopEmergencyAlarm()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val permissions = arrayOf(
                Manifest.permission.RECEIVE_SMS,
                Manifest.permission.READ_SMS,
                Manifest.permission.POST_NOTIFICATIONS
            )
            if (permissions.any { ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED }) {
                ActivityCompat.requestPermissions(this, permissions, 101)
            }
        }

        progressBar = findViewById(R.id.progressBar)
        statusTextView = findViewById(R.id.statusTextView)
        retryButton = findViewById(R.id.retryButton)

        retryButton.setOnClickListener {
            checkBackendStatus()
        }

        checkBackendStatus()
    }

    private fun checkBackendStatus() {
        progressBar.visibility = View.VISIBLE
        statusTextView.text = "Connecting to service..."
        retryButton.visibility = View.GONE

        lifecycleScope.launch {
            try {
                val response = RetrofitInstance.api.healthCheck()
                if (response.isSuccessful) {
                    // Backend is reachable, proceed to Login
                    startActivity(Intent(this@SplashScreen, LoginScreen::class.java))
                    Log.d(TAG, "Backend Connected ✅")
                    finish()
                } else {
                    showError()
                }
            } catch (e: Exception) {
                Log.d(TAG, "Backend Not Connected ❌")
                // Network error or backend is down
                showError()
            }
        }
    }

    private fun showError() {
        progressBar.visibility = View.GONE
        statusTextView.text = "Connection failed. Please try again."
        retryButton.visibility = View.VISIBLE
    }
}