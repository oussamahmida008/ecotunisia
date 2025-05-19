package com.oussama.weatherapp.data.repository

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import com.oussama.weatherapp.data.model.Channel
import com.oussama.weatherapp.data.model.Message
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.Date

/**
 * Repository for chat-related operations
 */
class ChatRepository {
    private val firestore = FirebaseFirestore.getInstance()
    private val channelsCollection = firestore.collection("channels")
    private val messagesCollection = firestore.collection("messages")

    // Coroutine scope for repository operations
    private val coroutineScope = kotlinx.coroutines.CoroutineScope(Dispatchers.IO + kotlinx.coroutines.SupervisorJob())

    /**
     * Create a new channel
     */
    suspend fun createChannel(
        name: String,
        description: String,
        creatorId: String,
        creatorName: String
    ): Result<Channel> = withContext(Dispatchers.IO) {
        try {
            val channel = Channel(
                id = channelsCollection.document().id,
                name = name,
                description = description,
                creatorId = creatorId,
                creatorName = creatorName,
                timestamp = Date(),
                memberCount = 1
            )

            channelsCollection.document(channel.id).set(channel).await()

            Result.success(channel)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get all channels
     */
    suspend fun getAllChannels(): Result<List<Channel>> = withContext(Dispatchers.IO) {
        try {
            val snapshot = channelsCollection
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .await()

            val channels = snapshot.toObjects(Channel::class.java)
            Result.success(channels)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Send a message to a channel
     */
    suspend fun sendMessage(
        text: String,
        senderId: String,
        senderName: String,
        channelId: String,
        latitude: Double? = null,
        longitude: Double? = null,
        imageUrl: String? = null,
        imageBase64: String? = null
    ): Result<Message> = withContext(Dispatchers.IO) {
        try {
            // Create a new document ID
            val messageId = messagesCollection.document().id

            // Determine if this message has an image
            val hasImage = imageBase64 != null || imageUrl != null

            // Create the message object
            val message = Message(
                id = messageId,
                text = text,
                senderId = senderId,
                senderName = senderName,
                channelId = channelId,
                timestamp = Date(),
                latitude = latitude,
                longitude = longitude,
                imageUrl = imageUrl,
                imageBase64 = imageBase64,
                hasImage = hasImage
            )

            // Log the message being sent
            Log.d("ChatRepository", "Sending message: $messageId to channel: $channelId")

            // Convert to a map to ensure all fields are properly set
            val messageMap = mapOf(
                "id" to message.id,
                "text" to message.text,
                "senderId" to message.senderId,
                "senderName" to message.senderName,
                "channelId" to message.channelId,
                "timestamp" to message.timestamp,
                "latitude" to message.latitude,
                "longitude" to message.longitude,
                "imageUrl" to message.imageUrl,
                "imageBase64" to message.imageBase64,
                "hasImage" to message.hasImage
            )

            // Save to Firestore
            messagesCollection.document(messageId).set(messageMap).await()

            // Log success
            Log.d("ChatRepository", "Message sent successfully: $messageId")

            Result.success(message)
        } catch (e: Exception) {
            // Log error
            Log.e("ChatRepository", "Error sending message: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Get messages for a channel as a Flow
     */
    fun getMessagesForChannel(channelId: String, descending: Boolean = false): Flow<List<Message>> = callbackFlow {
        try {
            Log.d("ChatRepository", "Starting to get messages for channel: $channelId")

            // First try to get messages without ordering (to avoid index issues)
            val simpleQuery = messagesCollection
                .whereEqualTo("channelId", channelId)

            // Try the simple query first
            try {
                val snapshot = simpleQuery.get().await()
                val messages = snapshot.toObjects(Message::class.java)

                // Sort the messages locally
                val sortedMessages = if (descending) {
                    messages.sortedByDescending { it.timestamp }
                } else {
                    messages.sortedBy { it.timestamp }
                }

                Log.d("ChatRepository", "Got ${sortedMessages.size} messages with simple query")
                trySend(sortedMessages).isSuccess
            } catch (e: Exception) {
                Log.w("ChatRepository", "Simple query failed, falling back to ordered query: ${e.message}")
                // Simple query failed, continue with the ordered query
            }

            // Set up the ordered query (which might require an index)
            val orderedQuery = messagesCollection
                .whereEqualTo("channelId", channelId)
                .orderBy("timestamp", if (descending) Query.Direction.DESCENDING else Query.Direction.ASCENDING)

            val listenerRegistration = orderedQuery.addSnapshotListener { snapshot, error ->
                if (error != null) {
                    // Check if it's an index error
                    if (error is FirebaseFirestoreException &&
                        error.code == FirebaseFirestoreException.Code.FAILED_PRECONDITION &&
                        error.message?.contains("requires an index") == true) {

                        // Log the error with the index creation link
                        Log.e("ChatRepository", "Index required for messages query: ${error.message}")

                        // Try to get messages without the ordering
                        coroutineScope.launch {
                            try {
                                val fallbackSnapshot = simpleQuery.get().await()
                                val fallbackMessages = fallbackSnapshot.toObjects(Message::class.java)

                                // Sort the messages locally
                                val sortedMessages = if (descending) {
                                    fallbackMessages.sortedByDescending { it.timestamp }
                                } else {
                                    fallbackMessages.sortedBy { it.timestamp }
                                }

                                Log.d("ChatRepository", "Got ${sortedMessages.size} messages with fallback query")
                                trySend(sortedMessages).isSuccess
                            } catch (e: Exception) {
                                Log.e("ChatRepository", "Fallback query failed: ${e.message}", e)
                                trySend(emptyList()).isSuccess
                            }
                        }
                    } else {
                        // For other errors, log and send empty list
                        Log.e("ChatRepository", "Error getting messages: ${error.message}", error)
                        trySend(emptyList()).isSuccess
                    }
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val messages = snapshot.toObjects(Message::class.java)
                    Log.d("ChatRepository", "Got ${messages.size} messages with ordered query")
                    trySend(messages).isSuccess
                }
            }

            awaitClose {
                Log.d("ChatRepository", "Closing messages listener for channel: $channelId")
                listenerRegistration.remove()
            }
        } catch (e: Exception) {
            // Handle any exceptions during query setup
            Log.e("ChatRepository", "Exception setting up messages query: ${e.message}", e)
            trySend(emptyList()).isSuccess
            close(e)
        }
    }

    /**
     * Get a channel by ID
     */
    suspend fun getChannelById(channelId: String): Result<Channel> = withContext(Dispatchers.IO) {
        try {
            val document = channelsCollection.document(channelId).get().await()
            if (document.exists()) {
                val channel = document.toObject(Channel::class.java)
                    ?: throw Exception("Failed to parse channel data")
                Result.success(channel)
            } else {
                Result.failure(Exception("Channel not found"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
