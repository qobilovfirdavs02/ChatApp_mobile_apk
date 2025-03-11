package com.example.chatapp3

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
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
        Log.d("LoginActivity", "LoginActivity ishga tushdi")

        val usernameInput = findViewById<EditText>(R.id.usernameInput)
        val passwordInput = findViewById<EditText>(R.id.passwordInput)
        val loginButton = findViewById<Button>(R.id.loginButton)
        val registerText = findViewById<TextView>(R.id.registerText)
        val forgotPasswordText = findViewById<TextView>(R.id.forgotPasswordText)

        val prefs = getSharedPreferences("ChatApp", MODE_PRIVATE)
        if (prefs.contains("username")) {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
            return
        }

        loginButton.setOnClickListener {
            val username = usernameInput.text.toString().trim()
            val password = passwordInput.text.toString().trim()
            if (username.isNotEmpty() && password.isNotEmpty()) {
                loginUser(username, password)
            } else {
                Toast.makeText(this, "Iltimos, barcha maydonlarni to‘ldiring", Toast.LENGTH_SHORT).show()
            }
        }

        registerText.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }

        forgotPasswordText.setOnClickListener {
            startActivity(Intent(this, ForgotPasswordActivity::class.java))
        }
    }

    private fun loginUser(username: String, password: String) {
        val json = JSONObject().apply {
            put("username", username)
            put("password", password)
        }
        val body = RequestBody.create("application/json".toMediaType(), json.toString())
        val request = Request.Builder()
            .url("https://web-production-545c.up.railway.app/login")
            .post(body)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    Log.e("LoginActivity", "Login xatosi: ${e.message}")
                    Toast.makeText(this@LoginActivity, "Server bilan aloqa yo‘q", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val responseBody = response.body?.string()
                runOnUiThread {
                    if (response.isSuccessful && responseBody != null) {
                        Log.d("LoginActivity", "Login muvaffaqiyatli: $responseBody")
                        val prefs = getSharedPreferences("ChatApp", MODE_PRIVATE)
                        prefs.edit().putString("username", username).apply()
                        startActivity(Intent(this@LoginActivity, MainActivity::class.java))
                        finish()
                        Toast.makeText(this@LoginActivity, "Xush kelibsiz, $username!", Toast.LENGTH_SHORT).show()
                    } else {
                        Log.e("LoginActivity", "Login xatosi: $responseBody")
                        Toast.makeText(this@LoginActivity, "Login yoki parol xato", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        })
    }
}