package com.oussama.weatherapp.ui.auth

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModelProvider
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.tabs.TabLayoutMediator
import com.oussama.weatherapp.R
import com.oussama.weatherapp.databinding.ActivityAuthBinding
import com.oussama.weatherapp.ui.main.MainActivity
import com.oussama.weatherapp.ui.viewmodel.AuthViewModel
import com.oussama.weatherapp.utils.FirebaseErrorHandler
import com.oussama.weatherapp.utils.GooglePlayServicesHelper
import kotlin.Exception

class AuthActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAuthBinding
    private lateinit var viewModel: AuthViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAuthBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Update Android security provider
        GooglePlayServicesHelper.updateAndroidSecurityProvider(this)

        // Check if Google Play Services is installed
        if (!GooglePlayServicesHelper.isGooglePlayServicesInstalled(this)) {
            Toast.makeText(this, getString(R.string.error_google_play_services), Toast.LENGTH_LONG).show()
            // Try to fix Google Play Services issues
            if (!GooglePlayServicesHelper.fixGooglePlayServicesIssues(this)) {
                // If issues can't be fixed, finish the activity
                finish()
                return
            }
        }

        // Check Google Play Services availability
        if (!GooglePlayServicesHelper.isGooglePlayServicesAvailable(this)) {
            Toast.makeText(this, getString(R.string.error_google_play_services), Toast.LENGTH_LONG).show()
            // Try to fix Google Play Services issues
            if (!GooglePlayServicesHelper.fixGooglePlayServicesIssues(this)) {
                // If issues can't be fixed, finish the activity
                finish()
                return
            }
        }

        try {
            // Initialize ViewModel
            viewModel = ViewModelProvider(this)[AuthViewModel::class.java]

            // Check if user is already logged in
            if (viewModel.isUserLoggedIn()) {
                navigateToMainActivity()
                return
            }

            // Set up ViewPager with tabs
            setupViewPager()

            // Observe login/register results
            observeAuthResults()
        } catch (e: Exception) {
            // Handle initialization errors
            Toast.makeText(this, "Authentication initialization failed", Toast.LENGTH_LONG).show()
            e.printStackTrace()
            finish()
        }
    }

    private fun setupViewPager() {
        val adapter = AuthPagerAdapter(this)
        binding.viewPager.adapter = adapter

        // Connect TabLayout with ViewPager2
        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
            tab.text = when (position) {
                0 -> getString(R.string.login)
                1 -> getString(R.string.register)
                else -> null
            }
        }.attach()
    }

    private fun observeAuthResults() {
        // Observe login result
        viewModel.loginResult.observe(this) { result ->
            result?.let { loginResult ->
                if (loginResult.isSuccess) {
                    navigateToMainActivity()
                }
            }
        }

        // Observe register result
        viewModel.registerResult.observe(this) { result ->
            result?.let { registerResult ->
                if (registerResult.isSuccess) {
                    navigateToMainActivity()
                }
            }
        }
    }

    private fun navigateToMainActivity() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }

    // ViewPager adapter for Login and Register fragments
    private inner class AuthPagerAdapter(fa: FragmentActivity) : FragmentStateAdapter(fa) {
        override fun getItemCount(): Int = 2

        override fun createFragment(position: Int): Fragment {
            return when (position) {
                0 -> LoginFragment()
                1 -> RegisterFragment()
                else -> throw IllegalArgumentException("Invalid position: $position")
            }
        }
    }
}
