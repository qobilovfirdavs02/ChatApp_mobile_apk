package com.example.chatapp3.chat

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log

import android.widget.ImageButton
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.chatapp3.Message
import com.example.chatapp3.R
import com.example.chatapp3.network.NetworkUtils
import com.example.chatapp3.network.WebSocketManager
import com.example.chatapp3.utils.MediaRecorderUtil
import java.io.File

class VoiceChat(
    private val context: Context,
    private val networkUtils: NetworkUtils,
    private val webSocketManager: WebSocketManager,
    private val currentUser: String,
    private val receiver: String,
    private val voiceButton: ImageButton
) {
    private val mediaRecorderUtil = MediaRecorderUtil(context)
    private var isRecording = false


    fun toggleRecording() {
        if (!isRecording) {
            startVoiceRecording()
        } else {
            stopVoiceRecording()
        }
    }

    private fun checkRecordingPermission(): Boolean {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(context as ChatActivity, arrayOf(Manifest.permission.RECORD_AUDIO), 100)
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
            Toast.makeText(context, "Yozish boshlandi", Toast.LENGTH_SHORT).show()
            Log.d("VoiceChat", "Yozish boshlandi: $filePath")
            voiceButton?.setImageResource(R.drawable.ic_stop)
        } else {
            Toast.makeText(context, "Yozishni boshlashda xato", Toast.LENGTH_SHORT).show()
        }
    }

    private fun stopVoiceRecording() {
        val filePath = mediaRecorderUtil.stopRecording()
        if (filePath != null) {
            isRecording = false
            Toast.makeText(context, "Yozish tugadi", Toast.LENGTH_SHORT).show()
            Log.d("VoiceChat", "Yozish tugadi: $filePath")
            voiceButton?.setImageResource(R.drawable.ic_mic)
            sendVoiceMessage(filePath)
        } else {
            Toast.makeText(context, "Yozishni to‘xtatishda xato", Toast.LENGTH_SHORT).show()
        }
    }

//    private fun sendVoiceMessage(filePath: String) {
//        val voiceFile = File(filePath)
//        networkUtils.sendVoice(voiceFile, currentUser, receiver, { message ->
//            // Serverdan qaytgan URL ni WebSocket orqali yuboramiz
//            val voiceUrl = message.content // Bu URL bo‘lishi kerak
//            webSocketManager.sendVoice(voiceUrl, message.id)
//            Log.d("VoiceChat", "Ovozli xabar yuborildi: $voiceUrl")
//            mediaRecorderUtil.deleteRecording(filePath) // Mahalliy faylni o‘chiramiz
//        }, { error ->
//            Log.e("VoiceChat", "Ovozli xabar yuborishda xato: $error")
//            mediaRecorderUtil.deleteRecording(filePath)
//            Toast.makeText(context, "Ovoz yuborishda xato: $error", Toast.LENGTH_SHORT).show()
//        })
//    }
private fun sendVoiceMessage(filePath: String) {
    val voiceFile = File(filePath)
    networkUtils.sendVoice(voiceFile, currentUser, receiver, { message ->
        val voiceMessage = Message(
            id = message.id,
            sender = currentUser,
            content = message.content, // URL keladi
            timestamp = System.currentTimeMillis().toString(),
            type = "voice" // Muhim
        )
        webSocketManager.sendVoice(message.content, message.id)
        mediaRecorderUtil.deleteRecording(filePath)
    }, { error ->
        mediaRecorderUtil.deleteRecording(filePath)
        Toast.makeText(context, "Xato: $error", Toast.LENGTH_SHORT).show()
    })
}

    fun handlePermissionResult(requestCode: Int, grantResults: IntArray) {
        if (requestCode == 100 && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            startVoiceRecording()
        } else {
            Toast.makeText(context, "Ovoz yozish uchun ruxsat kerak", Toast.LENGTH_SHORT).show()
        }
    }

    fun release() {
        mediaRecorderUtil.release()
    }
}