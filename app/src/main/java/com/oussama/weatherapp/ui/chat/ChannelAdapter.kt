package com.oussama.weatherapp.ui.chat

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.oussama.weatherapp.data.model.Channel
import com.oussama.weatherapp.databinding.ItemChannelBinding
import java.text.SimpleDateFormat
import java.util.Locale

class ChannelAdapter(private val onChannelClick: (Channel) -> Unit) : 
    ListAdapter<Channel, ChannelAdapter.ChannelViewHolder>(ChannelDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChannelViewHolder {
        val binding = ItemChannelBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ChannelViewHolder(binding, onChannelClick)
    }

    override fun onBindViewHolder(holder: ChannelViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ChannelViewHolder(
        private val binding: ItemChannelBinding,
        private val onChannelClick: (Channel) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {
        
        private val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
        
        fun bind(channel: Channel) {
            binding.apply {
                channelNameTextView.text = channel.name
                channelDescriptionTextView.text = channel.description
                creatorTextView.text = "Created by: ${channel.creatorName}"
                memberCountTextView.text = "${channel.memberCount} members"
                
                // Set click listener
                root.setOnClickListener {
                    onChannelClick(channel)
                }
            }
        }
    }

    class ChannelDiffCallback : DiffUtil.ItemCallback<Channel>() {
        override fun areItemsTheSame(oldItem: Channel, newItem: Channel): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Channel, newItem: Channel): Boolean {
            return oldItem == newItem
        }
    }
}
