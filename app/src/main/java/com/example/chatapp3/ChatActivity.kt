package com.example.chatapp3

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import okhttp3.*
import org.json.JSONObject

class ChatActivity : AppCompatActivity() {
    private val client by lazy { OkHttpClient() }
    private lateinit var webSocket: WebSocket
    private lateinit var messageAdapter: MessageAdapter
    private lateinit var currentUser: String
    private lateinit var receiver: String
    private val CHANNEL_ID = "chat_notifications"
    private val NOTIFICATION_ID = 1
    private var replyToMessageId: Int? = null // Javob berilayotgan xabar IDsi

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)
        Log.d("ChatActivity", "ChatActivity ishga tushdi")

        currentUser = intent.getStringExtra("username")?.trim() ?: ""
        receiver = intent.getStringExtra("receiver")?.trim() ?: ""

        val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.apply {
            title = receiver
            setDisplayHomeAsUpEnabled(true)
        }

        val recyclerView = findViewById<RecyclerView>(R.id.messageRecyclerView)
        val messageInput = findViewById<EditText>(R.id.messageInput)
        val sendButton = findViewById<Button>(R.id.sendButton)
        val exitButton = findViewById<Button>(R.id.exitButton)

        messageAdapter = MessageAdapter(currentUser)
        recyclerView.adapter = messageAdapter
        recyclerView.layoutManager = LinearLayoutManager(this).apply { stackFromEnd = true }

        // Swipe bilan javob berish
        messageAdapter.attachSwipeToReply(recyclerView)
        messageAdapter.onReplyRequested = { message ->
            replyToMessageId = message.id
            messageInput.hint = "Javob: ${message.content.take(20)}..."
        }

        createNotificationChannel()

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
                if (json.has("error")) {
                    runOnUiThread {
                        Toast.makeText(this@ChatActivity, json.getString("error"), Toast.LENGTH_SHORT).show()
                    }
                    return
                }
                val message = Message(
                    id = json.getInt("msg_id"),
                    sender = json.getString("sender"),
                    content = json.getString("content"),
                    timestamp = json.getString("timestamp"),
                    edited = json.optBoolean("edited", false),
                    reaction = json.optString("reaction", null).takeIf { it.isNotEmpty() },
                    replyToId = json.optInt("reply_to_id", -1).takeIf { it != -1 }
                )
                runOnUiThread {
                    messageAdapter.addMessage(message)
                    recyclerView.scrollToPosition(messageAdapter.itemCount - 1)
                    if (message.sender != currentUser) {
                        showNotification(message.sender, message.content)
                    }
                }
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                runOnUiThread {
                    Log.e("ChatActivity", "WebSocket xatosi: ${t.message}")
                    Toast.makeText(this@ChatActivity, "Ulanish xatosi", Toast.LENGTH_SHORT).show()
                }
            }
        })

        sendButton.setOnClickListener {
            val content = messageInput.text.toString().trim()
            if (content.isNotEmpty()) {
                val json = JSONObject().apply {
                    put("content", content)
                    put("action", "send")
                    if (replyToMessageId != null) {
                        put("reply_to_id", replyToMessageId)
                    }
                }
                webSocket.send(json.toString())
                messageInput.text.clear()
                replyToMessageId = null
                messageInput.hint = "Xabar yozing"
            }
        }

        exitButton.setOnClickListener {
            finish()
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Chat Notifications"
            val descriptionText = "Yangi xabarlar uchun bildirishnomalar"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun showNotification(sender: String, content: String) {
        val intent = Intent(this, ChatActivity::class.java).apply {
            putExtra("username", currentUser)
            putExtra("receiver", receiver)
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Yangi xabar: $sender")
            .setContentText(content)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, builder.build())
    }

    override fun onDestroy() {
        super.onDestroy()
        webSocket.close(1000, "Ilova yopildi")
        client.dispatcher.executorService.shutdown()
    }
}