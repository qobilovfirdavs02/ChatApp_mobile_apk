package com.example.chatapp3.utils

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import android.util.Log
import java.io.File
import java.io.IOException

class MediaRecorderUtil(private val context: Context) {

    private var mediaRecorder: MediaRecorder? = null
    private var outputFile: String? = null

    fun startRecording(): String? {
        // Fayl yo‘lini aniq belgilash
        val cacheDir = context.externalCacheDir ?: context.cacheDir // Agar externalCacheDir null bo‘lsa, ichki cache ishlatiladi
        if (!cacheDir.exists() && !cacheDir.mkdirs()) {
            Log.e("MediaRecorderUtil", "Cache katalogini yaratishda xato")
            return null
        }

        outputFile = "${cacheDir.absolutePath}/voice_message_${System.currentTimeMillis()}"
        val fileExtension = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) ".ogg" else ".m4a"
        outputFile += fileExtension

        mediaRecorder = MediaRecorder().apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) { // API 29+
                    setOutputFormat(MediaRecorder.OutputFormat.OGG)
                    setAudioEncoder(MediaRecorder.AudioEncoder.OPUS)
                } else { // API 28 va undan past
                    setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                    setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                }
                setOutputFile(outputFile)
                prepare()
                start()
                Log.d("MediaRecorderUtil", "Yozish boshlandi: $outputFile")
            } catch (e: IOException) {
                Log.e("MediaRecorderUtil", "Yozishni boshlashda xato (IOException): ${e.message}")
                release()
                return null
            } catch (e: IllegalStateException) {
                Log.e("MediaRecorderUtil", "Yozishni boshlashda xato (IllegalState): ${e.message}")
                release()
                return null
            } catch (e: SecurityException) {
                Log.e("MediaRecorderUtil", "Ruxsat xatosi: ${e.message}")
                release()
                return null
            }
        }
        return outputFile
    }

    fun stopRecording(): String? {
        mediaRecorder?.let { recorder ->
            try {
                recorder.stop()
                Log.d("MediaRecorderUtil", "Yozish to‘xtatildi: $outputFile")
            } catch (e: IllegalStateException) {
                Log.e("MediaRecorderUtil", "Yozishni to‘xtatishda xato: ${e.message}")
                return null
            } finally {
                recorder.release()
                mediaRecorder = null
            }
        } ?: run {
            Log.e("MediaRecorderUtil", "MediaRecorder ishga tushmagan")
            return null
        }
        return outputFile
    }

    fun isRecording(): Boolean {
        return mediaRecorder != null
    }

    fun release() {
        mediaRecorder?.release()
        mediaRecorder = null
        Log.d("MediaRecorderUtil", "Resurslar tozalandi")
    }

    fun deleteRecording(filePath: String?) {
        filePath?.let {
            val file = File(it)
            if (file.exists()) {
                if (file.delete()) {
                    Log.d("MediaRecorderUtil", "Fayl o‘chirildi: $filePath")
                } else {
                    Log.e("MediaRecorderUtil", "Faylni o‘chirishda xato: $filePath")
                }
            }
        }
    }
}