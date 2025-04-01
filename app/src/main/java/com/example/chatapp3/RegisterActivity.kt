package com.example.chatapp3

import android.content.Intent
import android.os.Bundle
import android.text.method.PasswordTransformationMethod
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import org.json.JSONObject
import java.io.IOException

class RegisterActivity : AppCompatActivity() {
    private val client by lazy { OkHttpClient() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)
        Log.d("RegisterActivity", "RegisterActivity ishga tushdi")

        val usernameInput = findViewById<EditText>(R.id.usernameInput)
        val emailInput = findViewById<EditText>(R.id.emailInput)
        val passwordInput = findViewById<EditText>(R.id.passwordInput)
        val verifyPasswordInput = findViewById<EditText>(R.id.verifyPasswordInput)
        val registerButton = findViewById<Button>(R.id.registerButton)

        // Parolni ko‘rish uchun ko‘z belgisini sozlash
        passwordInput.setCompoundDrawablesRelativeWithIntrinsicBounds(
            R.drawable.ic_lock, 0, R.drawable.ic_eye, 0
        )
        verifyPasswordInput.setCompoundDrawablesRelativeWithIntrinsicBounds(
            R.drawable.ic_lock, 0, R.drawable.ic_eye, 0
        )

        var isPasswordVisible = false
        passwordInput.setOnTouchListener { _, event ->
            if (event.action == android.view.MotionEvent.ACTION_UP) {
                if (event.rawX >= (passwordInput.right - passwordInput.compoundDrawables[2].bounds.width())) {
                    isPasswordVisible = !isPasswordVisible
                    passwordInput.transformationMethod = if (isPasswordVisible) null else PasswordTransformationMethod.getInstance()
                    return@setOnTouchListener true
                }
            }
            false
        }

        verifyPasswordInput.setOnTouchListener { _, event ->
            if (event.action == android.view.MotionEvent.ACTION_UP) {
                if (event.rawX >= (verifyPasswordInput.right - verifyPasswordInput.compoundDrawables[2].bounds.width())) {
                    isPasswordVisible = !isPasswordVisible
                    verifyPasswordInput.transformationMethod = if (isPasswordVisible) null else PasswordTransformationMethod.getInstance()
                    return@setOnTouchListener true
                }
            }
            false
        }

        registerButton.setOnClickListener {
            val username = usernameInput.text.toString().trim()
            val email = emailInput.text.toString().trim()
            val password = passwordInput.text.toString().trim()
            val verifyPassword = verifyPasswordInput.text.toString().trim()

            if (username.isEmpty() || email.isEmpty() || password.isEmpty() || verifyPassword.isEmpty()) {
                Toast.makeText(this, "Iltimos, barcha maydonlarni to‘ldiring", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (password != verifyPassword) {
                Toast.makeText(this, "Parollar mos kelmadi", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            registerUser(username, email, password)
        }
    }

    private fun registerUser(username: String, email: String, password: String) {
        val json = JSONObject().apply {
            put("username", username)
            put("email", email)
            put("password", password)
        }
        val body = RequestBody.create("application/json".toMediaTypeOrNull(), json.toString())
        val request = Request.Builder()
            .url("https://web-production-545c.up.railway.app/register")
//            .url("https://web-production-0e0f.up.railway.app/register")
            .post(body)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    Log.e("RegisterActivity", "Register xatosi: ${e.message}")
                    Toast.makeText(this@RegisterActivity, "Server bilan aloqa yo‘q", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val responseBody = response.body?.string()
                runOnUiThread {
                    if (response.isSuccessful && responseBody != null) {
                        Log.d("RegisterActivity", "Register muvaffaqiyatli: $responseBody")
                        Toast.makeText(this@RegisterActivity, "Ro‘yxatdan o‘tildi", Toast.LENGTH_SHORT).show()
                        startActivity(Intent(this@RegisterActivity, LoginActivity::class.java))
                        finish()
                    } else {
                        Log.e("RegisterActivity", "Register xatosi: $responseBody")
                        Toast.makeText(this@RegisterActivity, "Xatolik yuz berdi", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        })
    }
}