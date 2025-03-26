package com.demo.gpsdemo.ui.fragments

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.location.Location
import android.media.audiofx.EnvironmentalReverb
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.demo.gpsdemo.adapters.LocationAdapter
import com.demo.gpsdemo.data.LocationData
import com.demo.gpsdemo.databinding.FragmentGpsTrackBinding
import com.demo.gpsdemo.util.LocationService
import org.json.JSONArray
import org.json.JSONObject

class GpsTrackFragment : Fragment() {

    private var _binding: FragmentGpsTrackBinding? = null
    private val binding get() = _binding!!
    private lateinit var sharedPreferences: SharedPreferences
    private var isTracking = false
    private lateinit var locationAdapter: LocationAdapter
    private val locationList = mutableListOf<LocationData>()

    private val locationReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val latitude = intent?.getDoubleExtra("latitude", 0.0) ?: return
            val longitude = intent.getDoubleExtra("longitude", 0.0)
            val timestamp = intent.getStringExtra("timestamp") ?: ""

            Log.d("GpsTrackFragment", "✅ Received Location Update: Lat=$latitude, Lng=$longitude")

            requireActivity().runOnUiThread {
                saveLocation(latitude, longitude, timestamp)
                loadStoredLocations()
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentGpsTrackBinding.inflate(inflater, container, false)
        sharedPreferences = requireActivity().getSharedPreferences("GpsPrefs", Context.MODE_PRIVATE)

        isTracking = sharedPreferences.getBoolean("isTracking", false)
        Log.d("GpsTrackFragment", "Loaded isTracking: $isTracking")
        updateButtonState()

        // Restore saved interval
        val interval = sharedPreferences.getLong("interval_key", 10L)
        binding.etInterval.setText(interval.toString())

        binding.btnSaveInterval.setOnClickListener { saveInterval() }
        binding.btnTrack.setOnClickListener {
            Log.d("GpsTrackFragment", "✅ Track Button Clicked")
            toggleTracking()
        }


        setupRecyclerView()
        loadStoredLocations() // ✅ Load stored locations on start
        checkPermissions()

        return binding.root
    }

    private fun setupRecyclerView() {
        locationAdapter = LocationAdapter(mutableListOf())
        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = locationAdapter
        }
    }

    private fun saveInterval() {
        val interval = binding.etInterval.text.toString().toLongOrNull()
        if (interval != null && interval > 0) {
            sharedPreferences.edit().putLong("interval", interval).apply()
            Toast.makeText(requireContext(), "Interval saved: $interval sec", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(requireContext(), "Enter a valid interval", Toast.LENGTH_SHORT).show()
        }
    }

    private fun toggleTracking() {
        Log.d("GpsTrackFragment", "toggleTracking() called!")

        if (!hasLocationPermissions()) {
            Log.d("GpsTrackFragment", "Location permissions missing!")
            requestPermissions()
            return
        }

        val serviceIntent = Intent(requireContext(), LocationService::class.java)

        if (!isTracking) {
            Log.d("GpsTrackFragment", "Starting Tracking")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                requireContext().startForegroundService(serviceIntent)
            } else {
                requireContext().startService(serviceIntent)
            }
            isTracking = true
        } else {
            Log.d("GpsTrackFragment", "Stopping Tracking")
            requireActivity().stopService(serviceIntent)
            isTracking = false
        }

        sharedPreferences.edit().putBoolean("isTracking", isTracking).apply()
        updateButtonState()
    }



    private fun updateButtonState() {
        requireActivity().runOnUiThread {
            binding.btnTrack.text = if (isTracking) "Stop" else "Track"
            binding.btnTrack.isEnabled = true // ✅ Ensure button remains enabled
        }
    }

    private fun checkPermissions() {
        if (!hasLocationPermissions()) {
            requestPermissions()
        }
    }

    private fun hasLocationPermissions(): Boolean {
        return ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED &&
                (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q ||
                        ContextCompat.checkSelfPermission(
                            requireContext(),
                            Manifest.permission.ACCESS_BACKGROUND_LOCATION
                        ) == PackageManager.PERMISSION_GRANTED)
    }

    private fun requestPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            requestPermissions(
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE
            )
        } else {
            requestPermissions(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_BACKGROUND_LOCATION
                ),
                LOCATION_PERMISSION_REQUEST_CODE
            )
        }
    }





    private fun loadStoredLocations() {
        locationList.clear()
        val locationsJson = sharedPreferences.getString("locations", "[]")
        val locationsArray = JSONArray(locationsJson)

        Log.d("GpsTrackFragment", "Loading Locations: $locationsJson")

        if (locationsArray.length() == 0) {
            Log.e("GpsTrackFragment", "No locations found in SharedPreferences!")
        }

        val tempList = mutableListOf<LocationData>()
        for (i in 0 until locationsArray.length()) {
            val locationObj = locationsArray.getJSONObject(i)
            val lat = locationObj.getDouble("latitude")
            val lng = locationObj.getDouble("longitude")
            val timestamp = locationObj.getString("timestamp")
            tempList.add(LocationData(lat, lng, timestamp))
        }

        locationList.addAll(tempList.reversed())

        requireActivity().runOnUiThread {
            locationAdapter.updateList(locationList)  // ✅ Ensure RecyclerView updates
            binding.recyclerView.adapter?.notifyDataSetChanged()  // ✅ Force UI refresh
            binding.recyclerView.scrollToPosition(0)  // ✅ Auto-scroll to latest
        }

        Log.d("GpsTrackFragment", "Locations Loaded: ${locationList.size}")
    }




    private fun saveLocation(latitude: Double, longitude: Double, timestamp: String) {
        val sharedPreferences = requireActivity().getSharedPreferences("GpsPrefs", Context.MODE_PRIVATE)
        val locationsJson = sharedPreferences.getString("locations", "[]")
        val locationsArray = JSONArray(locationsJson)

        // Check if the last stored location is the same as the new one
        if (locationsArray.length() > 0) {
            val lastLocation = locationsArray.getJSONObject(locationsArray.length() - 1)
            val lastLat = lastLocation.getDouble("latitude")
            val lastLng = lastLocation.getDouble("longitude")
            if (lastLat == latitude && lastLng == longitude) {
                Log.d("GpsTrackFragment", "Duplicate location detected, skipping save.")
                return
            }
        }

        // Save new location
        val newLocation = JSONObject().apply {
            put("latitude", latitude)
            put("longitude", longitude)
            put("timestamp", timestamp)
        }

        locationsArray.put(newLocation)
        sharedPreferences.edit().putString("locations", locationsArray.toString()).apply()

        Log.d("GpsTrackFragment", "Location Saved: $newLocation")
        Log.d("GpsTrackFragment", "New Stored JSON: $locationsArray")
    }





    private var isReceiverRegistered = false

    private fun hasLocationPermission(): Boolean {
        return ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
    }

    override fun onResume() {
        super.onResume()
        if (!hasLocationPermission()) {
            Log.e("GpsTrackFragment", "Location permissions missing!")
            requestPermissions(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 1001)
        }
    }



    override fun onPause() {
        super.onPause()
        if (isReceiverRegistered) {
            requireContext().unregisterReceiver(locationReceiver)
            isReceiverRegistered = false
            Log.d("GpsTrackFragment", "BroadcastReceiver Unregistered")
        }
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1001
    }
}
