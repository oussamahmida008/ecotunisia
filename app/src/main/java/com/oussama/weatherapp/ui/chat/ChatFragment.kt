package com.oussama.weatherapp.ui.chat

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.oussama.weatherapp.R
import com.oussama.weatherapp.databinding.FragmentChatBinding
import com.oussama.weatherapp.ui.viewmodel.ChatViewModel

class ChatFragment : Fragment() {

    private var _binding: FragmentChatBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: ChatViewModel
    private lateinit var adapter: ChannelAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentChatBinding.inflate(inflater, container, false)
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

        // Observe ViewModel state
        observeViewModel()

        // Load channels
        viewModel.loadChannels()
    }

    private fun setupRecyclerView() {
        adapter = ChannelAdapter { channel ->
            viewModel.selectChannel(channel)
            // Navigate to channel detail with the channel ID as an argument
            val bundle = Bundle().apply {
                putString("channelId", channel.id)
            }
            findNavController().navigate(R.id.action_chatFragment_to_channelDetailFragment, bundle)
        }

        binding.channelsRecyclerView.adapter = adapter
        binding.channelsRecyclerView.layoutManager = LinearLayoutManager(requireContext())
    }

    private fun setupClickListeners() {
        // Create channel button click
        binding.createChannelButton.setOnClickListener {
            findNavController().navigate(R.id.action_chatFragment_to_createChannelFragment)
        }
    }

    private fun observeViewModel() {
        // Observe channels list
        viewModel.channels.observe(viewLifecycleOwner) { channels ->
            adapter.submitList(channels)
            binding.emptyTextView.visibility = if (channels.isEmpty()) View.VISIBLE else View.GONE
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
        // Refresh channels when returning to this fragment
        viewModel.loadChannels()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
