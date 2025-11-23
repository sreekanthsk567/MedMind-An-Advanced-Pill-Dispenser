package com.example.myapplication
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.myapplication.databinding.ActivityAiCompanionBinding
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.generationConfig
import kotlinx.coroutines.launch
class AiCompanionActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAiCompanionBinding
    private lateinit var aiChatAdapter: AiChatAdapter
    private val messageList = mutableListOf<AiMessage>()
    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleHelper.setLocale(newBase))
    }
    private val GEMINI_API_KEY = "Our private Gemini Key will be here"
    private val systemMessage: String = """
        You are "MedMind", a kind and supportive AI medical assistant.You are a part of a smart pill dispenser app.You are programmed to respond to queries relating to medicine, health and wellness.Instructions:ALWAYS SHOW EMPATHY: Being polite and gentle is a must.STICK TO THE FACTS: Use general medical knowledge.NO DIAGNOSIS: It is a must to not give any medical diagnosis.DISMISSAL OF QUESTION: When a user attempts to seek a diagnosis or serious medical advice (“Is it ok to take this pill?” or similar), You must reply that: “I cannot provide a medical diagnosis. Please consult with your doctor or a healthcare professional for personal medical advice.”CONCISE: Please limit your answer to a maximum of 2 paragraphs.
    """.trimIndent()
    private val generativeModel by lazy {
        val config = try {
            generationConfig { temperature = 0.7f }
        } catch (e: Throwable) {
            null
        }
        if (config != null) {
            GenerativeModel(
                modelName = "gemini-2.5-flash-preview-09-2025",
                apiKey = GEMINI_API_KEY,
                generationConfig = config
            )
        } else {
            try {
                GenerativeModel(
                    modelName = "gemini-2.5-preview-09-2025",
                    apiKey = GEMINI_API_KEY
                )
            } catch (ex: Throwable) {
                throw IllegalStateException("GenerativeModel not be constructed: ${ex.message}", ex)
            }
        }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAiCompanionBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupRecyclerView()
        binding.btnAiBack.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }
        binding.btnAiSend.setOnClickListener {
            val prompt = binding.etAiMessage.text.toString().trim()
            if (prompt.isNotEmpty() && GEMINI_API_KEY != "YOUR_GEMINI_API_KEY_HERE") {
                sendMessage(prompt)
                binding.etAiMessage.text.clear()
            } else if (GEMINI_API_KEY == "YOUR_GEMINI_API_KEY_HERE") {
                Toast.makeText(this, "API Key is missing in AiCompanionActivity.kt", Toast.LENGTH_LONG).show()
            }
        }
    }
    private fun setupRecyclerView() {
        aiChatAdapter = AiChatAdapter(messageList)
        binding.rvAiChat.apply {
            layoutManager = LinearLayoutManager(this@AiCompanionActivity).apply {
                stackFromEnd = true
            }
            adapter = aiChatAdapter
        }
    }
    private fun sendMessage(prompt: String) {
        addMessage(AiMessage(AiChatAdapter.ROLE_USER, prompt))
        showLoading(true)
        lifecycleScope.launch {
            try {
                val fullPrompt = "$systemMessage\n\nUser: $prompt"
                val response = generativeModel.generateContent(fullPrompt)
                response.text?.let { aiResponse ->
                    addMessage(AiMessage(AiChatAdapter.ROLE_MODEL, aiResponse))
                } ?: run {
                    addMessage(AiMessage(AiChatAdapter.ROLE_MODEL, "No response from AI."))
                }
            } catch (e: Exception) {
                Log.e("AiCompanionActivity", "Error generating content", e)
                addMessage(AiMessage(AiChatAdapter.ROLE_MODEL, "Error: ${e.message}"))
            } finally {
                showLoading(false)
            }
        }
    }
    private fun addMessage(message: AiMessage) {
        messageList.add(message)
        aiChatAdapter.notifyItemInserted(messageList.size - 1)
        binding.rvAiChat.scrollToPosition(messageList.size - 1)
    }
    private fun showLoading(isLoading: Boolean) {
        binding.pbAiLoading.visibility = if (isLoading) View.VISIBLE else View.GONE
        binding.btnAiSend.isEnabled = !isLoading
    }
}
