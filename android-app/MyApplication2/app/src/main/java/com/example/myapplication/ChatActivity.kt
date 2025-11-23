package com.example.myapplication

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.myapplication.databinding.ActivityChatBinding
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.firestore.ktx.toObject
import com.google.firebase.ktx.Firebase

class ChatActivity : AppCompatActivity() {
    private lateinit var binding: ActivityChatBinding
    private val auth = Firebase.auth
    private val db = Firebase.firestore
    private val senderId = auth.currentUser?.uid ?: ""
    private var receiverId: String? = null
    private var receiverName: String? = null
    private lateinit var chatAdapter: ChatAdapter
    private val messageList = mutableListOf<Message>()
    companion object {
        var isChatOpen = false
    }
    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleHelper.setLocale(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChatBinding.inflate(layoutInflater)
        setContentView(binding.root)
        isChatOpen = true
        receiverId = intent.getStringExtra("RECEIVER_ID")
        receiverName = intent.getStringExtra("RECEIVER_NAME")
        if (senderId.isEmpty() || receiverId == null) {
            Toast.makeText(this, "Error: Chat user not found", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        binding.tvChatTitle.text = "Chat with $receiverName"
        binding.btnChatBack.setOnClickListener { finish() }
        setupRecyclerView()
        listenForMessages()
        binding.btnSend.setOnClickListener {
            val messageText = binding.etMessage.text.toString().trim()
            if (messageText.isNotEmpty()) {
                sendMessage(messageText)
                binding.etMessage.text.clear()
            }
        }
        binding.btnRequestCall.setOnClickListener {
            val callMessage = "🚨 URGENT: I am requesting a call immediately!"
            sendMessage(callMessage)
            Toast.makeText(this, "Call request sent!", Toast.LENGTH_SHORT).show()
        }
    }
    override fun onDestroy() {
        super.onDestroy()
        isChatOpen = false
    }
    private fun setupRecyclerView() {
        chatAdapter = ChatAdapter(messageList, senderId)
        binding.rvChat.apply {
            layoutManager = LinearLayoutManager(this@ChatActivity).apply {
                stackFromEnd = true
            }
            adapter = chatAdapter
        }
    }
    private fun sendMessage(text: String) {
        val message = hashMapOf(
            "senderId" to senderId,
            "receiverId" to receiverId,
            "text" to text,
            "timestamp" to com.google.firebase.firestore.FieldValue.serverTimestamp()
        )

        db.collection("chats").add(message)
            .addOnFailureListener { e ->
                Toast.makeText(this, "Failed to send", Toast.LENGTH_SHORT).show()
            }
    }

    private fun listenForMessages() {
        db.collection("chats")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshots, e ->
                if (e != null) {
                    Log.w("ChatActivity", "Listen failed", e)
                    return@addSnapshotListener
                }

                if (snapshots != null) {
                    messageList.clear()
                    for (doc in snapshots) {
                        val msg = doc.toObject(Message::class.java)
                        // Filter to ensure we only see messages between these two users
                        if ((msg.senderId == senderId && msg.receiverId == receiverId) ||
                            (msg.senderId == receiverId && msg.receiverId == senderId)) {
                            messageList.add(msg)
                        }
                    }
                    chatAdapter.notifyDataSetChanged()
                    if (messageList.isNotEmpty()) {
                        binding.rvChat.scrollToPosition(messageList.size - 1)
                    }
                }
            }
    }
}