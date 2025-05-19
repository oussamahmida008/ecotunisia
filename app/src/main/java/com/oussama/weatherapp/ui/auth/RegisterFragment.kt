package com.oussama.weatherapp.ui.auth

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.snackbar.Snackbar
import com.oussama.weatherapp.R
import com.oussama.weatherapp.databinding.FragmentRegisterBinding
import com.oussama.weatherapp.ui.viewmodel.AuthViewModel
import com.oussama.weatherapp.utils.FirebaseErrorHandler
import kotlin.Exception

class RegisterFragment : Fragment() {

    private var _binding: FragmentRegisterBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: AuthViewModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRegisterBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Get the shared ViewModel
        viewModel = ViewModelProvider(requireActivity())[AuthViewModel::class.java]

        // Set up click listeners
        setupClickListeners()

        // Observe ViewModel state
        observeViewModel()
    }

    private fun setupClickListeners() {
        // Register button click
        binding.registerButton.setOnClickListener {
            val name = binding.nameEditText.text.toString().trim()
            val email = binding.emailEditText.text.toString().trim()
            val password = binding.passwordEditText.text.toString().trim()
            val confirmPassword = binding.confirmPasswordEditText.text.toString().trim()

            if (validateInputs(name, email, password, confirmPassword)) {
                viewModel.register(email, password, name)
            }
        }

        // Login text click (switch to login tab)
        binding.loginTextView.setOnClickListener {
            val viewPager = (activity as? AuthActivity)?.findViewById<androidx.viewpager2.widget.ViewPager2>(R.id.viewPager)
            viewPager?.currentItem = 0
        }
    }

    private fun observeViewModel() {
        // Observe loading state
        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
            binding.registerButton.isEnabled = !isLoading
        }

        // Observe register result
        viewModel.registerResult.observe(viewLifecycleOwner) { result ->
            result?.let { registerResult ->
                if (registerResult.isFailure) {
                    val throwable = registerResult.exceptionOrNull() ?: Exception("Unknown error")
                    val errorMessage = FirebaseErrorHandler.getErrorMessage(requireContext(), throwable)
                    Snackbar.make(binding.root, errorMessage, Snackbar.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun validateInputs(name: String, email: String, password: String, confirmPassword: String): Boolean {
        var isValid = true

        // Validate name
        if (name.isEmpty()) {
            binding.nameInputLayout.error = "Name is required"
            isValid = false
        } else {
            binding.nameInputLayout.error = null
        }

        // Validate email
        if (email.isEmpty()) {
            binding.emailInputLayout.error = "Email is required"
            isValid = false
        } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.emailInputLayout.error = "Invalid email format"
            isValid = false
        } else {
            binding.emailInputLayout.error = null
        }

        // Validate password
        if (password.isEmpty()) {
            binding.passwordInputLayout.error = "Password is required"
            isValid = false
        } else if (password.length < 6) {
            binding.passwordInputLayout.error = "Password must be at least 6 characters"
            isValid = false
        } else {
            binding.passwordInputLayout.error = null
        }

        // Validate confirm password
        if (confirmPassword.isEmpty()) {
            binding.confirmPasswordInputLayout.error = "Confirm password is required"
            isValid = false
        } else if (confirmPassword != password) {
            binding.confirmPasswordInputLayout.error = "Passwords do not match"
            isValid = false
        } else {
            binding.confirmPasswordInputLayout.error = null
        }

        return isValid
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
