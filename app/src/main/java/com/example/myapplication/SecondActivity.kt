package com.example.myapplication

import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.textfield.TextInputEditText
import android.widget.ImageButton


import android.widget.LinearLayout
import android.widget.TextView
import android.graphics.Color
import android.media.MediaPlayer

import android.util.Log
import android.os.Handler
import android.os.Looper
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.*
import java.util.concurrent.TimeUnit

class SecondActivity : AppCompatActivity() {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS) // Connection timeout (10 seconds)
        .readTimeout(60, TimeUnit.SECONDS)    // Read timeout (30 seconds)
        .writeTimeout(60, TimeUnit.SECONDS)   // Write timeout (20 seconds)
        .protocols(listOf(Protocol.HTTP_1_1)) // Force HTTP/1.1
        .build()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_secondary) // Sets the second layout


        // Bind the TextInputEditText
        val inputEditText = findViewById<TextInputEditText>(R.id.inputText)
        val sendButton = findViewById<ImageButton>(R.id.send)

        // Set click listener for the FloatingActionButton (FAB)

        sendButton.setOnClickListener {
            // Get the text from the input field
            val userInput = inputEditText.text?.toString()?.trim()

            // Check if the input is not empty
            if (!userInput.isNullOrEmpty()) {
                // Trigger the HTTP POST request
                makeHttpPostRequest(userInput) { responseText ->
                    runOnUiThread {
                        // Find the LinearLayout by its ID
                        val textLayout = findViewById<LinearLayout>(R.id.text_layout)
                        Log.d("response",responseText)
                        val jsonResponse = JSONObject(responseText)
                        val outputText = jsonResponse.optString("text_reply", "No output found") // Extract "output" field
                        val mp3_url = jsonResponse.optString("audio_url", "No output found")
                        Log.d("mp3url",mp3_url)
                        Handler(Looper.getMainLooper()).postDelayed({
                            // Code to execute after 1 second
                            playMp3FromUrl(mp3_url) { errorMessage ->
                                runOnUiThread {
                                    // Display the error message in the TextView if something goes wrong
                                }
                            }
                        }, 2000) // 1000ms = 1 second



                        // Create a new TextView
                        val newTextView = TextView(this).apply {
                            text = outputText // Set the response text
                            textSize = 18f // Set text size (in SP)
                            setTextColor(Color.parseColor("#FFFFFF")) // Set text color (#FFFFFF is white)
                        }

                        // Add the new TextView to the LinearLayout
                        textLayout.addView(newTextView)
                    }

                }
            } else {
                // Notify the user to enter some input
            }
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        // Check if the Volume Down button is pressed
        if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
            // Navigate to the new activity
            val intent = Intent(
                this@SecondActivity,
                MainActivity::class.java
            )
            startActivity(intent)
            return true // Indicate that the event has been handled
        }
        return super.onKeyDown(keyCode, event) // Pass other key events to the parent
    }

    private fun playMp3FromUrl(mp3Url: String, onError: (String) -> Unit) {
        // Make the HTTP GET request
        val request = Request.Builder()
            .url(mp3Url)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("HTTP", "Request failed: $e")
                onError("Request failed: ${e.message}")
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    // Save the MP3 file locally and play it
                    response.body?.byteStream()?.let { inputStream ->
                        try {
                            val file = File(cacheDir, "temp.mp3")
                            FileOutputStream(file).use { outputStream ->
                                inputStream.copyTo(outputStream) // Copy input stream to output stream
                            }
                            // Play the MP3 file
                            playMp3File(file)
                        } catch (e: Exception) {
                            Log.e("HTTP", "Error saving MP3: $e")
                            onError("Error saving MP3: ${e.message}")
                        } finally {
                            inputStream.close() // Ensure input stream is closed
                        }
                    } ?: onError("Empty response body")

                } else {
                    onError("Request failed with status: ${response.code}")
                }
            }
        })
    }

    private fun playMp3File(file: File) {
        val mediaPlayer = MediaPlayer()
        try {
            mediaPlayer.setDataSource(file.absolutePath) // Set the file as the data source
            mediaPlayer.prepare() // Prepare the MediaPlayer
            mediaPlayer.start() // Start playing the audio

            mediaPlayer.setOnCompletionListener {
                Log.d("MediaPlayer", "Playback completed")
                mediaPlayer.release() // Release resources after playback
            }
        } catch (e: IOException) {
            Log.e("MediaPlayer", "Error playing MP3: $e")
        }
    }

    private fun makeHttpPostRequest(input: String, callback: (String) -> Unit) {
        // Create the JSON object for the POST body
        val jsonBody = JSONObject()
        jsonBody.put("input", input)

        // Build the request body
        val requestBody = RequestBody.create(
            "application/json; charset=utf-8".toMediaTypeOrNull(),
            jsonBody.toString()
        )

        // Build the POST request
        val request = Request.Builder()
            .url("http://15.164.221.34:5009/chat") // Use HTTP, not HTTPS, as specified
            .post(requestBody)
            .build()

        // Make the HTTP call
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("HTTP", "Request failed: $e")
                callback("Request failed: ${e.message}")
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    response.body?.string()?.let { responseBody ->
                        Log.d("HTTP", "Response: $responseBody")
                        callback(responseBody) // Pass the response to the callback
                    } ?: callback("Empty response body")
                } else {
                    callback("Request failed with status: ${response.code}")
                }
            }
        })
    }


}