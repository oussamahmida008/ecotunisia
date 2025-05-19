package com.oussama.weatherapp.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import android.util.Log
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream

/**
 * Utility class for image operations
 */
object ImageUtils {

    private const val TAG = "ImageUtils"
    private const val MAX_IMAGE_DIMENSION = 800
    private const val COMPRESSION_QUALITY = 75

    /**
     * Convert an image URI to a Base64 encoded string
     * @param context The context
     * @param imageUri The URI of the image
     * @return Base64 encoded string or null if conversion fails
     */
    fun uriToBase64(context: Context, imageUri: Uri): String? {
        return try {
            // Open input stream from URI
            val inputStream: InputStream? = context.contentResolver.openInputStream(imageUri)
            inputStream?.use { stream ->
                // Decode the input stream to a bitmap
                val originalBitmap = BitmapFactory.decodeStream(stream)
                
                // Compress and resize the bitmap
                val compressedBitmap = compressAndResizeBitmap(originalBitmap)
                
                // Convert bitmap to Base64
                bitmapToBase64(compressedBitmap)
            }
        } catch (e: IOException) {
            Log.e(TAG, "Error converting URI to Base64", e)
            null
        }
    }

    /**
     * Convert a bitmap to a Base64 encoded string
     * @param bitmap The bitmap to convert
     * @return Base64 encoded string
     */
    private fun bitmapToBase64(bitmap: Bitmap): String {
        val byteArrayOutputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, COMPRESSION_QUALITY, byteArrayOutputStream)
        val byteArray = byteArrayOutputStream.toByteArray()
        return Base64.encodeToString(byteArray, Base64.DEFAULT)
    }

    /**
     * Convert a Base64 encoded string to a bitmap
     * @param base64String The Base64 encoded string
     * @return The bitmap or null if conversion fails
     */
    fun base64ToBitmap(base64String: String): Bitmap? {
        return try {
            val decodedBytes = Base64.decode(base64String, Base64.DEFAULT)
            BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
        } catch (e: Exception) {
            Log.e(TAG, "Error converting Base64 to Bitmap", e)
            null
        }
    }

    /**
     * Compress and resize a bitmap to reduce its size
     * @param originalBitmap The original bitmap
     * @return The compressed and resized bitmap
     */
    private fun compressAndResizeBitmap(originalBitmap: Bitmap): Bitmap {
        val width = originalBitmap.width
        val height = originalBitmap.height

        // Check if resizing is needed
        if (width <= MAX_IMAGE_DIMENSION && height <= MAX_IMAGE_DIMENSION) {
            return originalBitmap
        }

        // Calculate new dimensions while maintaining aspect ratio
        val newWidth: Int
        val newHeight: Int

        if (width > height) {
            val ratio = width.toFloat() / height.toFloat()
            newWidth = MAX_IMAGE_DIMENSION
            newHeight = (newWidth / ratio).toInt()
        } else {
            val ratio = height.toFloat() / width.toFloat()
            newHeight = MAX_IMAGE_DIMENSION
            newWidth = (newHeight / ratio).toInt()
        }

        // Create a scaled bitmap
        return Bitmap.createScaledBitmap(originalBitmap, newWidth, newHeight, true)
    }
}
