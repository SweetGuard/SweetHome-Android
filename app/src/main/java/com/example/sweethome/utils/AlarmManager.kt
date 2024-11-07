package com.example.sweethome.utils

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.example.sweethome.service.AudioService

class AlarmManagerHelper(private val context: Context) {

    private val audioManager = AudioManager(context)

    fun stopRecordingAndScheduleRestart() {
        // 현재 서비스 종료
        audioManager.stopAudioService()

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val restartIntent = Intent(context, AudioService::class.java)
        val pendingIntent = PendingIntent.getService(
            context, 0, restartIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val restartTime = System.currentTimeMillis() + 20 * 1000 // 1시간 후
        alarmManager.set(AlarmManager.RTC_WAKEUP, restartTime, pendingIntent)
    }
}