package com.example.sweethome

import android.content.Context
import android.content.SharedPreferences

class PermissionManager(context: Context) {
    private val sharedPreferences: SharedPreferences =
        context.getSharedPreferences("PermissionPrefs", Context.MODE_PRIVATE)

    fun savePermissionStatus(isGranted: Boolean) {
        sharedPreferences.edit().putBoolean("AUDIO_PERMISSION_GRANTED", isGranted).apply()
    }

    fun isPermissionGranted(): Boolean {
        return sharedPreferences.getBoolean("AUDIO_PERMISSION_GRANTED", false)
    }
}