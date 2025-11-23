package com.example.myapplication
import android.graphics.Color
import android.view.Gravity
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.databinding.ItemAiChatMessageBinding
data class AiMessage(val role: String, val text: String)
class AiChatAdapter(
    private val messageList: List<AiMessage>
) : RecyclerView.Adapter<AiChatAdapter.AiMessageViewHolder>() {
    companion object {
        const val ROLE_USER = "user"
        const val ROLE_MODEL = "model"
    }
    inner class AiMessageViewHolder(val binding: ItemAiChatMessageBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AiMessageViewHolder {
        val binding = ItemAiChatMessageBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return AiMessageViewHolder(binding)
    }
    override fun onBindViewHolder(holder: AiMessageViewHolder, position: Int) {
        val message = messageList[position]
        holder.binding.tvAiMessageText.text = message.text
        val context = holder.itemView.context
        if (message.role == ROLE_USER) {

            holder.binding.aiMessageRoot.gravity = Gravity.END
            holder.binding.tvAiRole.text = context.getString(R.string.ai_role_you)
            (holder.binding.cardMessage as CardView).setCardBackgroundColor(
                Color.parseColor("#007AFF")
            )
        } else {
            holder.binding.aiMessageRoot.gravity = Gravity.START
            holder.binding.tvAiRole.text = context.getString(R.string.ai_role_companion)
            (holder.binding.cardMessage as CardView).setCardBackgroundColor(
                Color.parseColor("#2C2C2E")
            )
        }
    }

    override fun getItemCount(): Int = messageList.size
}