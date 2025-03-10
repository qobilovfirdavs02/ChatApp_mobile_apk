package com.example.chatapp3

import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import okhttp3.*
import org.json.JSONObject

class ChatActivity : AppCompatActivity() {
    private lateinit var client: OkHttpClient
    private lateinit var webSocket: WebSocket
    private lateinit var messageAdapter: MessageAdapter
    private lateinit var currentUser: String
    private lateinit var receiver: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)
        Log.d("ChatActivity", "ChatActivity ishga tushdi")

        // Intent’dan ma’lumotlarni olish
        currentUser = intent.getStringExtra("username")?.trim() ?: ""
        receiver = intent.getStringExtra("receiver")?.trim() ?: ""
        Log.d("ChatActivity", "CurrentUser: $currentUser, Receiver: $receiver")

        // UI elementlarni bog‘lash
        val recyclerView = findViewById<RecyclerView>(R.id.messageRecyclerView)
        val messageInput = findViewById<EditText>(R.id.messageInput)
        val sendButton = findViewById<Button>(R.id.sendButton)
        val exitButton = findViewById<Button>(R.id.exitButton)

        // RecyclerView sozlash
        messageAdapter = MessageAdapter(currentUser)
        recyclerView.adapter = messageAdapter
        recyclerView.layoutManager = LinearLayoutManager(this).apply { stackFromEnd = true }
        Log.d("ChatActivity", "Message RecyclerView sozlandi")

        // WebSocket ulanishi
        client = OkHttpClient()
        val request = Request.Builder()
            .url("wss://web-production-545c.up.railway.app/ws/$currentUser/$receiver")
            .build()
        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.d("ChatActivity", "WebSocket ulanishi ochildi")
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                Log.d("ChatActivity", "Xabar keldi: $text")
                val json = JSONObject(text)
                val message = Message(
                    id = json.getInt("msg_id"),
                    sender = json.getString("sender"),
                    content = json.getString("content"),
                    timestamp = json.getString("timestamp"),
                    edited = json.getBoolean("edited")
                )
                runOnUiThread {
                    messageAdapter.addMessage(message)
                    recyclerView.scrollToPosition(messageAdapter.itemCount - 1)
                }
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                runOnUiThread {
                    Log.e("ChatActivity", "WebSocket xatosi: ${t.message}")
                    Toast.makeText(this@ChatActivity, "Xato: ${t.message}", Toast.LENGTH_SHORT).show()
                }
            }
        })

        // Xabar yuborish
        sendButton.setOnClickListener {
            val content = messageInput.text.toString()
            if (content.isNotEmpty()) {
                val json = JSONObject().apply {
                    put("content", content)
                    put("action", "send")
                }
                webSocket.send(json.toString())
                messageInput.text.clear()
                Log.d("ChatActivity", "Xabar yuborildi: $content")
            }
        }

        // Chiqish
        exitButton.setOnClickListener {
            finish()
            Log.d("ChatActivity", "ChatActivity yopildi")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        webSocket.close(1000, "Ilova yopildi")
        client.dispatcher.executorService.shutdown()
        Log.d("ChatActivity", "WebSocket yopildi")
    }
}