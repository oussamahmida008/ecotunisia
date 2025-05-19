package com.oussama.weatherapp.utils

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.util.Log
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.common.GooglePlayServicesUtil
import com.google.android.gms.security.ProviderInstaller

/**
 * Helper class to check Google Play Services availability
 */
object GooglePlayServicesHelper {
    private const val TAG = "GooglePlayServices"
    private const val PLAY_SERVICES_RESOLUTION_REQUEST = 9000
    private const val ERROR_DIALOG_REQUEST_CODE = 1

    /**
     * Check if Google Play Services is available and up to date
     * @param context The context to use for checking
     * @return true if Google Play Services is available and up to date
     */
    fun isGooglePlayServicesAvailable(context: Context): Boolean {
        val apiAvailability = GoogleApiAvailability.getInstance()
        val resultCode = apiAvailability.isGooglePlayServicesAvailable(context)

        if (resultCode != ConnectionResult.SUCCESS) {
            if (apiAvailability.isUserResolvableError(resultCode) && context is Activity) {
                apiAvailability.getErrorDialog(context, resultCode, PLAY_SERVICES_RESOLUTION_REQUEST)?.show()
            } else {
                Log.e(TAG, "This device is not supported for Google Play Services: $resultCode")
            }
            return false
        }

        return true
    }

    /**
     * Update Android security provider to protect against SSL exploits
     * @param activity The activity to use for updating
     */
    fun updateAndroidSecurityProvider(activity: Activity) {
        try {
            ProviderInstaller.installIfNeeded(activity)
            Log.d(TAG, "Security Provider updated successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error updating security provider", e)
            // Google Play services is unavailable, show error dialog
            val apiAvailability = GoogleApiAvailability.getInstance()
            val resultCode = apiAvailability.isGooglePlayServicesAvailable(activity)
            if (apiAvailability.isUserResolvableError(resultCode)) {
                apiAvailability.getErrorDialog(activity, resultCode, ERROR_DIALOG_REQUEST_CODE)?.show()
            }
        }
    }

    /**
     * Check if Google Play Services is installed and enabled
     * @param context The context to use for checking
     * @return true if Google Play Services is installed and enabled
     */
    fun isGooglePlayServicesInstalled(context: Context): Boolean {
        val pm = context.packageManager
        try {
            pm.getPackageInfo("com.google.android.gms", 0)
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Google Play Services not installed", e)
            return false
        }
    }

    /**
     * Try to fix Google Play Services issues
     * @param activity The activity to use for fixing
     * @return true if issues were fixed or no issues exist
     */
    fun fixGooglePlayServicesIssues(activity: Activity): Boolean {
        val apiAvailability = GoogleApiAvailability.getInstance()
        val resultCode = apiAvailability.isGooglePlayServicesAvailable(activity)

        if (resultCode == ConnectionResult.SUCCESS) {
            return true
        }

        if (apiAvailability.isUserResolvableError(resultCode)) {
            apiAvailability.showErrorDialogFragment(activity, resultCode, PLAY_SERVICES_RESOLUTION_REQUEST) {
                // Dialog dismissed
                Log.d(TAG, "Google Play Services dialog dismissed")
            }
            return false
        }

        Log.e(TAG, "This device is not supported for Google Play Services and the error is not resolvable")
        return false
    }
}
