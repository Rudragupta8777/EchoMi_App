package com.app.echomi

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.app.echomi.Network.RetrofitInstance
import com.echomi.app.network.FcmTokenRequest
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class MainActivity : AppCompatActivity() {
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        bottomNav.itemRippleColor = ColorStateList.valueOf(ContextCompat.getColor(this, R.color.ripple))

        auth = FirebaseAuth.getInstance()

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, gso)

        sharedPreferences = getSharedPreferences("auth", Context.MODE_PRIVATE)

        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController
        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        bottomNavigationView.setupWithNavController(navController)

        getAndSendFcmToken()

        // Handle popup if app is started fresh
        handleApprovalIntent(intent)
    }

    // Handle case where app is already running
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent) // Update the intent
        handleApprovalIntent(intent)
    }

    private fun handleApprovalIntent(intent: Intent?) {
        if (intent == null) return

        val approvalId = intent.getStringExtra("approvalId")

        if (!approvalId.isNullOrEmpty()) {
            Log.d("MainActivity", "🔔 Notification clicked. Opening Popup for ID: $approvalId")

            val popupIntent = Intent(this, ApprovalActivity::class.java).apply {
                putExtra("approvalId", approvalId)
                putExtra("company", intent.getStringExtra("company"))
                putExtra("callerNumber", intent.getStringExtra("callerNumber"))
                putExtra("callSid", intent.getStringExtra("callSid"))
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            startActivity(popupIntent)

            // Remove the extra so it doesn't trigger again on rotation
            intent.removeExtra("approvalId")
        }
    }

    private fun getAndSendFcmToken() {
        lifecycleScope.launch {
            try {
                val fcmToken = FirebaseMessaging.getInstance().token.await()
                Log.d("MainActivity", "FCM Token: $fcmToken")

                val idToken = getGoogleIdToken()
                if (idToken.isEmpty()) {
                    Log.e("MainActivity", "No Google ID token found")
                    return@launch
                }

                val authHeader = "Bearer $idToken"
                val request = FcmTokenRequest(fcmToken = fcmToken)
                val response = RetrofitInstance.api.updateFcmToken(request, authHeader)

                if (response.isSuccessful) {
                    Log.d("MainActivity", "✅ FCM token sent to backend successfully")
                } else {
                    Log.e("MainActivity", "❌ Failed to send FCM token: ${response.code()}")
                }
            } catch (e: Exception) {
                Log.e("MainActivity", "Error getting/sending FCM token", e)
            }
        }
    }

    private suspend fun getGoogleIdToken(): String {
        return try {
            val user = auth.currentUser
            if (user != null) {
                val tokenResult = user.getIdToken(false).await()
                tokenResult.token ?: ""
            } else {
                val account = GoogleSignIn.getLastSignedInAccount(this)
                account?.idToken ?: ""
            }
        } catch (e: Exception) {
            Log.e("MainActivity", "Error getting Google ID token", e)
            ""
        }
    }
}