package com.oussama.weatherapp.ui.profile

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatDelegate
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.oussama.weatherapp.R
import com.oussama.weatherapp.databinding.FragmentProfileBinding
import com.oussama.weatherapp.ui.auth.AuthActivity
import com.oussama.weatherapp.ui.viewmodel.ProfileViewModel

class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var viewModel: ProfileViewModel
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Initialize ViewModel
        viewModel = ViewModelProvider(this)[ProfileViewModel::class.java]
        
        // Set up UI components
        setupLanguageSpinner()
        setupDarkModeSwitch()
        
        // Set up click listeners
        setupClickListeners()
        
        // Observe ViewModel state
        observeViewModel()
        
        // Load user profile
        viewModel.loadUserProfile()
    }
    
    private fun setupLanguageSpinner() {
        val languages = arrayOf("English", "French")
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, languages)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.languageSpinner.adapter = adapter
        
        binding.languageSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val language = if (position == 0) "en" else "fr"
                viewModel.changeLanguage(requireContext(), language)
            }
            
            override fun onNothingSelected(parent: AdapterView<*>?) {
                // Do nothing
            }
        }
    }
    
    private fun setupDarkModeSwitch() {
        // Set initial state based on current night mode
        binding.darkModeSwitch.isChecked = 
            AppCompatDelegate.getDefaultNightMode() == AppCompatDelegate.MODE_NIGHT_YES
        
        binding.darkModeSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            } else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            }
        }
    }
    
    private fun setupClickListeners() {
        // Edit profile button click
        binding.editProfileButton.setOnClickListener {
            findNavController().navigate(R.id.action_profileFragment_to_editProfileFragment)
        }
        
        // Logout button click
        binding.logoutButton.setOnClickListener {
            viewModel.logout()
            startActivity(Intent(requireContext(), AuthActivity::class.java))
            requireActivity().finish()
        }
        
        // About section click
        binding.aboutLayout.setOnClickListener {
            Toast.makeText(context, "About EcoExplorer: Environmental awareness app for Tunisia", Toast.LENGTH_LONG).show()
        }
    }
    
    private fun observeViewModel() {
        // Observe user data
        viewModel.user.observe(viewLifecycleOwner) { user ->
            user?.let {
                binding.nameTextView.text = it.name
                binding.emailTextView.text = it.email
                binding.languageTextView.text = "Language: ${if (it.language == "en") "English" else "French"}"
                
                // Set language spinner selection
                binding.languageSpinner.setSelection(if (it.language == "en") 0 else 1)
                
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
        }
        
        // Observe error state
        viewModel.error.observe(viewLifecycleOwner) { error ->
            error?.let {
                Toast.makeText(context, it, Toast.LENGTH_LONG).show()
                viewModel.clearError()
            }
        }
    }
    
    override fun onResume() {
        super.onResume()
        // Refresh user profile when returning to this fragment
        viewModel.loadUserProfile()
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
