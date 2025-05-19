package com.oussama.weatherapp.data.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import java.util.Date

/**
 * Data class representing a location in the application
 */
@Parcelize
data class Location(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val userId: String = "",
    val userName: String = "",
    val timestamp: Date = Date(),
    val imageUrl: String? = null
) : Parcelable
