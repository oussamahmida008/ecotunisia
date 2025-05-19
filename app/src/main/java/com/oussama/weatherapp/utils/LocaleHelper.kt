package com.oussama.weatherapp.utils

import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import android.os.Build
import java.util.Locale

/**
 * Helper class for changing the app's locale dynamically
 */
object LocaleHelper {
    
    /**
     * Update the base context locale
     */
    fun updateBaseContextLocale(context: Context, language: String): Context {
        val locale = Locale(language)
        Locale.setDefault(locale)
        
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            updateResourcesLocale(context, locale)
        } else {
            updateResourcesLocaleLegacy(context, locale)
        }
    }
    
    /**
     * Set locale for Android N and above
     */
    private fun updateResourcesLocale(context: Context, locale: Locale): Context {
        val configuration = Configuration(context.resources.configuration)
        configuration.setLocale(locale)
        return context.createConfigurationContext(configuration)
    }
    
    /**
     * Set locale for Android M and below
     */
    @Suppress("DEPRECATION")
    private fun updateResourcesLocaleLegacy(context: Context, locale: Locale): Context {
        val resources = context.resources
        val configuration = resources.configuration
        configuration.locale = locale
        resources.updateConfiguration(configuration, resources.displayMetrics)
        return context
    }
    
    /**
     * Set the app's locale
     */
    fun setLocale(context: Context, language: String) {
        // Save the selected language to preferences
        context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
            .edit()
            .putString("language", language)
            .apply()
        
        // Update the locale
        updateConfiguration(context, language)
    }
    
    /**
     * Update the configuration with the new locale
     */
    private fun updateConfiguration(context: Context, language: String) {
        val locale = Locale(language)
        Locale.setDefault(locale)
        
        val resources: Resources = context.resources
        val configuration: Configuration = resources.configuration
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            configuration.setLocale(locale)
        } else {
            @Suppress("DEPRECATION")
            configuration.locale = locale
        }
        
        @Suppress("DEPRECATION")
        resources.updateConfiguration(configuration, resources.displayMetrics)
    }
}
