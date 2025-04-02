package com.example.chatapp3

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.text.method.PasswordTransformationMethod
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import org.json.JSONObject
import java.io.IOException

class LoginActivity : AppCompatActivity() {
    private val client by lazy { OkHttpClient() }

    private val requestPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (!isGranted) {
            Toast.makeText(this, "Bildirishnomalarni ko‘rish uchun ruxsat kerak", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)
        Log.d("LoginActivity", "LoginActivity ishga tushdi")

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

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

        // Parolni ko‘rish uchun ko‘z belgisini sozlash
        passwordInput.setCompoundDrawablesRelativeWithIntrinsicBounds(
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
        val body = RequestBody.create("application/json".toMediaTypeOrNull(), json.toString())
        val request = Request.Builder()
            .url("https://web-production-545c.up.railway.app/login")
//            .url("https://web-production-0e0f.up.railway.app/login")
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
                        val json = JSONObject(responseBody)
                        val prefs = getSharedPreferences("ChatApp", MODE_PRIVATE)
                        prefs.edit().putString("username", json.getString("username")).apply()
                        startActivity(Intent(this@LoginActivity, MainActivity::class.java))
                        finish()
                    } else {
                        Log.e("LoginActivity", "Login xatosi: $responseBody")
                        Toast.makeText(this@LoginActivity, "Login yoki parol xato", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        })
    }
}