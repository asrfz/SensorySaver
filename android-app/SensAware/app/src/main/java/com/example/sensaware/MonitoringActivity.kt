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

    private val backendUrl = "http://172.20.10.2:5000/trigger"   // CHANGE
    private val apiKey = "2BwfYdU0gG7QXEEi8wIwD1FUgpUWhd3y5A30zGb8"                           // CHANGE

    private var isMonitoring = true
    private var thresholdTriggered = false

    // Baseline collection
    private val baselinePulseValues = mutableListOf<Float>()
    private val baselineBreathingValues = mutableListOf<Float>()
    private var baselineReady = false
    private val baselineSamplesNeeded = 8

    // Rolling smoothing
    private val pulseHistory = mutableListOf<Float>()
    private val breathingHistory = mutableListOf<Float>()
    private val historySize = 5

    // sustained trigger
    private var elevatedCount = 0
    private val elevatedCountNeeded = 3

    private var baselinePulse = 0f
    private var baselineBreathing = 0f

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

        // 1) collect baseline first
        if (!baselineReady) {
            baselinePulseValues.add(latestPulse)
            baselineBreathingValues.add(latestBreathing)

            Log.d("SensAware", "Collecting baseline... HR=$latestPulse BR=$latestBreathing")

            if (baselinePulseValues.size >= baselineSamplesNeeded &&
                baselineBreathingValues.size >= baselineSamplesNeeded
            ) {
                baselinePulse = baselinePulseValues.average().toFloat()
                baselineBreathing = baselineBreathingValues.average().toFloat()
                baselineReady = true

                Log.d(
                    "SensAware",
                    "Baseline ready -> HR=$baselinePulse BR=$baselineBreathing"
                )
            }
            return
        }

        // 2) rolling history for smoothing
        pulseHistory.add(latestPulse)
        breathingHistory.add(latestBreathing)

        if (pulseHistory.size > historySize) pulseHistory.removeAt(0)
        if (breathingHistory.size > historySize) breathingHistory.removeAt(0)

        val avgPulse = pulseHistory.average().toFloat()
        val avgBreathing = breathingHistory.average().toFloat()

        val pulseDelta = avgPulse - baselinePulse
        val breathingDelta = avgBreathing - baselineBreathing

        Log.d(
            "SensAware",
            "Avg HR=$avgPulse (Δ $pulseDelta) | Avg BR=$avgBreathing (Δ $breathingDelta)"
        )

        // 3) smarter trigger
        // Demo-friendly:
        // pulse needs to rise ~8 bpm above baseline
        // breathing needs to rise ~3.5 above baseline
        val pulseElevated = pulseDelta >= 8f
        val breathingElevated = breathingDelta >= 3.5f

        val elevatedNow = pulseElevated || breathingElevated

        if (elevatedNow) {
            elevatedCount += 1
        } else {
            elevatedCount = 0
        }

        val thresholdMet = elevatedCount >= elevatedCountNeeded

        if (thresholdMet && !thresholdTriggered) {
            thresholdTriggered = true
            isMonitoring = false

            Log.d("SensAware", "TRIGGERED -> sustained elevation detected")

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