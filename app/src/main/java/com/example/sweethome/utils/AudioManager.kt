package com.example.sweethome.utils

import android.content.Context
import android.content.Intent
import com.example.sweethome.service.AudioService

class AudioManager(private val context: Context) {

    fun startAudioService() {
        val serviceIntent = Intent(context, AudioService::class.java)
        context.startService(serviceIntent)
    }

    fun stopAudioService() {
        val serviceIntent = Intent(context, AudioService::class.java)
        context.stopService(serviceIntent)
    }
}