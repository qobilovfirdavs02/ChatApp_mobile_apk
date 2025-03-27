package com.example.chatapp3

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.chatapp3.network.NetworkUtils
import com.example.chatapp3.network.WebSocketManager
import okhttp3.OkHttpClient
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

class ChatActivity : AppCompatActivity() {
    private val client by lazy { OkHttpClient() }
    private val networkUtils by lazy { NetworkUtils(client) }
    private lateinit var webSocketManager: WebSocketManager
    private lateinit var messageAdapter: MessageAdapter
    private lateinit var currentUser: String
    private lateinit var receiver: String
    private val CHANNEL_ID = "chat_notifications"
    private val NOTIFICATION_ID = 1
    private var replyToMessageId: Int? = null
    val dateFormat = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault())
    private lateinit var tempMediaUri: Uri
    var isChatActive = false

    private val pickMediaLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let { sendMedia(it) }
    }
    private val takePictureLauncher = registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success) sendMedia(tempMediaUri)
    }
    private val recordVideoLauncher = registerForActivityResult(ActivityResultContracts.CaptureVideo()) { success ->
        if (success) sendMedia(tempMediaUri)
    }
    private val cameraPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) takePicture() else Toast.makeText(this, "Kamera ruxsati rad etildi", Toast.LENGTH_SHORT).show()
    }
    private val videoPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) recordVideo() else Toast.makeText(this, "Kamera ruxsati rad etildi", Toast.LENGTH_SHORT).show()
    }

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

        // WebSocketManager’ni ishga tushirish
        webSocketManager = WebSocketManager(client, messageAdapter, recyclerView, currentUser, receiver, dateFormat, this)

        createNotificationChannel()

        sendButton.setOnClickListener {
            val content = messageInput.text.toString().trim()
            if (content.isNotEmpty()) {
                val json = JSONObject().apply {
                    put("content", content)
                    put("action", "send")
                    if (replyToMessageId != null) put("reply_to_id", replyToMessageId)
                }
                webSocketManager.sendMessage(json.toString())
                messageInput.text.clear()
                replyToMessageId = null
                messageInput.hint = "Xabar yozing"
            }
        }

        mediaButton.setOnClickListener { showMediaOptionsDialog() }
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

    private fun showMediaOptionsDialog() {
        val options = arrayOf("Galereyadan tanlash", "Suratga olish", "Video yozish")
        AlertDialog.Builder(this)
            .setTitle("Media yuborish")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> pickMediaLauncher.launch("image/* video/*")
                    1 -> checkCameraPermissionAndTakePicture()
                    2 -> checkCameraPermissionAndRecordVideo()
                }
            }
            .setNegativeButton("Bekor qilish", null)
            .show()
    }

    private fun checkCameraPermissionAndTakePicture() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            takePicture()
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private fun checkCameraPermissionAndRecordVideo() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            recordVideo()
        } else {
            videoPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private fun takePicture() {
        val photoFile = createTempFile("photo", ".jpg")
        tempMediaUri = FileProvider.getUriForFile(this, "${packageName}.fileprovider", photoFile)
        takePictureLauncher.launch(tempMediaUri)
    }

    private fun recordVideo() {
        val videoFile = createTempFile("video", ".mp4")
        tempMediaUri = FileProvider.getUriForFile(this, "${packageName}.fileprovider", videoFile)
        recordVideoLauncher.launch(tempMediaUri)
    }

    private fun createTempFile(prefix: String, suffix: String): File {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val storageDir = getExternalFilesDir(null)
        return File.createTempFile("${prefix}_${timeStamp}_", suffix, storageDir)
    }

    private fun sendMedia(uri: Uri) {
        Log.d("ChatActivity", "Sending media: $uri")
        val file = uriToFile(uri)
        val recyclerView = findViewById<RecyclerView>(R.id.messageRecyclerView)
        val dummyMessage = Message(
            id = (0..Int.MAX_VALUE).random(),
            sender = currentUser,
            content = "Yuklanmoqda...",
            timestamp = dateFormat.format(Date()),
            edited = false,
            reaction = null,
            replyToId = replyToMessageId
        )
        messageAdapter.addMessage(dummyMessage)
        val position = messageAdapter.itemCount - 1
        recyclerView.scrollToPosition(position)

        val progressBar = recyclerView.findViewHolderForAdapterPosition(position)
            ?.itemView?.findViewById<android.widget.ProgressBar>(R.id.uploadProgress)

        networkUtils.sendImage(
            imageFile = file,
            sender = currentUser,
            receiver = receiver,
            onProgress = { progress ->
                runOnUiThread {
                    progressBar?.isVisible = true
                    progressBar?.progress = progress
                }
            },
            onSuccess = { message ->
                runOnUiThread {
                    progressBar?.isVisible = false
                    messageAdapter.messages[position] = message
                    messageAdapter.notifyItemChanged(position)
                    recyclerView.scrollToPosition(messageAdapter.itemCount - 1)
                }
                val json = JSONObject().apply {
                    put("content", message.content)
                    put("action", "send")
                    if (replyToMessageId != null) put("reply_to_id", replyToMessageId)
                }
                webSocketManager.sendMessage(json.toString())
                replyToMessageId = null
            },
            onFailure = { error ->
                runOnUiThread {
                    Log.e("ChatActivity", error)
                    progressBar?.isVisible = false
                    messageAdapter.messages[position].content = "Yuklash xatosi"
                    messageAdapter.notifyItemChanged(position)
                    Toast.makeText(this, "Yuklash xatosi", Toast.LENGTH_SHORT).show()
                }
            }
        )
    }

    private fun uriToFile(uri: Uri): File {
        val inputStream = contentResolver.openInputStream(uri)
        val file = createTempFile("media", ".tmp")
        inputStream?.use { input ->
            FileOutputStream(file).use { output ->
                input.copyTo(output)
            }
        }
        return file
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Chat Notifications"
            val descriptionText = "New message notifications"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply { description = descriptionText }
            val notificationManager: NotificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun showNotification(sender: String, content: String) {
        val intent = Intent(this, ChatActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("username", currentUser)
            putExtra("receiver", receiver)
        }
        val pendingIntent: PendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_chat_notification)
            .setContentTitle("Yangi xabar: $sender")
            .setContentText(content)
            .setStyle(NotificationCompat.BigTextStyle().bigText(content))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, builder.build())
    }
}