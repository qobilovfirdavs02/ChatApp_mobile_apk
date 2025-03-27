package com.example.chatapp3.network

import android.util.Log
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.example.chatapp3.ChatActivity
import com.example.chatapp3.Message
import com.example.chatapp3.MessageAdapter
import okhttp3.*
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

class WebSocketManager(
    private val client: OkHttpClient,
    private val messageAdapter: MessageAdapter,
    private val recyclerView: RecyclerView,
    private val currentUser: String,
    private val receiver: String,
    private val dateFormat: SimpleDateFormat,
    private val activity: ChatActivity
) {
    private lateinit var webSocket: WebSocket

    init {
        setupWebSocket()
    }

    private fun setupWebSocket() {
        val request = Request.Builder()
            .url("wss://web-production-545c.up.railway.app/ws/$currentUser/$receiver")
            .build()
        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.d("WebSocketManager", "WebSocket ulanishi ochildi")
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                Log.d("WebSocketManager", "Xabar keldi: $text")
                val json = JSONObject(text)
                if (json.has("error")) {
                    activity.runOnUiThread {
                        Toast.makeText(activity, json.getString("error"), Toast.LENGTH_SHORT).show()
                    }
                    return
                }
                val action = json.optString("action", "send")
                when (action) {
                    "send" -> {
                        val rawTimestamp = json.getString("timestamp")
                        val formattedTimestamp = try {
                            val date = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()).parse(rawTimestamp)
                            dateFormat.format(date)
                        } catch (e: Exception) {
                            rawTimestamp
                        }
                        val message = Message(
                            id = json.getInt("msg_id"),
                            sender = json.getString("sender"),
                            content = json.getString("content"),
                            timestamp = formattedTimestamp,
                            edited = json.optBoolean("edited", false),
                            deleted = json.optBoolean("deleted", false),
                            reaction = json.optString("reaction", null).takeIf { it.isNotEmpty() },
                            replyToId = json.optInt("reply_to_id", -1).takeIf { it != -1 }
                        )
                        activity.runOnUiThread {
                            val existingMessage = messageAdapter.messages.find { it.id == message.id }
                            if (existingMessage == null) {
                                if (!message.deleted) {
                                    messageAdapter.addMessage(message)
                                    recyclerView.scrollToPosition(messageAdapter.itemCount - 1)
                                }
                            } else if (message.deleted) {
                                existingMessage.deleted = true
                                existingMessage.content = "This message was deleted"
                                val position = messageAdapter.messages.indexOf(existingMessage)
                                messageAdapter.notifyItemChanged(position)
                            }
                            if (message.sender != currentUser && !activity.isChatActive) {
                                activity.showNotification(message.sender, message.content)
                            }
                        }
                    }
                    "edit" -> {
                        val msgId = json.getInt("msg_id")
                        val newContent = json.getString("content")
                        activity.runOnUiThread {
                            val message = messageAdapter.messages.find { it.id == msgId }
                            message?.let {
                                it.content = newContent
                                it.edited = true
                                val position = messageAdapter.messages.indexOf(it)
                                messageAdapter.notifyItemChanged(position)
                            }
                        }
                    }
                    "delete" -> {
                        val msgId = json.getInt("msg_id")
                        val deleteForAll = json.optBoolean("delete_for_all", false)
                        activity.runOnUiThread {
                            val message = messageAdapter.messages.find { it.id == msgId }
                            message?.let {
                                if (deleteForAll) {
                                    it.deleted = true
                                    it.content = "This message was deleted"
                                    val position = messageAdapter.messages.indexOf(it)
                                    messageAdapter.notifyItemChanged(position)
                                } else {
                                    it.deleted = true
                                    it.content = "This message was deleted"
                                    val position = messageAdapter.messages.indexOf(it)
                                    messageAdapter.notifyItemChanged(position)
                                }
                            }
                        }
                    }
                    "delete_permanent" -> {
                        val msgId = json.getInt("msg_id")
                        activity.runOnUiThread {
                            val message = messageAdapter.messages.find { it.id == msgId }
                            message?.let {
                                val position = messageAdapter.messages.indexOf(it)
                                messageAdapter.messages.removeAt(position)
                                messageAdapter.notifyItemRemoved(position)
                            }
                        }
                    }
                }
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                activity.runOnUiThread {
                    Log.e("WebSocketManager", "WebSocket xatosi: ${t.message}")
                    Toast.makeText(activity, "Ulanish xatosi", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                Log.d("WebSocketManager", "WebSocket yopilmoqda: $reason")
            }
        })
    }

    fun sendMessage(message: String) {
        webSocket.send(message)
    }

    fun closeWebSocket() {
        webSocket.close(1000, "Activity yopildi")
    }
}