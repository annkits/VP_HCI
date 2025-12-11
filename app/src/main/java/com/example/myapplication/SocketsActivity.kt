package com.example.myapplication

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.TextView
import android.widget.Button
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import org.json.JSONObject
import org.zeromq.SocketType
import org.zeromq.ZContext
import org.zeromq.ZMQ
import java.io.File

class SocketsActivity : AppCompatActivity() {

    private lateinit var tvLog: TextView
    private lateinit var btnSendHelloToPC: Button
    private lateinit var btnSendLocationToPC: Button

    private val handler = Handler(Looper.getMainLooper())
    private lateinit var bBackToMain: Button


    private val pcIp = "10.0.2.2"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_sockets)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        tvLog = findViewById<TextView>(R.id.tv_log)
        btnSendHelloToPC = findViewById<Button>(R.id.btn_send_hello_to_pc)
        btnSendLocationToPC = findViewById<Button>(R.id.btn_send_location_to_pc)
        bBackToMain = findViewById<Button>(R.id.back_to_main)

        bBackToMain.setOnClickListener{
            val backToMain = Intent(this, MainActivity::class.java)
            startActivity(backToMain)
        }

        btnSendHelloToPC.setOnClickListener {
            clearLog()
            sendToServer("Hello from Android!")
        }

        btnSendLocationToPC.setOnClickListener {
            clearLog()
            val json = readLocationJson()
            sendToServer(json)
        }
        appendLog("Запуск клиента -> ПК ($pcIp:6000)")
    }

    private fun readLocationJson(): String {
        val file = File(filesDir, "location.json")
        var jsonString: String

        if (!file.exists() || file.length().toInt() == 0) {
            val jsonObject = JSONObject()
            jsonObject.put("error", "no_data")
            jsonObject.put("message", "Файл location.json пуст или не существует")
            jsonString = jsonObject.toString()
        } else { jsonString = file.readText() }

        return jsonString
    }

    private fun sendToServer(message: String) {
        Thread {
            ZContext().use { context ->
                context.createSocket(SocketType.REQ).use { socket ->
                    try {
                        socket.connect("tcp://$pcIp:6000")
                        Thread.sleep(200)
                        appendLog("Подключено к серверу")

                        socket.send(message.toByteArray(ZMQ.CHARSET), 0)
                        appendLog("Отправлено: $message")

                        val reply = socket.recv(0)
                        val replyText = String(reply, ZMQ.CHARSET)
                        appendLog("Ответ от сервера: $replyText")

                    } catch (e: Exception) {
                        appendLog("Ошибка: ${e.message}")
                    }
                }
            }
        }.start()
    }

    private fun appendLog(text: String) {
        handler.post {
            tvLog.append(text + "\n")
        }
    }

    private fun clearLog() {
        handler.post { tvLog.text = "" }
    }
}