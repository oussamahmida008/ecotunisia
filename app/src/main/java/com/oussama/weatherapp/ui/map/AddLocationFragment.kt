package com.oussama.weatherapp.ui.map

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.google.firebase.storage.FirebaseStorage
import com.oussama.weatherapp.databinding.FragmentAddLocationBinding
import com.oussama.weatherapp.ui.viewmodel.MapViewModel
import com.oussama.weatherapp.utils.MapUtils
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AddLocationFragment : Fragment() {

    private var _binding: FragmentAddLocationBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: MapViewModel

    private var selectedLatitude: Double = 0.0
    private var selectedLongitude: Double = 0.0
    private var photoUri: Uri? = null
    private var photoUrl: String? = null
    private var currentMarker: Marker? = null

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            takePhoto()
        } else {
            Toast.makeText(context, "Camera permission is required to take photos", Toast.LENGTH_SHORT).show()
        }
    }

    private val takePictureLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            photoUri?.let { uri ->
                binding.photoPreviewImageView.setImageURI(uri)
                binding.photoPreviewImageView.visibility = View.VISIBLE
                uploadPhotoToFirebase(uri)
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAddLocationBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize ViewModel
        viewModel = ViewModelProvider(requireActivity())[MapViewModel::class.java]

        // Set up MapView
        setupMapView()

        // Set up click listeners
        setupClickListeners()

        // Observe ViewModel state
        observeViewModel()

        // Handle back button press
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                // Show confirmation dialog if user has entered data
                if (hasUserEnteredData()) {
                    showDiscardChangesDialog()
                } else {
                    // No data entered, just navigate back
                    navigateBack()
                }
            }
        })
    }

    private fun setupMapView() {
        // Configure the map
        MapUtils.configureMapView(binding.mapView, requireContext())

        // Center map on Tunisia
        MapUtils.centerMapOnTunisia(binding.mapView)

        // Add map click listener
        val mapEventsReceiver = object : MapEventsReceiver {
            override fun singleTapConfirmedHelper(p: GeoPoint): Boolean {
                selectedLatitude = p.latitude
                selectedLongitude = p.longitude
                updateMapMarker(p)
                return true
            }

            override fun longPressHelper(p: GeoPoint): Boolean {
                return false
            }
        }

        val mapEventsOverlay = MapEventsOverlay(mapEventsReceiver)
        binding.mapView.overlays.add(mapEventsOverlay)
    }

    private fun updateMapMarker(point: GeoPoint) {
        // Remove existing marker if any
        currentMarker?.let {
            binding.mapView.overlays.remove(it)
        }

        // Create new marker
        val marker = Marker(binding.mapView)
        marker.position = point
        marker.title = "Selected Location"

        // Set marker icon
        val icon: Drawable? = ContextCompat.getDrawable(requireContext(), android.R.drawable.ic_menu_mylocation)
        marker.icon = icon

        // Add marker to map
        binding.mapView.overlays.add(marker)

        // Store reference to current marker
        currentMarker = marker

        // Refresh the map
        binding.mapView.invalidate()
    }

    private fun setupClickListeners() {
        // Back button click
        binding.backButton.setOnClickListener {
            // Show confirmation dialog if user has entered data
            if (hasUserEnteredData()) {
                showDiscardChangesDialog()
            } else {
                // No data entered, just navigate back
                navigateBack()
            }
        }

        // Save button click
        binding.saveButton.setOnClickListener {
            val title = binding.titleEditText.text.toString().trim()
            val description = binding.descriptionEditText.text.toString().trim()

            if (validateInputs(title, description)) {
                viewModel.addLocation(title, description, selectedLatitude, selectedLongitude, photoUrl)
            }
        }

        // Take photo button click
        binding.takePhotoButton.setOnClickListener {
            if (ContextCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.CAMERA
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                takePhoto()
            } else {
                requestPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }
    }

    /**
     * Check if user has entered any data
     */
    private fun hasUserEnteredData(): Boolean {
        return binding.titleEditText.text.toString().isNotEmpty() ||
                binding.descriptionEditText.text.toString().isNotEmpty() ||
                photoUri != null ||
                (selectedLatitude != 0.0 && selectedLongitude != 0.0)
    }

    /**
     * Show dialog to confirm discarding changes
     */
    private fun showDiscardChangesDialog() {
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle(com.oussama.weatherapp.R.string.discard_changes_title)
            .setMessage(com.oussama.weatherapp.R.string.discard_changes_message)
            .setPositiveButton(com.oussama.weatherapp.R.string.discard) { _, _ ->
                navigateBack()
            }
            .setNegativeButton(com.oussama.weatherapp.R.string.cancel, null)
            .show()
    }

    /**
     * Navigate back to previous screen
     */
    private fun navigateBack() {
        findNavController().popBackStack()
    }

    private fun takePhoto() {
        val photoFile = createImageFile()
        photoUri = FileProvider.getUriForFile(
            requireContext(),
            "${requireContext().packageName}.fileprovider",
            photoFile
        )

        val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri)
        takePictureLauncher.launch(takePictureIntent)
    }

    private fun createImageFile(): File {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val imageFileName = "JPEG_${timeStamp}_"
        val storageDir = requireContext().getExternalFilesDir(null)
        return File.createTempFile(imageFileName, ".jpg", storageDir)
    }

    private fun uploadPhotoToFirebase(uri: Uri) {
        binding.progressBar.visibility = View.VISIBLE

        val storageRef = FirebaseStorage.getInstance().reference
        val photoRef = storageRef.child("location_images/${System.currentTimeMillis()}.jpg")

        photoRef.putFile(uri)
            .continueWithTask { task ->
                if (!task.isSuccessful) {
                    task.exception?.let { throw it }
                }
                photoRef.downloadUrl
            }
            .addOnCompleteListener { task ->
                binding.progressBar.visibility = View.GONE
                if (task.isSuccessful) {
                    photoUrl = task.result.toString()
                    Toast.makeText(context, "Photo uploaded successfully", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "Failed to upload photo", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun observeViewModel() {
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

        // Observe add location result
        viewModel.addLocationResult.observe(viewLifecycleOwner) { result ->
            result?.onSuccess {
                Toast.makeText(context, "Location added successfully", Toast.LENGTH_SHORT).show()
                findNavController().popBackStack()
                viewModel.resetAddLocationResult()
            }
        }
    }

    private fun validateInputs(title: String, description: String): Boolean {
        var isValid = true

        // Validate title
        if (title.isEmpty()) {
            binding.titleInputLayout.error = "Title is required"
            isValid = false
        } else {
            binding.titleInputLayout.error = null
        }

        // Validate description
        if (description.isEmpty()) {
            binding.descriptionInputLayout.error = "Description is required"
            isValid = false
        } else {
            binding.descriptionInputLayout.error = null
        }

        // Validate location
        if (selectedLatitude == 0.0 && selectedLongitude == 0.0) {
            Toast.makeText(context, "Please select a location on the map", Toast.LENGTH_SHORT).show()
            isValid = false
        }

        return isValid
    }

    override fun onResume() {
        super.onResume()
        binding.mapView.onResume()
    }

    override fun onPause() {
        super.onPause()
        binding.mapView.onPause()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
