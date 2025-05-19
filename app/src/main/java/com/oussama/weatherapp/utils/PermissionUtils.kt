package com.oussama.weatherapp.utils

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.location.LocationManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.activity.result.IntentSenderRequest
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationSettingsRequest
import com.google.android.gms.location.LocationSettingsStatusCodes
import com.google.android.gms.location.Priority
import com.google.android.gms.location.SettingsClient
import com.oussama.weatherapp.R

/**
 * Utility class for handling runtime permissions and location settings
 */
object PermissionUtils {
    private const val TAG = "PermissionUtils"

    /**
     * Check if location permissions are granted
     */
    fun hasLocationPermissions(context: Context): Boolean {
        val fineLocationGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val coarseLocationGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        return fineLocationGranted || coarseLocationGranted
    }

    /**
     * Check if precise location permission is granted
     */
    fun hasPreciseLocationPermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Check if location services are enabled
     */
    fun isLocationEnabled(context: Context): Boolean {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }

    /**
     * Show dialog explaining why the permission is needed
     */
    fun showPermissionRationaleDialog(
        fragment: Fragment,
        title: String,
        message: String,
        onPositiveClick: () -> Unit,
        onNegativeClick: () -> Unit
    ) {
        AlertDialog.Builder(fragment.requireContext())
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton(R.string.ok) { _, _ -> onPositiveClick() }
            .setNegativeButton(R.string.cancel) { _, _ -> onNegativeClick() }
            .create()
            .show()
    }

    /**
     * Show dialog when permission is permanently denied
     */
    fun showPermissionDeniedDialog(
        fragment: Fragment,
        title: String,
        message: String
    ) {
        AlertDialog.Builder(fragment.requireContext())
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton(R.string.open_settings) { _, _ ->
                // Open app settings
                openAppSettings(fragment)
            }
            .setNegativeButton(R.string.cancel, null)
            .create()
            .show()
    }

    /**
     * Open application settings
     */
    private fun openAppSettings(fragment: Fragment) {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", fragment.requireContext().packageName, null)
        }
        fragment.startActivity(intent)
    }

    /**
     * Check and request location settings
     */
    fun checkLocationSettings(
        fragment: Fragment,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit,
        locationSettingsLauncher: (IntentSenderRequest) -> Unit
    ) {
        // Create location request with high accuracy
        val locationRequest = LocationRequest.Builder(10000) // Update interval in milliseconds
            .setPriority(Priority.PRIORITY_HIGH_ACCURACY)
            .setWaitForAccurateLocation(false)
            .setMinUpdateIntervalMillis(5000)
            .setMaxUpdateDelayMillis(10000)
            .build()

        val builder = LocationSettingsRequest.Builder()
            .addLocationRequest(locationRequest)
            .setAlwaysShow(true)

        // Get the SettingsClient
        val client: SettingsClient = LocationServices.getSettingsClient(fragment.requireContext())
        // Check if the current location settings are satisfied
        val task = client.checkLocationSettings(builder.build())

        task.addOnSuccessListener {
            // Location settings are satisfied
            onSuccess()
        }

        task.addOnFailureListener { exception ->
            when {
                // Check if the exception is resolvable
                exception is ResolvableApiException -> {
                    try {
                        // Show the dialog by calling startResolutionForResult()
                        val intentSenderRequest = IntentSenderRequest.Builder(exception.resolution).build()
                        locationSettingsLauncher(intentSenderRequest)
                    } catch (sendEx: IntentSender.SendIntentException) {
                        // Log the error
                        Log.e(TAG, "Error showing location settings resolution dialog", sendEx)
                        onFailure(sendEx)
                    }
                }
                // Check if it's a settings change unavailable exception
                exception is ApiException &&
                exception.statusCode == LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE -> {
                    // Location settings are not satisfied, but we have no way to fix it
                    Log.e(TAG, "Location settings are inadequate, and cannot be fixed here. Check your device settings.")
                    onFailure(exception)
                }
                // Handle other exceptions
                else -> {
                    Log.e(TAG, "Unknown error checking location settings", exception)
                    onFailure(exception)
                }
            }
        }
    }
}
