package com.oussama.weatherapp.ui.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.oussama.weatherapp.data.model.User
import com.oussama.weatherapp.data.repository.UserRepository
import kotlinx.coroutines.launch
import kotlin.Result

/**
 * ViewModel for authentication screens
 */
class AuthViewModel : ViewModel() {

    private val userRepository = UserRepository()

    private val _loginResult = MutableLiveData<Result<User>>()
    val loginResult: LiveData<Result<User>> = _loginResult

    private val _registerResult = MutableLiveData<Result<User>>()
    val registerResult: LiveData<Result<User>> = _registerResult

    private val _currentUser = MutableLiveData<User?>()
    val currentUser: LiveData<User?> = _currentUser

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    init {
        checkCurrentUser()
    }

    /**
     * Login with email and password
     */
    fun login(email: String, password: String) {
        _isLoading.value = true
        viewModelScope.launch {
            val result = userRepository.loginUser(email, password)
            _loginResult.value = result
            _isLoading.value = false
        }
    }

    /**
     * Register a new user
     */
    fun register(email: String, password: String, name: String) {
        _isLoading.value = true
        viewModelScope.launch {
            val result = userRepository.registerUser(email, password, name)
            _registerResult.value = result
            _isLoading.value = false
        }
    }

    /**
     * Check if user is already logged in
     */
    fun checkCurrentUser() {
        viewModelScope.launch {
            val result = userRepository.getCurrentUser()
            if (result.isSuccess) {
                _currentUser.value = result.getOrNull()
            }
        }
    }

    /**
     * Logout the current user
     */
    fun logout() {
        userRepository.logout()
        _currentUser.value = null
    }

    /**
     * Check if user is logged in
     */
    fun isUserLoggedIn(): Boolean {
        return userRepository.isUserLoggedIn()
    }
}
