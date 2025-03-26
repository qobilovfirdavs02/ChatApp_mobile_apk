package com.example.chatapp3

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.widget.ImageView
import com.bumptech.glide.Glide
import com.github.chrisbanes.photoview.PhotoView

class ImagePreviewActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_image_preview)

        val imageUrl = intent.getStringExtra("image_url")
        val imageView = findViewById<ImageView>(R.id.fullImageView)

        Glide.with(this)
            .load(imageUrl)
            .into(imageView)

        imageView.setOnClickListener { finish() } // Orqaga qaytish uchun
    }
}