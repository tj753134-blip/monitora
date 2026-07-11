// MonitorService.kt
package com.universidade.monitor

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import kotlinx.coroutines.*
import kotlinx.serialization.json.*

class MonitorService : Service() {

    companion object {
        private const val CHANNEL_ID = "monitor_server_channel"
        private const val NOTIFICATION_ID = 1002
    }

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var isRunning = true

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, createNotification())

        serviceScope.launch {
            startListening()
        }
    }

    private suspend fun startListening() {
        try {
            SupabaseManager.connect()
            Log.d("Monitor", "Escutando dados do Supabase")
        } catch (e: Exception) {
            Log.e("Monitor", "Erro ao conectar", e)
            delay(5000)
            startListening()
        }
    }

    private fun handleData(data: JsonObject) {
        val clientId = data["client_id"]?.jsonPrimitive?.content ?: "unknown"
        val timestamp = data["timestamp"]?.jsonPrimitive?.long ?: 0
        val screenshot = data["screenshot"]?.jsonPrimitive?.content ?: ""
        val location = data["location"]?.jsonObject
        val apps = data["apps"]?.jsonPrimitive?.content ?: ""
        val logs = data["logs"]?.jsonPrimitive?.content ?: ""
        val battery = data["battery"]?.jsonPrimitive?.int ?: -1
        val network = data["network"]?.jsonPrimitive?.content ?: "N/A"

        // Salva no banco local
        saveToDatabase(clientId, timestamp, screenshot, location, apps, logs, battery, network)

        // Envia broadcast para Dashboard
        val intent = Intent("NEW_DATA").apply {
            putExtra("client_id", clientId)
            putExtra("timestamp", timestamp)
            putExtra("screenshot", screenshot)
            putExtra("location", location?.toString() ?: "")
            putExtra("apps", apps)
            putExtra("logs", logs)
            putExtra("battery", battery)
            putExtra("network", network)
        }
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
        
        Log.d("Monitor", "Dados recebidos de $clientId")
    }

    private fun saveToDatabase(
        clientId: String,
        timestamp: Long,
        screenshot: String,
        location: JsonObject?,
        apps: String,
        logs: String,
        battery: Int,
        network: String
    ) {
        // SQLite simples em memória
        // Para projeto completo, use Room
    }

    override fun onDestroy() {
        super.onDestroy()
        isRunning = false
        serviceScope.cancel()
        SupabaseManager.disconnect()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Monitor Servidor",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Monitor Ativo")
            .setContentText("Recebendo dados...")
            .setSmallIcon(android.R.drawable.ic_menu_view)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}