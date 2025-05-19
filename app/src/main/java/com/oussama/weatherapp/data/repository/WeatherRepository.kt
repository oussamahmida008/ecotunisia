package com.oussama.weatherapp.data.repository

import com.oussama.weatherapp.data.model.Weather
import com.oussama.weatherapp.data.remote.WeatherApiService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Repository for weather data
 */
class WeatherRepository(
    private val weatherApiService: WeatherApiService = WeatherApiService.create()
) {
    
    /**
     * Get current weather for a city
     */
    suspend fun getWeatherForCity(city: String): Result<Weather> = withContext(Dispatchers.IO) {
        try {
            val response = weatherApiService.getCurrentWeather(
                apiKey = WeatherApiService.API_KEY,
                location = city
            )
            
            // Map API response to our domain model
            val weather = Weather(
                city = response.location.name,
                temperature = response.current.temp_c,
                feelsLike = response.current.feelslike_c,
                humidity = response.current.humidity,
                windSpeed = response.current.wind_kph,
                windDirection = response.current.wind_dir,
                condition = response.current.condition.text,
                iconUrl = "https:${response.current.condition.icon}",
                lastUpdated = response.current.last_updated
            )
            
            Result.success(weather)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Get weather for multiple Tunisian cities
     */
    suspend fun getWeatherForTunisianCities(): Result<List<Weather>> = withContext(Dispatchers.IO) {
        try {
            // List of major Tunisian cities
            val cities = listOf("Tunis", "Sfax", "Sousse", "Kairouan", "Bizerte", "Gabes", "Ariana", "Gafsa", "Monastir", "Medenine")
            
            val weatherList = cities.mapNotNull { city ->
                try {
                    val response = weatherApiService.getCurrentWeather(
                        apiKey = WeatherApiService.API_KEY,
                        location = city
                    )
                    
                    Weather(
                        city = response.location.name,
                        temperature = response.current.temp_c,
                        feelsLike = response.current.feelslike_c,
                        humidity = response.current.humidity,
                        windSpeed = response.current.wind_kph,
                        windDirection = response.current.wind_dir,
                        condition = response.current.condition.text,
                        iconUrl = "https:${response.current.condition.icon}",
                        lastUpdated = response.current.last_updated
                    )
                } catch (e: Exception) {
                    null
                }
            }
            
            if (weatherList.isEmpty()) {
                Result.failure(Exception("Failed to fetch weather data for Tunisian cities"))
            } else {
                Result.success(weatherList)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
