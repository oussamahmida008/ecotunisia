package com.oussama.weatherapp.ui.main

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.firebase.auth.FirebaseAuth
import com.oussama.weatherapp.R
import com.oussama.weatherapp.databinding.ActivityMainBinding
import com.oussama.weatherapp.ui.auth.AuthActivity
import com.oussama.weatherapp.utils.GooglePlayServicesHelper

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Update Android security provider
        GooglePlayServicesHelper.updateAndroidSecurityProvider(this)

        // Check if Google Play Services is installed
        if (!GooglePlayServicesHelper.isGooglePlayServicesInstalled(this)) {
            Toast.makeText(this, getString(R.string.error_google_play_services), Toast.LENGTH_LONG).show()
            // Try to fix Google Play Services issues
            if (!GooglePlayServicesHelper.fixGooglePlayServicesIssues(this)) {
                // If issues can't be fixed, finish the activity
                finish()
                return
            }
        }

        // Check Google Play Services availability
        if (!GooglePlayServicesHelper.isGooglePlayServicesAvailable(this)) {
            Toast.makeText(this, getString(R.string.error_google_play_services), Toast.LENGTH_LONG).show()
            // Try to fix Google Play Services issues
            if (!GooglePlayServicesHelper.fixGooglePlayServicesIssues(this)) {
                // If issues can't be fixed, finish the activity
                finish()
                return
            }
        }

        try {
            // Initialize Firebase Auth
            auth = FirebaseAuth.getInstance()

            // Check if user is signed in
            if (auth.currentUser == null) {
                // Not signed in, launch the Auth activity
                startActivity(Intent(this, AuthActivity::class.java))
                finish()
                return
            }
        } catch (e: Exception) {
            // Handle Firebase initialization error
            Toast.makeText(this, "Authentication service unavailable", Toast.LENGTH_LONG).show()
            e.printStackTrace()
            finish()
            return
        }

        // Set up Navigation
        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController

        // Connect the bottom navigation view with the navigation controller
        binding.bottomNavigation.setupWithNavController(navController)
    }

    /**
     * Check if Google Play Services is available and up to date
     */
    private fun checkPlayServices(): Boolean {
        return GooglePlayServicesHelper.isGooglePlayServicesAvailable(this)
    }

    override fun onStart() {
        super.onStart()
        // Check if user is signed in when activity starts
        if (auth.currentUser == null) {
            // Not signed in, launch the Auth activity
            startActivity(Intent(this, AuthActivity::class.java))
            finish()
        }
    }
}
