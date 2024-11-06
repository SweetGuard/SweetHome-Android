package com.example.sweethome.repository

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.lang.Exception
import java.net.HttpURLConnection
import java.net.URL

class CameraRepository(private val serverUrl: String) {

    private val coroutineScope = CoroutineScope(Dispatchers.IO)

    fun toggleCamera(turnOn: Boolean, onResult: (Boolean) -> Unit) {
        coroutineScope.launch {
            val endpoint = if (turnOn) "/start" else "/stop"
            val url = URL("$serverUrl$endpoint")

            (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                setRequestProperty("Content-Type", "application/json")
                doOutput = true

                val success = responseCode == 200
                disconnect()
                onResult(success)
            }
        }
    }

    fun checkCameraStatus(onStatus: (Boolean) -> Unit) {
        coroutineScope.launch {
            try {
                val url = URL("$serverUrl/camera/check")
                (url.openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    val response = inputStream.bufferedReader().readText().toBoolean()
                    disconnect()
                    onStatus(response)
                    Log.d("CameraRepository", response.toString())
                }
            } catch (e: Exception) {
                e.printStackTrace()
                onStatus(false)
            }
        }
    }
}