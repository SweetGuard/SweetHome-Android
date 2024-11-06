package com.example.sweethome

import android.Manifest
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.example.sweethome.manager.CameraManager
import com.example.sweethome.repository.CameraRepository
import com.example.sweethome.ui.CameraControlScreen
import com.example.sweethome.ui.MainScreen
import com.example.sweethome.ui.RecordingControlScreen
import com.example.sweethome.ui.theme.SweetHomeTheme
import com.example.sweethome.manager.PermissionManager
import com.example.sweethome.manager.RecordingManager
import com.example.sweethome.ui.UIHandler
import com.example.sweethome.utils.AudioManager

class MainActivity : ComponentActivity() {
    private lateinit var permissionManager: PermissionManager
    private lateinit var recordingManager: RecordingManager
    private lateinit var audioManager: AudioManager
    private lateinit var cameraManager: CameraManager
    private var isRecording by mutableStateOf(false)
    private var isCameraOn by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialization
        permissionManager = PermissionManager(this)
        audioManager = AudioManager(this)
        cameraManager = CameraManager(CameraRepository(BuildConfig.SERVER_URL))
        recordingManager = RecordingManager(this, audioManager)

        // 음성 권한 확인
        checkAudioPermission()

        setContent {
            SweetHomeTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val uiHandler = remember { UIHandler() }
                    AppContent(uiHandler)
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
                    if (recordingManager.getSavedRecordingState()) {
                        recordingManager.toggleRecording(true)
                        isRecording = true
                    }
                },
                onPermissionDenied = { showPermissionRationaleDialog() }
            )
        } else {
            if (recordingManager.getSavedRecordingState()) {
                recordingManager.toggleRecording(true)
                isRecording = true
            }
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
            if (!isRecording && recordingManager.getSavedRecordingState()) {
                recordingManager.toggleRecording(true)
                isRecording = true
            }
        }
    }

    private fun toggleRecording(start: Boolean) {
        isRecording = start
        recordingManager.toggleRecording(start)
    }

    private fun toggleCamera(turnOn: Boolean) {
        cameraManager.toggleCamera(turnOn) { success ->
            isCameraOn = success
        }
    }

    private fun checkCameraStatus() {
        cameraManager.checkCameraStatus { status ->
            isCameraOn = status
        }
    }

    @Composable
    fun AppContent(uiHandler: UIHandler) {
        when (uiHandler.currentScreen.value) {
            "main" -> MainScreen(
                onNavigateToRecording = { uiHandler.navigateToRecording() },
                onNavigateToCamera = {
                    checkCameraStatus()
                    uiHandler.navigateToCamera() }
            )
            "recording" -> RecordingControlScreen(
                isRecording = isRecording,
                onToggleRecording = { toggleRecording(it) },
                onNavigateBack = { uiHandler.navigateBackToMain() }
            )
            "camera" -> CameraControlScreen (
                isCameraOn = isCameraOn,
                onFetchCameraStatus = { checkCameraStatus() },
                onToggleCamera = { toggleCamera(it) },
                onNavigateBack = { uiHandler.navigateBackToMain() }
            )
        }
    }
}