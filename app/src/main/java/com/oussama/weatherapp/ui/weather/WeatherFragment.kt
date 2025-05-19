package com.oussama.weatherapp.ui.weather

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import com.oussama.weatherapp.databinding.FragmentWeatherBinding
import com.oussama.weatherapp.ui.viewmodel.WeatherViewModel

class WeatherFragment : Fragment() {

    private var _binding: FragmentWeatherBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var viewModel: WeatherViewModel
    private lateinit var adapter: WeatherAdapter
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentWeatherBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Initialize ViewModel
        viewModel = ViewModelProvider(this)[WeatherViewModel::class.java]
        
        // Set up RecyclerView
        setupRecyclerView()
        
        // Observe ViewModel state
        observeViewModel()
        
        // Load weather data
        viewModel.loadWeatherData()
    }
    
    private fun setupRecyclerView() {
        adapter = WeatherAdapter()
        binding.weatherRecyclerView.adapter = adapter
        binding.weatherRecyclerView.layoutManager = GridLayoutManager(requireContext(), 2)
    }
    
    private fun observeViewModel() {
        // Observe weather list
        viewModel.weatherList.observe(viewLifecycleOwner) { weatherList ->
            adapter.submitList(weatherList)
            binding.errorTextView.visibility = View.GONE
        }
        
        // Observe loading state
        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        }
        
        // Observe error state
        viewModel.error.observe(viewLifecycleOwner) { error ->
            error?.let {
                binding.errorTextView.text = it
                binding.errorTextView.visibility = View.VISIBLE
                Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            }
        }
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
