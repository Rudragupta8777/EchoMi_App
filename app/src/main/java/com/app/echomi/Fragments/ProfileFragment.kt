package com.app.echomi.Fragments

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.airbnb.lottie.LottieAnimationView
import com.app.echomi.ContactSelectionScreen
import com.app.echomi.LoginScreen
import com.app.echomi.R
import com.app.echomi.SplashScreen
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

class ProfileFragment : Fragment() {

    private lateinit var loadingAnimationView: LottieAnimationView
    private lateinit var nameTextView: TextView
    private lateinit var emailTextView: TextView
    private lateinit var manageContactsButton: MaterialButton
    private lateinit var logoutButton: MaterialButton
    private lateinit var profileCard: LinearLayout // To hide the whole card while loading

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_profile, container, false)

        nameTextView = view.findViewById(R.id.nameTextView)
        emailTextView = view.findViewById(R.id.emailTextView)
        manageContactsButton = view.findViewById(R.id.manageContactsButton)
        logoutButton = view.findViewById(R.id.logoutButton)
        loadingAnimationView = view.findViewById(R.id.loadingAnimationView)
        profileCard = view.findViewById(R.id.materialCardView)

        showLoading(true)

        // Simulate loading user data
        loadUserData()

        return view
    }

    private fun loadUserData() {
        Handler(Looper.getMainLooper()).postDelayed({
            try {
                val currentUser = Firebase.auth.currentUser
                nameTextView.text = currentUser?.displayName ?: "No Name"
                emailTextView.text = currentUser?.email ?: "No Email"

                showLoading(false)
                setupClickListeners()

            } catch (e: Exception) {
                showLoading(false)
                setupClickListeners() // Allow retry or logout even on error
            }
        }, 1000)
    }

    private fun setupClickListeners() {
        manageContactsButton.setOnClickListener {
            startActivity(Intent(activity, ContactSelectionScreen::class.java))
        }

        logoutButton.setOnClickListener {
            showLoading(true)

            // Sign out from Firebase
            Firebase.auth.signOut()

            // Sign out from Google
            val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).build()
            val googleSignInClient = GoogleSignIn.getClient(requireActivity(), gso)

            googleSignInClient.signOut().addOnCompleteListener {
                val intent = Intent(activity, SplashScreen::class.java) // or LoginScreen
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                requireActivity().finish()
            }
        }
    }

    private fun showLoading(isLoading: Boolean) {
        if (isLoading) {
            loadingAnimationView.visibility = View.VISIBLE
            loadingAnimationView.playAnimation()

            // Hide UI
            profileCard.visibility = View.INVISIBLE
            manageContactsButton.visibility = View.INVISIBLE
            logoutButton.visibility = View.INVISIBLE
        } else {
            loadingAnimationView.cancelAnimation()
            loadingAnimationView.visibility = View.GONE

            // Show UI
            profileCard.visibility = View.VISIBLE
            manageContactsButton.visibility = View.VISIBLE
            logoutButton.visibility = View.VISIBLE
        }
    }
}