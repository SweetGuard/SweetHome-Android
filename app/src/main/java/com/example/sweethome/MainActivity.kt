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
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.sweethome.ui.MainScreen
import com.example.sweethome.ui.theme.SweetHomeTheme

class MainActivity : ComponentActivity() {
    private lateinit var permissionManager: PermissionManager

    // 권한 요청을 처리할 런처
    private val requestAudioPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            // 권한 허용 시
            permissionManager.savePermissionStatus(true)
            Log.d("Permission", "Audio recording permission granted")
            startAudioService()
            setUpContent(true)
        } else {
            // 권한 거부 시
            permissionManager.savePermissionStatus(false)
            Log.d("Permission", "Audio Recording permission denied")
            showPermissionRationaleDialog()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        permissionManager = PermissionManager(this)

        // 음성 권한 확인
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requestAudioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        } else {
            startAudioService()
            setUpContent(true)
        }
    }

    private fun setUpContent(isRecording: Boolean) {
        setContent {
            SweetHomeTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    MainScreen(
                        isRecording = isRecording,
                        onStartAudio = { startAudioService() },
                        onStopAudio = { stopAudioService() }
                    )
                }
            }
        }
    }

    private fun startAudioService() {
        val serviceIntent = Intent(this, AudioService::class.java)
        startService(serviceIntent)
    }

    private fun stopAudioService() {
        val serviceIntent = Intent(this, AudioService::class.java)
        stopService(serviceIntent)
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
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
            text = "Hello $name!",
            modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    SweetHomeTheme {
        Greeting("Android")
    }
}