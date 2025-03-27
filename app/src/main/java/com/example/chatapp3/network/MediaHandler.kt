package com.example.chatapp3.network

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.util.Log
import android.widget.ProgressBar
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.example.chatapp3.Message
import com.example.chatapp3.MessageAdapter
import com.example.chatapp3.R
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

class MediaHandler(
    private val activity: AppCompatActivity,
    private val networkUtils: NetworkUtils,
    private val messageAdapter: MessageAdapter,
    private val recyclerView: RecyclerView,
    private val currentUser: String,
    private val receiver: String,
    private val dateFormat: SimpleDateFormat,
    private val sendMessageCallback: (String) -> Unit // WebSocket uchun callback
) {
    private lateinit var tempMediaUri: Uri

    private val pickMediaLauncher: ActivityResultLauncher<String> = activity.registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let { sendMedia(it) }
    }

    private val takePictureLauncher: ActivityResultLauncher<Uri> = activity.registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success) sendMedia(tempMediaUri)
    }

    private val recordVideoLauncher: ActivityResultLauncher<Uri> = activity.registerForActivityResult(ActivityResultContracts.CaptureVideo()) { success ->
        if (success) sendMedia(tempMediaUri)
    }

    private val cameraPermissionLauncher: ActivityResultLauncher<String> = activity.registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) takePicture() else Toast.makeText(activity, "Kamera ruxsati rad etildi", Toast.LENGTH_SHORT).show()
    }

    private val videoPermissionLauncher: ActivityResultLauncher<String> = activity.registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) recordVideo() else Toast.makeText(activity, "Kamera ruxsati rad etildi", Toast.LENGTH_SHORT).show()
    }

    fun pickMedia() {
        pickMediaLauncher.launch("image/* video/*")
    }

    fun checkCameraPermissionAndTakePicture() {
        if (ContextCompat.checkSelfPermission(activity, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            takePicture()
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    fun checkCameraPermissionAndRecordVideo() {
        if (ContextCompat.checkSelfPermission(activity, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            recordVideo()
        } else {
            videoPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private fun takePicture() {
        val photoFile = createTempFile("photo", ".jpg")
        tempMediaUri = FileProvider.getUriForFile(activity, "${activity.packageName}.fileprovider", photoFile)
        takePictureLauncher.launch(tempMediaUri)
    }

    private fun recordVideo() {
        val videoFile = createTempFile("video", ".mp4")
        tempMediaUri = FileProvider.getUriForFile(activity, "${activity.packageName}.fileprovider", videoFile)
        recordVideoLauncher.launch(tempMediaUri)
    }

    private fun createTempFile(prefix: String, suffix: String): File {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val storageDir = activity.getExternalFilesDir(null)
        return File.createTempFile("${prefix}_${timeStamp}_", suffix, storageDir)
    }

    fun sendMedia(uri: Uri, replyToMessageId: Int? = null) {
        Log.d("MediaHandler", "Sending media: $uri")
        val file = uriToFile(uri)
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
            ?.itemView?.findViewById<ProgressBar>(R.id.uploadProgress)

        networkUtils.sendImage(
            imageFile = file,
            sender = currentUser,
            receiver = receiver,
            onProgress = { progress ->
                activity.runOnUiThread {
                    progressBar?.isVisible = true
                    progressBar?.progress = progress
                }
            },
            onSuccess = { message ->
                activity.runOnUiThread {
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
                sendMessageCallback(json.toString())
            },
            onFailure = { error ->
                activity.runOnUiThread {
                    Log.e("MediaHandler", error)
                    progressBar?.isVisible = false
                    messageAdapter.messages[position].content = "Yuklash xatosi"
                    messageAdapter.notifyItemChanged(position)
                    Toast.makeText(activity, "Yuklash xatosi", Toast.LENGTH_SHORT).show()
                }
            }
        )
    }

    private fun uriToFile(uri: Uri): File {
        val inputStream = activity.contentResolver.openInputStream(uri)
        val file = createTempFile("media", ".tmp")
        inputStream?.use { input ->
            FileOutputStream(file).use { output ->
                input.copyTo(output)
            }
        }
        return file
    }
}