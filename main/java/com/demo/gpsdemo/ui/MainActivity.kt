package com.demo.gpsdemo.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.demo.gpsdemo.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val sharedPreferences = getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
        val hasVisitedHome = sharedPreferences.getBoolean("hasVisitedHome", false)


        if (hasVisitedHome) {
            startActivity(Intent(this, DashBoardActivity::class.java))
            finish()
            return
        }


        val binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnLogin.setOnClickListener {
            sharedPreferences.edit().putBoolean("hasVisitedHome", true).apply()
            startActivity(Intent(this, DashBoardActivity::class.java))
            finish()
        }
    }
}

