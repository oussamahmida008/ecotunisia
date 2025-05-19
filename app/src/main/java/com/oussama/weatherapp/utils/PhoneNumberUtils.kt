package com.oussama.weatherapp.utils

import android.app.AlertDialog
import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.util.Log
import android.widget.Toast
import com.oussama.weatherapp.R
import java.util.regex.Pattern

/**
 * Utility class for phone number operations
 */
object PhoneNumberUtils {
    private const val TAG = "PhoneNumberUtils"

    /**
     * Check if the app is running on an emulator
     * @return true if running on an emulator, false otherwise
     */
    private fun isEmulator(): Boolean {
        return (Build.BRAND.startsWith("generic") && Build.DEVICE.startsWith("generic")
                || Build.FINGERPRINT.startsWith("generic")
                || Build.FINGERPRINT.startsWith("unknown")
                || Build.HARDWARE.contains("goldfish")
                || Build.HARDWARE.contains("ranchu")
                || Build.MODEL.contains("google_sdk")
                || Build.MODEL.contains("Emulator")
                || Build.MODEL.contains("Android SDK built for x86")
                || Build.MANUFACTURER.contains("Genymotion")
                || Build.PRODUCT.contains("sdk_google")
                || Build.PRODUCT.contains("google_sdk")
                || Build.PRODUCT.contains("sdk")
                || Build.PRODUCT.contains("sdk_x86")
                || Build.PRODUCT.contains("sdk_gphone64_arm64")
                || Build.PRODUCT.contains("vbox86p")
                || Build.PRODUCT.contains("emulator")
                || Build.PRODUCT.contains("simulator"))
    }

    // Pattern to match phone numbers in text
    // This pattern matches:
    // - Any sequence of at least 4 digits (with optional separators)
    // - Much more inclusive to catch all potential phone numbers
    private val PHONE_PATTERN = Pattern.compile(
        "\\d{4,}"  // Match any sequence of 4 or more digits
    )

    /**
     * Check if a string contains a phone number
     * @param text The text to check
     * @return true if the text contains a phone number
     */
    fun containsPhoneNumber(text: String): Boolean {
        // First try with the pattern
        val patternResult = PHONE_PATTERN.matcher(text).find()

        // If pattern fails, check if the text contains only digits
        val digitsOnlyResult = if (!patternResult) {
            // Remove all non-digit characters and check if we have at least 4 digits
            val digitsOnly = text.replace(Regex("[^0-9]"), "")
            digitsOnly.length >= 4
        } else {
            false
        }

        val result = patternResult || digitsOnlyResult
        Log.d(TAG, "Checking for phone number in text: '$text', pattern result: $patternResult, digits only result: $digitsOnlyResult, final result: $result")
        return result
    }

    /**
     * Extract phone number from text
     * @param text The text to extract from
     * @return The extracted phone number or null if none found
     */
    fun extractPhoneNumber(text: String): String? {
        // First try with the pattern
        val matcher = PHONE_PATTERN.matcher(text)
        val patternResult = if (matcher.find()) {
            matcher.group()
        } else {
            null
        }

        // If pattern fails, check if the text contains only digits
        val result = if (patternResult != null) {
            patternResult
        } else {
            // Remove all non-digit characters
            val digitsOnly = text.replace(Regex("[^0-9]"), "")
            if (digitsOnly.length >= 4) {
                digitsOnly
            } else {
                null
            }
        }

        Log.d(TAG, "Extracting phone number from text: '$text', result: $result")
        return result
    }

    /**
     * Format phone number for display
     * @param phoneNumber The phone number to format
     * @return The formatted phone number
     */
    fun formatPhoneNumber(phoneNumber: String): String {
        // Remove all non-digit characters except the leading +
        var digitsOnly = phoneNumber.replace(Regex("[^\\d+]"), "")

        // Simple formatting for display
        if (digitsOnly.startsWith("+")) {
            // International format
            return digitsOnly
        } else if (digitsOnly.length == 10) {
            // US format: (XXX) XXX-XXXX
            return "(${digitsOnly.substring(0, 3)}) ${digitsOnly.substring(3, 6)}-${digitsOnly.substring(6)}"
        } else if (digitsOnly.length == 8) {
            // 8-digit format: XXXX-XXXX (common in many countries)
            return "${digitsOnly.substring(0, 4)}-${digitsOnly.substring(4)}"
        } else if (digitsOnly.length >= 4 && digitsOnly.length < 8) {
            // Short number format: just add spaces every 2 digits
            val formatted = StringBuilder()
            for (i in digitsOnly.indices) {
                if (i > 0 && i % 2 == 0) {
                    formatted.append(" ")
                }
                formatted.append(digitsOnly[i])
            }
            return formatted.toString()
        } else {
            // Just return the original number if we can't format it
            return phoneNumber
        }
    }

    /**
     * Format phone number for dialing
     * @param phoneNumber The phone number to format
     * @return The formatted phone number for dialing
     */
    fun formatPhoneNumberForDialing(phoneNumber: String): String {
        // Remove all non-digit characters except the leading +
        return phoneNumber.replace(Regex("[^\\d+]"), "")
    }

    /**
     * Open the phone dialer with the given phone number
     * @param context The context
     * @param phoneNumber The phone number to dial
     * @return true if the dialer was opened successfully, false otherwise
     */
    fun dialPhoneNumber(context: Context, phoneNumber: String): Boolean {
        try {
            val formattedNumber = formatPhoneNumberForDialing(phoneNumber)

            // Log if running on emulator
            if (isEmulator()) {
                Log.d(TAG, "Running on emulator, will try emulator-specific approaches if needed")
            }

            // First try the standard approach with ACTION_DIAL
            val dialIntent = Intent(Intent.ACTION_DIAL).apply {
                data = Uri.parse("tel:$formattedNumber")
            }

            // Check if there's an app to handle the dial intent
            if (isIntentResolvable(context, dialIntent)) {
                Log.d(TAG, "Using standard ACTION_DIAL intent")
                context.startActivity(dialIntent)
                return true
            }

            // If standard approach fails, try emulator-specific approaches
            if (isEmulator() && tryEmulatorDialer(context, formattedNumber)) {
                Log.d(TAG, "Successfully used emulator-specific dialer approach")
                return true
            }

            // If all approaches fail, show the dialog
            Log.e(TAG, "No app available to handle dial intent")
            showNoDialerAppDialog(context, formattedNumber)
            return false
        } catch (e: Exception) {
            Log.e(TAG, "Error dialing phone number", e)

            // Show a toast with the error message
            Toast.makeText(
                context,
                context.getString(R.string.error_dialing) + ": " + e.message,
                Toast.LENGTH_LONG
            ).show()
            return false
        }
    }

    /**
     * Try to open the emulator's dialer app
     * @param context The context
     * @param phoneNumber The phone number to dial
     * @return true if successful, false otherwise
     */
    private fun tryEmulatorDialer(context: Context, phoneNumber: String): Boolean {
        Log.d(TAG, "Trying emulator-specific dialer approaches")

        // List of known emulator dialer package and activity names
        val dialerIntents = listOf(
            // Google Emulator Dialer
            Intent().apply {
                component = ComponentName("com.android.dialer", "com.android.dialer.DialtactsActivity")
                action = Intent.ACTION_DIAL
                data = Uri.parse("tel:$phoneNumber")
            },
            // Alternative Emulator Dialer
            Intent().apply {
                component = ComponentName("com.android.dialer", "com.android.dialer.main.impl.MainActivity")
                action = Intent.ACTION_DIAL
                data = Uri.parse("tel:$phoneNumber")
            },
            // Another alternative
            Intent("android.intent.action.DIAL").apply {
                data = Uri.parse("tel:$phoneNumber")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
        )

        // Try each intent until one works
        for (intent in dialerIntents) {
            try {
                if (isIntentResolvable(context, intent)) {
                    Log.d(TAG, "Found working emulator dialer intent: ${intent.component}")
                    context.startActivity(intent)
                    return true
                }
            } catch (e: Exception) {
                Log.d(TAG, "Failed to start emulator dialer with intent: ${intent.component}", e)
            }
        }

        return false
    }

    /**
     * Check if an intent can be resolved to an activity
     * @param context The context
     * @param intent The intent to check
     * @return true if the intent can be resolved, false otherwise
     */
    private fun isIntentResolvable(context: Context, intent: Intent): Boolean {
        val packageManager = context.packageManager
        val activities = packageManager.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY)
        return activities.size > 0
    }

    /**
     * Show a dialog when no dialer app is available
     * @param context The context
     * @param phoneNumber The phone number to dial
     */
    private fun showNoDialerAppDialog(context: Context, phoneNumber: String) {
        val builder = AlertDialog.Builder(context)
        builder.setTitle(context.getString(R.string.no_app_for_call))
        builder.setMessage(context.getString(R.string.no_dialer_app_message, phoneNumber))

        // Add a "Copy Number" button
        builder.setPositiveButton(context.getString(R.string.copy_number)) { dialog, _ ->
            // Copy the phone number to the clipboard
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("Phone Number", phoneNumber)
            clipboard.setPrimaryClip(clip)

            // Show a toast confirming the copy
            Toast.makeText(
                context,
                context.getString(R.string.number_copied_to_clipboard),
                Toast.LENGTH_SHORT
            ).show()

            dialog.dismiss()
        }

        // Add a "Cancel" button
        builder.setNegativeButton(context.getString(R.string.cancel_operation)) { dialog, _ ->
            dialog.dismiss()
        }

        // Show the dialog
        builder.show()
    }

    /**
     * Open the SMS app with the given phone number
     * @param context The context
     * @param phoneNumber The phone number to message
     * @return true if the SMS app was opened successfully, false otherwise
     */
    fun sendSmsToPhoneNumber(context: Context, phoneNumber: String): Boolean {
        try {
            val formattedNumber = formatPhoneNumberForDialing(phoneNumber)

            // Log if running on emulator
            if (isEmulator()) {
                Log.d(TAG, "Running on emulator, will try emulator-specific approaches if needed")
            }

            // First try the standard approach with ACTION_SENDTO
            val smsIntent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("smsto:$formattedNumber")
            }

            // Check if there's an app to handle the SMS intent
            if (isIntentResolvable(context, smsIntent)) {
                Log.d(TAG, "Using standard ACTION_SENDTO intent")
                context.startActivity(smsIntent)
                return true
            }

            // If standard approach fails, try emulator-specific approaches
            if (isEmulator() && tryEmulatorMessaging(context, formattedNumber)) {
                Log.d(TAG, "Successfully used emulator-specific messaging approach")
                return true
            }

            // If all approaches fail, show the dialog
            Log.e(TAG, "No app available to handle SMS intent")
            showNoSmsAppDialog(context, formattedNumber)
            return false
        } catch (e: Exception) {
            Log.e(TAG, "Error sending SMS to phone number", e)

            // Show a toast with the error message
            Toast.makeText(
                context,
                context.getString(R.string.error_messaging) + ": " + e.message,
                Toast.LENGTH_LONG
            ).show()
            return false
        }
    }

    /**
     * Try to open the emulator's messaging app
     * @param context The context
     * @param phoneNumber The phone number to message
     * @return true if successful, false otherwise
     */
    private fun tryEmulatorMessaging(context: Context, phoneNumber: String): Boolean {
        Log.d(TAG, "Trying emulator-specific messaging approaches")

        // List of known emulator messaging package and activity names
        val messagingIntents = listOf(
            // Google Emulator Messaging
            Intent().apply {
                component = ComponentName("com.android.mms", "com.android.mms.ui.ComposeMessageActivity")
                action = Intent.ACTION_SENDTO
                data = Uri.parse("smsto:$phoneNumber")
            },
            // Alternative Emulator Messaging
            Intent().apply {
                component = ComponentName("com.google.android.apps.messaging", "com.google.android.apps.messaging.ui.ConversationListActivity")
                action = Intent.ACTION_SENDTO
                data = Uri.parse("smsto:$phoneNumber")
            },
            // Another alternative
            Intent("android.intent.action.SENDTO").apply {
                data = Uri.parse("smsto:$phoneNumber")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            },
            // Try with ACTION_VIEW as a fallback
            Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse("sms:$phoneNumber")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
        )

        // Try each intent until one works
        for (intent in messagingIntents) {
            try {
                if (isIntentResolvable(context, intent)) {
                    Log.d(TAG, "Found working emulator messaging intent: ${intent.component}")
                    context.startActivity(intent)
                    return true
                }
            } catch (e: Exception) {
                Log.d(TAG, "Failed to start emulator messaging with intent: ${intent.component}", e)
            }
        }

        return false
    }

    /**
     * Show a dialog when no SMS app is available
     * @param context The context
     * @param phoneNumber The phone number to message
     */
    private fun showNoSmsAppDialog(context: Context, phoneNumber: String) {
        val builder = AlertDialog.Builder(context)
        builder.setTitle(context.getString(R.string.no_app_for_sms))
        builder.setMessage(context.getString(R.string.no_sms_app_message, phoneNumber))

        // Add a "Copy Number" button
        builder.setPositiveButton(context.getString(R.string.copy_number)) { dialog, _ ->
            // Copy the phone number to the clipboard
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("Phone Number", phoneNumber)
            clipboard.setPrimaryClip(clip)

            // Show a toast confirming the copy
            Toast.makeText(
                context,
                context.getString(R.string.number_copied_to_clipboard),
                Toast.LENGTH_SHORT
            ).show()

            dialog.dismiss()
        }

        // Add a "Cancel" button
        builder.setNegativeButton(context.getString(R.string.cancel_operation)) { dialog, _ ->
            dialog.dismiss()
        }

        // Show the dialog
        builder.show()
    }
}
