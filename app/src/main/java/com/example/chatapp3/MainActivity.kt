package com.example.chatapp3

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import okhttp3.*
import org.json.JSONArray
import java.io.IOException

class MainActivity : AppCompatActivity() {
    private val client = OkHttpClient()
    private lateinit var userAdapter: UserAdapter
    private lateinit var currentUser: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        Log.d("MainActivity", "MainActivity ishga tushdi")

        // SharedPreferences’dan username ni olish
        val prefs = getSharedPreferences("ChatApp", MODE_PRIVATE)
        currentUser = prefs.getString("username", null) ?: run {
            Log.d("MainActivity", "Username topilmadi, LoginActivity ga o‘tish")
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        // UI elementlarni bog‘lash
        val searchInput = findViewById<EditText>(R.id.searchInput)
        val recyclerView = findViewById<RecyclerView>(R.id.userRecyclerView)
        val logoutButton = findViewById<Button>(R.id.logoutButton)

        // RecyclerView sozlash
        userAdapter = UserAdapter(currentUser)
        recyclerView.adapter = userAdapter
        recyclerView.layoutManager = LinearLayoutManager(this)
        Log.d("MainActivity", "RecyclerView sozlandi")

        // Qidiruv funksiyasi
        searchInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                fetchUsers(s.toString())
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        // Boshlang‘ich ro‘yxat
        fetchUsers("")

        // Tizimdan chiqish tugmasi logikasi
        logoutButton.setOnClickListener {
            Log.d("MainActivity", "Logout tugmasi bosildi")
            showLogoutConfirmationDialog()
        }
    }

    private fun fetchUsers(query: String) {
        val request = Request.Builder()
            .url("http://192.168.99.253:8000/users?query=$query")
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

    // Birinchi tasdiqlash dialogi
    private fun showLogoutConfirmationDialog() {
        AlertDialog.Builder(this)
            .setTitle("Tizimdan chiqish")
            .setMessage("Tizimdan chiqishni xohlaysizmi?")
            .setPositiveButton("Ha") { _, _ ->
                Log.d("MainActivity", "Birinchi dialogda Ha bosildi")
                showFinalConfirmationDialog() // Ikkinchi dialog
            }
            .setNegativeButton("Yo‘q") { dialog, _ ->
                Log.d("MainActivity", "Birinchi dialogda Yo‘q bosildi")
                dialog.dismiss()
            }
            .setCancelable(false)
            .show()
    }

    // Ikkinchi tasdiqlash dialogi
    private fun showFinalConfirmationDialog() {
        AlertDialog.Builder(this)
            .setTitle("Tasdiqlash")
            .setMessage("Aniqmisiz?")
            .setPositiveButton("Ha") { _, _ ->
                Log.d("MainActivity", "Ikkinchi dialogda Ha bosildi")
                logout() // Tizimdan chiqish
            }
            .setNegativeButton("Yo‘q") { dialog, _ ->
                Log.d("MainActivity", "Ikkinchi dialogda Yo‘q bosildi")
                dialog.dismiss()
            }
            .setCancelable(false)
            .show()
    }

    // Tizimdan chiqish funksiyasi
    private fun logout() {
        val prefs = getSharedPreferences("ChatApp", MODE_PRIVATE)
        prefs.edit().remove("username").apply() // Username ni o‘chirish
        startActivity(Intent(this, LoginActivity::class.java))
        finish()
        Toast.makeText(this, "Tizimdan chiqildi", Toast.LENGTH_SHORT).show()
        Log.d("MainActivity", "Tizimdan chiqildi")
    }
}