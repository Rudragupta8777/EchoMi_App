package com.app.echomi

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.airbnb.lottie.LottieAnimationView
import com.app.echomi.Network.RetrofitInstance
import com.app.echomi.data.LocationRequest
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.launch

class LocationSetupScreen : AppCompatActivity() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var btnDetectLocation: MaterialButton
    private lateinit var loadingAnimationView: LottieAnimationView
    private lateinit var contentContainer: LinearLayout

    companion object {
        private const val TAG = "LocationSetupScreen"
    }

    private val locationPermissionRequest = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        when {
            permissions.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false) ||
                    permissions.getOrDefault(Manifest.permission.ACCESS_COARSE_LOCATION, false) -> {
                // Permission granted
                fetchLocationAndSave()
            }
            else -> {
                // Permission denied
                Toast.makeText(this, "Location permission is required to assist deliveries.", Toast.LENGTH_LONG).show()
                showLoading(false)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_location_setup_screen)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        btnDetectLocation = findViewById(R.id.btnDetectLocation)
        loadingAnimationView = findViewById(R.id.loadingAnimationView)
        contentContainer = findViewById(R.id.contentContainer)

        btnDetectLocation.setOnClickListener {
            checkPermissionsAndFetchLocation()
        }
    }

    private fun checkPermissionsAndFetchLocation() {
        showLoading(true)
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fetchLocationAndSave()
        } else {
            locationPermissionRequest.launch(arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ))
        }
    }

    private fun fetchLocationAndSave() {
        try {
            fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
                if (location != null) {
                    sendLocationToBackend(location.latitude, location.longitude)
                } else {
                    showLoading(false)
                    Toast.makeText(this, "Could not get location. Please ensure GPS is ON.", Toast.LENGTH_LONG).show()
                }
            }.addOnFailureListener {
                showLoading(false)
                Toast.makeText(this, "Error fetching location.", Toast.LENGTH_SHORT).show()
            }
        } catch (e: SecurityException) {
            showLoading(false)
            Log.e(TAG, "Security Exception: Missing location permission", e)
        }
    }

    private fun sendLocationToBackend(latitude: Double, longitude: Double) {
        lifecycleScope.launch {
            try {
                val request = LocationRequest(latitude, longitude)
                val response = RetrofitInstance.api.setDeliveryLocation(request)

                if (response.isSuccessful) {
                    Log.d(TAG, "Location saved successfully!")
                    Toast.makeText(this@LocationSetupScreen, "Coordinates Locked", Toast.LENGTH_SHORT).show()

                    // Proceed to next step in onboarding
                    val intent = Intent(this@LocationSetupScreen, SetupScreen::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
                    startActivity(intent)
                    finish()
                } else {
                    throw Exception("Backend failed to save location")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error saving location", e)
                showLoading(false)
                Toast.makeText(this@LocationSetupScreen, "Failed to save location.", Toast.LENGTH_SHORT).show()
            }
        }
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
}