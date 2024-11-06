package com.example.sweethome

import android.content.Context
import android.content.SharedPreferences
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts

class PermissionManager(private val context: Context) {

    private val sharedPreferences: SharedPreferences =
        context.getSharedPreferences("PermissionPrefs", Context.MODE_PRIVATE)

    private fun savePermissionStatus(isGranted: Boolean) {
        sharedPreferences.edit().putBoolean("AUDIO_PERMISSION_GRANTED", isGranted).apply()
    }

    fun isPermissionGranted(): Boolean {
        return sharedPreferences.getBoolean("AUDIO_PERMISSION_GRANTED", false)
    }

    fun requestAudioPermission(
        onPermissionGranted: () -> Unit,
        onPermissionDenied: () -> Unit
    ) {
        val requestAudioPermissionLauncher = (context as ComponentActivity)
            .registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
                savePermissionStatus(isGranted)
                if (isGranted) {
                    onPermissionGranted()
                } else {
                    onPermissionDenied()
                }
            }
        requestAudioPermissionLauncher.launch(android.Manifest.permission.RECORD_AUDIO)
    }
}