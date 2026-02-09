package com.app.echomi

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.airbnb.lottie.LottieAnimationView
import com.app.echomi.Network.RetrofitInstance
import com.app.echomi.data.FirebaseLoginRequest
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class LoginScreen : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var googleSignInClient: GoogleSignInClient

    // UI Elements
    private lateinit var signInButton: MaterialButton
    private lateinit var loadingAnimationView: LottieAnimationView
    private lateinit var contentContainer: LinearLayout

    companion object {
        private const val TAG = "LoginActivity"
    }

    private val googleSignInLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)!!
            firebaseAuthWithGoogle(account.idToken!!)
        } catch (e: ApiException) {
            Log.w(TAG, "Google sign in failed", e)
            showLoading(false)
            Toast.makeText(this, "Google Sign-In Failed.", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_login_screen)

        // Check if user is already logged in
        auth = Firebase.auth
        if (auth.currentUser != null) {
            navigateToMainActivity()
            return
        }

        checkPermissions()
        initializeViews()
        setupGoogleSignIn()

        signInButton.setOnClickListener {
            signIn()
        }
    }

    private fun initializeViews() {
        signInButton = findViewById(R.id.signInButton)
        loadingAnimationView = findViewById(R.id.loadingAnimationView)
        contentContainer = findViewById(R.id.contentContainer)

        // Configure Lottie
        try {
            loadingAnimationView.speed = 1.0f
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load animation: ${e.message}")
            loadingAnimationView.visibility = View.GONE
        }
    }

    private fun checkPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECEIVE_SMS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECEIVE_SMS, Manifest.permission.READ_SMS), 101)
            }
        }
    }

    private fun setupGoogleSignIn() {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, gso)
    }

    private fun signIn() {
        showLoading(true)
        val signInIntent = googleSignInClient.signInIntent
        googleSignInLauncher.launch(signInIntent)
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        lifecycleScope.launch {
            try {
                val authResult = auth.signInWithCredential(credential).await()
                val firebaseUser = authResult.user!!

                // Send user data to backend
                val request = FirebaseLoginRequest(
                    email = firebaseUser.email!!,
                    name = firebaseUser.displayName!!,
                    firebaseUid = firebaseUser.uid
                )

                val response = RetrofitInstance.api.loginWithFirebase(request)
                if (response.isSuccessful) {
                    Log.d(TAG, "Login successful, navigating to MainActivity ✅")
                    Toast.makeText(this@LoginScreen, "Access Granted", Toast.LENGTH_SHORT).show()
                    navigateToMainActivity()
                } else {
                    throw Exception("Backend login failed")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Firebase/Backend auth failed", e)
                showLoading(false)
                Toast.makeText(this@LoginScreen, "Authentication Failed.", Toast.LENGTH_SHORT).show()
                auth.signOut()
                googleSignInClient.signOut()
            }
        }
    }

    private fun navigateToMainActivity() {
        val intent = Intent(this, SetupScreen::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(intent)
        finish()
    }

    private fun showLoading(isLoading: Boolean) {
        if (isLoading) {
            contentContainer.visibility = View.GONE
            loadingAnimationView.visibility = View.VISIBLE
            loadingAnimationView.playAnimation()
        } else {
            contentContainer.visibility = View.VISIBLE
            loadingAnimationView.visibility = View.GONE
            loadingAnimationView.cancelAnimation()
        }
    }

    override fun onPause() {
        super.onPause()
        if (::loadingAnimationView.isInitialized) {
            loadingAnimationView.pauseAnimation()
        }
    }

    override fun onResume() {
        super.onResume()
        if (::loadingAnimationView.isInitialized && loadingAnimationView.visibility == View.VISIBLE) {
            loadingAnimationView.playAnimation()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::loadingAnimationView.isInitialized) {
            loadingAnimationView.cancelAnimation()
        }
    }
}