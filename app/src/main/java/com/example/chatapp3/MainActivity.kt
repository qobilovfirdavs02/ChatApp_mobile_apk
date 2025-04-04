package com.example.chatapp3

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.chatapp3.chat.ChatActivity
import okhttp3.*
import org.json.JSONArray
import java.io.IOException

class MainActivity : AppCompatActivity() {
    private lateinit var userAdapter: UserAdapter
    private lateinit var currentUser: String
    private val client by lazy { OkHttpClient() }

    override fun onCreate(savedInstanceState: Bundle?) {
        // Rejimni yuklash
        val prefs = getSharedPreferences("ChatApp", MODE_PRIVATE)
        val isDarkMode = prefs.getBoolean("isDarkMode", false)
        setTheme(if (isDarkMode) R.style.Theme_ChatApp_Dark else R.style.Theme_ChatApp_Light)

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        Log.d("MainActivity", "MainActivity ishga tushdi")

        currentUser = prefs.getString("username", null) ?: run {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        val searchInput = findViewById<EditText>(R.id.searchInput)
        val recyclerView = findViewById<RecyclerView>(R.id.userRecyclerView)
        val logoutButton = findViewById<Button>(R.id.logoutButton)
        val themeToggleButton = Button(this).apply {
            text = if (isDarkMode) "Kunduzgi rejim" else "Tungi rejim"
            setOnClickListener {
                val newMode = !isDarkMode
                prefs.edit().putBoolean("isDarkMode", newMode).apply()
                recreate() // Activity’ni qayta yuklash
            }
        }
        findViewById<LinearLayout>(R.id.headerLayout)?.addView(themeToggleButton)

        userAdapter = UserAdapter(currentUser) { selectedUser ->
            val intent = Intent(this, ChatActivity::class.java)
            intent.putExtra("username", currentUser)
            intent.putExtra("receiver", selectedUser.username)
            startActivity(intent)
        }
        recyclerView.adapter = userAdapter
        recyclerView.layoutManager = LinearLayoutManager(this)

        searchInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                fetchUsers(s.toString())
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        fetchUsers("")

        logoutButton.setOnClickListener {
            it.animate()
                .scaleX(0.9f)
                .scaleY(0.9f)
                .setDuration(100)
                .withEndAction {
                    it.animate().scaleX(1f).scaleY(1f).setDuration(100).start()
                    showLogoutConfirmationDialog()
                }
                .start()
        }
    }

    private fun fetchUsers(query: String) {
        val request = Request.Builder()
            .url("https://web-production-545c.up.railway.app/users?query=$query")
//            .url("https://web-production-0e0f.up.railway.app/users?query=$query")
            .get()
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    Log.e("MainActivity", "Server xatosi: ${e.message}")
                    Toast.makeText(this@MainActivity, "Server bilan aloqa yo‘q: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                try {
                    val responseBody = response.body?.string() ?: "[]"
                    Log.d("MainActivity", "Serverdan javob: $responseBody")
                    val jsonArray = JSONArray(responseBody)
                    val users = (0 until jsonArray.length()).map {
                        User(jsonArray.getJSONObject(it).getString("username"))
                    }.filter { it.username != currentUser }
                    runOnUiThread {
                        userAdapter.updateUsers(users)
                        Log.d("MainActivity", "Foydalanuvchilar yangilandi: ${users.size}")
                    }
                } catch (e: Exception) {
                    runOnUiThread {
                        Log.e("MainActivity", "JSON xatosi: ${e.message}")
                        Toast.makeText(this@MainActivity, "Ma’lumotlarni olishda xato: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        })
    }

    private fun showLogoutConfirmationDialog() {
        AlertDialog.Builder(this)
            .setTitle("Tizimdan chiqish")
            .setMessage("Tizimdan chiqishni xohlaysizmi?")
            .setPositiveButton("Ha") { _, _ ->
                Log.d("MainActivity", "Birinchi dialogda Ha bosildi")
                showFinalConfirmationDialog()
            }
            .setNegativeButton("Yo‘q") { dialog, _ ->
                Log.d("MainActivity", "Birinchi dialogda Yo‘q bosildi")
                dialog.dismiss()
            }
            .setCancelable(false)
            .show()
    }

    private fun showFinalConfirmationDialog() {
        AlertDialog.Builder(this)
            .setTitle("Tasdiqlash")
            .setMessage("Aminmisiz?")
            .setPositiveButton("Ha") { _, _ ->
                Log.d("MainActivity", "Ikkinchi dialogda Ha bosildi")
                logout()
            }
            .setNegativeButton("Yo‘q") { dialog, _ ->
                Log.d("MainActivity", "Ikkinchi dialogda Yo‘q bosildi")
                dialog.dismiss()
            }
            .setCancelable(false)
            .show()
    }

    private fun logout() {
        val prefs = getSharedPreferences("ChatApp", MODE_PRIVATE)
        prefs.edit().remove("username").apply()
        startActivity(Intent(this, LoginActivity::class.java))
        finish()
        Toast.makeText(this, "Tizimdan chiqildi", Toast.LENGTH_SHORT).show()
        Log.d("MainActivity", "Tizimdan chiqildi")
    }
}