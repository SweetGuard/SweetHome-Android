package com.example.sweethome

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.DataOutputStream
import java.io.File
import java.io.FileOutputStream
import java.lang.Exception
import java.net.HttpURLConnection
import java.net.URL

class AudioService : Service() {

    private val SAMPLE_RATE = 16000
    private val BUFFER_SIZE = AudioRecord.getMinBufferSize(
        SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO,
        AudioFormat.ENCODING_PCM_16BIT
    )
    private var audioRecord: AudioRecord? = null
    private val scope = CoroutineScope(Dispatchers.IO + Job())

    override fun onCreate() {
        super.onCreate()
        startForegroundService()
        startRecording()
    }

    /**
     * 지속적으로 음성 녹음을 받기 위한 ForegroundService 시작
     * 알림 표시 필수
     */
    private fun startForegroundService() {
        val channelId = "AudioServiceChannel"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                    channelId, "Audio Service", NotificationManager.IMPORTANCE_LOW)
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }

        val notification: Notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("SweetHome")
            .setContentText("백그라운드에서 음성 수집 중...")
            .setSmallIcon(R.mipmap.ic_launcher)
            .build()

        startForeground(1, notification)
    }

    private fun startRecording() {
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            if (audioRecord == null) {
                audioRecord = AudioRecord(
                    MediaRecorder.AudioSource.MIC, SAMPLE_RATE,
                    AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT, BUFFER_SIZE
                )
                audioRecord?.startRecording()

                scope.launch {
                    val buffer = ByteArray(BUFFER_SIZE)
                    val audioDataBuffer = mutableListOf<Byte>()
                    while (isActive && audioRecord?.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                        val read = audioRecord?.read(buffer, 0, buffer.size) ?: 0
                        if (read > 0) {
                            audioDataBuffer.addAll(buffer.copyOf(read).toList())

                            // 1초씩 처리
                            if (audioDataBuffer.size >= SAMPLE_RATE * 2) {
                                Log.d("AudioService", "1초 처리 완료")
                                sendAudioToServer(audioDataBuffer.toByteArray())
                                audioDataBuffer.clear()
                                Log.d("AudioService", "buffer clear")
                            }
                        }
                    }
                }
            }
        } else {
            Log.e("AudioService", "음성 녹음을 시작할 수 없습니다. 권한이 필요합니다.")
        }
    }

    private fun sendAudioToServer(audioData: ByteArray) {
        var tempFile: File? = null
        try {
            Log.d("AudioService", "send 메소드 진입")
            tempFile = File(cacheDir, "temp_segment.wav")
            FileOutputStream(tempFile).use { fos ->
                fos.write(audioData)
            }

            val url = URL("http://125.131.208.226:8000/classify-audio")
            Log.d("AudioService", "conn 설정")
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            connection.doOutput = true
            connection.setRequestProperty("Content-Type", "application/octet-stream")
            Log.d("AudioService", "CONN 설정 완료")

            DataOutputStream(connection.outputStream).use { dos ->
                tempFile.inputStream().copyTo(dos)
            }
            Log.d("AudioService", "send 완료")
            val responseCode = connection.responseCode
            if (responseCode == 200) {
                Log.d("AudioService", "음성 전송 성공")
            } else {
                Log.e("AudioService", "음성 전송 실패: $responseCode")
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            tempFile?.delete()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        audioRecord?.stop()
        audioRecord?.release()
        audioRecord = null
        scope.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}