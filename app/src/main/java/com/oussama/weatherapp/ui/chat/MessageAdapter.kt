package com.oussama.weatherapp.ui.chat

import android.graphics.Bitmap
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import android.widget.Toast
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.oussama.weatherapp.R
import com.oussama.weatherapp.data.model.Message
import com.oussama.weatherapp.databinding.ItemMessageBinding
import com.oussama.weatherapp.utils.ImageUtils
import com.oussama.weatherapp.utils.PhoneNumberUtils
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * Adapter for displaying messages in a RecyclerView
 */
class MessageAdapter : ListAdapter<Message, MessageAdapter.MessageViewHolder>(MessageDiffCallback()) {

    companion object {
        private const val TAG = "MessageAdapter"
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageViewHolder {
        val binding = ItemMessageBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return MessageViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MessageViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class MessageViewHolder(
        private val binding: ItemMessageBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        private val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())

        fun bind(message: Message) {
            binding.apply {
                // Set sender name and timestamp
                senderNameTextView.text = message.senderName
                timestampTextView.text = timeFormat.format(message.timestamp)

                // Set message text
                messageTextView.text = message.text

                // Handle image if present
                if (message.hasImage) {
                    messageImageView.visibility = View.VISIBLE

                    // First try to load from imageUrl if available
                    if (message.imageUrl != null) {
                        Glide.with(messageImageView.context)
                            .load(message.imageUrl)
                            .centerCrop()
                            .into(messageImageView)
                    }
                    // Otherwise try to load from Base64 string
                    else if (message.imageBase64 != null) {
                        // Convert Base64 to Bitmap
                        val bitmap = ImageUtils.base64ToBitmap(message.imageBase64)
                        if (bitmap != null) {
                            messageImageView.setImageBitmap(bitmap)
                        } else {
                            // Hide image view if conversion fails
                            messageImageView.visibility = View.GONE
                        }
                    } else {
                        // No image data available
                        messageImageView.visibility = View.GONE
                    }
                } else {
                    messageImageView.visibility = View.GONE
                }

                // Handle location if present
                if (message.latitude != null && message.longitude != null) {
                    locationCardView.visibility = View.VISIBLE
                    locationCardView.setOnClickListener {
                        // TODO: Open map with this location
                    }
                } else {
                    locationCardView.visibility = View.GONE
                }

                // Set up single-tap listener for all messages
                setupSingleTapListener(message.text)
            }
        }

        /**
         * Set up single-tap listener for all messages
         */
        private fun setupSingleTapListener(messageText: String) {
            Log.d(TAG, "Setting up single-tap listener for message: '$messageText'")

            // Make sure the message card is clickable and focusable
            binding.messageCardView.isFocusable = true
            binding.messageCardView.isClickable = true

            // Set click listener on the message card for all messages
            binding.messageCardView.setOnClickListener { view ->
                Log.d(TAG, "Message clicked: '$messageText'")

                // Show a toast to confirm the click is working
                Toast.makeText(view.context, "Message tapped", Toast.LENGTH_SHORT).show()

                // Check if the message contains a phone number
                if (PhoneNumberUtils.containsPhoneNumber(messageText)) {
                    Log.d(TAG, "Phone number detected in message")

                    // Extract the phone number
                    val phoneNumber = PhoneNumberUtils.extractPhoneNumber(messageText)

                    if (phoneNumber != null) {
                        Log.d(TAG, "Showing popup menu for phone number: $phoneNumber")

                        // Show popup menu for phone number actions
                        showPhoneNumberPopupMenu(view, phoneNumber)
                    } else {
                        Log.e(TAG, "Failed to extract phone number from message that contains one")
                    }
                } else {
                    Log.d(TAG, "No phone number detected in message")
                }
            }
        }

        /**
         * Show popup menu with phone number actions
         */
        private fun showPhoneNumberPopupMenu(view: View, phoneNumber: String) {
            Log.d(TAG, "Showing phone number popup menu for number: $phoneNumber")

            val context = view.context
            val popupMenu = PopupMenu(context, view)

            // Format the phone number for display
            val formattedNumber = PhoneNumberUtils.formatPhoneNumber(phoneNumber)
            Log.d(TAG, "Formatted phone number: $formattedNumber")

            // Inflate the menu
            popupMenu.menu.add(0, 1, 0, context.getString(R.string.call) + " " + formattedNumber)
            popupMenu.menu.add(0, 2, 1, context.getString(R.string.message) + " " + formattedNumber)
            Log.d(TAG, "Added menu items for Call and Message")

            // Set up item click listener
            popupMenu.setOnMenuItemClickListener { menuItem ->
                Log.d(TAG, "Menu item clicked: ${menuItem.title}")

                when (menuItem.itemId) {
                    1 -> { // Call
                        Log.d(TAG, "Call option selected for number: $phoneNumber")
                        // The dialPhoneNumber method now handles all error cases internally
                        // and returns a boolean indicating success or failure
                        PhoneNumberUtils.dialPhoneNumber(context, phoneNumber)
                        true
                    }
                    2 -> { // Message
                        Log.d(TAG, "Message option selected for number: $phoneNumber")
                        // The sendSmsToPhoneNumber method now handles all error cases internally
                        // and returns a boolean indicating success or failure
                        PhoneNumberUtils.sendSmsToPhoneNumber(context, phoneNumber)
                        true
                    }
                    else -> false
                }
            }

            // Show the popup menu
            Log.d(TAG, "Displaying popup menu")
            popupMenu.show()
        }
    }

    class MessageDiffCallback : DiffUtil.ItemCallback<Message>() {
        override fun areItemsTheSame(oldItem: Message, newItem: Message): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Message, newItem: Message): Boolean {
            return oldItem == newItem
        }
    }
}
