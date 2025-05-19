package com.oussama.weatherapp.data.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import java.util.Date

/**
 * Data class representing a user in the application
 */
@Parcelize
data class User(
    val id: String = "",
    val email: String = "",
    val name: String = "",
    val language: String = "en",
    val registrationDate: Date = Date(),
    val photoUrl: String? = null
) : Parcelable
