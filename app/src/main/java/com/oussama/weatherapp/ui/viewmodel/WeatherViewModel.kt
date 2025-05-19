package com.oussama.weatherapp.ui.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.oussama.weatherapp.data.model.Weather
import com.oussama.weatherapp.data.repository.WeatherRepository
import kotlinx.coroutines.launch

/**
 * ViewModel for weather screens
 */
class WeatherViewModel : ViewModel() {
    
    private val weatherRepository = WeatherRepository()
    
    private val _weatherList = MutableLiveData<List<Weather>>()
    val weatherList: LiveData<List<Weather>> = _weatherList
    
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading
    
    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error
    
    /**
     * Load weather data for Tunisian cities
     */
    fun loadWeatherData() {
        _isLoading.value = true
        _error.value = null
        
        viewModelScope.launch {
            val result = weatherRepository.getWeatherForTunisianCities()
            
            if (result.isSuccess) {
                _weatherList.value = result.getOrDefault(emptyList())
            } else {
                _error.value = result.exceptionOrNull()?.message ?: "Unknown error"
            }
            
            _isLoading.value = false
        }
    }
    
    /**
     * Get weather for a specific city
     */
    fun getWeatherForCity(city: String) {
        _isLoading.value = true
        _error.value = null
        
        viewModelScope.launch {
            val result = weatherRepository.getWeatherForCity(city)
            
            if (result.isSuccess) {
                val weather = result.getOrNull()
                weather?.let {
                    val currentList = _weatherList.value?.toMutableList() ?: mutableListOf()
                    
                    // Replace if city already exists in the list, otherwise add
                    val index = currentList.indexOfFirst { it.city == city }
                    if (index != -1) {
                        currentList[index] = it
                    } else {
                        currentList.add(it)
                    }
                    
                    _weatherList.value = currentList
                }
            } else {
                _error.value = result.exceptionOrNull()?.message ?: "Unknown error"
            }
            
            _isLoading.value = false
        }
    }
    
    /**
     * Clear any error
     */
    fun clearError() {
        _error.value = null
    }
}
