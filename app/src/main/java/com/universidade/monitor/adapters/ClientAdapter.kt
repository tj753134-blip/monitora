// adapters/ClientAdapter.kt
package com.universidade.monitor.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.universidade.monitor.R
import java.text.SimpleDateFormat
import java.util.*

class ClientAdapter(
    private val clients: Map<String, MutableList<DataItem>>,
    private val dateFormat: SimpleDateFormat
) : RecyclerView.Adapter<ClientAdapter.ViewHolder>() {

    data class DataItem(
        val timestamp: Long,
        val screenshot_url: String,
        val location: String,
        val apps: String,
        val logs: String,
        val battery: Int,
        val network: String
    )

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_client, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val clientId = clients.keys.elementAt(position)
        val dataList = clients[clientId] ?: emptyList()
        val lastData = dataList.firstOrNull()

        holder.tvClientId.text = clientId.take(12)
        holder.tvStatus.text = "🟢 Online"
        holder.tvTime.text = lastData?.timestamp?.let { dateFormat.format(Date(it)) } ?: "--:--:--"
        holder.tvNetwork.text = "📶 ${lastData?.network ?: "N/A"}"
        
        holder.tvBattery.text = if (lastData?.battery != -1) {
            "🔋 ${lastData.battery}%"
        } else {
            "🔋 N/A"
        }
        
        holder.tvApps.text = "Apps: ${lastData?.apps?.take(40) ?: "N/A"}"
        holder.tvLogs.text = "Logs: ${lastData?.logs?.take(100) ?: "N/A"}"
        holder.tvLocation.text = "📍 ${lastData?.location?.take(50) ?: "N/A"}"

        // Screenshot URL
        if (!lastData?.screenshot_url.isNullOrEmpty()) {
            Glide.with(holder.itemView.context)
                .load(lastData.screenshot_url)
                .placeholder(android.R.drawable.ic_menu_camera)
                .error(android.R.drawable.ic_menu_report_image)
                .into(holder.ivScreenshot)
            holder.ivScreenshot.visibility = View.VISIBLE
        } else {
            holder.ivScreenshot.visibility = View.GONE
        }

        // Expandir/collapsar
        holder.cardView.setOnClickListener {
            holder.detailsLayout.visibility = if (holder.detailsLayout.visibility == View.VISIBLE) {
                View.GONE
            } else {
                View.VISIBLE
            }
        }
    }

    override fun getItemCount(): Int = clients.size

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val cardView: CardView = itemView.findViewById(R.id.cardView)
        val tvClientId: TextView = itemView.findViewById(R.id.tvClientId)
        val tvStatus: TextView = itemView.findViewById(R.id.tvStatus)
        val tvTime: TextView = itemView.findViewById(R.id.tvTime)
        val tvNetwork: TextView = itemView.findViewById(R.id.tvNetwork)
        val tvBattery: TextView = itemView.findViewById(R.id.tvBattery)
        val tvApps: TextView = itemView.findViewById(R.id.tvApps)
        val tvLogs: TextView = itemView.findViewById(R.id.tvLogs)
        val tvLocation: TextView = itemView.findViewById(R.id.tvLocation)
        val ivScreenshot: ImageView = itemView.findViewById(R.id.ivScreenshot)
        val detailsLayout: LinearLayout = itemView.findViewById(R.id.detailsLayout)
    }
}