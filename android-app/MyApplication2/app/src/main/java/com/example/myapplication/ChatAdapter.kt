package com.example.myapplication

import android.view.Gravity
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.databinding.ItemChatMessageBinding
class ChatAdapter(
    private val messageList: List<Message>,
    private val currentUserId: String
) : RecyclerView.Adapter<ChatAdapter.MessageViewHolder>() {
    companion object {
        private const val VIEW_TYPE_SENT = 1
        private const val VIEW_TYPE_RECEIVED = 2
    }
    inner class MessageViewHolder(val binding: ItemChatMessageBinding) : RecyclerView.ViewHolder(binding.root)
    override fun getItemViewType(position: Int): Int {
        val message = messageList[position]
        return if (message.senderId == currentUserId) {
            VIEW_TYPE_SENT
        } else {
            VIEW_TYPE_RECEIVED
        }
    }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageViewHolder {
        val binding = ItemChatMessageBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return MessageViewHolder(binding)
    }
    override fun onBindViewHolder(holder: MessageViewHolder, position: Int) {
        val message = messageList[position]
        holder.binding.tvMessageText.text = message.text
        val context = holder.itemView.context
        if (getItemViewType(position) == VIEW_TYPE_SENT) {
            holder.binding.messageRoot.gravity = Gravity.END
            holder.binding.tvMessageText.background = ContextCompat.getDrawable(context, R.drawable.chat_bubble_sent)
            holder.binding.tvMessageText.setTextColor(ContextCompat.getColor(context, android.R.color.white))
        } else {
            holder.binding.messageRoot.gravity = Gravity.START
            holder.binding.tvMessageText.background = ContextCompat.getDrawable(context, R.drawable.chat_bubble_received)
            holder.binding.tvMessageText.setTextColor(ContextCompat.getColor(context, android.R.color.white))
        }
    }

    override fun getItemCount(): Int = messageList.size
}