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
import com.oussama.weatherapp.databinding.FragmentLoginBinding
import com.oussama.weatherapp.ui.viewmodel.AuthViewModel
import com.oussama.weatherapp.utils.FirebaseErrorHandler
import kotlin.Exception

class LoginFragment : Fragment() {

    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: AuthViewModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
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
        // Login button click
        binding.loginButton.setOnClickListener {
            val email = binding.emailEditText.text.toString().trim()
            val password = binding.passwordEditText.text.toString().trim()

            if (validateInputs(email, password)) {
                viewModel.login(email, password)
            }
        }

        // Register text click (switch to register tab)
        binding.registerTextView.setOnClickListener {
            val viewPager = (activity as? AuthActivity)?.findViewById<androidx.viewpager2.widget.ViewPager2>(R.id.viewPager)
            viewPager?.currentItem = 1
        }

        // Forgot password click
        binding.forgotPasswordTextView.setOnClickListener {
            // TODO: Implement forgot password functionality
            Toast.makeText(context, "Forgot password functionality not implemented yet", Toast.LENGTH_SHORT).show()
        }
    }

    private fun observeViewModel() {
        // Observe loading state
        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
            binding.loginButton.isEnabled = !isLoading
        }

        // Observe login result
        viewModel.loginResult.observe(viewLifecycleOwner) { result ->
            result?.let { loginResult ->
                if (loginResult.isFailure) {
                    val throwable = loginResult.exceptionOrNull() ?: Exception("Unknown error")
                    val errorMessage = FirebaseErrorHandler.getErrorMessage(requireContext(), throwable)
                    Snackbar.make(binding.root, errorMessage, Snackbar.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun validateInputs(email: String, password: String): Boolean {
        var isValid = true

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

        return isValid
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
