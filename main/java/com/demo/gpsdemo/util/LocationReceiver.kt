package com.demo.gpsdemo.util

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class LocationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        val latitude = intent?.getDoubleExtra("latitude", 0.0) ?: return
        val longitude = intent.getDoubleExtra("longitude", 0.0)
        val timestamp = intent.getStringExtra("timestamp") ?: ""

        Log.d("LocationReceiver", "✅ Received Location Update: Lat=$latitude, Lng=$longitude")
    }
}
