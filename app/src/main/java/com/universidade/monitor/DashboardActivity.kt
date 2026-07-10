// DashboardActivity.kt
package com.universidade.monitor

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.universidade.monitor.adapters.ClientAdapter
import java.text.SimpleDateFormat
import java.util.*

class DashboardActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var tvCount: TextView
    private lateinit var adapter: ClientAdapter
    private val clients = mutableMapOf<String, MutableList<ClientAdapter.DataItem>>()
    private val dateFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == "NEW_DATA") {
                val clientId = intent.getStringExtra("client_id") ?: return
                val timestamp = intent.getLongExtra("timestamp", 0)
                val screenshotUrl = intent.getStringExtra("screenshot_url") ?: ""
                val location = intent.getStringExtra("location") ?: ""
                val apps = intent.getStringExtra("apps") ?: ""
                val logs = intent.getStringExtra("logs") ?: ""
                val battery = intent.getIntExtra("battery", -1)
                val network = intent.getStringExtra("network") ?: "N/A"

                val item = ClientAdapter.DataItem(
                    timestamp = timestamp,
                    screenshot_url = screenshotUrl,
                    location = location,
                    apps = apps,
                    logs = logs,
                    battery = battery,
                    network = network
                )

                if (!clients.containsKey(clientId)) {
                    clients[clientId] = mutableListOf()
                }
                clients[clientId]?.add(0, item)

                if (clients[clientId]?.size ?: 0 > 50) {
                    clients[clientId]?.removeAt(clients[clientId]?.size?.minus(1) ?: 0)
                }

                adapter.notifyDataSetChanged()
                tvCount.text = "${clients.size} clientes"
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard)

        tvCount = findViewById(R.id.tvCount)
        recyclerView = findViewById(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = ClientAdapter(clients, dateFormat)
        recyclerView.adapter = adapter

        LocalBroadcastManager.getInstance(this).registerReceiver(
            receiver,
            IntentFilter("NEW_DATA")
        )
    }

    override fun onDestroy() {
        super.onDestroy()
        LocalBroadcastManager.getInstance(this).unregisterReceiver(receiver)
    }
}