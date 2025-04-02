package com.example.chatapp3

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.media.MediaPlayer
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import org.json.JSONObject
import android.content.ClipboardManager
import android.content.ClipData
import com.example.chatapp3.chat.ChatActivity

class MessageAdapter(private val currentUser: String) : RecyclerView.Adapter<MessageAdapter.MessageViewHolder>() {
    val messages = mutableListOf<Message>()
    var onReplyRequested: ((Message) -> Unit)? = null
    lateinit var context: Context

    companion object {
        private const val TYPE_SENT = 0
        private const val TYPE_RECEIVED = 1
        private const val TYPE_VOICE_SENT = 2    // Ovozli xabar (jo‘natilgan)
        private const val TYPE_VOICE_RECEIVED = 3 // Ovozli xabar (qabul qilingan)
    }

    override fun getItemViewType(position: Int): Int {
        val message = messages[position]
        return when {
            message.sender == currentUser && message.type == "voice" -> TYPE_VOICE_SENT
            message.sender != currentUser && message.type == "voice" -> TYPE_VOICE_RECEIVED
            message.sender == currentUser -> TYPE_SENT
            else -> TYPE_RECEIVED
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageViewHolder {
        context = parent.context
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            TYPE_SENT -> TextImageMessageViewHolder(
                inflater.inflate(R.layout.item_message_sent, parent, false)
            )
            TYPE_RECEIVED -> TextImageMessageViewHolder(
                inflater.inflate(R.layout.item_message_received, parent, false)
            )
            TYPE_VOICE_SENT -> VoiceMessageViewHolder(
                inflater.inflate(R.layout.item_voice_message_sent, parent, false)
            )
            TYPE_VOICE_RECEIVED -> VoiceMessageViewHolder(
                inflater.inflate(R.layout.item_voice_message_received, parent, false)
            )
            else -> throw IllegalArgumentException("Noma’lum xabar turi")
        }
    }

    override fun onBindViewHolder(holder: MessageViewHolder, position: Int) {
        val message = messages[position]
        when (holder) {
            is TextImageMessageViewHolder -> holder.bindTextOrImageMessage(message, position)
            is VoiceMessageViewHolder -> holder.bindVoiceMessage(message, position)
        }
    }

    override fun getItemCount() = messages.size

    fun addMessage(message: Message) {
        messages.add(message)
        notifyItemInserted(messages.size - 1)
    }

    fun updateUsers(users: List<User>) {}

    fun attachSwipeToReply(recyclerView: RecyclerView) {
        val itemTouchHelper = ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.RIGHT) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean = false

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.adapterPosition
                val message = messages[position]
                onReplyRequested?.invoke(message)
                notifyItemChanged(position)
            }
        })
        itemTouchHelper.attachToRecyclerView(recyclerView)
    }

    private fun showOtherUserOptionsDialog(context: Context, message: Message, position: Int) {
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_reaction_options, null)
        val dialog = AlertDialog.Builder(context)
            .setView(dialogView)
            .setNegativeButton("Cancel", null)
            .create()
        val reactionLayout = dialogView.findViewById<LinearLayout>(R.id.reaction_layout)
        val reactions = arrayOf("👍", "❤️", "😂", "😮", "😢", "😡")

        reactions.forEach { reaction ->
            val textView = TextView(context).apply {
                text = reaction
                textSize = 20f
                setPadding(8, 4, 8, 4)
                setOnClickListener {
                    message.reaction = reaction
                    notifyItemChanged(position)
                    val json = JSONObject().apply {
                        put("content", message.content)
                        put("action", "react")
                        put("msg_id", message.id)
                        put("reaction", reaction)
                    }
                    (context as? ChatActivity)?.sendMessage(json.toString())
                    dialog.dismiss()
                }
            }
            reactionLayout.addView(textView)
        }

        dialogView.findViewById<Button>(R.id.delete_for_me_button)?.setOnClickListener {
            deleteMessage(message, position, false)
            dialog.dismiss()
        }
        dialogView.findViewById<Button>(R.id.copy_button)?.setOnClickListener {
            copyMessage(context, message)
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun showEditDeleteDialog(context: Context, message: Message, position: Int) {
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_reaction_options_owner, null)
        val dialog = AlertDialog.Builder(context)
            .setView(dialogView)
            .setNegativeButton("Cancel", null)
            .create()
        val reactionLayout = dialogView.findViewById<LinearLayout>(R.id.reaction_layout)
        val reactions = arrayOf("👍", "❤️", "😂", "😮", "😢", "😡")

        reactions.forEach { reaction ->
            val textView = TextView(context).apply {
                text = reaction
                textSize = 20f
                setPadding(8, 4, 8, 4)
                setOnClickListener {
                    message.reaction = reaction
                    notifyItemChanged(position)
                    val json = JSONObject().apply {
                        put("content", message.content)
                        put("action", "react")
                        put("msg_id", message.id)
                        put("reaction", reaction)
                    }
                    (context as? ChatActivity)?.sendMessage(json.toString())
                    dialog.dismiss()
                }
            }
            reactionLayout.addView(textView)
        }

        dialogView.findViewById<Button>(R.id.edit_button)?.setOnClickListener {
            showEditDialog(context, message, position)
            dialog.dismiss()
        }
        dialogView.findViewById<Button>(R.id.delete_for_me_button)?.setOnClickListener {
            deleteMessage(message, position, false)
            dialog.dismiss()
        }
        dialogView.findViewById<Button>(R.id.delete_for_all_button)?.setOnClickListener {
            deleteMessage(message, position, true)
            dialog.dismiss()
        }
        dialogView.findViewById<Button>(R.id.copy_button)?.setOnClickListener {
            copyMessage(context, message)
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun showImageOptionsDialog(context: Context, message: Message, position: Int) {
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_reaction_options_owner, null)
        val dialog = AlertDialog.Builder(context)
            .setView(dialogView)
            .setNegativeButton("Cancel", null)
            .create()
        val reactionLayout = dialogView.findViewById<LinearLayout>(R.id.reaction_layout)
        val reactions = arrayOf("👍", "❤️", "😂", "😮", "😢", "😡")

        reactions.forEach { reaction ->
            val textView = TextView(context).apply {
                text = reaction
                textSize = 20f
                setPadding(8, 4, 8, 4)
                setOnClickListener {
                    message.reaction = reaction
                    notifyItemChanged(position)
                    val json = JSONObject().apply {
                        put("content", message.content)
                        put("action", "react")
                        put("msg_id", message.id)
                        put("reaction", reaction)
                    }
                    (context as? ChatActivity)?.sendMessage(json.toString())
                    dialog.dismiss()
                }
            }
            reactionLayout.addView(textView)
        }

        dialogView.findViewById<Button>(R.id.delete_for_me_button)?.setOnClickListener {
            deleteMessage(message, position, false)
            dialog.dismiss()
        }
        dialogView.findViewById<Button>(R.id.delete_for_all_button)?.setOnClickListener {
            deleteMessage(message, position, true)
            dialog.dismiss()
        }
        dialogView.findViewById<Button>(R.id.copy_button)?.setOnClickListener {
            copyMessage(context, message)
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun showPermanentDeleteDialog(context: Context, message: Message, position: Int) {
        AlertDialog.Builder(context)
            .setTitle("Xabarni to‘liq o‘chirish")
            .setMessage("Bu xabarni izsiz o‘chirishni xohlaysizmi?")
            .setPositiveButton("Ha") { _, _ ->
                permanentDeleteMessage(message, position)
            }
            .setNegativeButton("Yo‘q", null)
            .show()
    }

    private fun showEditDialog(context: Context, message: Message, position: Int) {
        val editText = EditText(context).apply {
            setText(message.content)
        }
        AlertDialog.Builder(context)
            .setTitle("Xabarni tahrirlash")
            .setView(editText)
            .setPositiveButton("Saqlash") { _, _ ->
                val newContent = editText.text.toString().trim()
                if (newContent.isNotEmpty()) {
                    editMessage(message, newContent, position)
                }
            }
            .setNegativeButton("Bekor qilish", null)
            .show()
    }

    private fun editMessage(message: Message, newContent: String, position: Int) {
        message.content = newContent
        message.edited = true
        notifyItemChanged(position)

        val activity = context as? ChatActivity
        activity?.let {
            val json = JSONObject().apply {
                put("msg_id", message.id)
                put("content", newContent)
                put("action", "edit")
            }
            it.sendMessage(json.toString())
        }
    }

    private fun deleteMessage(message: Message, position: Int, deleteForAll: Boolean) {
        if (deleteForAll && message.sender == currentUser) {
            message.deleted = true
            message.content = "This message was deleted"
            notifyItemChanged(position)

            val activity = context as? ChatActivity
            activity?.let {
                val json = JSONObject().apply {
                    put("msg_id", message.id)
                    put("action", "delete")
                    put("delete_for_all", true)
                }
                it.sendMessage(json.toString())
            }
        } else {
            messages.removeAt(position)
            notifyItemRemoved(position)
        }
    }

    private fun permanentDeleteMessage(message: Message, position: Int) {
        messages.removeAt(position)
        notifyItemRemoved(position)

        val activity = context as? ChatActivity
        activity?.let {
            val json = JSONObject().apply {
                put("msg_id", message.id)
                put("action", "delete_permanent")
            }
            it.sendMessage(json.toString())
        }
    }

    private fun copyMessage(context: Context, message: Message) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Chat Message", message.content)
        clipboard.setPrimaryClip(clip)
        val toastText = if (message.content.startsWith("https://")) "Surat URL’i nusxalandi" else "Xabar nusxalandi"
        Toast.makeText(context, toastText, Toast.LENGTH_SHORT).show()
    }

    abstract inner class MessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)

    inner class TextImageMessageViewHolder(itemView: View) : MessageViewHolder(itemView) {
        val senderText: TextView? = itemView.findViewById(R.id.senderText)
        val contentText: TextView = itemView.findViewById(R.id.contentText)
        val contentImage: ImageView = itemView.findViewById(R.id.contentImage)
        val uploadProgress: ProgressBar? = itemView.findViewById(R.id.uploadProgress)
        val timestampText: TextView = itemView.findViewById(R.id.timestampText)
        val reactionText: TextView = itemView.findViewById(R.id.messageReaction)
        val replyText: TextView? = itemView.findViewById(R.id.replyContent)

        fun bindTextOrImageMessage(message: Message, position: Int) {
            senderText?.text = if (message.sender == currentUser) "" else message.sender
            timestampText.text = message.timestamp
            reactionText.text = message.reaction ?: ""

            val isImageUrl = message.content.startsWith("https://") &&
                    (message.content.endsWith(".jpg") ||
                            message.content.endsWith(".jpeg") ||
                            message.content.endsWith(".png"))

            if (message.deleted) {
                contentText.visibility = View.VISIBLE
                (contentImage.parent as View).visibility = View.GONE
                contentText.text = "This message was deleted"
                contentText.setTextColor(Color.GRAY)
                contentText.setTypeface(null, Typeface.ITALIC)
                itemView.setOnLongClickListener {
                    if (message.sender == currentUser) {
                        showPermanentDeleteDialog(context, message, position)
                    } else {
                        showOtherUserOptionsDialog(context, message, position)
                    }
                    true
                }
            } else if (isImageUrl) {
                contentText.visibility = View.GONE
                (contentImage.parent as View).visibility = View.VISIBLE
                contentImage.visibility = View.VISIBLE
                uploadProgress?.visibility = View.GONE
                Glide.with(itemView.context)
                    .load(message.content)
                    .error(android.R.drawable.ic_menu_close_clear_cancel)
                    .into(contentImage)
                contentImage.setOnClickListener {
                    val intent = Intent(itemView.context, ImagePreviewActivity::class.java)
                    intent.putExtra("image_url", message.content)
                    itemView.context.startActivity(intent)
                }
                itemView.setOnLongClickListener {
                    if (message.sender == currentUser) {
                        showImageOptionsDialog(context, message, position)
                    } else {
                        showOtherUserOptionsDialog(context, message, position)
                    }
                    true
                }
            } else {
                contentText.visibility = View.VISIBLE
                (contentImage.parent as View).visibility = View.GONE
                contentImage.visibility = View.GONE
                uploadProgress?.visibility = View.GONE
                contentText.text = message.content + if (message.edited) " (edited)" else ""
                contentText.setTextColor(Color.BLACK)
                contentText.setTypeface(null, Typeface.NORMAL)
                itemView.setOnLongClickListener {
                    if (message.sender == currentUser) {
                        showEditDeleteDialog(context, message, position)
                    } else {
                        showOtherUserOptionsDialog(context, message, position)
                    }
                    true
                }
            }

            if (message.replyToId != null) {
                val repliedMessage = messages.find { it.id == message.replyToId }
                replyText?.visibility = View.VISIBLE
                replyText?.text = repliedMessage?.content ?: "Xabar topilmadi"
            } else {
                replyText?.visibility = View.GONE
            }
        }
    }

    inner class VoiceMessageViewHolder(itemView: View) : MessageViewHolder(itemView) {
        val senderText: TextView? = itemView.findViewById(R.id.senderText)
        val playButton: Button = itemView.findViewById(R.id.playButton)
        val timestampText: TextView = itemView.findViewById(R.id.timestampText)
        val reactionText: TextView = itemView.findViewById(R.id.messageReaction)
        val replyText: TextView? = itemView.findViewById(R.id.replyContent)
        var mediaPlayer: MediaPlayer? = null

        fun bindVoiceMessage(message: Message, position: Int) {
            senderText?.text = if (message.sender == currentUser) "" else message.sender
            timestampText.text = message.timestamp
            reactionText.text = message.reaction ?: ""

            playButton.setOnClickListener {
                mediaPlayer?.let {
                    if (it.isPlaying) {
                        it.pause()
                        playButton.text = "Play"
                    } else {
                        it.start()
                        playButton.text = "Pause"
                    }
                } ?: run {
                    mediaPlayer = MediaPlayer().apply {
                        setDataSource(message.content) // content hozircha filePath
                        prepare()
                        start()
                        playButton.text = "Pause"
                        setOnCompletionListener {
                            playButton.text = "Play"
                            reset()
                            mediaPlayer = null
                        }
                    }
                }
            }

            itemView.setOnLongClickListener {
                if (message.sender == currentUser) {
                    showImageOptionsDialog(context, message, position) // Ovoz uchun ham shu dialog
                } else {
                    showOtherUserOptionsDialog(context, message, position)
                }
                true
            }

            if (message.replyToId != null) {
                val repliedMessage = messages.find { it.id == message.replyToId }
                replyText?.visibility = View.VISIBLE
                replyText?.text = repliedMessage?.content ?: "Xabar topilmadi"
            } else {
                replyText?.visibility = View.GONE
            }
        }
    }
}