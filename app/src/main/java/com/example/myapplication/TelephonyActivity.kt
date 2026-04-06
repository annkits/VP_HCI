package com.example.myapplication

import android.Manifest
import android.content.ContentValues.TAG
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
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
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

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

    @RequiresApi(Build.VERSION_CODES.S)
    override fun onResume() {
        super.onResume()

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            tvInfo.text = "Эта функция доступна только на Android 12+"
            return
        }

        if (!hasRequiredPermissions()) {
            tvInfo.text = "Нет нужных разрешений"
            requestPermissions()
            return
        }

        getInfo()

        buttonBackToMain.setOnClickListener{
            val backToMain = Intent(this, MainActivity::class.java)
            startActivity(backToMain)
        }

        buttonGetInfo.setOnClickListener{
            getInfo()
        }
    }

    private fun hasRequiredPermissions(): Boolean {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestPermissions() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.READ_PHONE_STATE
            ),
            1
        )
    }

    @RequiresApi(Build.VERSION_CODES.S)
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
        }

        val telephonyManager = getSystemService(TelephonyManager::class.java)
        val cellInfoList = telephonyManager.allCellInfo

        val sb = StringBuilder()
        sb.append("Найдено вышек: ${cellInfoList.size}\n")

        cellInfoList.forEachIndexed { index, info ->
            sb.append("Вышка №${index + 1}:\n")
            sb.append("Зарегестрирована: ${info.isRegistered}\n")

            if (info is CellInfoLte) {
                sb.append("Тип: LTE (4G)\n")

                val identity = info.cellIdentity as CellIdentityLte

                sb.append("Идентификатор: ${identity.ci}\n")
                sb.append("Физ идентификатор: ${identity.pci}\n")
                sb.append("Код зоны отслеживания: ${identity.tac}\n")
                sb.append("Частота: ${identity.earfcn}\n")
                sb.append("Доступные диапазоны: ${identity.bands}\n")
                sb.append("Ширина полосы частот: ${identity.bandwidth}\n")
                sb.append("Мобильный код страны: ${identity.mccString}\n")
                sb.append("Код мобильной сети, оператор: ${identity.mncString}, ${identity.operatorAlphaLong}\n")
                sb.append("Эта вышка также обслуживает сеть: ${identity.additionalPlmns}")

                val signal = info.cellSignalStrength as CellSignalStrengthLte
                sb.append("Мощность сигнала: ${signal.dbm} dBm\n")
                sb.append("Чистая мощность сигнала (RSRP): ${signal.rsrp}\n")
                sb.append("Качество сигнала (RSRQ): ${signal.rsrq}\n")
                sb.append("Отношение сигнал/шум (RSSNR): ${signal.rssnr}\n")
                sb.append("Используется таблица CQI ${signal.cqiTableIndex}\n")
                sb.append("Текущий индекс качества (CQI): ${signal.cqi}\n")
                sb.append("Примерное расстояние до вышки: ${signal.timingAdvance}\n")
                sb.append("Уровень сигнала: ${signal.level}\n")
            }

            if (info is CellInfoGsm) {
                sb.append("Тип: GSM (2G)\n")

                val identity = info.cellIdentity as CellIdentityGsm
                sb.append("Код локальной зоны: ${identity.lac}\n")
                sb.append("Мобильный код страны: ${identity.mccString}\n")
                sb.append("Код мобильной сети, оператор: ${identity.mncString}, ${info.cellIdentity.operatorAlphaLong}\n")
                sb.append("Идентификатор соты: ${identity.cid}\n")
                sb.append("Частота: ${identity.arfcn}\n")
                sb.append("Base Station Identity Code: ${identity.bsic}\n")

                val signal = info.cellSignalStrength as CellSignalStrengthGsm
                sb.append("Общая мощность сигнала: ${signal.rssi} dBm\n")
                sb.append("Уровень сигнала: ${signal.level}\n")
                sb.append("Примерное расстояние до вышки: ${signal.timingAdvance}\n")
            }

            if (info is CellInfoNr) {
                sb.append("Тип: NR (5G)\n")

                val identity = info.cellIdentity as CellIdentityNr
                sb.append("ID вышки: ${identity.nci}\n")
                sb.append("Физ идентификатор: ${identity.pci}\n")
                sb.append("Код зоны отслеживания: ${identity.tac}\n")
                sb.append("Частота: ${identity.nrarfcn}\n")
                sb.append("Доступные диапазоны: ${identity.bands}\n")

                val signal = info.cellSignalStrength as CellSignalStrengthNr
                sb.append("Чистая мощность сигнала (RSRP): ${signal.ssRsrp}\n")
                sb.append("Качество сигнала (RSRQ): ${signal.ssRsrq}\n")
                sb.append("Отношение сигнал/шум (ssSinr): ${signal.ssSinr}\n")
                sb.append("Уровень сигнала: ${signal.level}\n")
            }
        }

        tvInfo.text = sb.toString()
        saveCellInfo(cellInfoList)
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

        Toast.makeText(this, "Записано в telephony.json", Toast.LENGTH_SHORT).show()
    }
}