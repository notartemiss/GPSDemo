package com.demo.gpsdemo.adapters

import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.demo.gpsdemo.data.LocationData
import com.demo.gpsdemo.databinding.ItemLocationBinding
import java.text.SimpleDateFormat
import java.util.*

class LocationAdapter(private var locationList: MutableList<LocationData>) :
    RecyclerView.Adapter<LocationAdapter.LocationViewHolder>() {

    class LocationViewHolder(private val binding: ItemLocationBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(location: LocationData) {
            binding.tvLatitude.text = "Lat: ${location.latitude}"
            binding.tvLongitude.text = "Lng: ${location.longitude}"
            binding.tvTimestamp.text = "Time: ${convertTo12HourFormat(location.timestamp)}"
        }

        // Convert time to 12-hour format
        private fun convertTo12HourFormat(timestamp: String): String {
            return try {
                val inputFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                val outputFormat = SimpleDateFormat("hh:mm a", Locale.getDefault()) // 12-hour format
                val date = inputFormat.parse(timestamp)
                outputFormat.format(date ?: Date()) // Return formatted date or current date as fallback
            } catch (e: Exception) {
                timestamp // Return original timestamp in case of error
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LocationViewHolder {
        val binding = ItemLocationBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return LocationViewHolder(binding)
    }

    override fun onBindViewHolder(holder: LocationViewHolder, position: Int) {
        holder.bind(locationList[position])
    }

    override fun getItemCount() = locationList.size

    fun updateList(newList: List<LocationData>) {
        Log.d("LocationAdapter", "Updating RecyclerView on Phone with ${newList.size} items") // Debug log
        locationList.clear()
        locationList.addAll(newList)
        notifyDataSetChanged()
    }


}

