package com.oussama.weatherapp.data.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import java.util.Date

/**
 * Data class representing a message in the chat
 */
@Parcelize
data class Message(
    val id: String = "",
    val text: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val channelId: String = "",
    val timestamp: Date = Date(),
    val latitude: Double? = null,
    val longitude: Double? = null,
    val imageUrl: String? = null,
    val imageBase64: String? = null,
    val hasImage: Boolean = false
) : Parcelable
