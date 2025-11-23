package com.example.myapplication
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.Locale

class AdherenceAdapter(private val logs: List<AdherenceLog>) : RecyclerView.Adapter<AdherenceAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val status: TextView = view.findViewById(R.id.tvLogStatus)
        val details: TextView = view.findViewById(R.id.tvLogDetails)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_adherence_log, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val log = logs[position]
        val fmt = SimpleDateFormat("MMM dd, hh:mm a", Locale.US)
        if (log.taken) {
            holder.status.text = "[TAKEN]"
            holder.status.setTextColor(Color.parseColor("#4CAF50"))
        } else {
            holder.status.text = "[MISSED]"
            holder.status.setTextColor(Color.parseColor("#F44336"))
        }
        holder.details.text = "${log.pillName ?: "Pill"} (${log.pills}) - ${fmt.format(log.timestamp)}"
    }

    override fun getItemCount() = logs.size
}