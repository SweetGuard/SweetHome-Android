package com.example.sweethome.ui

import android.app.AlertDialog
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.sweethome.ui.theme.SweetHomeTheme

@Composable
fun MainScreen(
    isRecording: Boolean,
    onStartAudio: () -> Unit,
    onStopAudio: () -> Unit
) {
    var currentRecordingState by remember { mutableStateOf(isRecording) }
    var showStopDialog by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf(
        if (isRecording) "음성을 수신 중..."
        else "수신 대기 중...") }

    if (showStopDialog) {
        AlertDialog(
            onDismissRequest = { showStopDialog = false },
            title = { Text("음성 수신 중지") },
            text = { Text("음성 수신을 정말로 중지하시겠습니까?") },
            confirmButton = {
                TextButton(onClick = {
                    currentRecordingState = false
                    statusMessage = "녹음 대기 중..."
                    onStopAudio()
                    showStopDialog = false
                }) {
                    Text("중지")
                }
            },
            dismissButton = {
                TextButton(onClick = { showStopDialog = false }) {
                    Text("취소")
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = statusMessage, style = MaterialTheme.typography.headlineSmall)
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = {
                if (isRecording) {
                    showStopDialog = true
                } else {
                    currentRecordingState = true
                    statusMessage = "음성 수신 중..."
                    onStartAudio()
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isRecording) MaterialTheme.colorScheme.error
                else MaterialTheme.colorScheme.primary
            )
        ) {
            Text(if (isRecording) "녹음 중지" else "녹음 시작")
        }
    }
}

@Preview(showBackground = true)
@Composable
fun MainScreenPreview() {
    SweetHomeTheme {
        MainScreen(isRecording = true, onStartAudio = {}, onStopAudio = {})
    }
}
