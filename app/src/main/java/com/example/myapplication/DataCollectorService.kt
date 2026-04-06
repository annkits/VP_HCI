package com.example.myapplication

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.location.Location
import android.os.Build
import android.os.IBinder
import android.os.Looper
import android.telephony.CellIdentityGsm
import android.telephony.CellIdentityLte
import android.telephony.CellIdentityNr
import android.telephony.CellInfoGsm
import android.telephony.CellInfoLte
import android.telephony.CellInfoNr
import android.telephony.CellSignalStrengthGsm
import android.telephony.CellSignalStrengthLte
import android.telephony.CellSignalStrengthNr
import android.telephony.TelephonyManager
import android.util.Log
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.annotation.RequiresPermission
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.Priority
import kotlinx.coroutines.*
import org.json.JSONArray
import org.json.JSONObject
import org.zeromq.SocketType
import org.zeromq.ZContext
import org.zeromq.ZMQ
import java.io.File
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit

class DataCollectorService : Service() {

    private val CHANNEL_ID = "DataCollectorChannel"
    private val serviceScope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    private val pcIp = "172.20.225.212"
    private val port = 6000
    private val context = ZContext()
    private val sendQueue = LinkedBlockingQueue<String>(32)

    private lateinit var myFusedLocationProviderClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback

    override fun onCreate() {
        super.onCreate()
        myFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)
        createNotificationChannel()
    }

    @RequiresApi(Build.VERSION_CODES.R)
    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Сбор данных")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .build()

        startForeground(1, notification)

        startDataCollector()
        startZmqWorker()

        return START_STICKY
    }

    @RequiresApi(Build.VERSION_CODES.R)
    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
    private fun startDataCollector() {
        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 10000).build()
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                val currentLoc = locationResult.lastLocation
                if (currentLoc != null) {
                    saveLocation(currentLoc)
                    sendData()
                }
            }
        }

        try {
            myFusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())
        } catch (e: SecurityException) {
            Log.e("Сервис", "Нет доступа к местоположению")
        }

        serviceScope.launch {
            while (isActive) {
                updateTelephonyInfo()
                sendData()
                delay(15000)
            }
        }
    }

    private fun startZmqWorker() {
        serviceScope.launch(Dispatchers.IO) {
            var socket: ZMQ.Socket? = null
            while (isActive) {
                try {
                    if (socket == null) {
                        socket = context.createSocket(SocketType.REQ)
                        socket.receiveTimeOut = 3500
                        socket.connect("tcp://$pcIp:$port")
                    }

                    val message = sendQueue.poll(2, TimeUnit.SECONDS) ?: continue
                    socket.send(message.toByteArray(ZMQ.CHARSET), 0)
                    val reply = socket.recv(0)
                    Log.d("ZMQ", "Ответ сервера: ${String(reply, ZMQ.CHARSET)}")
                } catch (e: Exception) {
                    Log.e("ZMQ", "Ошибка: ${e.message}")
                    socket?.close()
                    socket = null
                    delay(3000)
                }
            }
            socket?.close()
        }
    }

    private fun sendData() {
        serviceScope.launch(Dispatchers.IO) {
            try {
                val locFile = File(filesDir, "location.json")
                val telFile = File(filesDir, "telephony.json")

                val lastLoc = if (locFile.exists() && locFile.length() > 2) {
                    val arr = JSONArray(locFile.readText())
                    arr.optJSONObject(arr.length() - 1) ?: JSONObject()
                } else JSONObject()

                val lastTel = if (telFile.exists() && telFile.length() > 2) {
                    val arr = JSONArray(telFile.readText())
                    arr.optJSONObject(arr.length() - 1) ?: JSONObject()
                } else JSONObject()

                val finalPacket = JSONObject().apply {
                    put("type", "data_update")
                    put("location", lastLoc)
                    put("telephony", lastTel)
                }

                sendQueue.offer(finalPacket.toString())

            } catch (e: Exception) {
                Log.e("DATA", "Ошибка сборки пакета: ${e.message}")
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.R)
    @RequiresPermission(Manifest.permission.ACCESS_FINE_LOCATION)
    private fun updateTelephonyInfo() {
        val tm = getSystemService(TelephonyManager::class.java)
        try {
            val info = tm.allCellInfo
            saveCellInfo(info)
        } catch (e: SecurityException) {
            Log.e("Сервис", "Нет доступа к информации о телефоне")
        }
    }

    @RequiresApi(Build.VERSION_CODES.R)
    private fun saveCellInfo(cellInfoList: List<android.telephony.CellInfo>) {
        val file = File(filesDir, "telephony.json")

        val jsonArray = if (file.exists() && file.length() > 0) {
            JSONArray(file.readText())
        } else {
            JSONArray()
        }

        val jsonObject = JSONObject()
        jsonObject.put("time", System.currentTimeMillis())

        val cellsArray = JSONArray()
        cellInfoList.forEach { info ->
            val cellJson = JSONObject()

            cellJson.put("registered", info.isRegistered)
            cellJson.put("connectionStatus", info.cellConnectionStatus)

            if (info is CellInfoLte) {
                cellJson.put("type", "LTE")
                val identity = info.cellIdentity as CellIdentityLte
                val identityJson = JSONObject()
                identityJson.put("ci", if (identity.ci != android.telephony.CellInfo.UNAVAILABLE) identity.ci else null)
                identityJson.put("pci", if (identity.pci != android.telephony.CellInfo.UNAVAILABLE) identity.pci else null)
                identityJson.put("tac", if (identity.tac != android.telephony.CellInfo.UNAVAILABLE) identity.tac else null)
                identityJson.put("earfcn", if (identity.earfcn != android.telephony.CellInfo.UNAVAILABLE) identity.earfcn else null)
                identityJson.put("bands", JSONArray(identity.bands))  // Массив band-ов
                identityJson.put("bandwidth", if (identity.bandwidth != android.telephony.CellInfo.UNAVAILABLE) identity.bandwidth else null)
                identityJson.put("mcc", identity.mccString)
                identityJson.put("mnc", identity.mncString)
                identityJson.put("operator", identity.operatorAlphaLong)
                cellJson.put("identity", identityJson)

                val signal = info.cellSignalStrength as CellSignalStrengthLte
                val signalJson = JSONObject()
                signalJson.put("dbm", if (signal.dbm != android.telephony.CellInfo.UNAVAILABLE) signal.dbm else null)
                signalJson.put("rsrp", if (signal.rsrp != -2147483647) signal.rsrp else null)  // Фильтр недоступных
                signalJson.put("rsrq", if (signal.rsrq != -2147483647) signal.rsrq else null)
                signalJson.put("rssnr", if (signal.rssnr != -2147483647) signal.rssnr else null)
                signalJson.put("cqi", if (signal.cqi != -2147483647) signal.cqi else null)
                signalJson.put("timingAdvance", if (signal.timingAdvance != 2147483647) signal.timingAdvance else null)
                signalJson.put("level", signal.level)
                cellJson.put("signal", signalJson)
            }
            if (info is CellInfoGsm) {
                cellJson.put("type", "GSM")
                val identity = info.cellIdentity as CellIdentityGsm
                val identityJson = JSONObject()
                identityJson.put("mcc", identity.mccString)
                identityJson.put("mnc", identity.mncString)
                identityJson.put("operator", identity.operatorAlphaLong)
                if (identity.lac != android.telephony.CellInfo.UNAVAILABLE) {identityJson.put("lac", identity.lac)}
                if (identity.cid != android.telephony.CellInfo.UNAVAILABLE) {identityJson.put("cid", identity.cid)}
                if (identity.arfcn != android.telephony.CellInfo.UNAVAILABLE) {identityJson.put("arfcn", identity.arfcn)}
                if (identity.bsic != android.telephony.CellInfo.UNAVAILABLE) {identityJson.put("bsic", identity.bsic)}
                cellJson.put("identity", identityJson)

                val signal = info.cellSignalStrength as CellSignalStrengthGsm
                val signalJson = JSONObject()
                if (signal.dbm != android.telephony.CellInfo.UNAVAILABLE) {signalJson.put("dbm", signal.dbm)}
                if (signal.level != 99 && signal.level != android.telephony.CellInfo.UNAVAILABLE) {signalJson.put("level", signal.level)}
                if (signal.timingAdvance != android.telephony.CellInfo.UNAVAILABLE) { signalJson.put("timingAdvance", signal.timingAdvance)}
                cellJson.put("signal", signalJson)
            }
            if (info is CellInfoNr) {
                cellJson.put("type", "NR")
                val identity = info.cellIdentity as CellIdentityNr
                val identityJson = JSONObject()
                identityJson.put("mcc", identity.mccString)
                identityJson.put("mnc", identity.mncString)
                identityJson.put("operator", identity.operatorAlphaLong)
                if (identity.nci != android.telephony.CellInfo.UNAVAILABLE_LONG) {identityJson.put("nci", identity.nci)}
                if (identity.pci != android.telephony.CellInfo.UNAVAILABLE) {identityJson.put("pci", identity.pci)}
                if (identity.tac != android.telephony.CellInfo.UNAVAILABLE) {identityJson.put("tac", identity.tac)}
                if (identity.nrarfcn != android.telephony.CellInfo.UNAVAILABLE) {identityJson.put("nrarfcn", identity.nrarfcn)}
                if (identity.bands.isNotEmpty()) {
                    val bandsArray = JSONArray()
                    identity.bands.forEach { band ->
                        if (band != android.telephony.CellInfo.UNAVAILABLE) {
                            bandsArray.put(band)
                        }
                    }
                    if (bandsArray.length() > 0) {
                        identityJson.put("bands", bandsArray)
                    }
                }
                cellJson.put("identity", identityJson)

                val signal = info.cellSignalStrength as CellSignalStrengthNr
                val signalJson = JSONObject()
                if (signal.ssRsrp != android.telephony.CellInfo.UNAVAILABLE) {signalJson.put("ssRsrp", signal.ssRsrp)}
                if (signal.ssRsrq != android.telephony.CellInfo.UNAVAILABLE) {signalJson.put("ssRsrq", signal.ssRsrq)}
                if (signal.ssSinr != android.telephony.CellInfo.UNAVAILABLE) {signalJson.put("ssSinr", signal.ssSinr)}
                if (signal.level != android.telephony.CellInfo.UNAVAILABLE) {signalJson.put("level", signal.level)}
                cellJson.put("signal", signalJson)
            }
            cellsArray.put(cellJson)
        }

        jsonObject.put("cells", cellsArray)
        jsonArray.put(jsonObject)

        file.writeText(jsonArray.toString(2))
    }

    private fun saveLocation(location: Location?) {
        val file = File(filesDir, "location.json")

        val jsonArray = if (file.exists() && file.length() > 0) {
            JSONArray(file.readText())
        } else {
            JSONArray()
        }

        val jsonObject = JSONObject()
        if (location != null) {
            jsonObject.put("latitude", location.latitude)
            jsonObject.put("longitude", location.longitude)
            jsonObject.put("altitude", location.altitude)
            jsonObject.put("accuracy", location.accuracy)
            jsonObject.put("time", location.time)

            jsonArray.put(jsonObject)
            file.writeText(jsonArray.toString(2))

        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                CHANNEL_ID,
                "Data Service Channel",
                NotificationManager.IMPORTANCE_LOW
            )
            getSystemService(NotificationManager::class.java).createNotificationChannel(serviceChannel)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        serviceScope.cancel()
        myFusedLocationProviderClient.removeLocationUpdates(locationCallback)
        context.close()
        super.onDestroy()
    }
}