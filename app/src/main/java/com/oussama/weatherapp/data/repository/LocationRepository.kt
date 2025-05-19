package com.oussama.weatherapp.data.repository

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.oussama.weatherapp.data.model.Location
import com.oussama.weatherapp.data.model.PredefinedLocations
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.Date

/**
 * Repository for location-related operations
 */
class LocationRepository {
    companion object {
        private const val TAG = "LocationRepository"
    }

    private val firestore = FirebaseFirestore.getInstance()
    private val locationsCollection = firestore.collection("locations")

    /**
     * Add a new location
     */
    suspend fun addLocation(
        title: String,
        description: String,
        latitude: Double,
        longitude: Double,
        userId: String,
        userName: String,
        imageUrl: String? = null
    ): Result<Location> = withContext(Dispatchers.IO) {
        try {
            val location = Location(
                id = locationsCollection.document().id,
                title = title,
                description = description,
                latitude = latitude,
                longitude = longitude,
                userId = userId,
                userName = userName,
                timestamp = Date(),
                imageUrl = imageUrl
            )

            locationsCollection.document(location.id).set(location).await()

            Result.success(location)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get all locations
     */
    suspend fun getAllLocations(): Result<List<Location>> = withContext(Dispatchers.IO) {
        try {
            // Ensure we have a valid Firebase Auth token before querying
            val currentUser = FirebaseAuth.getInstance().currentUser
            if (currentUser == null) {
                Log.e(TAG, "User not authenticated when trying to get all locations")
                return@withContext Result.failure(Exception("User not authenticated"))
            }

            // Force token refresh to ensure we have the latest authentication state
            try {
                currentUser.getIdToken(true).await()
                Log.d(TAG, "Successfully refreshed Firebase auth token")
            } catch (e: Exception) {
                Log.w(TAG, "Failed to refresh token, continuing with existing token: ${e.message}")
            }

            // Now perform the query with fresh authentication
            Log.d(TAG, "Querying all locations")
            val snapshot = locationsCollection
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .await()

            val locations = snapshot.toObjects(Location::class.java)
            Log.d(TAG, "Retrieved ${locations.size} locations")
            Result.success(locations)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting all locations: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Get locations for a specific user
     */
    suspend fun getUserLocations(userId: String): Result<List<Location>> = withContext(Dispatchers.IO) {
        try {
            // Ensure we have a valid Firebase Auth token before querying
            val currentUser = FirebaseAuth.getInstance().currentUser
            if (currentUser == null) {
                Log.e(TAG, "User not authenticated when trying to get user locations")
                return@withContext Result.failure(Exception("User not authenticated"))
            }

            // Force token refresh to ensure we have the latest authentication state
            try {
                currentUser.getIdToken(true).await()
                Log.d(TAG, "Successfully refreshed Firebase auth token")
            } catch (e: Exception) {
                Log.w(TAG, "Failed to refresh token, continuing with existing token: ${e.message}")
            }

            // Verify the requested userId matches the authenticated user
            if (userId != currentUser.uid) {
                Log.w(TAG, "Requested userId ($userId) doesn't match authenticated user (${currentUser.uid})")
            }

            // Now perform the query with fresh authentication
            Log.d(TAG, "Querying locations for userId: $userId")
            val snapshot = locationsCollection
                .whereEqualTo("userId", userId)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .await()

            val locations = snapshot.toObjects(Location::class.java)
            Log.d(TAG, "Retrieved ${locations.size} locations for user $userId")
            Result.success(locations)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting user locations: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Delete a location
     */
    suspend fun deleteLocation(locationId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            locationsCollection.document(locationId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Update a location
     */
    suspend fun updateLocation(location: Location): Result<Location> = withContext(Dispatchers.IO) {
        try {
            locationsCollection.document(location.id).set(location).await()
            Result.success(location)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get a location by ID
     */
    suspend fun getLocationById(locationId: String): Result<Location> = withContext(Dispatchers.IO) {
        try {
            // Ensure we have a valid Firebase Auth token before querying
            val currentUser = FirebaseAuth.getInstance().currentUser
            if (currentUser == null) {
                Log.e(TAG, "User not authenticated when trying to get location by ID")
                return@withContext Result.failure(Exception("User not authenticated"))
            }

            // Force token refresh to ensure we have the latest authentication state
            try {
                currentUser.getIdToken(true).await()
                Log.d(TAG, "Successfully refreshed Firebase auth token")
            } catch (e: Exception) {
                Log.w(TAG, "Failed to refresh token, continuing with existing token: ${e.message}")
            }

            // Now perform the query with fresh authentication
            Log.d(TAG, "Querying location with ID: $locationId")
            val document = locationsCollection.document(locationId).get().await()

            if (document.exists()) {
                val location = document.toObject(Location::class.java)
                if (location != null) {
                    Log.d(TAG, "Retrieved location: ${location.title}")
                    return@withContext Result.success(location)
                }
            }

            Log.e(TAG, "Location with ID $locationId not found")
            Result.failure(Exception("Location not found"))
        } catch (e: Exception) {
            Log.e(TAG, "Error getting location by ID: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Seed predefined eco-friendly locations in Tunisia
     */
    suspend fun seedPredefinedLocations(): Result<List<Location>> = withContext(Dispatchers.IO) {
        try {
            // Ensure we have a valid Firebase Auth token before querying
            val currentUser = FirebaseAuth.getInstance().currentUser
            if (currentUser == null) {
                Log.e(TAG, "User not authenticated when trying to seed locations")
                return@withContext Result.failure(Exception("User not authenticated"))
            }

            // Get predefined locations
            val locations = PredefinedLocations.getTunisianEcoLocations(
                userId = currentUser.uid,
                userName = currentUser.displayName ?: "EcoExplorer Admin"
            )

            // Add each location to Firestore in a batch
            val batch = firestore.batch()

            locations.forEach { location ->
                val docRef = locationsCollection.document(location.id)
                batch.set(docRef, location)
            }

            // Commit the batch
            batch.commit().await()

            Log.d(TAG, "Successfully seeded ${locations.size} predefined locations")
            Result.success(locations)
        } catch (e: Exception) {
            Log.e(TAG, "Error seeding predefined locations: ${e.message}", e)
            Result.failure(e)
        }
    }
}
