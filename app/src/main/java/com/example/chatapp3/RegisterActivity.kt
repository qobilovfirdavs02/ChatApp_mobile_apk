package com.example.chatapp3

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import okhttp3.*
import org.json.JSONObject
import java.io.IOException
import okhttp3.MediaType.Companion.toMediaType

class RegisterActivity : AppCompatActivity() {
    private val client = OkHttpClient()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        val usernameInput = findViewById<EditText>(R.id.usernameInput)
        val emailInput = findViewById<EditText>(R.id.emailInput)
        val passwordInput = findViewById<EditText>(R.id.passwordInput)
        val registerButton = findViewById<Button>(R.id.registerButton)

        registerButton.setOnClickListener {
            val username = usernameInput.text.toString()
            val email = emailInput.text.toString()
            val password = passwordInput.text.toString()
            register(username, email, password)
        }
    }

    private fun register(username: String, email: String, password: String) {
        val json = JSONObject().apply {
            put("username", username)
            put("email", email)
            put("password", password)
        }
        val body = RequestBody.create("application/json".toMediaType(), json.toString())
        val request = Request.Builder()
            .url("http://192.168.99.253:8000/register")
            .post(body)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread { Toast.makeText(this@RegisterActivity, "Xato: ${e.message}", Toast.LENGTH_SHORT).show() }
            }

            override fun onResponse(call: Call, response: Response) {
                val responseBody = response.body?.string()
                if (response.isSuccessful) {
                    runOnUiThread {
                        Toast.makeText(this@RegisterActivity, "Ro‘yxatdan o‘tish muvaffaqiyatli", Toast.LENGTH_SHORT).show()
                        finish() // Login sahifasiga qaytish
                    }
                } else {
                    runOnUiThread { Toast.makeText(this@RegisterActivity, responseBody, Toast.LENGTH_SHORT).show() }
                }
            }
        })
    }
}