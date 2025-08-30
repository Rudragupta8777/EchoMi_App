package com.echomi.app

import android.content.ContentValues.TAG
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.echomi.app.network.RetrofitInstance
import kotlinx.coroutines.launch
import kotlin.jvm.java

class SplashActivity : AppCompatActivity() {

    private lateinit var progressBar: ProgressBar
    private lateinit var statusTextView: TextView
    private lateinit var retryButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_splash)

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
                    startActivity(Intent(this@SplashActivity, LoginActivity::class.java))
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
