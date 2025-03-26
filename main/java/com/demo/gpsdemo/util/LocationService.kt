package com.demo.gpsdemo.util

import android.Manifest
import android.annotation.SuppressLint
import android.app.*
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.location.Location
import android.os.*
import android.provider.Settings
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import com.demo.gpsdemo.R
import com.google.android.gms.location.*
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

class LocationService : Service() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private lateinit var sharedPreferences: SharedPreferences
    private var lastLatitude: Double? = null
    private var lastLongitude: Double? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForegroundService()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        sharedPreferences = getSharedPreferences("GpsPrefs", Context.MODE_PRIVATE)
//
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
//            val pm = getSystemService(PowerManager::class.java)
//            if (!pm.isIgnoringBatteryOptimizations(packageName)) {
//                val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
//                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
//                startActivity(intent)
//            }
//        }

    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (hasLocationPermission()) {
            startLocationUpdates()
        } else {
            Log.e("LocationService", "❌ Location permission not granted, stopping service.")
            stopSelf()
        }
        return START_STICKY
    }

    private fun startForegroundService() {
        val notification = NotificationCompat.Builder(this, "location_channel")
            .setContentTitle("GPS Tracker")
            .setContentText("Tracking location in background")
            .setSmallIcon(R.drawable.ic_location)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        startForeground(1, notification)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "location_channel",
                "Location Updates",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    @SuppressLint("MissingPermission")
    private fun startLocationUpdates() {
        if (!hasLocationPermission()) return

        var interval = sharedPreferences.getLong("interval_key", 10L) * 1000 // Convert to milliseconds

        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, interval)
            .setMinUpdateIntervalMillis(2000) // Request update every 2 seconds
            .setMinUpdateDistanceMeters(1f)   // Allow updates even for 1-meter movement
            .setGranularity(Granularity.GRANULARITY_FINE) // More precise locations
            .setWaitForAccurateLocation(true) // Ensures accuracy
            .build()



        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                Log.d("LocationService", "Location update received: ${locationResult.lastLocation}")
                locationResult.lastLocation?.let { sendLocationUpdate(it) }
            }
        }

        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())
    }

    private fun sendLocationUpdate(location: Location) {
        val latitude = location.latitude
        val longitude = location.longitude
        val timestamp = getCurrentTimeFormatted()

        if (lastLatitude != null && lastLongitude != null) {
            val distance = calculateDistance(lastLatitude!!, lastLongitude!!, latitude, longitude)
            if (distance < 3) {  // Allow updates for small movements
                Log.d("LocationService", "Skipping location update: too close ($distance m)")
                return
            }

        }

        lastLatitude = latitude
        lastLongitude = longitude

        saveLocation(location)

        val intent = Intent("GPS_LOCATION_UPDATE").apply {
            putExtra("latitude", latitude)
            putExtra("longitude", longitude)
            putExtra("timestamp", timestamp)
        }
        sendBroadcast(intent)

        Log.d("LocationService", "Broadcasted location: $latitude, $longitude at $timestamp")
    }

    private fun saveLocation(location: Location) {
        val locationsJson = sharedPreferences.getString("locations", "[]")
        val locationsArray = JSONArray(locationsJson)

        // Prevent duplicate entries
        if (locationsArray.length() > 0) {
            val lastLocation = locationsArray.getJSONObject(locationsArray.length() - 1)
            if (lastLocation.getDouble("latitude") == location.latitude &&
                lastLocation.getDouble("longitude") == location.longitude) {
                Log.d("LocationService", "Skipping duplicate location")
                return
            }
        }

        val newLocation = JSONObject().apply {
            put("latitude", location.latitude)
            put("longitude", location.longitude)
            put("timestamp", getCurrentTimeFormatted())
        }

        locationsArray.put(newLocation)
        sharedPreferences.edit().putString("locations", locationsArray.toString()).apply()

        Log.d("LocationService", "✅ Location Saved: $newLocation")
    }



    private fun hasLocationPermission(): Boolean {
        return ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
    }

    private fun getCurrentTimeFormatted(): String {
        val sdf = SimpleDateFormat("hh:mm a", Locale.getDefault())
        return sdf.format(Date())
    }

    private fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Float {
        val results = FloatArray(1)
        Location.distanceBetween(lat1, lon1, lat2, lon2, results)
        return results[0]
    }

    override fun onDestroy() {
        super.onDestroy()
        fusedLocationClient.removeLocationUpdates(locationCallback)

        // Explicitly close the FusedLocationProviderClient
        fusedLocationClient.flushLocations()

        stopForeground(true)
        Log.d("LocationService", "❌ Service destroyed, cleaned up resources.")
    }


    override fun onBind(intent: Intent?): IBinder? = null
}

