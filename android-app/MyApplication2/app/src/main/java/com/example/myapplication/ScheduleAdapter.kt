package com.example.myapplication
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.util.Locale
class ScheduleAdapter(
    private val schedules: MutableList<Schedule>,
    private val onDeleteClick: (Schedule) -> Unit
) : RecyclerView.Adapter<ScheduleAdapter.ViewHolder>() {
    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val title: TextView = view.findViewById(R.id.tvScheduleTitle)
        val details: TextView = view.findViewById(R.id.tvScheduleDetails)
        val deleteBtn: ImageButton = view.findViewById(R.id.btnDeleteSchedule)
    }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_schedule, parent, false)
        return ViewHolder(view)
    }
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = schedules[position]
        val timeStr = item.times.joinToString(", ") { String.format(Locale.US, "%02d:%02d", it.hour, it.minute) }
        val daysStr = if (item.type == "DAILY") "Daily" else item.daysOfWeek.joinToString(", ")
        holder.title.text = "${item.pillName} (Ch ${item.servo})"
        holder.details.text = "$daysStr at $timeStr"
        holder.deleteBtn.setOnClickListener { onDeleteClick(item) }
    }
    override fun getItemCount() = schedules.size
}