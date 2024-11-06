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
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.sweethome.BuildConfig
import com.example.sweethome.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.lang.Exception
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
    private var mediaPlayer: MediaPlayer? = null
    private val client = OkHttpClient.Builder().readTimeout(3, TimeUnit.SECONDS).build()

    override fun onCreate() {
        super.onCreate()
        startForeground(1, createNotification())
        initiateAudioRecording()
//        startRecording()
    }

    /**
     * 백그라운드에서 녹음 진행 시,
     * 알림 표시 필수
     */
    private fun createNotification(): Notification {
        val channelId = "AudioServiceChannel"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId, "Audio Service", NotificationManager.IMPORTANCE_LOW)
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }

        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("SweetHome")
            .setContentText("백그라운드에서 녹음 진행 중...")
            .setSmallIcon(R.mipmap.ic_launcher)
            .build()
    }

    private fun initiateAudioRecording() {
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) !=
            PackageManager.PERMISSION_GRANTED) {
            Log.e("AudioService", "Audio recording permission is not granted")
            return
            /* TODO: return 대신 설정 화면으로 이동하기 */
        }

        audioRecord = AudioRecord(
            MediaRecorder.AudioSource.MIC, SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, BUFFER_SIZE
        )
        audioRecord?.startRecording()
        connectToWebSocket()

        scope.launch {
            val buffer = ByteArray(BUFFER_SIZE)
            while (isActive && audioRecord?.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                val read = audioRecord?.read(buffer, 0, buffer.size) ?: 0
                if (read > 0) sendAudioToServer(buffer.copyOf(read))
            }
        }
    }

    /**
     * WebSocket 연결 및 처리
     */
    private fun connectToWebSocket() {

        val request = Request.Builder().url(BuildConfig.WS_SERVER_URL).build()

        webSocket = client.newWebSocket(request, object : WebSocketListener() {

            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.d("AudioService", "WebSocket 연결 성공")
            }

            // 상황 발생 시 음성 재생
            override fun onMessage(webSocket: WebSocket, text: String) {
                Log.d("AudioService", "서버로부터 메시지 수신: $text")
                handleMessage(JSONObject(text))
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.e("AudioService", "WebSocket 연결 실패: ${t.message}")
                // 일정 시간 후 재연결 시도
                scope.launch {
                    reconnectWebSocket()
                }
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                Log.d("AudioService", "WebSocket 연결 종료: $reason")
            }
        })
    }

    private suspend fun reconnectWebSocket() {
        delay(5000)
        connectToWebSocket()
    }

    private fun handleMessage(jsonMessage: JSONObject) {

        val status = jsonMessage.optInt("status")
        val code = jsonMessage.optInt("code")
        val message = jsonMessage.optString("message")

        if (status == 200 && code == 200100) {
            Log.d("AudioService", message)
            playAudio(R.raw.warning_message_korean)

            webSocket.close(1000, "위험 상황 발생 후 연결 종료")

            scope.launch {
                delay(10000) // 메시지 재생 시간 고려
                recordAudioForSeconds(10)?.let { sendAudioFileToServer(it) }
            }
        }
    }

    /**
     * 위험 상황 후속 처리 - 음성 재생
     */
    private fun playAudio(audioResId: Int) {
        mediaPlayer?.release()
        mediaPlayer = MediaPlayer.create(this, audioResId)
        mediaPlayer?.start()
    }

    /**
     * 서버로 녹음 데이터 전송
     */
    private fun sendAudioToServer(audioData: ByteArray) {
        if (this::webSocket.isInitialized) {
            webSocket.send(ByteString.of(*audioData))
//            Log.d("AudioService", "Websocket을 통해 녹음 데이터 전송")
        } else {
            Log.e("AudioService", "Websocket 연결되지 않음, 전송 실패")
        }
    }

    private fun recordAudioForSeconds(seconds: Int): ByteArray? {

        val outputStream = ByteArrayOutputStream()
        val audioData = ByteArray(BUFFER_SIZE)

        audioRecord?.apply { startRecording() } ?: return null

        val  endTime = System.currentTimeMillis() + seconds * 1000

        while (System.currentTimeMillis() < endTime) {
            val readBytes = audioRecord?.read(audioData, 0, audioData.size) ?: 0
            if (readBytes > 0) {
                outputStream.write(audioData, 0, readBytes)
            }
        }

        audioRecord?.stop()
        audioRecord?.release()

        return outputStream.toByteArray()
    }

    private fun sendAudioFileToServer(audioData: ByteArray) {

        val requestFile = audioData.toRequestBody("audio/wav".toMediaTypeOrNull())
        val multipartBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("file", "audio.wav", requestFile)
            .build()

        val request = Request.Builder()
            .url("${BuildConfig.SERVER_URL}/handle_abnormal_situation_file")
            .post(multipartBody)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onResponse(call: Call, response: Response) {
                Log.d("AudioService", "서버에 녹음 파일 전송 성공")
            }

            override fun onFailure(call: Call, e: IOException) {
                Log.e("AudioService", "녹음 파일 전송 실패: ${e.message}")
            }
        })
    }

    /**
     * 녹음 시작 & websocket 연결
     */
//    private fun startRecording() {
//        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
//            if (audioRecord == null) {
//                audioRecord = AudioRecord(
//                    MediaRecorder.AudioSource.MIC, SAMPLE_RATE,
//                    AudioFormat.CHANNEL_IN_MONO,
//                    AudioFormat.ENCODING_PCM_16BIT, BUFFER_SIZE
//                )
//                audioRecord?.startRecording()
//                connectToWebSocket()
//
//                scope.launch {
//                    val buffer = ByteArray(BUFFER_SIZE)
//                    while (isActive && audioRecord?.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
//                        val read = audioRecord?.read(buffer, 0, buffer.size) ?: 0
//                        if (read > 0) {
//                            sendAudioToServer(buffer.copyOf(read))
//                        }
//                    }
//                }
//            }
//        } else {
//            Log.e("AudioService", "음성 녹음을 시작할 수 없습니다. 권한이 필요합니다.")
//            /*
//            * TODO
//            * - 권한 설정 함수로 이동
//            * */
//        }
//    }

    private fun stopRecording() {
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