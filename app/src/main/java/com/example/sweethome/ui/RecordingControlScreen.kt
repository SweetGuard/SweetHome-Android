package com.example.sweethome.ui

import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties

@Composable
fun RecordingControlScreen(
    isRecording: Boolean,
    onToggleRecording: (Boolean) -> Unit,
    onNavigateBack: () -> Unit
) {
    val backPressedDispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher
    val currentNavigateBack by rememberUpdatedState(onNavigateBack)
    var showStopDialog by remember { mutableStateOf(false) }

    // 뒤로 가기
    DisposableEffect(backPressedDispatcher) {
        val callback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                currentNavigateBack()
            }
        }
        backPressedDispatcher?.addCallback(callback)

        onDispose {
            callback.remove()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = if (isRecording) "녹음 중..." else "녹음 중지 상태입니다.",
            style = MaterialTheme.typography.headlineSmall
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = {
                if (isRecording) {
                    showStopDialog = true
                } else {
                    onToggleRecording(true)
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
            Text(if (isRecording) "녹음 중지" else "녹음 재시작")
        }
    }

    if (showStopDialog) {
        AlertDialog(
            onDismissRequest = { showStopDialog = false },
            title = { Text("녹음 중지") },
            text = { Text("녹음을 정말로 중지하시겠습니까?") },
            confirmButton = {
                TextButton(onClick = {
                    showStopDialog = false
                    onToggleRecording(false)
                }) {
                    Text("예")
                }
            },
            dismissButton = {
                TextButton(onClick = { showStopDialog = false }) {
                    Text("아니오")
                }
            },
            properties = DialogProperties(dismissOnClickOutside = false)
        )
    }
}