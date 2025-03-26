package com.demo.gpsdemo.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.demo.gpsdemo.R
import com.demo.gpsdemo.databinding.ActivityDashBoardBinding
import com.demo.gpsdemo.ui.fragments.GpsTrackFragment
import com.demo.gpsdemo.ui.fragments.HistoryFragment
import com.demo.gpsdemo.ui.fragments.HomeFragment
import com.demo.gpsdemo.ui.fragments.SettingFragment
import com.demo.gpsdemo.util.LocationService

class DashBoardActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDashBoardBinding
    private val REQUEST_PERMISSIONS_CODE = 100


    private val permissions = arrayOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION,
        Manifest.permission.FOREGROUND_SERVICE_LOCATION
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding= ActivityDashBoardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        window.statusBarColor = ContextCompat.getColor(this, R.color.black)


        // Load default fragment
        loadFragment(HomeFragment())

        // Handle navigation selection
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> loadFragment(HomeFragment())
                R.id.nav_history -> loadFragment(HistoryFragment())
                R.id.nav_gps -> loadFragment(GpsTrackFragment())
                R.id.nav_settings -> loadFragment(SettingFragment())
            }
            true
        }


    }


    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
    }


    private fun requestPermissionsIfNeeded() {
        if (permissions.all { ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED }) {
            startLocationService()  // ✅ Start service if permissions are already granted
        } else {
            ActivityCompat.requestPermissions(this, permissions, REQUEST_PERMISSIONS_CODE)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == REQUEST_PERMISSIONS_CODE) {
            if (grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                startLocationService()  // ✅ Start service after permissions are granted
            } else {
                Toast.makeText(this, "Permissions required for GPS tracking!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun startLocationService() {
        val serviceIntent = Intent(this, LocationService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent)  // ✅ Required for Android 8+ (Oreo)
        } else {
            startService(serviceIntent)
        }
    }
}