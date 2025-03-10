package com.example.chatapp3

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import org.json.JSONObject
import java.io.IOException

class LoginActivity : AppCompatActivity() {
    private val client = OkHttpClient()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        val usernameInput = findViewById<EditText>(R.id.usernameInput)
        val passwordInput = findViewById<EditText>(R.id.passwordInput)
        val loginButton = findViewById<Button>(R.id.loginButton)

        loginButton.setOnClickListener {
            val username = usernameInput.text.toString()
            val password = passwordInput.text.toString()
            login(username, password)
        }
    }

    private fun login(username: String, password: String) {
        val json = JSONObject().apply {
            put("username", username)
            put("password", password)
        }
        val body = RequestBody.create("application/json".toMediaType(), json.toString())
        val request = Request.Builder()
            .url("http://192.168.99.253:8000/login")
            .post(body)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread { Toast.makeText(this@LoginActivity, "Xato: ${e.message}", Toast.LENGTH_SHORT).show() }
            }

            override fun onResponse(call: Call, response: Response) {
                val responseBody = response.body?.string()
                if (response.isSuccessful) {
                    runOnUiThread {
                        val prefs = getSharedPreferences("ChatApp", MODE_PRIVATE)
                        prefs.edit().putString("username", username).apply()
                        startActivity(Intent(this@LoginActivity, MainActivity::class.java))
                        finish()
                    }
                } else {
                    runOnUiThread { Toast.makeText(this@LoginActivity, responseBody, Toast.LENGTH_SHORT).show() }
                }
            }
        })
    }
}