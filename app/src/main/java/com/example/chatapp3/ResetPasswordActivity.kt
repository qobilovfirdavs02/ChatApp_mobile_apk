package com.example.chatapp3

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import okhttp3.*
import org.json.JSONObject
import java.io.IOException
import okhttp3.MediaType.Companion.toMediaType

class ResetPasswordActivity : AppCompatActivity() {
    private val client = OkHttpClient()
    private var email: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_reset_password)

        val emailInput = findViewById<EditText>(R.id.emailInput)
        val resetButton = findViewById<Button>(R.id.resetButton)
        val resetCodeInput = findViewById<EditText>(R.id.resetCodeInput)
        val verifyButton = findViewById<Button>(R.id.verifyButton)

        resetButton.setOnClickListener {
            email = emailInput.text.toString()
            resetPassword(email)
        }

        verifyButton.setOnClickListener {
            val resetCode = resetCodeInput.text.toString()
            verifyResetCode(email, resetCode)
        }
    }

    private fun resetPassword(email: String) {
        val json = JSONObject().apply {
            put("email", email)
        }
        val body = RequestBody.create("application/json".toMediaType(), json.toString())
        val request = Request.Builder()
            .url("https://web-production-545c.up.railway.app/reset-password")
            .post(body)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread { Toast.makeText(this@ResetPasswordActivity, "Xato: ${e.message}", Toast.LENGTH_SHORT).show() }
            }

            override fun onResponse(call: Call, response: Response) {
                val responseBody = response.body?.string()
                if (response.isSuccessful) {
                    runOnUiThread { Toast.makeText(this@ResetPasswordActivity, "Kod emailingizga yuborildi", Toast.LENGTH_SHORT).show() }
                } else {
                    runOnUiThread { Toast.makeText(this@ResetPasswordActivity, responseBody, Toast.LENGTH_SHORT).show() }
                }
            }
        })
    }

    private fun verifyResetCode(email: String, resetCode: String) {
        val json = JSONObject().apply {
            put("email", email)
            put("reset_code", resetCode)
        }
        val body = RequestBody.create("application/json".toMediaType(), json.toString())
        val request = Request.Builder()
            .url("https://web-production-545c.up.railway.app/verify-reset-code")
            .post(body)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread { Toast.makeText(this@ResetPasswordActivity, "Xato: ${e.message}", Toast.LENGTH_SHORT).show() }
            }

            override fun onResponse(call: Call, response: Response) {
                val responseBody = response.body?.string()
                if (response.isSuccessful) {
                    runOnUiThread {
                        Toast.makeText(this@ResetPasswordActivity, "Kod to‘g‘ri", Toast.LENGTH_SHORT).show()
                        val intent = Intent(this@ResetPasswordActivity, SetNewPasswordActivity::class.java)
                        intent.putExtra("email", email)
                        startActivity(intent)
                    }
                } else {
                    runOnUiThread { Toast.makeText(this@ResetPasswordActivity, responseBody, Toast.LENGTH_SHORT).show() }
                }
            }
        })
    }
}