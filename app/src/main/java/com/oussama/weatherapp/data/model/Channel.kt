package com.oussama.weatherapp.data.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import java.util.Date

/**
 * Data class representing a chat channel
 */
@Parcelize
data class Channel(
    val id: String = "",
    val name: String = "",
    val description: String = "",
    val creatorId: String = "",
    val creatorName: String = "",
    val timestamp: Date = Date(),
    val memberCount: Int = 0
) : Parcelable
