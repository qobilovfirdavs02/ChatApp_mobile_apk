package com.example.chatapp3.ui

import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.chatapp3.network.MediaHandler

class UIUtils(private val activity: AppCompatActivity, private val mediaHandler: MediaHandler) {

    fun showMediaOptionsDialog() {
        val options = arrayOf("Galereyadan tanlash", "Suratga olish", "Video yozish")
        AlertDialog.Builder(activity)
            .setTitle("Media yuborish")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> mediaHandler.pickMedia()
                    1 -> mediaHandler.checkCameraPermissionAndTakePicture()
                    2 -> mediaHandler.checkCameraPermissionAndRecordVideo()
                }
            }
            .setNegativeButton("Bekor qilish", null)
            .show()
    }
}