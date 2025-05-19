package com.oussama.weatherapp.ui.map

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.bumptech.glide.Glide
import com.oussama.weatherapp.R
import com.oussama.weatherapp.data.model.Location
import com.oussama.weatherapp.databinding.FragmentLocationDetailBinding
import com.oussama.weatherapp.ui.viewmodel.MapViewModel
import com.oussama.weatherapp.utils.MapUtils
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.overlay.Marker
import java.text.SimpleDateFormat
import java.util.Locale

class LocationDetailFragment : Fragment() {

    private var _binding: FragmentLocationDetailBinding? = null
    private val binding get() = _binding!!

    private lateinit var mapViewModel: MapViewModel
    private val args: LocationDetailFragmentArgs by navArgs()
    private var location: Location? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLocationDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize ViewModels
        mapViewModel = ViewModelProvider(requireActivity())[MapViewModel::class.java]

        // Set up toolbar
        setupToolbar()

        // Set up MapView
        setupMapView()

        // Set up click listeners
        setupClickListeners()

        // Load location details
        loadLocationDetails(args.locationId)

        // Observe ViewModel state
        observeViewModel()
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            findNavController().popBackStack()
        }
    }

    private fun setupMapView() {
        // Configure the map
        MapUtils.configureMapView(binding.mapView, requireContext())
    }

    private fun setupClickListeners() {
        // Copy location button
        binding.copyLocationButton.setOnClickListener {
            copyLocationToClipboard()
        }
    }

    private fun loadLocationDetails(locationId: String) {
        binding.progressBar.visibility = View.VISIBLE
        mapViewModel.loadLocationDetails(locationId)
    }

    private fun observeViewModel() {
        // Observe location details
        mapViewModel.locationDetails.observe(viewLifecycleOwner) { locationResult ->
            binding.progressBar.visibility = View.GONE

            locationResult?.onSuccess { loc ->
                location = loc
                updateUI(loc)
            }

            locationResult?.onFailure { error ->
                Toast.makeText(context, error.message, Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun updateUI(location: Location) {
        // Update toolbar title
        binding.toolbar.title = location.title

        // Update text views
        binding.titleTextView.text = location.title
        binding.userNameTextView.text = getString(R.string.added_by, location.userName)

        // Format date
        val dateFormat = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
        binding.dateTextView.text = getString(R.string.added_on, dateFormat.format(location.timestamp))

        // Update coordinates
        binding.coordinatesTextView.text = getString(
            R.string.coordinates,
            location.latitude,
            location.longitude
        )

        // Update description
        binding.descriptionTextView.text = location.description

        // Update image if available
        if (location.imageUrl != null && location.imageUrl.isNotEmpty()) {
            binding.locationImageView.visibility = View.VISIBLE
            Glide.with(this)
                .load(location.imageUrl)
                .centerCrop()
                .into(binding.locationImageView)
        } else {
            binding.locationImageView.visibility = View.GONE
        }

        // Update map
        updateMapMarker(location)
    }

    private fun updateMapMarker(location: Location) {
        // Clear existing markers
        binding.mapView.overlays.removeIf { it is Marker }

        // Create new marker
        val marker = Marker(binding.mapView)
        marker.position = GeoPoint(location.latitude, location.longitude)
        marker.title = location.title
        marker.snippet = location.description

        // Set marker icon
        val icon: Drawable? = ContextCompat.getDrawable(requireContext(), android.R.drawable.ic_menu_mylocation)
        marker.icon = icon

        // Add marker to map
        binding.mapView.overlays.add(marker)

        // Center map on marker
        MapUtils.centerMapOn(binding.mapView, location.latitude, location.longitude, 15.0)

        // Refresh the map
        binding.mapView.invalidate()
    }

    private fun copyLocationToClipboard() {
        location?.let { loc ->
            // Format a comprehensive location text with all relevant information
            val locationText = buildString {
                append("${loc.title}\n")
                append("${loc.description}\n\n")
                append("Coordinates: ${loc.latitude}, ${loc.longitude}\n")
                append("Added by: ${loc.userName}\n")

                // Format date
                val dateFormat = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
                append("Added on: ${dateFormat.format(loc.timestamp)}\n")

                // Add URL for Google Maps
                append("\nView on Google Maps:\n")
                append("https://www.google.com/maps/search/?api=1&query=${loc.latitude},${loc.longitude}")
            }

            // Copy to clipboard
            val clipboard = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("Location Details", locationText)
            clipboard.setPrimaryClip(clip)

            // Show success message
            Toast.makeText(context, R.string.location_copied, Toast.LENGTH_SHORT).show()
        }
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
        binding.mapView.onDetach()
        _binding = null
    }
}
