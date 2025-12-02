package com.example.myapplication

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.gms.location.CurrentLocationRequest
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


class LocationActivity : AppCompatActivity() {

    val value: Int = 0
    val LOG_TAG: String = "LOCATION_ACTIVITY"
    private lateinit var bBackToMain: Button

    companion object {
        private const val PERMISSION_REQUEST_ACCESS_LOCATION= 100
    }

    private lateinit var myFusedLocationProviderClient: FusedLocationProviderClient
    private lateinit var tvLatitude: TextView
    private lateinit var tvLongitude: TextView
    private lateinit var tvAltitude: TextView
    private lateinit var tvTime: TextView
    private lateinit var buttonGetLocation: Button

    private val cancellationTokenSource = CancellationTokenSource()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_location)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        bBackToMain = findViewById<Button>(R.id.back_to_main)

        myFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)

        tvLatitude = findViewById<TextView>(R.id.tvLatitude)
        tvLongitude = findViewById<TextView>(R.id.tvLongitude)
        tvAltitude = findViewById<TextView>(R.id.tvAltitude)
        tvTime = findViewById<TextView>(R.id.tvTime)
        buttonGetLocation = findViewById<Button>(R.id.buttonGetLocation)
    }

    override fun onResume() {
        super.onResume()

        bBackToMain.setOnClickListener{
            val backToMain = Intent(this, MainActivity::class.java)
            startActivity(backToMain)
        }

        buttonGetLocation.setOnClickListener {
            getFreshLocation()
        }

        getCurrentLocation()
    }

    private fun getFreshLocation() {
        if(checkPermissions()){
            if(isLocationEnabled()){
                if (ActivityCompat.checkSelfPermission(
                        this,
                        Manifest.permission.ACCESS_FINE_LOCATION
                    ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                        this,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    requestPermissions()
                    return
                }
                val currentLocationRequest = CurrentLocationRequest.Builder().setPriority(Priority.PRIORITY_HIGH_ACCURACY).build()
                myFusedLocationProviderClient.getCurrentLocation(currentLocationRequest, cancellationTokenSource.token).addOnSuccessListener { location: Location? ->
                    if(location == null){
                        Toast.makeText(applicationContext, "problems with signal", Toast.LENGTH_SHORT).show()
                    } else {
                        tvLatitude.setText(location.latitude.toString())
                        tvLongitude.setText(location.longitude.toString())
                        tvAltitude.setText(location.altitude.toString())
                        tvTime.text = formatLocationTime(location.time)
                        saveLocation(location)
                    }
                }

            } else{
                // open settings to enable location
                Toast.makeText(applicationContext, "Enable location in settings", Toast.LENGTH_SHORT).show()
                val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                startActivity(intent)
            }
        } else {
            Log.w(LOG_TAG, "location permission is not allowed");
            tvLatitude.setText("Permission is not granted")
            tvLongitude.setText("Permission is not granted")
            requestPermissions()
        }
    }

    private fun getCurrentLocation() {
        if(checkPermissions()){
            if(isLocationEnabled()){
                if (ActivityCompat.checkSelfPermission(
                        this,
                        Manifest.permission.ACCESS_FINE_LOCATION
                    ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                        this,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    requestPermissions()
                    return
                }
                myFusedLocationProviderClient.lastLocation.addOnCompleteListener(this){ task->
                    val location: Location?=task.result
                    if(location == null){
                        Toast.makeText(applicationContext, "problems with signal", Toast.LENGTH_SHORT).show()
                    } else {
                        tvLatitude.setText(location.latitude.toString())
                        tvLongitude.setText(location.longitude.toString())
                        tvAltitude.setText(location.altitude.toString())
                        tvTime.text = formatLocationTime(location.time)
                        saveLocation(location)
                    }
                }

            } else{
                // open settings to enable location
                Toast.makeText(applicationContext, "Enable location in settings", Toast.LENGTH_SHORT).show()
                val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                startActivity(intent)
            }
        } else {
            Log.w(LOG_TAG, "location permission is not allowed");
            tvLatitude.setText("Permission is not granted")
            tvLongitude.setText("Permission is not granted")
            requestPermissions()
        }
    }

    private fun formatLocationTime(timeMillis: Long): String {
        val sdf = SimpleDateFormat("dd.MM.yyyy HH:mm:ss", Locale("ru", "RU"))
        return sdf.format(Date(timeMillis))
    }

    private fun saveLocation(location: Location) {
        val file = File(filesDir, "location.json")

        val jsonArray = if (file.exists() && file.length() > 0) {
            JSONArray(file.readText())
        } else {
            JSONArray()
        }

        val jsonObject = JSONObject()
        jsonObject.put("latitude", location.latitude)
        jsonObject.put("longitude", location.longitude)
        jsonObject.put("altitude", location.altitude)
        jsonObject.put("time", location.time)

        jsonArray.put(jsonObject)
        file.writeText(jsonArray.toString(2))
        Toast.makeText(this, "Записано в locations.json", Toast.LENGTH_SHORT).show()
    }

    private fun requestPermissions() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(android.Manifest.permission.ACCESS_COARSE_LOCATION,
                android.Manifest.permission.ACCESS_FINE_LOCATION),
            PERMISSION_REQUEST_ACCESS_LOCATION
        )
    }

    private fun checkPermissions(): Boolean{
        if( ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED )
        {
            return true
        } else {
            return false
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if(requestCode == PERMISSION_REQUEST_ACCESS_LOCATION)
        {
            if(grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                Toast.makeText(applicationContext, "Permission granted", Toast.LENGTH_SHORT).show()
                getCurrentLocation()
            } else {
                Toast.makeText(applicationContext, "Denied by user", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun isLocationEnabled(): Boolean{
        val locationManager: LocationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) || locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)

    }
}