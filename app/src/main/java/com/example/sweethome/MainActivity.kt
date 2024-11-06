package com.example.sweethome

import android.Manifest
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.sweethome.repository.CameraRepository
import com.example.sweethome.ui.CameraControlScreen
import com.example.sweethome.ui.MainScreen
import com.example.sweethome.ui.RecordingControlScreen
import com.example.sweethome.ui.theme.SweetHomeTheme
import com.example.sweethome.utils.AudioManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import java.lang.Exception
import java.net.HttpURLConnection
import java.net.URL

class MainActivity : ComponentActivity() {
    private lateinit var permissionManager: PermissionManager
    private lateinit var cameraRepository: CameraRepository
    private lateinit var audioManager: AudioManager
    private var isRecording by mutableStateOf(false)
    private var isCameraOn by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialization
        permissionManager = PermissionManager(this)
        audioManager = AudioManager(this)
        cameraRepository = CameraRepository(BuildConfig.SERVER_URL)

        // 음성 권한 확인
        checkAudioPermission()

        setContent {
            SweetHomeTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    var currentScreen by remember { mutableStateOf("main") }

                    when (currentScreen) {
                        "main" -> MainScreen(
                            onNavigateToRecording = { currentScreen = "recording" },
                            onNavigateToCamera = { currentScreen = "camera" }
                        )
                        "recording" -> RecordingControlScreen(
                            isRecording = isRecording,
                            onToggleRecording = { toggleRecording(it) },
                            onNavigateBack = { currentScreen = "main" }
                        )
                        "camera" -> CameraControlScreen (
                            isCameraOn = isCameraOn,
                            onFetchCameraStatus = { checkCameraStatus() },
                            onToggleCamera = { toggleCamera(it) }
                        )
                    }
                }
            }
        }
    }

    /**
     * 녹음 권한 확인하기
     */
    private fun checkAudioPermission() {
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED) {
            permissionManager.requestAudioPermission (
                onPermissionGranted = {
                    audioManager.startAudioService()
                    isRecording = true
                },
                onPermissionDenied = { showPermissionRationaleDialog() }
            )
        } else {
            audioManager.startAudioService()
            isRecording = true
        }
    }

    /**
     * 설정 화면으로 이동
     * 권한 설정 취소 시 앱 꺼짐
     */
    private fun showPermissionRationaleDialog() {
        AlertDialog.Builder(this).apply {
            setMessage("앱을 사용하려면 마이크 권한이 필요합니다. 설정에서 권한을 허용해 주세요.")
            setPositiveButton("설정으로 이동") { _, _ ->
                val settingsIntent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.fromParts("package", packageName, null)
                }
                startActivity(settingsIntent)
            }
            setNegativeButton("취소") { dialog, _ ->
                dialog.dismiss()
                finish()
            }
            create()
            show()
        }
    }

    /**
     * 설정 화면에서 마이크 권한을 허용한 후 다시 앱 화면으로 돌아왔을 때,
     * 권한 체크 후 백그라운드에서 녹음 시작
     */
    override fun onResume() {
        super.onResume()
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO)
            == PackageManager.PERMISSION_GRANTED) {
            if (!isRecording) {
                audioManager.startAudioService()
                isRecording = true
            }
        }
    }

    private fun toggleRecording(start: Boolean) {
        isRecording = start
        if (start) audioManager.startAudioService() else audioManager.stopAudioService()
    }

    private fun toggleCamera(turnOn: Boolean) {
        cameraRepository.toggleCamera(turnOn) { success ->
            if (success) isCameraOn = turnOn
        }
    }

    private fun checkCameraStatus() {
        cameraRepository.checkCameraStatus { status ->
            isCameraOn = status
        }
    }
}