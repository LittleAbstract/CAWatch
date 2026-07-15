package com.security.careactivator.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.security.careactivator.R
import com.security.careactivator.db.DetectedCaEntity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * RecyclerView adapter for newly detected user CAs.
 */
class DetectedCaAdapter(
    private var events: List<DetectedCaEntity>,
    private val onItemClick: (DetectedCaEntity) -> Unit
) : RecyclerView.Adapter<DetectedCaAdapter.ViewHolder>() {

    companion object {
        private val DATE_FORMAT = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val issuer: TextView = view.findViewById(R.id.textIssuer)
        val fingerprint: TextView = view.findViewById(R.id.textFingerprint)
        val date: TextView = view.findViewById(R.id.textDate)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_detected_ca, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val event = events[position]
        val shortFingerprint = if (event.fingerprint.length > 24) {
            "${event.fingerprint.take(24)}…"
        } else {
            event.fingerprint
        }

        holder.issuer.text = event.issuer.ifBlank { "(unknown issuer)" }
        holder.fingerprint.text = shortFingerprint
        holder.date.text = DATE_FORMAT.format(Date(event.detectedAt))
        holder.itemView.setOnClickListener { onItemClick(event) }
    }

    override fun getItemCount(): Int = events.size

    fun updateData(newEvents: List<DetectedCaEntity>) {
        events = newEvents
        notifyDataSetChanged()
    }
}
