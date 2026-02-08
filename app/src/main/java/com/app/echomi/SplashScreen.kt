package com.app.echomi

import android.Manifest
import android.app.NotificationManager
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.airbnb.lottie.LottieAnimationView
import com.app.echomi.Network.RetrofitInstance
import com.app.echomi.Services.MyFirebaseMessagingService
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.launch

class SplashScreen : AppCompatActivity() {
    private lateinit var loadingAnimationView: LottieAnimationView
    private lateinit var statusTextView: TextView
    private lateinit var retryButton: Button
    private lateinit var auth: FirebaseAuth

    private val REQUEST_CODE_NOTIFICATIONS = 1001

    companion object {
        private const val TAG = "SplashScreen"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Debug log to see if data reached SplashScreen
        if (intent.hasExtra("approvalId")) {
            Log.d(TAG, "🔔 SplashScreen received Approval ID: ${intent.getStringExtra("approvalId")}")
        }

        auth = Firebase.auth
        checkUserAuthentication()
    }

    private fun checkUserAuthentication() {
        val currentUser = auth.currentUser

        if (currentUser != null) {
            Log.d(TAG, "User already authenticated: ${currentUser.email}")
            navigateToMainActivity()
        } else {
            setContentView(R.layout.activity_splash_screen)
            initializeViews()
            requestPermissions()
            checkBackendStatus()
        }
    }

    // --- CRITICAL FIX START ---
    private fun navigateToMainActivity() {
        val mainIntent = Intent(this, MainActivity::class.java)

        // COPY ALL DATA (EXTRAS) FROM NOTIFICATION TO MAIN ACTIVITY
        if (intent.extras != null) {
            Log.d(TAG, "➡️ Forwarding extras to MainActivity")
            mainIntent.putExtras(intent.extras!!)
        }

        // Ensure the flags are clean
        mainIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK

        startActivity(mainIntent)
        finish()
    }
    // --- CRITICAL FIX END ---

    private fun initializeViews() {
        loadingAnimationView = findViewById(R.id.loadingAnimationView)
        statusTextView = findViewById(R.id.statusTextView)
        retryButton = findViewById(R.id.retryButton)

        retryButton.setOnClickListener {
            checkBackendStatus()
        }

        try {
            loadingAnimationView.speed = 1.0f
            loadingAnimationView.playAnimation()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load animation: ${e.message}")
            loadingAnimationView.visibility = View.GONE
        }

        loadingAnimationView.addLottieOnCompositionLoadedListener { composition ->
            if (composition == null) {
                Log.e(TAG, "Failed to load Lottie animation")
                loadingAnimationView.visibility = View.GONE
            }
        }

        MyFirebaseMessagingService.stopEmergencyAlarm()
    }

    private fun requestPermissions() {
        requestNotificationPermission()
        requestDoNotDisturbPermission()
        requestSmsPermissions()
    }

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
                Log.d(TAG, "✅ Notification permission granted")
            } else {
                Log.w(TAG, "❌ Notification permission denied")
            }
        }
    }

    private fun checkBackendStatus() {
        loadingAnimationView.visibility = View.VISIBLE
        statusTextView.text = "Connecting to service..."
        retryButton.visibility = View.GONE

        lifecycleScope.launch {
            try {
                val response = RetrofitInstance.api.healthCheck()
                if (response.isSuccessful) {
                    startActivity(Intent(this@SplashScreen, LoginScreen::class.java))
                    finish()
                } else {
                    showError()
                }
            } catch (e: Exception) {
                showError()
            }
        }
    }

    private fun showError() {
        loadingAnimationView.visibility = View.GONE
        statusTextView.text = "Connection failed. Please try again."
        retryButton.visibility = View.VISIBLE
    }
}