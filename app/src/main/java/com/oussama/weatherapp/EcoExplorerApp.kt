package com.oussama.weatherapp

import android.app.Application
import android.content.Context
import android.content.res.Configuration
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.oussama.weatherapp.utils.LocaleHelper
import com.oussama.weatherapp.utils.MapUtils
import java.util.Locale

class EcoExplorerApp : Application() {

    override fun onCreate() {
        super.onCreate()

        try {
            // Initialize Firebase
            if (FirebaseApp.getApps(this).isEmpty()) {
                android.util.Log.d("EcoExplorerApp", "Initializing Firebase from scratch")

                // First try to initialize using the google-services.json file
                try {
                    FirebaseApp.initializeApp(this)
                    android.util.Log.d("EcoExplorerApp", "Firebase initialized from google-services.json")
                } catch (e: Exception) {
                    android.util.Log.w("EcoExplorerApp", "Failed to initialize Firebase from google-services.json, using explicit options", e)

                    // If that fails, use explicit options
                    val options = com.google.firebase.FirebaseOptions.Builder()
                        .setApplicationId("1:359104886772:android:bc91351b670b3efdf1eba1")
                        .setApiKey("AIzaSyCWHTr_jlgQgvSx0fwcR_XNtnTHFzixtcY")
                        .setProjectId("myweathermapapp")
                        .setDatabaseUrl("https://myweathermapapp.firebaseio.com")
                        .setStorageBucket("myweathermapapp.appspot.com")
                        .setGcmSenderId("359104886772")
                        .build()

                    FirebaseApp.initializeApp(this, options)
                    android.util.Log.d("EcoExplorerApp", "Firebase initialized with explicit options")
                }
            } else {
                android.util.Log.d("EcoExplorerApp", "Firebase already initialized, getting instance")
                FirebaseApp.getInstance()
            }

            // Configure Firebase Auth
            val auth = FirebaseAuth.getInstance()

            // Disable app verification for testing (remove in production)
            auth.firebaseAuthSettings.setAppVerificationDisabledForTesting(true)

            // Check if user is already signed in
            val currentUser = auth.currentUser
            if (currentUser != null) {
                android.util.Log.d("EcoExplorerApp", "User already signed in: ${currentUser.uid}")
            } else {
                android.util.Log.d("EcoExplorerApp", "No user currently signed in")
            }

            android.util.Log.d("EcoExplorerApp", "Firebase initialized successfully")

        } catch (e: Exception) {
            android.util.Log.e("EcoExplorerApp", "Firebase initialization failed", e)
            e.printStackTrace()
        }

        try {
            // Initialize OSMDroid map configuration
            MapUtils.initialize(this)
            android.util.Log.d("EcoExplorerApp", "Map utils initialized successfully")
        } catch (e: Exception) {
            android.util.Log.e("EcoExplorerApp", "Map utils initialization failed", e)
            e.printStackTrace()
        }

        val savedLanguage = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
            .getString("language", Locale.getDefault().language)
        savedLanguage?.let {
            LocaleHelper.setLocale(this, it)
        }
    }

    override fun attachBaseContext(base: Context) {
        val language = base.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
            .getString("language", Locale.getDefault().language) ?: Locale.getDefault().language

        super.attachBaseContext(LocaleHelper.updateBaseContextLocale(base, language))
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        val language = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
            .getString("language", Locale.getDefault().language) ?: Locale.getDefault().language
        LocaleHelper.setLocale(this, language)
    }
}
