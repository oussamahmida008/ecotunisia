package com.oussama.weatherapp.ui.viewmodel

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.oussama.weatherapp.data.model.User
import com.oussama.weatherapp.data.repository.UserRepository
import com.oussama.weatherapp.utils.LocaleHelper
import kotlinx.coroutines.launch

/**
 * ViewModel for profile screens
 */
class ProfileViewModel : ViewModel() {

    private val userRepository = UserRepository()

    private val _user = MutableLiveData<User?>()
    val user: LiveData<User?> = _user

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    private val _updateProfileResult = MutableLiveData<Result<User>?>()
    val updateProfileResult: LiveData<Result<User>?> = _updateProfileResult

    /**
     * Load current user profile
     */
    fun loadUserProfile() {
        _isLoading.value = true
        _error.value = null

        viewModelScope.launch {
            val result = userRepository.getCurrentUser()

            if (result.isSuccess) {
                _user.value = result.getOrNull()
            } else {
                _error.value = result.exceptionOrNull()?.message ?: "Unknown error"
            }

            _isLoading.value = false
        }
    }

    /**
     * Update user profile
     */
    fun updateProfile(name: String, language: String, photoUrl: String? = null) {
        _isLoading.value = true
        _error.value = null

        viewModelScope.launch {
            val result = userRepository.updateUserProfile(name, language, photoUrl)

            _updateProfileResult.value = result

            if (result.isSuccess) {
                _user.value = result.getOrNull()
            } else {
                _error.value = result.exceptionOrNull()?.message ?: "Unknown error"
            }

            _isLoading.value = false
        }
    }

    /**
     * Change app language
     */
    fun changeLanguage(context: Context, language: String) {
        LocaleHelper.setLocale(context, language)

        // Update user profile with new language preference
        _user.value?.let { currentUser ->
            if (currentUser.language != language) {
                updateProfile(currentUser.name, language)
            }
        }
    }

    /**
     * Logout the current user
     */
    fun logout() {
        userRepository.logout()
        _user.value = null
    }

    /**
     * Clear any error
     */
    fun clearError() {
        _error.value = null
    }

    /**
     * Reset update profile result
     */
    fun resetUpdateProfileResult() {
        _updateProfileResult.value = null
    }
}
