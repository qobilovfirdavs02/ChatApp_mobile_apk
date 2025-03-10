package com.example.chatapp3

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import org.json.JSONObject
import java.io.IOException


class SetNewPasswordActivity : AppCompatActivity() {
    private val client = OkHttpClient()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_set_new_password)

        val email = intent.getStringExtra("email") ?: ""
        val newPasswordInput = findViewById<EditText>(R.id.newPasswordInput)
        val setPasswordButton = findViewById<Button>(R.id.setPasswordButton)

        setPasswordButton.setOnClickListener {
            val newPassword = newPasswordInput.text.toString()
            setNewPassword(email, newPassword)
        }
    }

    private fun setNewPassword(email: String, newPassword: String) {
        val json = JSONObject().apply {
            put("email", email)
            put("new_password", newPassword)
        }
        val body = RequestBody.create("application/json".toMediaType(), json.toString())
        val request = Request.Builder()
            .url("https://web-production-545c.up.railway.app/set-new-password")
            .post(body)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread { Toast.makeText(this@SetNewPasswordActivity, "Xato: ${e.message}", Toast.LENGTH_SHORT).show() }
            }

            override fun onResponse(call: Call, response: Response) {
                val responseBody = response.body?.string()
                if (response.isSuccessful) {
                    runOnUiThread {
                        Toast.makeText(this@SetNewPasswordActivity, "Yangi parol o‘rnatildi", Toast.LENGTH_SHORT).show()
                        finish() // Login sahifasiga qaytish
                    }
                } else {
                    runOnUiThread { Toast.makeText(this@SetNewPasswordActivity, responseBody, Toast.LENGTH_SHORT).show() }
                }
            }
        })
    }
}