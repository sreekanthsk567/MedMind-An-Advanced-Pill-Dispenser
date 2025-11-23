package com.example.myapplication
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.databinding.ItemPatientBinding
class PatientAdapter(
    private val patientList: List<User>,
    private val onAdherenceClick: (User) -> Unit,
    private val onChatClick: (User) -> Unit
) : RecyclerView.Adapter<PatientAdapter.PatientViewHolder>() {
    inner class PatientViewHolder(private val binding: ItemPatientBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(patient: User) {
            val context = binding.root.context
            binding.tvPatientName.text = patient.fullName
            binding.tvPatientEmail.text = patient.email
            val chamber1Text = context.getString(R.string.chamber_1, patient.pillsChamber1)
            val chamber2Text = context.getString(R.string.chamber_2, patient.pillsChamber2)
            binding.tvPillCount.text = "$chamber1Text | $chamber2Text"

            binding.btnAdherence.setOnClickListener { onAdherenceClick(patient) }
            binding.btnChat.setOnClickListener { onChatClick(patient) }
        }
    }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PatientViewHolder {
        val binding = ItemPatientBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return PatientViewHolder(binding)
    }
    override fun onBindViewHolder(holder: PatientViewHolder, position: Int) {
        holder.bind(patientList[position])
    }
    override fun getItemCount(): Int = patientList.size
}