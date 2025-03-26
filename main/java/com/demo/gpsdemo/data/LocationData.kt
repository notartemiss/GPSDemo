package com.demo.gpsdemo.data

import java.text.SimpleDateFormat
import java.util.*

data class LocationData(
    val latitude: Double,
    val longitude: Double,
    val timestamp: String
) {
    fun getFormattedTime(): String {
        return try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()) // Adjust if needed
            val outputFormat = SimpleDateFormat("hh:mm a", Locale.getDefault()) // 12-hour format with AM/PM
            val date = inputFormat.parse(timestamp)
            outputFormat.format(date ?: Date())
        } catch (e: Exception) {
            timestamp // Fallback if parsing fails
        }
    }
}
