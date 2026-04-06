package com.example.myapplication

import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.myapplication.LocationActivity.Companion.PERMISSION_REQUEST_ACCESS_LOCATION

class MainActivity : AppCompatActivity() {

    private lateinit var buttonCalculator: Button
    private lateinit var buttonMediaPlayer: Button

    private lateinit var buttonLocationActivity: Button

    private lateinit var buttonSocketsActivity: Button
    private lateinit var buttonTelephonyActivity: Button

    private val REQUIRED_PERMISSIONS = arrayOf(
        android.Manifest.permission.ACCESS_FINE_LOCATION,
        android.Manifest.permission.ACCESS_COARSE_LOCATION,
        android.Manifest.permission.READ_PHONE_STATE
    )
    private val REQUEST_CODE_PERMISSIONS = 101

    @RequiresApi(Build.VERSION_CODES.O)
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
        buttonTelephonyActivity = findViewById<Button>(R.id.buttonTelephonyActivity)

        if (allPermissionsGranted()) {
            val intent = Intent(this, DataCollectorService::class.java)
            startForegroundService(intent)
        } else {
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS)
        }
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(baseContext, it) == PackageManager.PERMISSION_GRANTED
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                val intent = Intent(this, DataCollectorService::class.java)
                startForegroundService(intent)
            } else {
                Toast.makeText(this, "Разрешения не даны. Сервис не запущен.", Toast.LENGTH_LONG).show()
            }
        }
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

        buttonTelephonyActivity.setOnClickListener {
            val intent = Intent(this, TelephonyActivity::class.java)
            startActivity(intent)
        }
    }
}