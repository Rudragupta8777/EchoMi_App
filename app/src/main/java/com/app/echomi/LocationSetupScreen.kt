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
import com.mapbox.common.MapboxOptions
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.MapView
import com.mapbox.maps.Style
import com.app.echomi.BuildConfig
import com.mapbox.maps.plugin.compass.compass
import com.mapbox.maps.plugin.scalebar.scalebar
import kotlinx.coroutines.launch

class LocationSetupScreen : AppCompatActivity() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var btnDetectLocation: MaterialButton
    private lateinit var loadingAnimationView: LottieAnimationView
    private lateinit var contentContainer: LinearLayout
    private lateinit var mapView: MapView

    private var isUpdateMode = false

    companion object {
        private const val TAG = "LocationSetupScreen"
    }

    private val locationPermissionRequest = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        when {
            permissions.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false) ||
                    permissions.getOrDefault(Manifest.permission.ACCESS_COARSE_LOCATION, false) -> {
                fetchCurrentLocationAndMoveMap()
            }
            else -> {
                Toast.makeText(this, "Location permission is required to assist deliveries.", Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // IMPORTANT: Set Mapbox token BEFORE setting content view
        MapboxOptions.accessToken = BuildConfig.MAPBOX_ACCESS_TOKEN

        enableEdgeToEdge()
        setContentView(R.layout.activity_location_setup_screen)

        isUpdateMode = intent.getBooleanExtra("IS_UPDATE_MODE", false)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        btnDetectLocation = findViewById(R.id.btnDetectLocation)
        loadingAnimationView = findViewById(R.id.loadingAnimationView)
        contentContainer = findViewById(R.id.contentContainer)
        mapView = findViewById(R.id.mapView)

        if (isUpdateMode) {
            btnDetectLocation.text = "OVERRIDE TARGET"
        }

        // Load the slick built-in Dark Style
        mapView.mapboxMap.loadStyle(Style.DARK) { style ->
            // Map is fully loaded, hide compass/scale for cleaner UI
            mapView.compass.updateSettings { enabled = false }
            mapView.scalebar.updateSettings { enabled = false }

            // Check permissions and jump to user's location
            checkPermissions()
        }

        btnDetectLocation.setOnClickListener {
            // Get exact coordinates of the crosshair (center of the screen)
            val centerPoint = mapView.mapboxMap.cameraState.center
            val latitude = centerPoint.latitude()
            val longitude = centerPoint.longitude()

            Log.d(TAG, "Selected coordinates: Lat $latitude, Lng $longitude")
            sendLocationToBackend(latitude, longitude)
        }
    }

    private fun checkPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            locationPermissionRequest.launch(arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ))
        } else {
            fetchCurrentLocationAndMoveMap()
        }
    }

    private fun fetchCurrentLocationAndMoveMap() {
        try {
            fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
                if (location != null) {
                    // Fly camera to user's current GPS location
                    val point = Point.fromLngLat(location.longitude, location.latitude)
                    val cameraOptions = CameraOptions.Builder()
                        .center(point)
                        .zoom(16.0)
                        .build()

                    mapView.mapboxMap.setCamera(cameraOptions)
                }
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "Security Exception", e)
        }
    }

    private fun sendLocationToBackend(latitude: Double, longitude: Double) {
        showLoading(true)
        lifecycleScope.launch {
            try {
                val request = LocationRequest(latitude, longitude)
                val response = if (isUpdateMode) {
                    RetrofitInstance.api.updateDeliveryLocation(request)
                } else {
                    RetrofitInstance.api.setDeliveryLocation(request)
                }

                if (response.isSuccessful) {
                    Toast.makeText(this@LocationSetupScreen, "Coordinates Locked", Toast.LENGTH_SHORT).show()

                    if (isUpdateMode) {
                        finish()
                    } else {
                        val intent = Intent(this@LocationSetupScreen, SetupScreen::class.java)
                        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
                        startActivity(intent)
                        finish()
                    }
                } else {
                    throw Exception("Backend rejected coordinates")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error syncing target", e)
                showLoading(false)
                Toast.makeText(this@LocationSetupScreen, "Sync failed. Retry.", Toast.LENGTH_SHORT).show()
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