package com.app.echomi.Fragments

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.airbnb.lottie.LottieAnimationView
import com.app.echomi.ContactSelectionScreen
import com.app.echomi.LocationSetupScreen
import com.app.echomi.Network.RetrofitInstance
import com.app.echomi.R
import com.app.echomi.SplashScreen
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.launch

class ProfileFragment : Fragment() {

    private lateinit var loadingAnimationView: LottieAnimationView
    private lateinit var nameTextView: TextView
    private lateinit var emailTextView: TextView
    private lateinit var locationTextView: TextView
    private lateinit var manageContactsButton: MaterialButton
    private lateinit var updateLocationButton: MaterialButton
    private lateinit var logoutButton: MaterialButton
    private lateinit var profileCard: LinearLayout

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_profile, container, false)

        nameTextView = view.findViewById(R.id.nameTextView)
        emailTextView = view.findViewById(R.id.emailTextView)
        locationTextView = view.findViewById(R.id.locationTextView)
        manageContactsButton = view.findViewById(R.id.manageContactsButton)
        updateLocationButton = view.findViewById(R.id.updateLocationButton)
        logoutButton = view.findViewById(R.id.logoutButton)
        loadingAnimationView = view.findViewById(R.id.loadingAnimationView)
        profileCard = view.findViewById(R.id.materialCardView)

        setupClickListeners()
        return view
    }

    override fun onResume() {
        super.onResume()
        // Reload data every time fragment comes into view (e.g. after map update)
        loadUserData()
    }

    private fun loadUserData() {
        showLoading(true)

        // 1. Load Firebase Data
        val currentUser = Firebase.auth.currentUser
        nameTextView.text = currentUser?.displayName ?: "UNKNOWN OP"
        emailTextView.text = currentUser?.email ?: "NO UPLINK"

        // 2. Fetch Location Data from Backend
        lifecycleScope.launch {
            try {
                val response = RetrofitInstance.api.getDeliveryLocation()
                if (response.isSuccessful) {
                    val location = response.body()?.deliveryLocation
                    if (location != null) {
                        val latStr = String.format("%.4f", location.latitude)
                        val lngStr = String.format("%.4f", location.longitude)
                        locationTextView.text = "LAT: $latStr | LNG: $lngStr"
                    } else {
                        locationTextView.text = "UNINITIALIZED"
                    }
                } else {
                    locationTextView.text = "SYSTEM ERROR"
                }
            } catch (e: Exception) {
                Log.e("ProfileFragment", "Error fetching location", e)
                locationTextView.text = "OFFLINE"
            } finally {
                showLoading(false)
            }
        }
    }

    private fun setupClickListeners() {
        manageContactsButton.setOnClickListener {
            startActivity(Intent(activity, ContactSelectionScreen::class.java))
        }

        updateLocationButton.setOnClickListener {
            // Launch map with Update flag
            val intent = Intent(activity, LocationSetupScreen::class.java)
            intent.putExtra("IS_UPDATE_MODE", true)
            startActivity(intent)
        }

        logoutButton.setOnClickListener {
            showLoading(true)
            Firebase.auth.signOut()
            val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).build()
            val googleSignInClient = GoogleSignIn.getClient(requireActivity(), gso)

            googleSignInClient.signOut().addOnCompleteListener {
                val intent = Intent(activity, SplashScreen::class.java)
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
            profileCard.visibility = View.INVISIBLE
            manageContactsButton.visibility = View.INVISIBLE
            updateLocationButton.visibility = View.INVISIBLE
            logoutButton.visibility = View.INVISIBLE
        } else {
            loadingAnimationView.cancelAnimation()
            loadingAnimationView.visibility = View.GONE
            profileCard.visibility = View.VISIBLE
            manageContactsButton.visibility = View.VISIBLE
            updateLocationButton.visibility = View.VISIBLE
            logoutButton.visibility = View.VISIBLE
        }
    }
}