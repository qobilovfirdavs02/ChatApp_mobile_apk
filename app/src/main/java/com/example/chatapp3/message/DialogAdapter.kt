package com.example.chatapp3

import android.content.Context
import android.content.ClipboardManager
import android.content.ClipData
import android.view.LayoutInflater
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import org.json.JSONObject

class DialogAdapter(private val context: Context, private val currentUser: String) {

    fun showOtherUserOptionsDialog(message: Message, position: Int, onAction: (JSONObject) -> Unit, onDeleteForMe: () -> Unit) {
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_reaction_options, null)
        val dialog = AlertDialog.Builder(context).setView(dialogView).setNegativeButton("Cancel", null).create()
        val reactionLayout = dialogView.findViewById<LinearLayout>(R.id.reaction_layout)
        val reactions = arrayOf("👍", "❤️", "😂", "😮", "😢", "😡")

        reactions.forEach { reaction ->
            reactionLayout.addView(TextView(context).apply {
                text = reaction
                textSize = 20f
                setPadding(8, 4, 8, 4)
                setOnClickListener {
                    val json = JSONObject().apply {
                        put("msg_id", message.id)
                        put("action", "react")
                        put("reaction", reaction)
                    }
                    onAction(json)
                    dialog.dismiss()
                }
            })
        }
        dialogView.findViewById<android.widget.Button>(R.id.delete_for_me_button)?.setOnClickListener {
            onDeleteForMe()
            dialog.dismiss()
        }
        dialogView.findViewById<android.widget.Button>(R.id.copy_button)?.setOnClickListener {
            copyMessage(message)
            dialog.dismiss()
        }
        dialog.show()
    }

    fun showEditDeleteDialog(message: Message, position: Int, onAction: (JSONObject) -> Unit, onDeleteForMe: () -> Unit) {
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_reaction_options_owner, null)
        val dialog = AlertDialog.Builder(context).setView(dialogView).setNegativeButton("Cancel", null).create()
        val reactionLayout = dialogView.findViewById<LinearLayout>(R.id.reaction_layout)
        val reactions = arrayOf("👍", "❤️", "😂", "😮", "😢", "😡")

        reactions.forEach { reaction ->
            reactionLayout.addView(TextView(context).apply {
                text = reaction
                textSize = 20f
                setPadding(8, 4, 8, 4)
                setOnClickListener {
                    val json = JSONObject().apply {
                        put("msg_id", message.id)
                        put("action", "react")
                        put("reaction", reaction)
                    }
                    onAction(json)
                    dialog.dismiss()
                }
            })
        }
        dialogView.findViewById<android.widget.Button>(R.id.edit_button)?.setOnClickListener {
            showEditDialog(message) { newContent ->
                val json = JSONObject().apply {
                    put("msg_id", message.id)
                    put("content", newContent)
                    put("action", "edit")
                }
                onAction(json)
            }
            dialog.dismiss()
        }
        dialogView.findViewById<android.widget.Button>(R.id.delete_for_me_button)?.setOnClickListener {
            onDeleteForMe()
            dialog.dismiss()
        }
        dialogView.findViewById<android.widget.Button>(R.id.delete_for_all_button)?.setOnClickListener {
            val json = JSONObject().apply {
                put("msg_id", message.id)
                put("action", "delete")
                put("delete_for_all", true)
            }
            onAction(json)
            dialog.dismiss()
        }
        dialogView.findViewById<android.widget.Button>(R.id.copy_button)?.setOnClickListener {
            copyMessage(message)
            dialog.dismiss()
        }
        dialog.show()
    }

    fun showImageOptionsDialog(message: Message, position: Int, onAction: (JSONObject) -> Unit, onDeleteForMe: () -> Unit) {
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_reaction_options_owner, null)
        val dialog = AlertDialog.Builder(context).setView(dialogView).setNegativeButton("Cancel", null).create()
        val reactionLayout = dialogView.findViewById<LinearLayout>(R.id.reaction_layout)
        val reactions = arrayOf("👍", "❤️", "😂", "😮", "😢", "😡")

        reactions.forEach { reaction ->
            reactionLayout.addView(TextView(context).apply {
                text = reaction
                textSize = 20f
                setPadding(8, 4, 8, 4)
                setOnClickListener {
                    val json = JSONObject().apply {
                        put("msg_id", message.id)
                        put("action", "react")
                        put("reaction", reaction)
                    }
                    onAction(json)
                    dialog.dismiss()
                }
            })
        }
        dialogView.findViewById<android.widget.Button>(R.id.delete_for_me_button)?.setOnClickListener {
            onDeleteForMe()
            dialog.dismiss()
        }
        dialogView.findViewById<android.widget.Button>(R.id.delete_for_all_button)?.setOnClickListener {
            val json = JSONObject().apply {
                put("msg_id", message.id)
                put("action", "delete")
                put("delete_for_all", true)
            }


            onAction(json)
            dialog.dismiss()
        }
        dialogView.findViewById<android.widget.Button>(R.id.copy_button)?.setOnClickListener {
            copyMessage(message)
            dialog.dismiss()
        }
        dialog.show()
    }

    fun showPermanentDeleteDialog(message: Message, onAction: (JSONObject) -> Unit) {
        AlertDialog.Builder(context)
            .setTitle("Xabarni to‘liq o‘chirish")
            .setMessage("Bu xabarni izsiz o‘chirishni xohlaysizmi?")
            .setPositiveButton("Ha") { _, _ ->
                val json = JSONObject().apply {
                    put("msg_id", message.id)
                    put("action", "delete_permanent")
                }
                onAction(json)
            }
            .setNegativeButton("Yo‘q", null)
            .show()
    }

    private fun showEditDialog(message: Message, onEdit: (String) -> Unit) {
        val editText = EditText(context).apply { setText(message.content) }
        AlertDialog.Builder(context)
            .setTitle("Xabarni tahrirlash")
            .setView(editText)
            .setPositiveButton("Saqlash") { _, _ ->
                val newContent = editText.text.toString().trim()
                if (newContent.isNotEmpty()) onEdit(newContent)
            }
            .setNegativeButton("Bekor qilish", null)
            .show()
    }

    private fun copyMessage(message: Message) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Chat Message", message.content)
        clipboard.setPrimaryClip(clip)
        val toastText = if (message.content.startsWith("https://")) "URL nusxalandi" else "Xabar nusxalandi"
        Toast.makeText(context, toastText, Toast.LENGTH_SHORT).show()
    }
}