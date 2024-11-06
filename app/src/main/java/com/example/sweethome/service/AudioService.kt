package com.example.sweethome.service

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
import com.example.sweethome.BuildConfig
import com.example.sweethome.R
import com.example.sweethome.utils.AudioManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString
import java.io.DataOutputStream
import java.io.File
import java.io.FileOutputStream
import java.lang.Exception
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.TimeUnit

class AudioService : Service() {

    private val SAMPLE_RATE = 16000
    private val BUFFER_SIZE = AudioRecord.getMinBufferSize(
        SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO,
        AudioFormat.ENCODING_PCM_16BIT
    )
    private var audioRecord: AudioRecord? = null
    private val scope = CoroutineScope(Dispatchers.IO + Job())
    private lateinit var webSocket: WebSocket

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
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }

        val notification: Notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("SweetHome")
            .setContentText("백그라운드에서 녹음 진행 중...")
            .setSmallIcon(R.mipmap.ic_launcher)
            .build()

        startForeground(1, notification)
    }

    /**
     * 녹음 시작 & websocket 연결
     */
    private fun startRecording() {
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            if (audioRecord == null) {
                audioRecord = AudioRecord(
                    MediaRecorder.AudioSource.MIC, SAMPLE_RATE,
                    AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT, BUFFER_SIZE
                )
                audioRecord?.startRecording()
                connectToWebSocket()

                scope.launch {
                    val buffer = ByteArray(BUFFER_SIZE)
                    while (isActive && audioRecord?.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                        val read = audioRecord?.read(buffer, 0, buffer.size) ?: 0
                        if (read > 0) {
                            sendAudioToServer(buffer.copyOf(read))
                        }
                    }
                }
            }
        } else {
            Log.e("AudioService", "음성 녹음을 시작할 수 없습니다. 권한이 필요합니다.")
            /*
            * TODO
            * - 권한 설정 함수로 이동
            * */
        }
    }

    /**
     * WebSocket 연결 및 처리
     */
    private fun connectToWebSocket() {
        val client = OkHttpClient.Builder()
            .readTimeout(3, TimeUnit.SECONDS)
            .build()

        val request = Request.Builder().url(
            "ws://192.168.56.1:8000/ws/audio").build()
        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.d("AudioService", "WebSocket 연결 성공")
            }

            // 상황 종료 시 서버에서 음성 수신 중지 요청
            override fun onMessage(webSocket: WebSocket, text: String) {
                Log.d("AudioService", "서버로부터 메시지 수신: $text")
                if (text == "STOP") stopSelf()
            }

            override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
                // server에서 음성 파일을 냈을 때 처리
                /* TODO - 괜찮으세요? etc */
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.e("AudioService", "WebSocket 연결 실패: ${t.message}")
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                Log.d("AudioService", "WebSocket 연결 종료: ${reason}")
            }
        })
    }

    /**
     * 서버로 녹음 데이터 전송
     */
    private fun sendAudioToServer(audioData: ByteArray) {
        if (this::webSocket.isInitialized) {
            webSocket.send(ByteString.of(*audioData))
            Log.d("AudioService", "Websocket을 통해 녹음 데이터 전송")
        } else {
            Log.e("AudioService", "Websocket 연결되지 않음, 전송 실패")
        }
    }

    fun stopRecording() {
        audioRecord?.stop()
        audioRecord?.release()
        audioRecord = null
        webSocket.close(1000, "녹음이 중지되었습니다.")
        scope.cancel()
        stopSelf()
    }

    override fun onDestroy() {
        super.onDestroy()
        stopRecording()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}