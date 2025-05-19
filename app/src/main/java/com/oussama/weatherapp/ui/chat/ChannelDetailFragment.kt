package com.oussama.weatherapp.ui.chat

import android.Manifest
import android.app.Activity
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.oussama.weatherapp.R
import com.oussama.weatherapp.databinding.FragmentChannelDetailBinding
import com.oussama.weatherapp.ui.viewmodel.ChatViewModel
import com.oussama.weatherapp.utils.ImageUtils
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Fragment for displaying channel details and messages
 */
class ChannelDetailFragment : Fragment() {

    private var _binding: FragmentChannelDetailBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: ChatViewModel
    private lateinit var adapter: MessageAdapter

    private val handler = Handler(Looper.getMainLooper())
    private var retryRunnable: Runnable? = null
    private var currentChannelId: String? = null
    private var retryCount = 0
    private val maxRetries = 3

    // Image handling
    private var currentPhotoPath: String? = null
    private var currentPhotoUri: Uri? = null

    // Permission launchers
    private val cameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            // Permission granted, launch camera
            launchCamera()
        } else {
            // Permission denied
            Toast.makeText(
                requireContext(),
                R.string.camera_permission_required,
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private val storagePermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            // Permission granted, launch gallery
            openGallery()
        } else {
            // Permission denied
            Toast.makeText(
                requireContext(),
                R.string.storage_permission_required,
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    // Activity result launchers
    private val takePictureLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            // Image captured successfully
            currentPhotoUri?.let { uri ->
                processImageFromUri(uri)
            }
        }
    }

    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                processImageFromUri(uri)
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentChannelDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize ViewModel
        viewModel = ViewModelProvider(requireActivity())[ChatViewModel::class.java]

        // Set up RecyclerView
        setupRecyclerView()

        // Set up click listeners
        setupClickListeners()

        // Get channel ID from arguments
        arguments?.getString("channelId")?.let { channelId ->
            // Load the channel if it's not already selected
            if (viewModel.selectedChannel.value?.id != channelId) {
                loadChannel(channelId)
            }
        }

        // Observe ViewModel state
        observeViewModel()

        // Reset any previous send message result
        viewModel.resetSendMessageResult()
    }

    private fun loadChannel(channelId: String) {
        // Store the current channel ID for potential retries
        currentChannelId = channelId
        retryCount = 0

        // Cancel any existing retry
        cancelRetry()

        // Load the channel
        viewModel.getChannelById(channelId)
    }

    private fun retryLoadingChannel() {
        if (retryCount < maxRetries) {
            retryCount++

            // Create a new retry runnable
            retryRunnable = Runnable {
                currentChannelId?.let { channelId ->
                    Snackbar.make(
                        binding.root,
                        getString(R.string.retrying_load_messages, retryCount, maxRetries),
                        Snackbar.LENGTH_SHORT
                    ).show()

                    // Try loading the channel again
                    viewModel.getChannelById(channelId)
                }
            }

            // Schedule the retry after a delay (increasing with each retry)
            handler.postDelayed(retryRunnable!!, 5000L * retryCount)
        }
    }

    private fun cancelRetry() {
        retryRunnable?.let {
            handler.removeCallbacks(it)
            retryRunnable = null
        }
    }

    private fun setupRecyclerView() {
        adapter = MessageAdapter()

        binding.messagesRecyclerView.adapter = adapter
        binding.messagesRecyclerView.layoutManager = LinearLayoutManager(requireContext()).apply {
            // Display messages from bottom to top (newest at the bottom)
            stackFromEnd = true
        }
    }

    private fun setupClickListeners() {
        // Send button click
        binding.sendButton.setOnClickListener {
            sendMessage()
        }

        // Also send message when pressing Enter on the keyboard
        binding.messageEditText.setOnEditorActionListener { _, actionId, event ->
            if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_SEND ||
                (event != null && event.keyCode == android.view.KeyEvent.KEYCODE_ENTER && event.action == android.view.KeyEvent.ACTION_DOWN)) {
                sendMessage()
                return@setOnEditorActionListener true
            }
            false
        }

        // Attach button click
        binding.attachButton.setOnClickListener {
            showImageOptionsDialog()
        }
    }

    private fun sendMessage() {
        val text = binding.messageEditText.text.toString().trim()

        if (text.isNotEmpty()) {
            // Hide keyboard
            val imm = requireContext().getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
            imm.hideSoftInputFromWindow(binding.messageEditText.windowToken, 0)

            // Send message
            viewModel.sendMessage(text)

            // Clear input field
            binding.messageEditText.text?.clear()

            // Scroll to bottom of messages
            binding.messagesRecyclerView.postDelayed({
                val messageCount = viewModel.messages.value?.size ?: 0
                if (messageCount > 0) {
                    binding.messagesRecyclerView.smoothScrollToPosition(messageCount - 1)
                }
            }, 300)
        }
    }

    private fun observeViewModel() {
        // Observe selected channel
        viewModel.selectedChannel.observe(viewLifecycleOwner) { channel ->
            channel?.let {
                binding.channelNameTextView.text = it.name
            }
        }

        // Observe messages
        viewModel.messages.observe(viewLifecycleOwner) { messages ->
            Log.d("ChannelDetailFragment", "Received ${messages.size} messages")

            // Create a new list to force DiffUtil to run
            val messagesList = ArrayList(messages)

            // Update the adapter with the new list
            adapter.submitList(messagesList)

            if (messages.isNotEmpty()) {
                // Scroll to the bottom (most recent message)
                binding.messagesRecyclerView.post {
                    binding.messagesRecyclerView.scrollToPosition(messages.size - 1)
                }
                binding.emptyTextView.visibility = View.GONE
            } else {
                // Check if we're waiting for the index to be created
                if (binding.emptyTextView.text.contains(getString(R.string.setting_up_chat).substringBefore("\n"))) {
                    // Keep the "setting up" message
                } else {
                    // Show the regular empty message
                    binding.emptyTextView.text = getString(R.string.no_messages)
                    binding.emptyTextView.visibility = View.VISIBLE
                }
            }
        }

        // Observe loading state
        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
            binding.sendButton.isEnabled = !isLoading
        }

        // Observe error state
        viewModel.error.observe(viewLifecycleOwner) { error ->
            error?.let {
                if (it.contains("requires an index") || it.contains("index that is being created")) {
                    // Show a more user-friendly message for index errors
                    val message = getString(R.string.index_being_created)
                    Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG).show()

                    // Show a message in the empty view instead of the default "No messages" text
                    binding.emptyTextView.text = getString(R.string.setting_up_chat)
                    binding.emptyTextView.visibility = View.VISIBLE

                    // Schedule a retry
                    retryLoadingChannel()

                    // Log the index error
                    Log.d("ChannelDetailFragment", "Index error detected, scheduled retry: $it")
                } else {
                    // Show the regular error message
                    Snackbar.make(binding.root, it, Snackbar.LENGTH_LONG).show()
                    Log.e("ChannelDetailFragment", "Error: $it")
                }
                viewModel.clearError()
            }
        }

        // Observe send message result
        viewModel.sendMessageResult.observe(viewLifecycleOwner) { result ->
            result?.let {
                if (it.isSuccess) {
                    // Message sent successfully
                    Log.d("ChannelDetailFragment", "Message sent successfully: ${it.getOrNull()?.id}")
                } else if (it.isFailure) {
                    // Message sending failed
                    val exception = it.exceptionOrNull()
                    val errorMessage = exception?.message ?: "Failed to send message"
                    Toast.makeText(context, errorMessage, Toast.LENGTH_SHORT).show()
                    Log.e("ChannelDetailFragment", "Failed to send message: $errorMessage", exception)
                }
                viewModel.resetSendMessageResult()
            }
        }
    }

    /**
     * Show dialog with options to take a photo or select from gallery
     */
    private fun showImageOptionsDialog() {
        val options = arrayOf(
            getString(R.string.take_photo_option),
            getString(R.string.choose_gallery_option)
        )

        AlertDialog.Builder(requireContext())
            .setTitle(R.string.add_image)
            .setItems(options) { _, which ->
                when (which) {
                    0 -> checkCameraPermission()
                    1 -> checkStoragePermission()
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    /**
     * Check if camera permission is granted, request if not
     */
    private fun checkCameraPermission() {
        when {
            ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED -> {
                // Permission already granted
                launchCamera()
            }
            shouldShowRequestPermissionRationale(Manifest.permission.CAMERA) -> {
                // Show rationale and request permission
                AlertDialog.Builder(requireContext())
                    .setTitle(R.string.camera_permission_required)
                    .setMessage(R.string.camera_permission_rationale)
                    .setPositiveButton(R.string.ok) { _, _ ->
                        cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                    }
                    .setNegativeButton(R.string.cancel, null)
                    .show()
            }
            else -> {
                // Request permission directly
                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }
    }

    /**
     * Check if storage permission is granted, request if not
     */
    private fun checkStoragePermission() {
        // For Android 13+ (API 33+), we need to request READ_MEDIA_IMAGES
        // For older versions, we need READ_EXTERNAL_STORAGE
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_IMAGES
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }

        when {
            ContextCompat.checkSelfPermission(
                requireContext(),
                permission
            ) == PackageManager.PERMISSION_GRANTED -> {
                // Permission already granted
                openGallery()
            }
            shouldShowRequestPermissionRationale(permission) -> {
                // Show rationale and request permission
                AlertDialog.Builder(requireContext())
                    .setTitle(R.string.storage_permission_required)
                    .setMessage(R.string.storage_permission_rationale)
                    .setPositiveButton(R.string.ok) { _, _ ->
                        storagePermissionLauncher.launch(permission)
                    }
                    .setNegativeButton(R.string.cancel, null)
                    .show()
            }
            else -> {
                // Request permission directly
                storagePermissionLauncher.launch(permission)
            }
        }
    }

    /**
     * Launch camera to take a photo
     */
    private fun launchCamera() {
        val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)

        // Create a file to save the image
        val photoFile: File? = try {
            createImageFile()
        } catch (ex: IOException) {
            Log.e("ChannelDetailFragment", "Error creating image file", ex)
            Toast.makeText(
                requireContext(),
                R.string.error_creating_image_file,
                Toast.LENGTH_SHORT
            ).show()
            null
        }

        // Continue only if the file was successfully created
        photoFile?.let {
            currentPhotoUri = FileProvider.getUriForFile(
                requireContext(),
                "${requireContext().packageName}.fileprovider",
                it
            )

            takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, currentPhotoUri)
            takePictureLauncher.launch(takePictureIntent)
        }
    }

    /**
     * Create a temporary image file
     */
    @Throws(IOException::class)
    private fun createImageFile(): File {
        // Create an image file name
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val imageFileName = "JPEG_${timeStamp}_"

        val storageDir = requireContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        val image = File.createTempFile(
            imageFileName,
            ".jpg",
            storageDir
        )

        // Save the file path for use with ACTION_VIEW intents
        currentPhotoPath = image.absolutePath
        return image
    }

    /**
     * Open gallery to select an image
     */
    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        pickImageLauncher.launch(intent)
    }

    /**
     * Process image from URI and convert to Base64
     */
    private fun processImageFromUri(uri: Uri) {
        // Show loading indicator
        binding.progressBar.visibility = View.VISIBLE

        try {
            // Get current user
            val currentUser = FirebaseAuth.getInstance().currentUser
            if (currentUser == null) {
                // User not authenticated
                binding.progressBar.visibility = View.GONE
                Toast.makeText(
                    requireContext(),
                    "You must be logged in to share images",
                    Toast.LENGTH_SHORT
                ).show()
                return
            }

            // Convert image to Base64 in a background thread
            Thread {
                try {
                    // Convert image to Base64
                    val base64Image = ImageUtils.uriToBase64(requireContext(), uri)

                    // Update UI on main thread
                    activity?.runOnUiThread {
                        // Hide loading indicator
                        binding.progressBar.visibility = View.GONE

                        if (base64Image != null) {
                            // Send message with Base64 image
                            sendImageMessage(base64Image)
                            Log.d("ChannelDetailFragment", "Image converted to Base64 successfully")
                        } else {
                            // Show error
                            Toast.makeText(
                                requireContext(),
                                "Failed to process image",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                } catch (e: Exception) {
                    // Update UI on main thread
                    activity?.runOnUiThread {
                        // Hide loading indicator
                        binding.progressBar.visibility = View.GONE

                        // Show error
                        Log.e("ChannelDetailFragment", "Error processing image", e)
                        Toast.makeText(
                            requireContext(),
                            "Error processing image: ${e.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }.start()
        } catch (e: Exception) {
            // Handle any unexpected exceptions
            binding.progressBar.visibility = View.GONE
            Log.e("ChannelDetailFragment", "Unexpected error during image processing", e)
            Toast.makeText(
                requireContext(),
                "Unexpected error: ${e.message}",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    /**
     * Send a message with an image
     */
    private fun sendImageMessage(imageBase64: String) {
        // Send a message with the Base64 image
        viewModel.sendMessage(
            text = getString(R.string.image_message_text),  // Default text for image messages
            imageBase64 = imageBase64
        )

        // Scroll to bottom of messages after a delay to ensure the new message is added
        binding.messagesRecyclerView.postDelayed({
            val messageCount = viewModel.messages.value?.size ?: 0
            if (messageCount > 0) {
                binding.messagesRecyclerView.smoothScrollToPosition(messageCount - 1)
            }
        }, 500)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Cancel any pending retries
        cancelRetry()
        _binding = null
    }
}
