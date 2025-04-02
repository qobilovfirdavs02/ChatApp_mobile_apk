package com.example.chatapp3.network

import android.os.Handler
import android.os.Looper
import android.util.Log
import com.example.chatapp3.Message
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okio.BufferedSink
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit

class NetworkUtils(private val client: OkHttpClient) {

    // Client’ga timeout qo‘shish (agar ChatActivity’da sozlanmagan bo‘lsa)
    init {
        val enhancedClient = client.newBuilder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    fun sendImage(
        imageFile: File,
        sender: String,
        receiver: String,
        onProgress: (Int) -> Unit,
        onSuccess: (Message) -> Unit,
        onFailure: (String) -> Unit
    ) {
        val mainHandler = Handler(Looper.getMainLooper())

        val requestBody = object : RequestBody() {
            override fun contentType(): MediaType? = "multipart/form-data".toMediaTypeOrNull()

            override fun contentLength(): Long = imageFile.length() // Fayl hajmini aniq qaytarish

            override fun writeTo(sink: BufferedSink) {
                val byteArray = imageFile.readBytes()
                val total = byteArray.size.toLong()
                var uploaded = 0L
                val chunkSize = 8192L

                byteArray.inputStream().use { input ->
                    while (uploaded < total) {
                        val toWrite = minOf(chunkSize, total - uploaded).toInt()
                        sink.write(byteArray, uploaded.toInt(), toWrite)
                        uploaded += toWrite
                        val progress = (uploaded * 100 / total).toInt()
                        mainHandler.post { onProgress(progress) }
                    }
                }
            }
        }

        val multipartBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("file", imageFile.name, requestBody)
            .addFormDataPart("sender", sender)
            .addFormDataPart("receiver", receiver)
            .build()

        val request = Request.Builder()
            .url("https://web-production-545c.up.railway.app/upload")
            .post(multipartBody)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("NetworkUtils", "Image upload failed: ${e.message}")
                mainHandler.post { onFailure("Image upload failed: ${e.message}") }
            }

            override fun onResponse(call: Call, response: Response) {
                val responseBody = response.body?.string()
                Log.d("NetworkUtils", "Image upload response: code=${response.code}, body=$responseBody")
                if (response.isSuccessful && responseBody != null) {
                    try {
                        val json = JSONObject(responseBody)
                        val fileUrl = json.optString("file_url", null)
                        if (fileUrl == null) {
                            Log.e("NetworkUtils", "file_url topilmadi")
                            mainHandler.post { onFailure("No file_url in response") }
                            return
                        }
                        val message = Message(
                            id = (0..Int.MAX_VALUE).random(),
                            sender = sender,
                            content = fileUrl,
                            timestamp = "now",
                            edited = false,
                            reaction = null,
                            replyToId = null,
                            type = "image" // Rasm uchun type qo‘shildi
                        )
                        mainHandler.post { onSuccess(message) }
                    } catch (e: Exception) {
                        Log.e("NetworkUtils", "JSON parsing error: ${e.message}")
                        mainHandler.post { onFailure("Response parsing error: ${e.message}") }
                    }
                } else {
                    Log.e("NetworkUtils", "Upload failed: code=${response.code}, message=${response.message}, body=$responseBody")
                    mainHandler.post { onFailure("Upload failed: ${response.code}, ${response.message}") }
                }
            }
        })
    }

    fun sendVoice(
        voiceFile: File,
        sender: String,
        receiver: String,
        onSuccess: (Message) -> Unit,
        onFailure: (String) -> Unit
    ) {
        val mainHandler = Handler(Looper.getMainLooper())

        val requestBody = RequestBody.create("audio/ogg".toMediaTypeOrNull(), voiceFile)

        val multipartBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("file", voiceFile.name, requestBody)
            .addFormDataPart("sender", sender)
            .addFormDataPart("receiver", receiver)
            .build()

        val request = Request.Builder()
            .url("https://web-production-545c.up.railway.app/upload")
            .post(multipartBody)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("NetworkUtils", "Voice upload failed: ${e.message}")
                mainHandler.post { onFailure("Voice upload failed: ${e.message}") }
            }

            override fun onResponse(call: Call, response: Response) {
                val responseBody = response.body?.string()
                Log.d("NetworkUtils", "Voice upload response: code=${response.code}, body=$responseBody")
                if (response.isSuccessful && responseBody != null) {
                    try {
                        val json = JSONObject(responseBody)
                        val fileUrl = json.optString("file_url", null)
                        if (fileUrl == null) {
                            Log.e("NetworkUtils", "file_url topilmadi")
                            mainHandler.post { onFailure("No file_url in response") }
                            return
                        }
                        val message = Message(
                            id = (0..Int.MAX_VALUE).random(),
                            sender = sender,
                            content = fileUrl,
                            timestamp = "now",
                            edited = false,
                            reaction = null,
                            replyToId = null,
                            type = "voice" // Ovoz uchun type qo‘shildi
                        )
                        mainHandler.post { onSuccess(message) }
                    } catch (e: Exception) {
                        Log.e("NetworkUtils", "JSON parsing error: ${e.message}")
                        mainHandler.post { onFailure("Response parsing error: ${e.message}") }
                    }
                } else {
                    Log.e("NetworkUtils", "Upload failed: code=${response.code}, message=${response.message}, body=$responseBody")
                    mainHandler.post { onFailure("Upload failed: ${response.code}, ${response.message}") }
                }
            }
        })
    }
}