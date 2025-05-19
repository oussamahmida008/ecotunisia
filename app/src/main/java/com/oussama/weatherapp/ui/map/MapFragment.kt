package com.oussama.weatherapp.ui.map

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.oussama.weatherapp.R
import com.oussama.weatherapp.data.model.Location
import com.oussama.weatherapp.data.repository.UserRepository
import com.oussama.weatherapp.databinding.FragmentMapBinding
import com.oussama.weatherapp.ui.viewmodel.MapViewModel
import com.oussama.weatherapp.utils.MapUtils
import com.oussama.weatherapp.utils.PermissionUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay

class MapFragment : Fragment() {
    private val TAG = "MapFragment"

    private var _binding: FragmentMapBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: MapViewModel
    private var locationOverlay: MyLocationNewOverlay? = null
    private var locationPermissionRequested = false

    // User repository for authentication
    private val userRepository = UserRepository()

    // Permission launchers
    private val fineLocationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        locationPermissionRequested = true
        if (isGranted) {
            Log.d(TAG, "Fine location permission granted")
            // Permission granted, check location settings
            checkLocationSettings()
        } else {
            Log.d(TAG, "Fine location permission denied")
            // Try with coarse location as fallback
            requestCoarseLocationPermission()
        }
    }

    private val coarseLocationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            Log.d(TAG, "Coarse location permission granted")
            // Permission granted, check location settings
            checkLocationSettings()
        } else {
            Log.d(TAG, "Coarse location permission denied")
            // Both permissions denied
            handlePermissionDenied()
        }
    }

    // Location settings launcher
    private val locationSettingsLauncher = registerForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            Log.d(TAG, "Location settings enabled by user")
            // Location settings enabled
            enableMyLocation()
        } else {
            Log.d(TAG, "Location settings request denied")
            // Location settings not enabled
            showLocationSettingsSnackbar()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMapBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize ViewModel
        viewModel = ViewModelProvider(this)[MapViewModel::class.java]

        // Set up MapView
        setupMapView()

        // Set up click listeners
        setupClickListeners()

        // Observe ViewModel state
        observeViewModel()

        // Load locations
        viewModel.loadAllLocations()

        // Check for location permission
        checkLocationPermission()
    }

    private fun setupMapView() {
        // Configure the map
        MapUtils.configureMapView(binding.mapView, requireContext())

        // Center map on Tunisia
        MapUtils.centerMapOnTunisia(binding.mapView)
    }

    private fun setupClickListeners() {
        // Add location button click
        binding.addLocationFab.setOnClickListener {
            findNavController().navigate(R.id.action_mapFragment_to_addLocationFragment)
        }

        // My location button click
        binding.myLocationFab.setOnClickListener {
            centerOnMyLocation()
        }

        // Center on Tunisia button click
        binding.centerOnTunisiaFab.setOnClickListener {
            MapUtils.centerMapOnTunisia(binding.mapView)
        }

        // Filter buttons
        binding.allLocationsButton.setOnClickListener {
            viewModel.loadAllLocations()
        }

        binding.myLocationsButton.setOnClickListener {
            viewModel.loadUserLocations()
        }

        // Add eco locations button
        binding.addEcoLocationsButton.setOnClickListener {
            showAddEcoLocationsDialog()
        }
    }

    /**
     * Center the map on the user's current location
     */
    private fun centerOnMyLocation() {
        try {
            // Check if location permissions are granted
            if (!PermissionUtils.hasLocationPermissions(requireContext())) {
                checkLocationPermission()
                return
            }

            // Get current location from the overlay
            val myLocation = locationOverlay?.myLocation
            if (myLocation != null) {
                Log.d(TAG, "Centering map on current location: $myLocation")
                MapUtils.centerMapOn(binding.mapView, myLocation.latitude, myLocation.longitude)
            } else {
                // No location available yet, show a message
                Snackbar.make(
                    binding.root,
                    "Waiting for location...",
                    Snackbar.LENGTH_SHORT
                ).show()

                // Try to enable location tracking if it's not already enabled
                if (locationOverlay == null) {
                    enableMyLocation()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error centering on current location", e)
            Toast.makeText(
                requireContext(),
                "Could not center on your location",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    /**
     * Show dialog to confirm adding predefined eco-friendly locations
     */
    private fun showAddEcoLocationsDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.add_eco_locations)
            .setMessage(R.string.add_eco_locations_description)
            .setPositiveButton(R.string.ok) { _, _ ->
                viewModel.seedPredefinedLocations()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun observeViewModel() {
        // Observe all locations
        viewModel.locations.observe(viewLifecycleOwner) { locations ->
            updateMapMarkers(locations)
        }

        // Observe user locations
        viewModel.userLocations.observe(viewLifecycleOwner) { locations ->
            updateMapMarkers(locations)
        }

        // Observe loading state
        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        }

        // Observe error state
        viewModel.error.observe(viewLifecycleOwner) { error ->
            error?.let {
                // Check if it's an index error
                if (it == R.string.error_index_required.toString()) {
                    // Show a Snackbar for index errors
                    Snackbar.make(
                        binding.root,
                        getString(R.string.error_index_required),
                        Snackbar.LENGTH_LONG
                    ).setAction(getString(R.string.retry)) {
                        // User manually triggered retry
                        viewModel.loadUserLocations()
                    }.show()

                    // Show a message in the empty view
                    binding.emptyTextView.text = getString(R.string.setting_up_database)
                    binding.emptyTextView.visibility = View.VISIBLE
                }
                // Check if it's an authentication error
                else if (it.contains("permission") || it.contains("auth")) {
                    // Show a Snackbar with an action to refresh the token
                    Snackbar.make(
                        binding.root,
                        it,
                        Snackbar.LENGTH_LONG
                    ).setAction("Refresh Auth") {
                        refreshAuthToken()
                    }.show()
                } else {
                    // Show a regular Toast for other errors
                    Toast.makeText(context, it, Toast.LENGTH_LONG).show()
                }
                viewModel.clearError()
            }
        }

        // Observe selected location
        viewModel.selectedLocation.observe(viewLifecycleOwner) { location ->
            location?.let {
                val action = MapFragmentDirections.actionMapFragmentToLocationDetailFragment(it.id)
                findNavController().navigate(action)
                viewModel.selectLocation(null) // Reset selection
            }
        }
    }

    /**
     * Refresh the Firebase authentication token
     * This is useful when you get permission denied errors
     */
    private fun refreshAuthToken() {
        // Show loading indicator
        binding.progressBar.visibility = View.VISIBLE

        // Launch a coroutine to refresh the token
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val result = userRepository.refreshAuthToken()

                // Update UI on the main thread
                withContext(Dispatchers.Main) {
                    if (result.isSuccess) {
                        // Token refreshed successfully
                        Snackbar.make(
                            binding.root,
                            "Authentication refreshed successfully",
                            Snackbar.LENGTH_SHORT
                        ).show()

                        // Reload data
                        viewModel.loadAllLocations()
                    } else {
                        // Failed to refresh token
                        val error = result.exceptionOrNull()?.message ?: "Unknown error"
                        Snackbar.make(
                            binding.root,
                            "Failed to refresh authentication: $error",
                            Snackbar.LENGTH_LONG
                        ).setAction("Retry") {
                            refreshAuthToken()
                        }.show()
                    }

                    // Hide loading indicator
                    binding.progressBar.visibility = View.GONE
                }
            } catch (e: Exception) {
                // Handle any exceptions
                withContext(Dispatchers.Main) {
                    Snackbar.make(
                        binding.root,
                        "Error refreshing authentication: ${e.message}",
                        Snackbar.LENGTH_LONG
                    ).show()

                    // Hide loading indicator
                    binding.progressBar.visibility = View.GONE
                }
            }
        }
    }

    private fun updateMapMarkers(locations: List<Location>) {
        // Clear existing markers
        binding.mapView.overlays.removeIf { it is Marker }

        // Add markers for each location
        locations.forEach { location ->
            addMarker(location)
        }

        // Refresh the map
        binding.mapView.invalidate()
    }

    private fun addMarker(location: Location) {
        val marker = Marker(binding.mapView)
        marker.position = GeoPoint(location.latitude, location.longitude)
        marker.title = location.title
        marker.snippet = location.description

        // Set marker icon
        val icon: Drawable? = ContextCompat.getDrawable(requireContext(), android.R.drawable.ic_menu_mylocation)
        marker.icon = icon

        // Set marker click listener
        marker.setOnMarkerClickListener { clickedMarker, _ ->
            viewModel.selectLocation(location)
            true
        }

        // Add marker to map
        binding.mapView.overlays.add(marker)
    }

    private fun checkLocationPermission() {
        Log.d(TAG, "Checking location permission")

        // Check if location permission is already granted
        if (PermissionUtils.hasLocationPermissions(requireContext())) {
            Log.d(TAG, "Location permission already granted")
            // Permission already granted, check location settings
            checkLocationSettings()
            return
        }

        // Request fine location permission first
        when {
            // Should show rationale for fine location
            shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION) -> {
                Log.d(TAG, "Should show rationale for fine location")
                showLocationPermissionRationale()
            }
            // First time asking or user clicked "Don't ask again"
            else -> {
                if (locationPermissionRequested) {
                    // User has denied permission and clicked "Don't ask again"
                    Log.d(TAG, "Permission permanently denied")
                    showPermissionPermanentlyDeniedDialog()
                } else {
                    // First time asking
                    Log.d(TAG, "Requesting fine location permission for the first time")
                    requestFineLocationPermission()
                }
            }
        }
    }

    private fun requestFineLocationPermission() {
        fineLocationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
    }

    private fun requestCoarseLocationPermission() {
        // If fine location was denied, try with coarse location
        if (shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_COARSE_LOCATION)) {
            // Show rationale for coarse location
            PermissionUtils.showPermissionRationaleDialog(
                this,
                getString(R.string.location_permission_title),
                getString(R.string.location_permission_rationale),
                { coarseLocationPermissionLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION) },
                { handlePermissionDenied() }
            )
        } else {
            // Request coarse location directly
            coarseLocationPermissionLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
        }
    }

    private fun showLocationPermissionRationale() {
        PermissionUtils.showPermissionRationaleDialog(
            this,
            getString(R.string.location_permission_title),
            getString(R.string.location_permission_rationale),
            { requestFineLocationPermission() },
            { handlePermissionDenied() }
        )
    }

    private fun showPermissionPermanentlyDeniedDialog() {
        PermissionUtils.showPermissionDeniedDialog(
            this,
            getString(R.string.location_permission_denied_title),
            getString(R.string.location_permission_permanently_denied)
        )
    }

    private fun handlePermissionDenied() {
        Log.d(TAG, "Location permission denied")
        Snackbar.make(
            binding.root,
            getString(R.string.location_permission_denied_message),
            Snackbar.LENGTH_LONG
        ).setAction(getString(R.string.open_settings)) {
            // Open app settings
            startActivity(android.content.Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = android.net.Uri.fromParts("package", requireContext().packageName, null)
            })
        }.show()
    }

    private fun checkLocationSettings() {
        if (!PermissionUtils.isLocationEnabled(requireContext())) {
            Log.d(TAG, "Location is not enabled")
            showLocationSettingsDialog()
            return
        }

        // Location settings are enabled, enable my location on map
        enableMyLocation()
    }

    private fun showLocationSettingsDialog() {
        PermissionUtils.checkLocationSettings(
            this,
            { enableMyLocation() },
            { showLocationSettingsSnackbar() }
        ) { intentSenderRequest ->
            locationSettingsLauncher.launch(intentSenderRequest)
        }
    }

    private fun showLocationSettingsSnackbar() {
        Snackbar.make(
            binding.root,
            getString(R.string.location_settings_message),
            Snackbar.LENGTH_LONG
        ).setAction(getString(R.string.enable_location)) {
            showLocationSettingsDialog()
        }.show()
    }

    private fun enableMyLocation() {
        try {
            Log.d(TAG, "Enabling my location on map")

            // Check if location permissions are granted
            if (!PermissionUtils.hasLocationPermissions(requireContext())) {
                Log.w(TAG, "Location permissions not granted, requesting permissions")
                checkLocationPermission()
                return
            }

            // Check if location services are enabled
            if (!PermissionUtils.isLocationEnabled(requireContext())) {
                Log.w(TAG, "Location services not enabled, showing settings dialog")
                showLocationSettingsDialog()
                return
            }

            // Add location overlay to the map
            locationOverlay = MapUtils.addLocationOverlay(binding.mapView, requireContext())

            if (locationOverlay == null) {
                Log.e(TAG, "Failed to create location overlay")
                Snackbar.make(
                    binding.root,
                    getString(R.string.error_occurred),
                    Snackbar.LENGTH_LONG
                ).setAction(getString(R.string.retry)) {
                    enableMyLocation()
                }.show()
                return
            }

            // Refresh the map
            binding.mapView.invalidate()

            // We don't want to automatically center on user's location
            // This would override our initial center on Tunisia
            // Instead, we'll add a button to let the user manually center on their location if desired

            // Disable automatic following of location
            locationOverlay?.disableFollowLocation()

            // Show a message if only coarse location is available
            if (!PermissionUtils.hasPreciseLocationPermission(requireContext())) {
                Snackbar.make(
                    binding.root,
                    getString(R.string.precise_location_recommended),
                    Snackbar.LENGTH_LONG
                ).show()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error enabling location", e)
            Toast.makeText(
                requireContext(),
                getString(R.string.error_occurred),
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    override fun onResume() {
        super.onResume()
        try {
            // Resume the map
            binding.mapView.onResume()

            // Resume location updates if overlay exists
            locationOverlay?.enableMyLocation()

            Log.d(TAG, "MapFragment resumed")
        } catch (e: Exception) {
            Log.e(TAG, "Error in onResume", e)
        }
    }

    override fun onPause() {
        try {
            // Pause location updates to save battery
            locationOverlay?.disableMyLocation()

            // Pause the map
            binding.mapView.onPause()

            Log.d(TAG, "MapFragment paused")
        } catch (e: Exception) {
            Log.e(TAG, "Error in onPause", e)
        }
        super.onPause()
    }

    override fun onStop() {
        try {
            // Disable location updates when fragment is stopped
            locationOverlay?.disableFollowLocation()
            locationOverlay?.disableMyLocation()

            Log.d(TAG, "MapFragment stopped")
        } catch (e: Exception) {
            Log.e(TAG, "Error in onStop", e)
        }
        super.onStop()
    }

    override fun onDestroyView() {
        try {
            // Clean up location overlay
            locationOverlay?.disableMyLocation()
            locationOverlay = null

            // Clear map overlays
            binding.mapView.overlays.clear()

            Log.d(TAG, "MapFragment destroyed")
        } catch (e: Exception) {
            Log.e(TAG, "Error in onDestroyView", e)
        }
        super.onDestroyView()
        _binding = null
    }
}
