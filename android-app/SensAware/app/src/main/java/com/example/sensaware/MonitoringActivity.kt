package com.example.sensaware

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import com.presage.physiology.proto.MetricsProto.MetricsBuffer
import com.presagetech.smartspectra.SmartSpectraMode
import com.presagetech.smartspectra.SmartSpectraSdk
import com.presagetech.smartspectra.SmartSpectraView
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import kotlin.concurrent.thread

class MonitoringActivity : AppCompatActivity() {

    private lateinit var smartSpectraView: SmartSpectraView
    private val client = OkHttpClient()

    private val backendUrl = "http://192.168.1.23:5000/trigger"   // CHANGE THIS
    private val apiKey = "YOUR_API_KEY"                           // CHANGE THIS

    private var isMonitoring = true
    private var thresholdTriggered = false

    // demo-friendly thresholds
    private val pulseThreshold = 90f
    private val breathingThreshold = 18f

    private val smartSpectraSdk = SmartSpectraSdk.getInstance().apply {
        setApiKey(apiKey)
        setSmartSpectraMode(SmartSpectraMode.CONTINUOUS)
        setCameraPosition(CameraSelector.LENS_FACING_FRONT)
        setMeasurementDuration(30.0)
        setRecordingDelay(3)

        setMetricsBufferObserver { metricsBuffer ->
            handleMetricsBuffer(metricsBuffer)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_monitoring)

        smartSpectraView = findViewById(R.id.smart_spectra_view)

        findViewById<Button>(R.id.stopRecordingButton).setOnClickListener {
            isMonitoring = false
            finish()
        }
    }

    private fun handleMetricsBuffer(metrics: MetricsBuffer) {
        if (!isMonitoring) return

        val latestPulse = metrics.pulse.rateList.lastOrNull()?.value
        val latestBreathing = metrics.breathing.rateList.lastOrNull()?.value

        if (latestPulse == null || latestBreathing == null) return

        Log.d("SensAware", "HR=$latestPulse | BR=$latestBreathing")

        val thresholdMet =
            latestPulse >= pulseThreshold ||
            latestBreathing >= breathingThreshold

        if (thresholdMet && !thresholdTriggered) {
            thresholdTriggered = true
            isMonitoring = false

            Log.d("SensAware", "TRIGGERED")

            sendTriggerToBackend()

            val intent = Intent(this, AlertActivity::class.java)
            startActivity(intent)
            finish()
        }
    }

    private fun sendTriggerToBackend() {
        thread {
            try {
                val json = JSONObject().apply {
                    put("triggered", true)
                }

                val body = json.toString()
                    .toRequestBody("application/json".toMediaType())

                val request = Request.Builder()
                    .url(backendUrl)
                    .post(body)
                    .build()

                client.newCall(request).execute().use { response ->
                    Log.d("SensAware", "Trigger sent -> ${response.code}")
                }
            } catch (e: Exception) {
                Log.e("SensAware", "Trigger send failed", e)
            }
        }
    }
}