package com.oussama.weatherapp.ui.chat

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.oussama.weatherapp.R
import com.oussama.weatherapp.databinding.FragmentCreateChannelBinding
import com.oussama.weatherapp.ui.viewmodel.ChatViewModel

/**
 * Fragment for creating a new chat channel
 */
class CreateChannelFragment : Fragment() {

    private var _binding: FragmentCreateChannelBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var viewModel: ChatViewModel
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCreateChannelBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Initialize ViewModel
        viewModel = ViewModelProvider(requireActivity())[ChatViewModel::class.java]
        
        // Set up click listeners
        setupClickListeners()
        
        // Observe ViewModel state
        observeViewModel()
        
        // Reset any previous create channel result
        viewModel.resetCreateChannelResult()
    }
    
    private fun setupClickListeners() {
        // Create button click
        binding.createButton.setOnClickListener {
            val name = binding.channelNameEditText.text.toString().trim()
            val description = binding.channelDescriptionEditText.text.toString().trim()
            
            if (validateInput(name, description)) {
                viewModel.createChannel(name, description)
            }
        }
    }
    
    private fun validateInput(name: String, description: String): Boolean {
        if (name.isEmpty()) {
            binding.channelNameInputLayout.error = getString(R.string.error_field_required)
            return false
        } else {
            binding.channelNameInputLayout.error = null
        }
        
        if (description.isEmpty()) {
            binding.channelDescriptionInputLayout.error = getString(R.string.error_field_required)
            return false
        } else {
            binding.channelDescriptionInputLayout.error = null
        }
        
        return true
    }
    
    private fun observeViewModel() {
        // Observe loading state
        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
            binding.createButton.isEnabled = !isLoading
        }
        
        // Observe error state
        viewModel.error.observe(viewLifecycleOwner) { error ->
            error?.let {
                Toast.makeText(context, it, Toast.LENGTH_LONG).show()
                viewModel.clearError()
            }
        }
        
        // Observe create channel result
        viewModel.createChannelResult.observe(viewLifecycleOwner) { result ->
            result?.let {
                if (it.isSuccess) {
                    Toast.makeText(context, getString(R.string.channel_created_successfully), Toast.LENGTH_SHORT).show()
                    findNavController().navigateUp()
                }
            }
        }
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
