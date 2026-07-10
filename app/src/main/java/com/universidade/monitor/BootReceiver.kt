// BootReceiver.kt
package com.universidade.monitor

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action == Intent.ACTION_BOOT_COMPLETED) {
            Log.d("BootReceiver", "Dispositivo reiniciado")

            val prefs = context.getSharedPreferences("monitor", Context.MODE_PRIVATE)
            val configured = prefs.getBoolean("configured", false)
            val mode = prefs.getString("mode", "monitor")

            if (configured) {
                if (mode == "monitor") {
                    context.startService(Intent(context, MonitorService::class.java))
                } else {
                    context.startService(Intent(context, CollectorService::class.java))
                }
            }
        }
    }
}