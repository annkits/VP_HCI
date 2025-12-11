package com.example.myapplication

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class MainActivity : AppCompatActivity() {

    private lateinit var buttonCalculator: Button
    private lateinit var buttonMediaPlayer: Button

    private lateinit var buttonLocationActivity: Button

    private lateinit var buttonSocketsActivity: Button


    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        buttonCalculator = findViewById<Button>(R.id.buttonCalculator)
        buttonMediaPlayer = findViewById<Button>(R.id.buttonMediaPlayer)
        buttonLocationActivity = findViewById<Button>(R.id.buttonLocationActivity)
        buttonSocketsActivity = findViewById<Button>(R.id.buttonSocketsActivity)
    }

    override fun onResume() {
        super.onResume()
        buttonCalculator.setOnClickListener {
            val intent = Intent(this, CalculatorActivity::class.java)
            startActivity(intent)
        }

        buttonMediaPlayer.setOnClickListener {
            val intent = Intent(this, MusicPlayerActivity::class.java)
            startActivity(intent)
        }

        buttonLocationActivity.setOnClickListener {
            val intent = Intent(this, LocationActivity::class.java)
            startActivity(intent)
        }

        buttonSocketsActivity.setOnClickListener {
            val intent = Intent(this, SocketsActivity::class.java)
            startActivity(intent)
        }
    }
}