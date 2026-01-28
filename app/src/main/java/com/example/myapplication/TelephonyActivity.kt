package com.example.myapplication

import android.Manifest
import android.content.ContentValues.TAG
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.telephony.TelephonyManager
import android.util.Log
import android.widget.Button
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class TelephonyActivity : AppCompatActivity() {

    private lateinit var buttonBackToMain: Button
    private lateinit var buttonGetInfo: Button
    private lateinit var tvInfo: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_telephony)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        buttonBackToMain = findViewById<Button>(R.id.back_to_main)
        buttonGetInfo = findViewById<Button>(R.id.get_info)
        tvInfo = findViewById<TextView>(R.id.tv_log)
    }

    override fun onResume() {
        super.onResume()

        getInfo()

        buttonBackToMain.setOnClickListener{
            val backToMain = Intent(this, MainActivity::class.java)
            startActivity(backToMain)
        }

        buttonGetInfo.setOnClickListener{
            getInfo()
        }
    }

    private fun getInfo() {
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
        ) {
            Log.d(TAG, "Нет разрешений READ_PHONE_STATE или ACCESS_FINE_LOCATION для получения cell info")

            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    Manifest.permission.READ_PHONE_STATE,
                    Manifest.permission.ACCESS_FINE_LOCATION),
                1
            )
        } else {
            val telephonyManager = getSystemService(TelephonyManager::class.java)
            val cellInfoList = telephonyManager.allCellInfo
//          Log.d(TAG, cellInfoList.toString())
            tvInfo.setText(cellInfoList.toString())
        }
    }
}