package com.oussama.weatherapp.utils

import android.content.Context
import android.util.Log
import com.google.firebase.FirebaseException
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.oussama.weatherapp.R

/**
 * Helper class to handle Firebase authentication errors
 */
object FirebaseErrorHandler {
    private const val TAG = "FirebaseErrorHandler"

    /**
     * Get a user-friendly error message from a Firebase exception
     */
    fun getErrorMessage(context: Context, throwable: Throwable): String {
        Log.e(TAG, "Firebase error: ${throwable.message}", throwable)

        return when (throwable) {
            is FirebaseAuthWeakPasswordException -> {
                context.getString(R.string.error_weak_password)
            }
            is FirebaseAuthInvalidCredentialsException -> {
                context.getString(R.string.error_invalid_credentials)
            }
            is FirebaseAuthInvalidUserException -> {
                context.getString(R.string.error_user_not_found)
            }
            is FirebaseAuthUserCollisionException -> {
                context.getString(R.string.error_email_already_in_use)
            }
            is FirebaseNetworkException -> {
                context.getString(R.string.error_network)
            }
            is FirebaseException -> {
                throwable.message ?: context.getString(R.string.error_unknown)
            }
            else -> {
                throwable.message ?: context.getString(R.string.error_unknown)
            }
        }
    }
}
