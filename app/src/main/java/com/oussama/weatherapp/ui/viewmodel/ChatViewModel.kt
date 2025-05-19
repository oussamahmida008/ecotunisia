package com.oussama.weatherapp.ui.viewmodel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.FirebaseFirestoreException
import com.oussama.weatherapp.data.model.Channel
import com.oussama.weatherapp.data.model.Message
import com.oussama.weatherapp.data.repository.ChatRepository
import com.oussama.weatherapp.data.repository.UserRepository
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * ViewModel for chat screens
 */
class ChatViewModel : ViewModel() {

    private val chatRepository = ChatRepository()
    private val userRepository = UserRepository()

    private val _channels = MutableLiveData<List<Channel>>()
    val channels: LiveData<List<Channel>> = _channels

    private val _selectedChannel = MutableLiveData<Channel?>()
    val selectedChannel: LiveData<Channel?> = _selectedChannel

    private val _messages = MutableLiveData<List<Message>>()
    val messages: LiveData<List<Message>> = _messages

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    private val _createChannelResult = MutableLiveData<Result<Channel>?>()
    val createChannelResult: LiveData<Result<Channel>?> = _createChannelResult

    private val _sendMessageResult = MutableLiveData<Result<Message>?>()
    val sendMessageResult: LiveData<Result<Message>?> = _sendMessageResult

    /**
     * Load all channels
     */
    fun loadChannels() {
        _isLoading.value = true
        _error.value = null

        viewModelScope.launch {
            val result = chatRepository.getAllChannels()

            if (result.isSuccess) {
                _channels.value = result.getOrDefault(emptyList())
            } else {
                _error.value = result.exceptionOrNull()?.message ?: "Unknown error"
            }

            _isLoading.value = false
        }
    }

    /**
     * Create a new channel
     */
    fun createChannel(name: String, description: String) {
        val currentUser = userRepository.getCurrentFirebaseUser() ?: return

        _isLoading.value = true
        _error.value = null

        viewModelScope.launch {
            val result = chatRepository.createChannel(
                name = name,
                description = description,
                creatorId = currentUser.uid,
                creatorName = currentUser.displayName ?: "Unknown User"
            )

            _createChannelResult.value = result

            if (result.isSuccess) {
                // Refresh channels
                loadChannels()
            } else {
                _error.value = result.exceptionOrNull()?.message ?: "Unknown error"
            }

            _isLoading.value = false
        }
    }

    /**
     * Select a channel and load its messages
     */
    fun selectChannel(channel: Channel) {
        _selectedChannel.value = channel
        _isLoading.value = true
        _messages.value = emptyList() // Clear previous messages

        // Start collecting messages for this channel
        viewModelScope.launch {
            try {
                chatRepository.getMessagesForChannel(channel.id)
                    .catch { e ->
                        // Handle any errors in the flow
                        Log.e("ChatViewModel", "Error collecting messages: ${e.message}", e)

                        // Check if it's an index error
                        if (e is FirebaseFirestoreException &&
                            e.code == FirebaseFirestoreException.Code.FAILED_PRECONDITION &&
                            e.message?.contains("requires an index") == true) {

                            _error.value = "The messages query requires an index that is being created. Please try again in a few minutes."
                        } else {
                            _error.value = e.message ?: "Unknown error loading messages"
                        }
                        _isLoading.value = false
                    }
                    .collectLatest { messagesList ->
                        _messages.value = messagesList
                        _isLoading.value = false
                    }
            } catch (e: Exception) {
                Log.e("ChatViewModel", "Exception in selectChannel: ${e.message}", e)
                _error.value = e.message ?: "Unknown error loading messages"
                _isLoading.value = false
            }
        }
    }

    /**
     * Send a message to the selected channel
     */
    fun sendMessage(
        text: String,
        latitude: Double? = null,
        longitude: Double? = null,
        imageUrl: String? = null,
        imageBase64: String? = null
    ) {
        val currentUser = userRepository.getCurrentFirebaseUser() ?: return
        val channel = _selectedChannel.value ?: return

        _isLoading.value = true
        _error.value = null

        viewModelScope.launch {
            try {
                val result = chatRepository.sendMessage(
                    text = text,
                    senderId = currentUser.uid,
                    senderName = currentUser.displayName ?: "Unknown User",
                    channelId = channel.id,
                    latitude = latitude,
                    longitude = longitude,
                    imageUrl = imageUrl,
                    imageBase64 = imageBase64
                )

                _sendMessageResult.value = result

                if (result.isSuccess) {
                    // Add the new message to the current messages list to show it immediately
                    val newMessage = result.getOrNull()
                    if (newMessage != null) {
                        val currentMessages = _messages.value ?: emptyList()
                        _messages.value = currentMessages + newMessage

                        // Log success
                        Log.d("ChatViewModel", "Message sent and added to UI: ${newMessage.id}")
                    }
                } else {
                    _error.value = result.exceptionOrNull()?.message ?: "Unknown error"
                    Log.e("ChatViewModel", "Failed to send message: ${result.exceptionOrNull()?.message}")
                }
            } catch (e: Exception) {
                _error.value = e.message ?: "Unknown error sending message"
                Log.e("ChatViewModel", "Exception sending message: ${e.message}", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Send a message to a specific channel (without selecting it first)
     */
    fun sendMessageToChannel(
        text: String,
        senderId: String,
        senderName: String,
        channelId: String,
        latitude: Double? = null,
        longitude: Double? = null,
        imageUrl: String? = null,
        imageBase64: String? = null
    ) {
        _isLoading.value = true
        _error.value = null

        viewModelScope.launch {
            try {
                val result = chatRepository.sendMessage(
                    text = text,
                    senderId = senderId,
                    senderName = senderName,
                    channelId = channelId,
                    latitude = latitude,
                    longitude = longitude,
                    imageUrl = imageUrl,
                    imageBase64 = imageBase64
                )

                _sendMessageResult.value = result

                if (result.isSuccess) {
                    // Log success
                    Log.d("ChatViewModel", "Message sent to channel $channelId: ${result.getOrNull()?.id}")
                } else {
                    _error.value = result.exceptionOrNull()?.message ?: "Unknown error"
                    Log.e("ChatViewModel", "Failed to send message: ${result.exceptionOrNull()?.message}")
                }
            } catch (e: Exception) {
                _error.value = e.message ?: "Unknown error sending message"
                Log.e("ChatViewModel", "Exception sending message: ${e.message}", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Clear any error
     */
    fun clearError() {
        _error.value = null
    }

    /**
     * Reset create channel result
     */
    fun resetCreateChannelResult() {
        _createChannelResult.value = null
    }

    /**
     * Reset send message result
     */
    fun resetSendMessageResult() {
        _sendMessageResult.value = null
    }

    /**
     * Get a channel by ID and select it
     */
    fun getChannelById(channelId: String) {
        _isLoading.value = true
        _error.value = null

        viewModelScope.launch {
            val result = chatRepository.getChannelById(channelId)

            if (result.isSuccess) {
                val channel = result.getOrNull()
                if (channel != null) {
                    selectChannel(channel)
                } else {
                    _error.value = "Channel not found"
                }
            } else {
                _error.value = result.exceptionOrNull()?.message ?: "Unknown error"
            }

            _isLoading.value = false
        }
    }
}
