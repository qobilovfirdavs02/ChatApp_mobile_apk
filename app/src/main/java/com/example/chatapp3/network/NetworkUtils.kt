package com.example.chatapp3.network

import android.util.Log
import android.os.Handler
import android.os.Looper
import com.example.chatapp3.Message
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okio.BufferedSink
import org.json.JSONObject
import java.io.File
import java.io.IOException

class NetworkUtils(private val client: OkHttpClient) {

    fun sendImage(
        imageFile: File,
        sender: String,
        receiver: String,
        onProgress: (Int) -> Unit, // Yuklanish progressi callback
        onSuccess: (Message) -> Unit, // Muvaffaqiyatli yuklanganda
        onFailure: (String) -> Unit   // Xato bo‘lganda
    ) {
        val mainHandler = Handler(Looper.getMainLooper()) // UI thread uchun handler

        val requestBody = object : RequestBody() {
            override fun contentType(): MediaType? = "multipart/form-data".toMediaTypeOrNull()

            override fun writeTo(sink: BufferedSink) {
                val byteArray = imageFile.readBytes()
                var uploaded = 0L
                val total = byteArray.size.toLong()
                val chunkSize = 8192L // 8KB, Long turiga o'tkazildi

                byteArray.inputStream().use { input ->
                    while (uploaded < total) {
                        val toWrite = minOf(chunkSize, total - uploaded).toInt()
                        sink.write(byteArray, uploaded.toInt(), toWrite)
                        uploaded += toWrite.toLong() // Long bo‘lishi kerak

                        val progress = (uploaded * 100 / total).toInt()
                        mainHandler.post { onProgress(progress) } // UI threadda progressni yangilash
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
                mainHandler.post { onFailure("Image upload failed: ${e.message}") } // UI threadda xato qaytarish
            }

            override fun onResponse(call: Call, response: Response) {
                val responseBody = response.body?.string()
                if (response.isSuccessful && responseBody != null) {
                    try {
                        val json = JSONObject(responseBody)
                        val fileUrl = json.getString("file_url")
                        val message = Message(
                            id = (0..Int.MAX_VALUE).random(),
                            sender = sender,
                            content = fileUrl,
                            timestamp = "now",
                            edited = false,
                            reaction = null,
                            replyToId = null
                        )
                        mainHandler.post { onSuccess(message) } // UI threadda success chaqirish
                    } catch (e: Exception) {
                        Log.e("NetworkUtils", "JSON parsing error: ${e.message}")
                        mainHandler.post { onFailure("Response parsing error") }
                    }
                } else {
                    Log.e("NetworkUtils", "Upload failed: ${response.code}, response: $responseBody")
                    mainHandler.post { onFailure("Upload failed: ${response.code}, response: $responseBody") }
                }
            }
        })
    }
}
