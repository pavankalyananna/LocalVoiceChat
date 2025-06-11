package com.example.localvoicechat

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.appcompat.app.AppCompatActivity
import com.example.localvoicechat.databinding.ActivityMainBinding
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.createHotspotButton.setOnClickListener {
            val name = binding.nameEditText.text.toString().trim()
            if (name.isNotEmpty()) {
                requestPermissions(name, true)
            } else {
                binding.nameInputLayout.error = "Please enter a name"
            }
        }

        binding.joinHotspotButton.setOnClickListener {
            val name = binding.nameEditText.text.toString().trim()
            if (name.isNotEmpty()) {
                requestPermissions(name, false)
            } else {
                binding.nameInputLayout.error = "Please enter a name"
            }
        }
    }

    private fun requestPermissions(name: String, isHost: Boolean) {
        val requiredPermissions = arrayOf(
            android.Manifest.permission.RECORD_AUDIO,
            android.Manifest.permission.ACCESS_FINE_LOCATION,
            android.Manifest.permission.ACCESS_COARSE_LOCATION
        )

        // Check if all permissions are granted
        val allPermissionsGranted = requiredPermissions.all {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }

        if (allPermissionsGranted) {
            if (isHost) {
                enableHotspot(name)
            } else {
                joinHotspot(name)
            }
        } else {
            ActivityCompat.requestPermissions(this, requiredPermissions, 100)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 100) {
            val name = binding.nameEditText.text.toString().trim()
            if (grantResults.all { it == PackageManager.PERMISSION_GRANTED } && name.isNotEmpty()) {
                if (intent.getBooleanExtra("IS_HOST", false)) {
                    enableHotspot(name)
                } else {
                    joinHotspot(name)
                }
            } else {
                binding.nameInputLayout.error = "Required permissions denied"
            }
        }
    }

    private fun enableHotspot(name: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startActivity(Intent(Settings.ACTION_WIFI_SETTINGS))
        } else {
            startActivity(Intent(Settings.ACTION_WIRELESS_SETTINGS))
        }
        startGroupChat(name, true)
    }

    private fun joinHotspot(name: String) {
        startGroupChat(name, false)
    }

    private fun startGroupChat(name: String, isHost: Boolean) {
        Intent(this, GroupChatActivity::class.java).apply {
            putExtra("USER_NAME", name)
            putExtra("IS_HOST", isHost)
            startActivity(this)
        }
    }
}