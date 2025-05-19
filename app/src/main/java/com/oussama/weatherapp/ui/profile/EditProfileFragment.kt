package com.oussama.weatherapp.ui.profile

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.storage.FirebaseStorage
import com.oussama.weatherapp.R
import com.oussama.weatherapp.databinding.FragmentEditProfileBinding
import com.oussama.weatherapp.ui.viewmodel.ProfileViewModel
import java.util.UUID

class EditProfileFragment : Fragment() {

    private var _binding: FragmentEditProfileBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: ProfileViewModel

    private var selectedImageUri: Uri? = null
    private val storageRef = FirebaseStorage.getInstance().reference.child("profile_images")

    // Activity result launcher for image selection
    private val selectImageLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                selectedImageUri = uri
                binding.profileImageView.setImageURI(uri)
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentEditProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize ViewModel
        viewModel = ViewModelProvider(this)[ProfileViewModel::class.java]

        // Set up click listeners
        setupClickListeners()

        // Observe ViewModel state
        observeViewModel()

        // Load user profile
        viewModel.loadUserProfile()
    }

    private fun setupClickListeners() {
        // Change photo button click
        binding.changePhotoButton.setOnClickListener {
            openImagePicker()
        }

        // Save button click
        binding.saveButton.setOnClickListener {
            if (validateInputs()) {
                saveProfile()
            }
        }
    }

    private fun observeViewModel() {
        // Observe user data
        viewModel.user.observe(viewLifecycleOwner) { user ->
            user?.let {
                // Populate form fields with user data
                binding.nameEditText.setText(it.name)
                binding.emailTextView.text = it.email

                // Set language selection
                if (it.language == "en") {
                    binding.englishRadioButton.isChecked = true
                } else {
                    binding.frenchRadioButton.isChecked = true
                }

                // Load profile image if available
                it.photoUrl?.let { url ->
                    Glide.with(this)
                        .load(url)
                        .circleCrop()
                        .into(binding.profileImageView)
                }
            }
        }

        // Observe loading state
        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
            binding.saveButton.isEnabled = !isLoading
        }

        // Observe error state
        viewModel.error.observe(viewLifecycleOwner) { error ->
            error?.let {
                Toast.makeText(context, it, Toast.LENGTH_LONG).show()
                viewModel.clearError()
            }
        }

        // Observe update profile result
        viewModel.updateProfileResult.observe(viewLifecycleOwner) { result ->
            result?.let {
                if (it.isSuccess) {
                    Toast.makeText(context, getString(R.string.profile_updated), Toast.LENGTH_SHORT).show()
                    findNavController().navigateUp()
                }
                viewModel.resetUpdateProfileResult()
            }
        }
    }

    private fun openImagePicker() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        selectImageLauncher.launch(intent)
    }

    private fun validateInputs(): Boolean {
        val name = binding.nameEditText.text.toString().trim()

        if (name.isEmpty()) {
            binding.nameInputLayout.error = getString(R.string.error_field_required)
            return false
        }

        binding.nameInputLayout.error = null
        return true
    }

    private fun saveProfile() {
        val name = binding.nameEditText.text.toString().trim()
        val language = if (binding.englishRadioButton.isChecked) "en" else "fr"

        // If image was selected, upload it first
        if (selectedImageUri != null) {
            uploadImageAndSaveProfile(name, language)
        } else {
            // Otherwise just update the profile
            viewModel.updateProfile(name, language)
        }
    }

    private fun uploadImageAndSaveProfile(name: String, language: String) {
        binding.progressBar.visibility = View.VISIBLE
        binding.saveButton.isEnabled = false

        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser == null) {
            Toast.makeText(context, getString(R.string.error_not_logged_in), Toast.LENGTH_SHORT).show()
            binding.progressBar.visibility = View.GONE
            binding.saveButton.isEnabled = true
            return
        }

        // Create a unique filename
        val filename = "${currentUser.uid}_${UUID.randomUUID()}.jpg"
        val imageRef = storageRef.child(filename)

        selectedImageUri?.let { uri ->
            // Upload the image
            val uploadTask = imageRef.putFile(uri)

            // Register observers to listen for when the upload is done or if it fails
            uploadTask.continueWithTask { task ->
                if (!task.isSuccessful) {
                    task.exception?.let { throw it }
                }
                imageRef.downloadUrl
            }.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    // Get the download URL and update the profile
                    val downloadUrl = task.result.toString()
                    updateProfileWithImage(name, language, downloadUrl)
                } else {
                    // Handle failures
                    Toast.makeText(context, getString(R.string.error_uploading_image), Toast.LENGTH_SHORT).show()
                    binding.progressBar.visibility = View.GONE
                    binding.saveButton.isEnabled = true
                }
            }
        }
    }

    private fun updateProfileWithImage(name: String, language: String, imageUrl: String) {
        // Update profile with the image URL
        viewModel.updateProfile(name, language, imageUrl)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
