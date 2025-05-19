package com.oussama.weatherapp.data.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * Data class representing weather information from WeatherAPI
 */
@Parcelize
data class Weather(
    val city: String = "",
    val temperature: Double = 0.0,
    val feelsLike: Double = 0.0,
    val humidity: Int = 0,
    val windSpeed: Double = 0.0,
    val windDirection: String = "",
    val condition: String = "",
    val iconUrl: String = "",
    val lastUpdated: String = ""
) : Parcelable

/**
 * Data class for WeatherAPI response
 */
data class WeatherResponse(
    val location: WeatherLocation,
    val current: CurrentWeather
)

data class WeatherLocation(
    val name: String,
    val region: String,
    val country: String,
    val lat: Double,
    val lon: Double,
    val localtime: String
)

data class CurrentWeather(
    val temp_c: Double,
    val temp_f: Double,
    val is_day: Int,
    val condition: WeatherCondition,
    val wind_mph: Double,
    val wind_kph: Double,
    val wind_dir: String,
    val pressure_mb: Double,
    val pressure_in: Double,
    val precip_mm: Double,
    val precip_in: Double,
    val humidity: Int,
    val cloud: Int,
    val feelslike_c: Double,
    val feelslike_f: Double,
    val vis_km: Double,
    val vis_miles: Double,
    val uv: Double,
    val gust_mph: Double,
    val gust_kph: Double,
    val last_updated: String
)

data class WeatherCondition(
    val text: String,
    val icon: String,
    val code: Int
)
