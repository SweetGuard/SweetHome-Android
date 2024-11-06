package com.example.sweethome.manager

import com.example.sweethome.repository.CameraRepository

class CameraManager(private val cameraRepository: CameraRepository) {

    fun toggleCamera(turnOn: Boolean, onToggle: (Boolean) -> Unit) {
        cameraRepository.toggleCamera(turnOn) { success ->
            onToggle(success && turnOn)
        }
    }

    fun checkCameraStatus(onCheck: (Boolean) -> Unit) {
        cameraRepository.checkCameraStatus { status ->
            onCheck(status)
        }
    }
}