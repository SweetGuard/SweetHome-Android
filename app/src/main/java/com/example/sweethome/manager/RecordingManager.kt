package com.example.sweethome.manager

import android.content.Context
import android.content.SharedPreferences
import androidx.activity.ComponentActivity
import com.example.sweethome.utils.AudioManager

class RecordingManager(private val context: Context, private val audioManager: AudioManager) {

    private val sharedPreferences: SharedPreferences =
        context.getSharedPreferences("RecordingPrefs", Context.MODE_PRIVATE)

    /**
     * 녹음 상태를 SharedPreferences에 저장
     */
    private fun saveRecordingState(state: Boolean) {
        sharedPreferences.edit().putBoolean("IS_RECORDING", state).apply()
    }

    fun getSavedRecordingState(): Boolean {
        return sharedPreferences.getBoolean("IS_RECORDING", true)
    }

    fun toggleRecording(start: Boolean) {
        if (start) audioManager.startAudioService() else audioManager.stopAudioService()
        saveRecordingState(start)
    }

}