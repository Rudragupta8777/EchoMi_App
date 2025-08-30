package com.echomi.app

import android.os.Bundle
import android.util.Log
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController
        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        bottomNavigationView.setupWithNavController(navController)
        getAndSendFcmToken()
    }

    private fun getAndSendFcmToken() {
        lifecycleScope.launch {
            try {
                val token = FirebaseMessaging.getInstance().token.await()
                Log.d("MainActivity", "FCM Token: $token")
                // TODO: Create the API call in ApiService and a data class for the request
                // val request = FcmTokenRequest(token)
                // RetrofitInstance.api.updateFcmToken(request)
            } catch (e: Exception) {
                Log.e("MainActivity", "Error getting/sending FCM token", e)
            }
        }
    }
}