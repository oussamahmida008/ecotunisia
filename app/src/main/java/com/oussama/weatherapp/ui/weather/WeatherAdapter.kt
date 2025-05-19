package com.oussama.weatherapp.ui.weather

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.oussama.weatherapp.data.model.Weather
import com.oussama.weatherapp.databinding.ItemWeatherBinding

class WeatherAdapter : ListAdapter<Weather, WeatherAdapter.WeatherViewHolder>(WeatherDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WeatherViewHolder {
        val binding = ItemWeatherBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return WeatherViewHolder(binding)
    }

    override fun onBindViewHolder(holder: WeatherViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class WeatherViewHolder(private val binding: ItemWeatherBinding) : RecyclerView.ViewHolder(binding.root) {
        
        fun bind(weather: Weather) {
            binding.apply {
                cityTextView.text = weather.city
                temperatureTextView.text = "${weather.temperature.toInt()}°C"
                conditionTextView.text = weather.condition
                humidityTextView.text = "${weather.humidity}%"
                windTextView.text = "${weather.windSpeed} km/h"
                feelsLikeTextView.text = "${weather.feelsLike.toInt()}°C"
                
                // Load weather icon
                Glide.with(weatherIconImageView.context)
                    .load(weather.iconUrl)
                    .into(weatherIconImageView)
            }
        }
    }

    class WeatherDiffCallback : DiffUtil.ItemCallback<Weather>() {
        override fun areItemsTheSame(oldItem: Weather, newItem: Weather): Boolean {
            return oldItem.city == newItem.city
        }

        override fun areContentsTheSame(oldItem: Weather, newItem: Weather): Boolean {
            return oldItem == newItem
        }
    }
}
