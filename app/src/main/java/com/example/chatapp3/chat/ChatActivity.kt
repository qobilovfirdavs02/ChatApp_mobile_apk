package com.example.chatapp3.chat

import android.os.Bundle
import android.Manifest
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
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
import com.example.chatapp3.utils.MediaRecorderUtil
import com.example.chatapp3.utils.NotificationManager
import okhttp3.OkHttpClient
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale

class ChatActivity : AppCompatActivity() {
    private val client by lazy { OkHttpClient() }
    private val networkUtils by lazy { NetworkUtils(client) }
    private lateinit var webSocketManager: WebSocketManager
    private lateinit var mediaHandler: MediaHandler
    private lateinit var notificationManager: NotificationManager
    private lateinit var uiUtils: UIUtils
    private lateinit var mediaRecorderUtil: MediaRecorderUtil
    private lateinit var messageAdapter: MessageAdapter
    private lateinit var currentUser: String
    private lateinit var receiver: String
    private var replyToMessageId: Int? = null
    private var isRecording = false // Yozish holatini kuzatish
    val dateFormat = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault())
    var isChatActive = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)
        Log.d("ChatActivity", "ChatActivity ishga tushdi")

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
        val voiceButton = findViewById<ImageButton>(R.id.voiceButton) // Layout’da voiceButton qo‘shilgan deb taxmin qildim

        messageAdapter = MessageAdapter(currentUser)
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
        mediaRecorderUtil = MediaRecorderUtil(this) // MediaRecorderUtil qo‘shildi

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

        // Ovozli xabar uchun tugma
        voiceButton.setOnClickListener {
            if (!isRecording) {
                startVoiceRecording()
            } else {
                stopVoiceRecording()
            }
        }
    }

    fun sendMessage(message: String) {
        webSocketManager.sendMessage(message)
    }

    private fun checkRecordingPermission(): Boolean {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), 100)
            return false
        }
        return true
    }

    private fun startVoiceRecording() {
        if (!checkRecordingPermission()) {
            return
        }
        val filePath = mediaRecorderUtil.startRecording()
        if (filePath != null) {
            isRecording = true
            Toast.makeText(this, "Yozish boshlandi", Toast.LENGTH_SHORT).show()
            Log.d("ChatActivity", "Yozish boshlandi: $filePath")
            findViewById<ImageButton>(R.id.voiceButton).setImageResource(R.drawable.ic_stop)
        } else {
            Toast.makeText(this, "Yozishni boshlashda xato", Toast.LENGTH_SHORT).show()
        }
    }

    private fun stopVoiceRecording() {
        val filePath = mediaRecorderUtil.stopRecording()
        if (filePath != null) {
            isRecording = false
            Toast.makeText(this, "Yozish tugadi", Toast.LENGTH_SHORT).show()
            Log.d("ChatActivity", "Yozish tugadi: $filePath")
            findViewById<ImageButton>(R.id.voiceButton).setImageResource(R.drawable.ic_mic) // Mikrofon ikonasi
            sendVoiceMessage(filePath)
        } else {
            Toast.makeText(this, "Yozishni to‘xtatishda xato", Toast.LENGTH_SHORT).show()
        }
    }

    private fun sendVoiceMessage(filePath: String) {
        val voiceFile = File(filePath)
        networkUtils.sendVoice(voiceFile, currentUser, receiver, { message ->
            webSocketManager.sendVoice(message.content, message.id)
            Log.d("ChatActivity", "Ovozli xabar yuborildi: ${message.content}")
        }, { error ->
            Log.e("ChatActivity", "Ovozli xabar yuborishda xato: $error")
            mediaRecorderUtil.deleteRecording(filePath) // Xato bo‘lsa faylni o‘chirish
            Toast.makeText(this, "Ovoz yuborishda xato: $error", Toast.LENGTH_SHORT).show()
        })
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 100 && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            startVoiceRecording()
        } else {
            Toast.makeText(this, "Ovoz yozish uchun ruxsat kerak", Toast.LENGTH_SHORT).show()
        }
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
        mediaRecorderUtil.release() // MediaRecorder resurslarini tozalash
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    fun showNotification(sender: String, content: String) {
        notificationManager.showNotification(sender, content, currentUser, receiver)
    }
}