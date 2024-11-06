package com.example.sweethome.repository

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.lang.Exception
import java.net.HttpURLConnection
import java.net.URL

class CameraRepository(private val serverUrl: String) {

    private val coroutineScope = CoroutineScope(Dispatchers.IO)

    fun toggleCamera(turnOn: Boolean, onResult: (Boolean) -> Unit) {
        coroutineScope.launch {
            val endpoint = if (turnOn) "/camera/start" else "/camera/stop"
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
                val url = URL("$serverUrl/camera-check")
                (url.openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    Log.d("CameraRepository", "Response Code: $responseCode, Message: $responseMessage")
                    val response = inputStream.bufferedReader().readText()
                    disconnect()
                    val jsonResponse = JSONObject(response)
                    val state = jsonResponse.getBoolean("state")
                    onStatus(state)
                    Log.d("CameraRepository", state.toString())
                }
            } catch (e: Exception) {
                e.printStackTrace()
                onStatus(false)
            }
        }
    }
}