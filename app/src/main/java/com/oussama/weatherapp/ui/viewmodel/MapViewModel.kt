package com.oussama.weatherapp.ui.viewmodel

import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.oussama.weatherapp.R
import com.oussama.weatherapp.data.model.Location
import com.oussama.weatherapp.data.repository.LocationRepository
import com.oussama.weatherapp.data.repository.UserRepository
import kotlinx.coroutines.launch

/**
 * ViewModel for map screens
 */
class MapViewModel : ViewModel() {
    companion object {
        private const val TAG = "MapViewModel"
    }

    private val locationRepository = LocationRepository()
    private val userRepository = UserRepository()

    private val _locations = MutableLiveData<List<Location>>()
    val locations: LiveData<List<Location>> = _locations

    private val _userLocations = MutableLiveData<List<Location>>()
    val userLocations: LiveData<List<Location>> = _userLocations

    private val _selectedLocation = MutableLiveData<Location?>()
    val selectedLocation: LiveData<Location?> = _selectedLocation

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    private val _addLocationResult = MutableLiveData<Result<Location>?>()
    val addLocationResult: LiveData<Result<Location>?> = _addLocationResult

    private val _locationDetails = MutableLiveData<Result<Location>?>()
    val locationDetails: LiveData<Result<Location>?> = _locationDetails

    /**
     * Load all locations
     */
    fun loadAllLocations() {
        // Check if user is authenticated
        val currentUser = userRepository.getCurrentFirebaseUser()
        if (currentUser == null) {
            _error.value = "You must be logged in to view locations"
            return
        }

        _isLoading.value = true
        _error.value = null

        viewModelScope.launch {
            val result = locationRepository.getAllLocations()

            if (result.isSuccess) {
                _locations.value = result.getOrDefault(emptyList())
            } else {
                val errorMessage = result.exceptionOrNull()?.message ?: "Unknown error"
                _error.value = "Failed to load locations: $errorMessage"

                // If it's an authentication error, try to re-authenticate
                if (errorMessage.contains("permission") || errorMessage.contains("auth")) {
                    // Notify the user to log in again
                    _error.value = "Authentication error. Please log out and log in again."
                }
            }

            _isLoading.value = false
        }
    }

    /**
     * Load locations for the current user
     */
    fun loadUserLocations() {
        val currentUser = userRepository.getCurrentFirebaseUser()

        if (currentUser == null) {
            _error.value = "You must be logged in to view your locations"
            return
        }

        _isLoading.value = true
        _error.value = null

        viewModelScope.launch {
            val result = locationRepository.getUserLocations(currentUser.uid)

            if (result.isSuccess) {
                _userLocations.value = result.getOrDefault(emptyList())
            } else {
                val exception = result.exceptionOrNull()
                val errorMessage = exception?.message ?: "Unknown error"

                // Check if it's an index error
                if (errorMessage.contains("requires an index") ||
                    errorMessage.contains("FAILED_PRECONDITION") && errorMessage.contains("index")) {

                    // Show a user-friendly message for index errors
                    _error.value = R.string.error_index_required.toString()

                    // Schedule a retry after a delay
                    Handler(Looper.getMainLooper()).postDelayed({
                        loadUserLocations()
                    }, 10000) // Retry after 10 seconds

                    Log.d(TAG, "Index error detected, scheduled retry: $errorMessage")
                }
                // Check if it's an authentication error
                else if (errorMessage.contains("permission") || errorMessage.contains("auth")) {
                    // Notify the user to log in again
                    _error.value = "Authentication error. Please log out and log in again."
                }
                else {
                    _error.value = "Failed to load your locations: $errorMessage"
                }
            }

            _isLoading.value = false
        }
    }

    /**
     * Add a new location
     */
    fun addLocation(title: String, description: String, latitude: Double, longitude: Double, imageUrl: String? = null) {
        val currentUser = userRepository.getCurrentFirebaseUser() ?: return

        _isLoading.value = true
        _error.value = null

        viewModelScope.launch {
            val result = locationRepository.addLocation(
                title = title,
                description = description,
                latitude = latitude,
                longitude = longitude,
                userId = currentUser.uid,
                userName = currentUser.displayName ?: "Unknown User",
                imageUrl = imageUrl
            )

            _addLocationResult.value = result

            if (result.isSuccess) {
                // Refresh locations
                loadAllLocations()
                loadUserLocations()
            } else {
                _error.value = result.exceptionOrNull()?.message ?: "Unknown error"
            }

            _isLoading.value = false
        }
    }

    /**
     * Delete a location
     */
    fun deleteLocation(locationId: String) {
        _isLoading.value = true
        _error.value = null

        viewModelScope.launch {
            val result = locationRepository.deleteLocation(locationId)

            if (result.isSuccess) {
                // Refresh locations
                loadAllLocations()
                loadUserLocations()
            } else {
                _error.value = result.exceptionOrNull()?.message ?: "Unknown error"
            }

            _isLoading.value = false
        }
    }

    /**
     * Select a location
     */
    fun selectLocation(location: Location?) {
        _selectedLocation.value = location
    }

    /**
     * Clear any error
     */
    fun clearError() {
        _error.value = null
    }

    /**
     * Reset add location result
     */
    fun resetAddLocationResult() {
        _addLocationResult.value = null
    }

    /**
     * Load location details by ID
     */
    fun loadLocationDetails(locationId: String) {
        _isLoading.value = true
        _error.value = null
        _locationDetails.value = null

        viewModelScope.launch {
            try {
                // First try to find the location in the already loaded locations
                val allLocations = _locations.value ?: emptyList()
                val userLocations = _userLocations.value ?: emptyList()
                val combinedLocations = allLocations + userLocations

                val location = combinedLocations.find { it.id == locationId }

                if (location != null) {
                    _locationDetails.value = Result.success(location)
                    _isLoading.value = false
                    return@launch
                }

                // If not found in memory, fetch from repository
                val result = locationRepository.getLocationById(locationId)
                _locationDetails.value = result

                if (result.isFailure) {
                    val errorMessage = result.exceptionOrNull()?.message ?: "Unknown error"
                    _error.value = "Failed to load location details: $errorMessage"
                }
            } catch (e: Exception) {
                _locationDetails.value = Result.failure(e)
                _error.value = "Failed to load location details: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Get the current Firebase user
     */
    fun getCurrentUser() = userRepository.getCurrentFirebaseUser()

    /**
     * Seed predefined eco-friendly locations in Tunisia
     */
    fun seedPredefinedLocations() {
        val currentUser = userRepository.getCurrentFirebaseUser()
        if (currentUser == null) {
            _error.value = "You must be logged in to seed locations"
            return
        }

        _isLoading.value = true
        _error.value = null

        viewModelScope.launch {
            val result = locationRepository.seedPredefinedLocations()

            if (result.isSuccess) {
                // Refresh locations
                loadAllLocations()
                _error.value = "Successfully added ${result.getOrNull()?.size ?: 0} eco-friendly locations in Tunisia"
            } else {
                _error.value = "Failed to seed locations: ${result.exceptionOrNull()?.message ?: "Unknown error"}"
            }

            _isLoading.value = false
        }
    }
}
