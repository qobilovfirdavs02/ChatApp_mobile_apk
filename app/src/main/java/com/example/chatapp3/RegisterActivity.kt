package com.example.chatapp3

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import org.json.JSONObject
import java.io.IOException

class RegisterActivity : AppCompatActivity() {
    private val client = OkHttpClient()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)
        Log.d("RegisterActivity", "RegisterActivity ishga tushdi")

        val usernameInput = findViewById<EditText>(R.id.usernameInput)
        val emailInput = findViewById<EditText>(R.id.emailInput)
        val passwordInput = findViewById<EditText>(R.id.passwordInput)
        val registerButton = findViewById<Button>(R.id.registerButton)

        registerButton.setOnClickListener {
            val username = usernameInput.text.toString().trim()
            val email = emailInput.text.toString().trim()
            val password = passwordInput.text.toString().trim()
            if (username.isNotEmpty() && email.isNotEmpty() && password.isNotEmpty()) {
                registerUser(username, email, password)
            } else {
                Toast.makeText(this, "Iltimos, barcha maydonlarni to‘ldiring", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun registerUser(username: String, email: String, password: String) {
        val json = JSONObject().apply {
            put("username", username)
            put("email", email)
            put("password", password)
        }
        val body = RequestBody.create("application/json".toMediaType(), json.toString())
        val request = Request.Builder()
            .url("https://web-production-545c.up.railway.app/register")
            .post(body)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    Log.e("RegisterActivity", "Registratsiya xatosi: ${e.message}")
                    Toast.makeText(this@RegisterActivity, "Server bilan aloqa yo‘q", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val responseBody = response.body?.string()
                runOnUiThread {
                    if (response.isSuccessful && responseBody != null) {
                        Log.d("RegisterActivity", "Registratsiya muvaffaqiyatli: $responseBody")
                        Toast.makeText(this@RegisterActivity, "Ro‘yxatdan o‘tdingiz!", Toast.LENGTH_SHORT).show()
                        startActivity(Intent(this@RegisterActivity, LoginActivity::class.java))
                        finish()
                    } else {
                        Log.e("RegisterActivity", "Registratsiya xatosi: $responseBody")
                        Toast.makeText(this@RegisterActivity, "Bu username yoki email band", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        })
    }
}