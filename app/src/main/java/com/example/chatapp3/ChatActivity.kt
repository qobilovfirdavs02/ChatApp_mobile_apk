package com.example.chatapp3

import android.os.Bundle
import android.util.Log
import android.widget.EditText
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.chatapp3.network.MediaHandler
import com.example.chatapp3.network.NetworkUtils
import com.example.chatapp3.network.WebSocketManager
import com.example.chatapp3.ui.UIUtils
import com.example.chatapp3.utils.NotificationManager
import okhttp3.OkHttpClient
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

class ChatActivity : AppCompatActivity() {
    private val client by lazy { OkHttpClient() }
    private val networkUtils by lazy { NetworkUtils(client) }
    private lateinit var webSocketManager: WebSocketManager
    private lateinit var mediaHandler: MediaHandler
    private lateinit var notificationManager: NotificationManager
    private lateinit var uiUtils: UIUtils
    private lateinit var messageAdapter: MessageAdapter
    private lateinit var currentUser: String
    private lateinit var receiver: String
    private var replyToMessageId: Int? = null
    val dateFormat = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault())
    var isChatActive = false

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
        val sendButton = findViewById<ImageButton>(R.id.sendButton)
        val mediaButton = findViewById<ImageButton>(R.id.mediaButton)

        messageAdapter = MessageAdapter(currentUser)
        recyclerView.adapter = messageAdapter
        recyclerView.layoutManager = LinearLayoutManager(this).apply { stackFromEnd = true }

        messageAdapter.attachSwipeToReply(recyclerView)
        messageAdapter.onReplyRequested = { message ->
            replyToMessageId = message.id
            messageInput.hint = "Javob: ${message.content.take(20)}..."
        }

        webSocketManager = WebSocketManager(client, messageAdapter, recyclerView, currentUser, receiver, dateFormat, this)
        mediaHandler = MediaHandler(this, networkUtils, messageAdapter, recyclerView, currentUser, receiver, dateFormat, ::sendMessage)
        notificationManager = NotificationManager(this)
        uiUtils = UIUtils(this, mediaHandler)

        notificationManager.createNotificationChannel()

        sendButton.setOnClickListener {
            val content = messageInput.text.toString().trim()
            if (content.isNotEmpty()) {
                val json = JSONObject().apply {
                    put("content", content)
                    put("action", "send")
                    if (replyToMessageId != null) put("reply_to_id", replyToMessageId)
                }
                sendMessage(json.toString())
                messageInput.text.clear()
                replyToMessageId = null
                messageInput.hint = "Xabar yozing"
            }
        }

        mediaButton.setOnClickListener { uiUtils.showMediaOptionsDialog() }
    }

    fun sendMessage(message: String) {
        webSocketManager.sendMessage(message)
    }

    override fun onResume() {
        super.onResume()
        isChatActive = true
    }

    override fun onPause() {
        super.onPause()
        isChatActive = false
    }

    override fun onDestroy() {
        super.onDestroy()
        webSocketManager.closeWebSocket()
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    fun showNotification(sender: String, content: String) {
        notificationManager.showNotification(sender, content, currentUser, receiver)
    }
}