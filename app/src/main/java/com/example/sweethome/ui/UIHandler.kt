package com.example.sweethome.ui

import androidx.compose.runtime.mutableStateOf

class UIHandler {

    var currentScreen = mutableStateOf("main")

    fun navigateToRecording() {
        currentScreen.value = "recording"
    }

    fun navigateToCamera() {
        currentScreen.value = "camera"
    }

    fun navigateBackToMain() {
        currentScreen.value = "main"
    }
}