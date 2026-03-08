package com.example.sensaware

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class AlertActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_alert)

        val hr = intent.getFloatExtra("heart_rate", 0f)
        val br = intent.getFloatExtra("breathing_rate", 0f)

        findViewById<TextView>(R.id.alertMessage).text =
            "We noticed signs of rising stress.\n\nHeart Rate: ${hr.toInt()}\nBreathing Rate: ${br.toInt()}"

        findViewById<Button>(R.id.returnHomeButton).setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
            finish()
        }
    }
}