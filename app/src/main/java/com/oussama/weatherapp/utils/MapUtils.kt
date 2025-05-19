package com.oussama.weatherapp.utils

import android.content.Context
import android.os.Environment
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.compass.CompassOverlay
import org.osmdroid.views.overlay.compass.InternalCompassOrientationProvider
import org.osmdroid.views.overlay.gestures.RotationGestureOverlay
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay
import java.io.File

/**
 * Utility class for OSMDroid map configuration and setup
 */
object MapUtils {

    /**
     * Initialize OSMDroid configuration
     */
    fun initialize(context: Context) {
        try {
            // Get the configuration instance once to avoid multiple calls
            val configuration = Configuration.getInstance()

            // Set user agent to app package name with version
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            val userAgent = "EcoExplorer/${packageInfo.versionName} (${context.packageName})"
            configuration.userAgentValue = userAgent

            // Use internal cache directory instead of external storage
            val osmCacheDir = File(context.cacheDir, "osmdroid")
            if (!osmCacheDir.exists()) {
                osmCacheDir.mkdirs()
            }
            configuration.osmdroidTileCache = osmCacheDir

            // Use app-specific directory for base path
            val basePath = context.getDir("osmdroid", Context.MODE_PRIVATE)
            configuration.osmdroidBasePath = basePath

            // Set HTTP headers for OSM policy compliance
            try {
                // Set the user agent (most important header)
                configuration.userAgentValue = userAgent

                // Note: We're not setting additionalHttpRequestProperties directly
                // as it's causing compilation issues. The user agent is the most
                // important header for OSM policy compliance.
            } catch (e: Exception) {
                android.util.Log.e("MapUtils", "Error setting HTTP headers", e)
            }

            // Set tile download threads
            configuration.tileDownloadThreads = 2

            // Set tile download policy
            configuration.isMapViewRecyclerFriendly = true

            // Enable hardware acceleration
            configuration.isMapViewHardwareAccelerated = true

            android.util.Log.d("MapUtils", "OSMDroid initialized successfully with cache at ${osmCacheDir.absolutePath}")
        } catch (e: Exception) {
            android.util.Log.e("MapUtils", "Error initializing OSMDroid", e)
        }
    }

    /**
     * Configure a MapView with default settings
     */
    fun configureMapView(mapView: MapView, context: Context) {
        try {
            // Set tile source to OpenStreetMap
            mapView.setTileSource(TileSourceFactory.MAPNIK)

            // Enable multi-touch controls (pinch to zoom)
            mapView.setMultiTouchControls(true)

            // Set zoom levels
            mapView.minZoomLevel = 4.0
            mapView.maxZoomLevel = 19.0

            // Set initial zoom level
            mapView.controller.setZoom(10.0)

            // Enable hardware acceleration
            mapView.setHasTransientState(true)

            // Add compass overlay
            val compassOverlay = CompassOverlay(
                context,
                InternalCompassOrientationProvider(context),
                mapView
            )
            compassOverlay.enableCompass()
            mapView.overlays.add(compassOverlay)

            // Add rotation gesture overlay
            val rotationGestureOverlay = RotationGestureOverlay(mapView)
            rotationGestureOverlay.isEnabled = true
            mapView.overlays.add(rotationGestureOverlay)

            // Set tile loading mode
            mapView.isTilesScaledToDpi = true

            // Enable built-in zoom controls
            mapView.zoomController.setVisibility(org.osmdroid.views.CustomZoomButtonsController.Visibility.SHOW_AND_FADEOUT)

            android.util.Log.d("MapUtils", "MapView configured successfully")
        } catch (e: Exception) {
            android.util.Log.e("MapUtils", "Error configuring MapView", e)
        }
    }

    /**
     * Add user location overlay to the map
     */
    fun addLocationOverlay(mapView: MapView, context: Context): MyLocationNewOverlay? {
        try {
            // Create a location provider with appropriate settings
            val locationProvider = GpsMyLocationProvider(context).apply {
                // Set location update parameters
                locationUpdateMinTime = 1000 // 1 second
                locationUpdateMinDistance = 10f // 10 meters
                // Note: GpsMyLocationProvider doesn't have an addProvider method
                // It uses the default Android location providers
            }

            // Create the location overlay
            val locationOverlay = MyLocationNewOverlay(locationProvider, mapView)

            // Configure the overlay
            locationOverlay.enableMyLocation() // Enable location tracking
            // Don't enable follow mode by default as it overrides our map center
            // locationOverlay.enableFollowLocation()
            locationOverlay.isDrawAccuracyEnabled = true // Show accuracy circle

            // Set person icon
            // locationOverlay.setPersonIcon(...)

            // Add the overlay to the map
            mapView.overlays.add(locationOverlay)

            android.util.Log.d("MapUtils", "Location overlay added successfully")

            return locationOverlay
        } catch (e: Exception) {
            android.util.Log.e("MapUtils", "Error adding location overlay", e)
            return null
        }
    }

    /**
     * Center map on a specific location
     */
    fun centerMapOn(mapView: MapView, latitude: Double, longitude: Double, zoomLevel: Double = 13.0) {
        val geoPoint = GeoPoint(latitude, longitude)
        mapView.controller.setCenter(geoPoint)
        mapView.controller.setZoom(zoomLevel)
    }

    /**
     * Center map on Tunisia
     */
    fun centerMapOnTunisia(mapView: MapView) {
        // Center on Tunis, Tunisia
        centerMapOn(mapView, 36.8065, 10.1815, 7.0)
    }
}
