package com.oussama.weatherapp.data.repository

import android.net.Uri
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import com.oussama.weatherapp.data.model.User
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.Date
import kotlin.Result

/**
 * Repository for user-related operations
 */
class UserRepository {
    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()
    private val usersCollection = firestore.collection("users")

    /**
     * Register a new user
     */
    suspend fun registerUser(email: String, password: String, name: String): Result<User> = withContext(Dispatchers.IO) {
        try {
            // Disable reCAPTCHA verification for testing
            auth.firebaseAuthSettings.setAppVerificationDisabledForTesting(true)

            // Create authentication account
            val authResult = auth.createUserWithEmailAndPassword(email, password).await()
            val firebaseUser = authResult.user ?: throw Exception("Failed to create user")

            // Update display name
            val profileUpdates = UserProfileChangeRequest.Builder()
                .setDisplayName(name)
                .build()
            firebaseUser.updateProfile(profileUpdates).await()

            // Create user document in Firestore
            val user = User(
                id = firebaseUser.uid,
                email = email,
                name = name,
                language = "en",
                registrationDate = Date(),
                photoUrl = null
            )

            usersCollection.document(firebaseUser.uid).set(user).await()

            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Login user
     */
    suspend fun loginUser(email: String, password: String): Result<User> = withContext(Dispatchers.IO) {
        try {
            // Disable reCAPTCHA verification for testing
            auth.firebaseAuthSettings.setAppVerificationDisabledForTesting(true)

            val authResult = auth.signInWithEmailAndPassword(email, password).await()
            val firebaseUser = authResult.user ?: throw Exception("Failed to login")

            val userDoc = usersCollection.document(firebaseUser.uid).get().await()
            if (userDoc.exists()) {
                val user = userDoc.toObject(User::class.java)
                    ?: throw Exception("Failed to parse user data")
                Result.success(user)
            } else {
                Result.failure(Exception("User data not found"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get current user
     */
    suspend fun getCurrentUser(): Result<User?> = withContext(Dispatchers.IO) {
        try {
            val firebaseUser = auth.currentUser
            if (firebaseUser != null) {
                val userDoc = usersCollection.document(firebaseUser.uid).get().await()
                if (userDoc.exists()) {
                    val user = userDoc.toObject(User::class.java)
                    Result.success(user)
                } else {
                    Result.success(null)
                }
            } else {
                Result.success(null)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Update user profile
     */
    suspend fun updateUserProfile(name: String, language: String, photoUrl: String? = null): Result<User> = withContext(Dispatchers.IO) {
        try {
            val firebaseUser = auth.currentUser ?: throw Exception("User not logged in")

            // Create profile update builder
            val profileUpdatesBuilder = UserProfileChangeRequest.Builder()

            // Update display name if changed
            if (name != firebaseUser.displayName) {
                profileUpdatesBuilder.setDisplayName(name)
            }

            // Update photo URL if provided
            if (photoUrl != null) {
                profileUpdatesBuilder.setPhotoUri(Uri.parse(photoUrl))
            }

            // Apply updates if any
            val profileUpdates = profileUpdatesBuilder.build()
            if (name != firebaseUser.displayName || photoUrl != null) {
                firebaseUser.updateProfile(profileUpdates).await()
            }

            // Get current user data
            val userDoc = usersCollection.document(firebaseUser.uid).get().await()
            val currentUser = userDoc.toObject(User::class.java)
                ?: throw Exception("Failed to get current user data")

            // Create updated user object
            val updatedUser = currentUser.copy(
                name = name,
                language = language,
                photoUrl = photoUrl ?: currentUser.photoUrl
            )

            // Update in Firestore
            usersCollection.document(firebaseUser.uid).set(updatedUser).await()

            Result.success(updatedUser)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Logout user
     */
    fun logout() {
        auth.signOut()
    }

    /**
     * Check if user is logged in
     */
    fun isUserLoggedIn(): Boolean {
        return auth.currentUser != null
    }

    /**
     * Get current Firebase user
     */
    fun getCurrentFirebaseUser(): FirebaseUser? {
        return auth.currentUser
    }

    /**
     * Refresh the Firebase auth token
     * This is useful when you get permission denied errors
     */
    suspend fun refreshAuthToken(): Result<String> = withContext(Dispatchers.IO) {
        try {
            val firebaseUser = auth.currentUser ?: throw Exception("User not logged in")
            val tokenResult = firebaseUser.getIdToken(true).await()
            val token = tokenResult.token ?: throw Exception("Failed to get token")
            Result.success(token)
        } catch (e: Exception) {
            android.util.Log.e("UserRepository", "Error refreshing auth token: ${e.message}", e)
            Result.failure(e)
        }
    }
}
