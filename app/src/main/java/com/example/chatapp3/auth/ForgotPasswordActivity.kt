package com.example.chatapp3

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import org.json.JSONObject
import java.io.IOException

class ForgotPasswordActivity : AppCompatActivity() {
    private val client by lazy { OkHttpClient() }
    private var email: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_forgot_password)
        Log.d("ForgotPasswordActivity", "ForgotPasswordActivity ishga tushdi")

        val emailInput = findViewById<EditText>(R.id.emailInput)
        val sendCodeButton = findViewById<Button>(R.id.sendCodeButton)
        val codeInput = findViewById<EditText>(R.id.codeInput)
        val verifyCodeButton = findViewById<Button>(R.id.verifyCodeButton)
        val newPasswordInput = findViewById<EditText>(R.id.newPasswordInput)
        val resetPasswordButton = findViewById<Button>(R.id.resetPasswordButton)

        sendCodeButton.setOnClickListener {
            email = emailInput.text.toString().trim()
            if (email!!.isNotEmpty()) {
                sendCodeButton.isEnabled = false
                sendResetCode(email!!)
            } else {
                Toast.makeText(this, "Iltimos, emailingizni kiriting", Toast.LENGTH_SHORT).show()
            }
        }

        verifyCodeButton.setOnClickListener {
            val code = codeInput.text.toString().trim()
            if (code.isNotEmpty() && email != null) {
                verifyCodeButton.isEnabled = false
                verifyResetCode(email!!, code)
            } else {
                Toast.makeText(this, "Iltimos, kodni kiriting", Toast.LENGTH_SHORT).show()
            }
        }

        resetPasswordButton.setOnClickListener {
            val newPassword = newPasswordInput.text.toString().trim()
            if (newPassword.isNotEmpty() && email != null) {
                resetPasswordButton.isEnabled = false
                setNewPassword(email!!, newPassword)
            } else {
                Toast.makeText(this, "Iltimos, yangi parol kiriting", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun sendResetCode(email: String) {
        val json = JSONObject().apply {
            put("email", email)
        }
        val body = RequestBody.create("application/json".toMediaType(), json.toString())
        val request = Request.Builder()
            .url("https://web-production-545c.up.railway.app/reset-password")
//            .url("https://web-production-0e0f.up.railway.app/reset-password")
            .post(body)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    Log.e("ForgotPasswordActivity", "Kod yuborish xatosi: ${e.message}")
                    Toast.makeText(this@ForgotPasswordActivity, "Server bilan aloqa yo‘q", Toast.LENGTH_SHORT).show()
                    findViewById<Button>(R.id.sendCodeButton).isEnabled = true
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val responseBody = response.body?.string()
                runOnUiThread {
                    if (response.isSuccessful && responseBody != null) {
                        Log.d("ForgotPasswordActivity", "Kod yuborildi: $responseBody")
                        Toast.makeText(this@ForgotPasswordActivity, "Emailingizga kod yuborildi", Toast.LENGTH_SHORT).show()
                    } else {
                        Log.e("ForgotPasswordActivity", "Kod yuborish xatosi: $responseBody")
                        Toast.makeText(this@ForgotPasswordActivity, "Email topilmadi", Toast.LENGTH_SHORT).show()
                    }
                    findViewById<Button>(R.id.sendCodeButton).isEnabled = true
                }
            }
        })
    }

    private fun verifyResetCode(email: String, code: String) {
        val json = JSONObject().apply {
            put("email", email)
            put("reset_code", code)
        }
        val body = RequestBody.create("application/json".toMediaType(), json.toString())
        val request = Request.Builder()
            .url("https://web-production-545c.up.railway.app/verify-reset-code")
//            .url("https://web-production-0e0f.up.railway.app/verify-reset-code")
            .post(body)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    Log.e("ForgotPasswordActivity", "Kod tekshirish xatosi: ${e.message}")
                    Toast.makeText(this@ForgotPasswordActivity, "Server bilan aloqa yo‘q", Toast.LENGTH_SHORT).show()
                    findViewById<Button>(R.id.verifyCodeButton).isEnabled = true
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val responseBody = response.body?.string()
                runOnUiThread {
                    if (response.isSuccessful && responseBody != null) {
                        Log.d("ForgotPasswordActivity", "Kod tasdiqlandi: $responseBody")
                        Toast.makeText(this@ForgotPasswordActivity, "Kod tasdiqlandi, yangi parol kiriting", Toast.LENGTH_SHORT).show()
                        findViewById<EditText>(R.id.newPasswordInput).visibility = View.VISIBLE
                        findViewById<Button>(R.id.resetPasswordButton).visibility = View.VISIBLE
                    } else {
                        Log.e("ForgotPasswordActivity", "Kod tekshirish xatosi: $responseBody")
                        Toast.makeText(this@ForgotPasswordActivity, "Kod noto‘g‘ri", Toast.LENGTH_SHORT).show()
                    }
                    findViewById<Button>(R.id.verifyCodeButton).isEnabled = true
                }
            }
        })
    }

    private fun setNewPassword(email: String, newPassword: String) {
        val json = JSONObject().apply {
            put("email", email)
            put("new_password", newPassword)
        }
        val body = RequestBody.create("application/json".toMediaType(), json.toString())
        val request = Request.Builder()
            .url("https://web-production-545c.up.railway.app/set-new-password")
//            .url("https://web-production-0e0f.up.railway.app/set-new-password")
            .post(body)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    Log.e("ForgotPasswordActivity", "Parol o‘rnatish xatosi: ${e.message}")
                    Toast.makeText(this@ForgotPasswordActivity, "Server bilan aloqa yo‘q", Toast.LENGTH_SHORT).show()
                    findViewById<Button>(R.id.resetPasswordButton).isEnabled = true
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val responseBody = response.body?.string()
                runOnUiThread {
                    if (response.isSuccessful && responseBody != null) {
                        Log.d("ForgotPasswordActivity", "Parol o‘rnatildi: $responseBody")
                        Toast.makeText(this@ForgotPasswordActivity, "Parol muvaffaqiyatli almashtirildi", Toast.LENGTH_SHORT).show()
                        startActivity(Intent(this@ForgotPasswordActivity, LoginActivity::class.java))
                        finish()
                    } else {
                        Log.e("ForgotPasswordActivity", "Parol o‘rnatish xatosi: $responseBody")
                        Toast.makeText(this@ForgotPasswordActivity, "Xato yuz berdi", Toast.LENGTH_SHORT).show()
                        findViewById<Button>(R.id.resetPasswordButton).isEnabled = true
                    }
                }
            }
        })
    }
}