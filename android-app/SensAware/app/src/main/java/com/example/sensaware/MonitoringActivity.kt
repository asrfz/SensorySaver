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

    // CHANGE THESE
    private val backendUrl = "http://10.200.10.34:5000/vitals"
    private val apiKey = "2BwfYdU0gG7QXEEi8wIwD1FUgpUWhd3y5A30zGb8"

    private var isMonitoring = true
    private var thresholdTriggered = false

    // Demo-friendly thresholds:
    // high enough to avoid instant trigger,
    // low enough that intentional faster breathing can trigger
    private val pulseThreshold = 100f
    private val breathingThreshold = 27f

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

        Log.d(
            "SensAware",
            "Vitals → HR=$latestPulse | BR=$latestBreathing"
        )

        val thresholdMet =
            latestPulse >= pulseThreshold ||
                    latestBreathing >= breathingThreshold

        sendVitalsToBackend(
            pulse = latestPulse,
            breathing = latestBreathing,
            thresholdMet = thresholdMet
        )

        if (thresholdMet && !thresholdTriggered) {
            thresholdTriggered = true
            isMonitoring = false

            Log.d("SensAware", "⚠️ THRESHOLD TRIGGERED")

            sendThresholdEventToBackend(
                pulse = latestPulse,
                breathing = latestBreathing
            )

            val intent = Intent(this, AlertActivity::class.java).apply {
                putExtra("heart_rate", latestPulse)
                putExtra("breathing_rate", latestBreathing)
            }
            startActivity(intent)
            finish()
        }
    }

    private fun sendVitalsToBackend(
        pulse: Float,
        breathing: Float,
        thresholdMet: Boolean
    ) {
        thread {
            try {
                val json = JSONObject().apply {
                    put("heart_rate", pulse)
                    put("breathing_rate", breathing)
                    put("threshold_met", thresholdMet)
                    put("timestamp", System.currentTimeMillis())
                }

                val body = json.toString()
                    .toRequestBody("application/json".toMediaType())

                val request = Request.Builder()
                    .url(backendUrl)
                    .post(body)
                    .build()

                client.newCall(request).execute().use {
                    Log.d("SensAware", "Sent vitals")
                }
            } catch (e: Exception) {
                Log.e("SensAware", "Backend send failed", e)
            }
        }
    }

    private fun sendThresholdEventToBackend(
        pulse: Float,
        breathing: Float
    ) {
        thread {
            try {
                val json = JSONObject().apply {
                    put("event", "threshold_triggered")
                    put("heart_rate", pulse)
                    put("breathing_rate", breathing)
                    put("timestamp", System.currentTimeMillis())
                }

                val body = json.toString()
                    .toRequestBody("application/json".toMediaType())

                val request = Request.Builder()
                    .url(backendUrl)
                    .post(body)
                    .build()

                client.newCall(request).execute().use {
                    Log.d("SensAware", "Threshold event sent")
                }
            } catch (e: Exception) {
                Log.e("SensAware", "Threshold send failed", e)
            }
        }
    }
}