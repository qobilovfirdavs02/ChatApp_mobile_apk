package com.example.chatapp3.chat

import android.content.Context // Qo‘shildi
import android.os.Bundle
import android.util.Log
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.chatapp3.MessageAdapter
import com.example.chatapp3.R
import com.example.chatapp3.network.MediaHandler
import com.example.chatapp3.network.NetworkUtils
import com.example.chatapp3.network.WebSocketManager
import com.example.chatapp3.ui.UIUtils
import com.example.chatapp3.utils.NotificationManager
import okhttp3.OkHttpClient
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Locale

class ChatActivity : AppCompatActivity() {
    private val client by lazy { OkHttpClient() }
    private val networkUtils by lazy { NetworkUtils(client) }
    private lateinit var webSocketManager: WebSocketManager
    private lateinit var mediaHandler: MediaHandler
    private lateinit var notificationManager: NotificationManager
    private lateinit var uiUtils: UIUtils
    private lateinit var voiceChat: VoiceChat
    private lateinit var messageAdapter: MessageAdapter
    private lateinit var currentUser: String
    private lateinit var receiver: String
    private var replyToMessageId: Int? = null
    private val dateFormat = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault())
    var isChatActive = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)
        Log.d("ChatActivity", "ChatActivity ishga tushdi")

        if (!isNetworkAvailable()) {
            Toast.makeText(this, "Internet aloqasi yo‘q", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        currentUser = intent.getStringExtra("username")?.trim() ?: ""
        receiver = intent.getStringExtra("receiver")?.trim() ?: ""

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.apply {
            title = receiver
            setDisplayHomeAsUpEnabled(true)
        }

        val recyclerView = findViewById<RecyclerView>(R.id.messageRecyclerView)
        val messageInput = findViewById<EditText>(R.id.messageInput)
        val sendButton = findViewById<ImageButton>(R.id.sendButton)
        val mediaButton = findViewById<ImageButton>(R.id.mediaButton)
        val voiceButton = findViewById<ImageButton>(R.id.voiceButton)

        messageAdapter = MessageAdapter(currentUser, this)
        recyclerView.adapter = messageAdapter
        recyclerView.layoutManager = LinearLayoutManager(this).apply { stackFromEnd = true }

        messageAdapter.attachSwipeToReply(recyclerView)
        messageAdapter.onReplyRequested = { message ->
            replyToMessageId = message.id
            messageInput.hint = "Javob: ${message.content.take(20)}..."
        }

        webSocketManager = WebSocketManager(
            client,
            messageAdapter,
            recyclerView,
            currentUser,
            receiver,
            dateFormat,
            this
        )
        mediaHandler = MediaHandler(
            this,
            networkUtils,
            messageAdapter,
            recyclerView,
            currentUser,
            receiver,
            dateFormat,
            ::sendMessage
        )
        notificationManager = NotificationManager(this)
        uiUtils = UIUtils(this, mediaHandler)
        voiceChat = VoiceChat(this, networkUtils, webSocketManager, currentUser, receiver, voiceButton)

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

        voiceButton.setOnClickListener {
            voiceChat.toggleRecording()
        }
    }

    fun sendMessage(message: String) {
        try {
            webSocketManager.sendMessage(message)
        } catch (e: Exception) {
            Toast.makeText(this, "Xabar yuborishda xato: ${e.message}", Toast.LENGTH_SHORT).show()
            Log.e("ChatActivity", "Xabar yuborishda xato", e)
        }
    }

    private fun isNetworkAvailable(): Boolean {
        val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as android.net.ConnectivityManager
        val activeNetwork = connectivityManager.activeNetworkInfo
        return activeNetwork != null && activeNetwork.isConnected
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        voiceChat.handlePermissionResult(requestCode, grantResults)
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
        voiceChat.release()
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    fun showNotification(sender: String, content: String) {
        notificationManager.showNotification(sender, content, currentUser, receiver)
    }
}