package com.example.myapplication

import android.os.Bundle
import com.google.android.material.snackbar.Snackbar
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import com.example.myapplication.databinding.ActivityMainBinding
import android.view.Menu
import android.view.MenuItem
import okhttp3.*
import java.io.IOException
import android.util.Log
import android.widget.TextView
import com.google.android.material.textfield.TextInputEditText
import org.json.JSONObject
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import java.text.SimpleDateFormat
import java.util.*
import android.os.Looper
import android.os.Handler
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.content.BroadcastReceiver
import android.content.Context
import android.media.MediaPlayer
import java.io.File
import java.io.FileOutputStream

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val client = OkHttpClient()

    private val handler = Handler(Looper.getMainLooper())


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)



        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Bind the TextView for showing the response
        val responseTextView = findViewById<TextView>(R.id.textview_first)

        // Bind the TextInputEditText
        val inputEditText = findViewById<TextInputEditText>(R.id.textinput)

        val timeTextView: TextView = findViewById(R.id.time)
        val dateTextView: TextView = findViewById(R.id.date)
        val dayTextView: TextView = findViewById(R.id.weekday)



        // Update time every second
        handler.post(object : Runnable {
            override fun run() {
                val newTime = getCurrentTime()
                dateTextView.text = getCurrentDate()
                dayTextView.text = getDayOfWeek()

                if (timeTextView.text != newTime) { // Update only if the time has changed
                    timeTextView.text = newTime
                }
                Log.d("Time", newTime)
                handler.postDelayed(this, 1000) // Repeat every second
            }
        })


        // Set click listener for the FloatingActionButton (FAB)
        /*
        binding.fab.setOnClickListener {
            // Get the text from the input field
            val userInput = inputEditText.text?.toString()?.trim()

            // Check if the input is not empty
            if (!userInput.isNullOrEmpty()) {
                // Trigger the HTTP POST request
                makeHttpPostRequest(userInput) { responseText ->
                    runOnUiThread {
                        // Update the TextView with the response text
                        responseTextView.text = responseText
                    }
                }
            } else {
                // Notify the user to enter some input
                responseTextView.text = "Please enter some text in the input field."
            }
        }
        */

        // Set click listener for the FloatingActionButton (FAB)
        binding.fab.setOnClickListener {
            // Use a hardcoded MP3 URL
            val hardcodedMp3Url = "http://15.164.221.34:5009/audio/speech.mp3"

            // Trigger the playMp3FromUrl function
            playMp3FromUrl(hardcodedMp3Url) { errorMessage ->
                runOnUiThread {
                    // Display the error message in the TextView if something goes wrong
                }
            }
        }




        val batteryLevelTextView: TextView = findViewById(R.id.battery)

        // Register a BroadcastReceiver for the battery status
        val batteryStatusReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                val level = intent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
                val scale = intent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1

                // Calculate the battery percentage
                val batteryPercentage = if (level >= 0 && scale > 0) {
                    (level * 100) / scale
                } else {
                    0 // Default to 0 if level or scale is invalid
                }

                // Update the TextView with the battery percentage
                batteryLevelTextView.text = "Batt: $batteryPercentage%"
            }
        }

        // Register the receiver to listen for ACTION_BATTERY_CHANGED
        registerReceiver(batteryStatusReceiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED))

    }

    private fun getCurrentTime(): String {
        val dateFormat = SimpleDateFormat("hh:mm", Locale.getDefault())
        return dateFormat.format(Date()) // Get current time
    }

    private fun getCurrentDate(): String {
        // Format: day/month (e.g., "9/12")
        val dateFormat = SimpleDateFormat("d/M", Locale.getDefault())
        return dateFormat.format(Date())
    }

    private fun getDayOfWeek(): String {
        // Format: day of the week (e.g., "Mon", "Tue", "Wed")
        val dayFormat = SimpleDateFormat("EEE", Locale.getDefault())
        return dayFormat.format(Date())
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
                            // Save MP3 to cache directory
                            val file = File(cacheDir, "temp.mp3")
                            val outputStream = FileOutputStream(file)
                            inputStream.copyTo(outputStream)
                            outputStream.close()

                            // Play the MP3 using MediaPlayer
                            playMp3File(file)
                        } catch (e: Exception) {
                            Log.e("HTTP", "Error playing MP3: $e")
                            onError("Error playing MP3: ${e.message}")
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

/*
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
            .url("http://15.164.221.34:5004/orca") // Use HTTP, not HTTPS, as specified
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
    */
}
