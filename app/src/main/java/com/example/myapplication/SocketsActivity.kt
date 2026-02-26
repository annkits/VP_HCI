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
import org.json.JSONArray
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


    private val pcIp = "172.20.225.212"
    private val port = 6000

    private val context = ZContext()
    private var socket: ZMQ.Socket? = null
    private var isConnecting = false
    private var isConnected = false

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
            val json = readJson()
            sendToServer(json)
        }
        appendLog("Запуск клиента -> ПК ($pcIp:$port)")

    }

    private fun readJson(): String {
        val locFile = File(filesDir, "location.json")
        val locJson = if (locFile.exists()) {
            locFile.readText()
        } else "[]"
        val locArray = JSONArray(locJson)
        val lastLoc = if (locArray.length() > 0) {
            locArray.getJSONObject(locArray.length() - 1)
        } else JSONObject()

        val telFile = File(filesDir, "telephony.json")
        val telJson = if (telFile.exists()) {
            telFile.readText()
        } else "[]"
        val telArray = JSONArray(telJson)
        val lastTel = if (telArray.length() > 0) {
            telArray.getJSONObject(telArray.length() - 1)
        } else JSONObject()

        val combined = JSONObject()
        combined.put("location", lastLoc)
        combined.put("telephony", lastTel)

        return combined.toString(2)
    }

    private fun sendToServer(message: String) {
        Thread {
            try {
                if (!isConnected) {
                    connectIfNeeded()
                    Thread.sleep(800)
                }

                val sock = socket
                if (sock != null) {
                    val success = sock.send(message.toByteArray(ZMQ.CHARSET), 0)
                    if (!success) {
                        appendLog("Ошибка отправки")
                        socket?.close()
                    }
                    appendLog("Отправлено: $message")

                    val reply = sock.recv(0)
                    if (reply == null) {
                        appendLog("Таймаут: сервер не ответил")
                        socket?.close()
                    }
                    val replyText = String(reply, ZMQ.CHARSET)
                    appendLog("Ответ от сервера: $replyText")
                } else {
                    appendLog("Сокет не создан")
                    isConnected = false
                }

            } catch (e: Exception) {
                appendLog("Ошибка: ${e.message}")
                socket?.close()
                isConnected = false
                connectIfNeeded()
            }
        }.start()
    }

    private fun connectIfNeeded() {
        if (isConnecting || isConnected) {
            return
        }

        isConnecting = true

        Thread {
            try {
                appendLog("Подключение к tcp://$pcIp:$port...")

                val newSocket = context.createSocket(SocketType.REQ)
                newSocket.sendTimeOut = 2500
                newSocket.receiveTimeOut = 2500
                newSocket.connect("tcp://$pcIp:$port")

                socket?.close()
                socket = newSocket
                isConnected = true

                appendLog("Подключено")
            } catch (e: Exception) {
                appendLog("Ошибка: ${e.message}")
                isConnected = false
                Thread.sleep(2000)
            } finally {
                isConnecting = false
            }
        }.start()


    }

    override fun onDestroy() {
        socket?.close()
        context.close()
        isConnected = false
        super.onDestroy()
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