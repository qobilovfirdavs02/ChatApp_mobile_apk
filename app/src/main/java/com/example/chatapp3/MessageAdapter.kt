package com.example.chatapp3

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import org.json.JSONObject

class MessageAdapter(private val currentUser: String) : RecyclerView.Adapter<MessageAdapter.MessageViewHolder>() {
    val messages = mutableListOf<Message>()
    var onReplyRequested: ((Message) -> Unit)? = null
    lateinit var context: Context

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageViewHolder {
        context = parent.context
        val layout = if (viewType == 0) R.layout.item_message_sent else R.layout.item_message_received
        val view = LayoutInflater.from(parent.context).inflate(layout, parent, false)
        return MessageViewHolder(view)
    }

    override fun onBindViewHolder(holder: MessageViewHolder, position: Int) {
        val message = messages[position]
        holder.senderText?.text = if (message.sender == currentUser) "" else message.sender
        holder.timestampText.text = message.timestamp
        holder.reactionText.text = message.reaction ?: ""

        val isImageUrl = message.content.startsWith("https://") &&
                (message.content.endsWith(".jpg") ||
                        message.content.endsWith(".jpeg") ||
                        message.content.endsWith(".png"))

        if (message.deleted) {
            holder.contentText.visibility = View.VISIBLE
            (holder.contentImage.parent as View).visibility = View.GONE
            holder.contentText.text = "This message was deleted"
            holder.contentText.setTextColor(Color.GRAY)
            holder.contentText.setTypeface(null, Typeface.ITALIC)
        } else if (isImageUrl) {
            holder.contentText.visibility = View.GONE
            (holder.contentImage.parent as View).visibility = View.VISIBLE
            holder.contentImage.visibility = View.VISIBLE
            holder.uploadProgress?.visibility = View.GONE
            Glide.with(holder.itemView.context)
                .load(message.content)
                .error(android.R.drawable.ic_menu_close_clear_cancel)
                .into(holder.contentImage)
            holder.contentImage.setOnClickListener {
                val intent = Intent(holder.itemView.context, ImagePreviewActivity::class.java)
                intent.putExtra("image_url", message.content)
                holder.itemView.context.startActivity(intent)
            }
        } else {
            holder.contentText.visibility = View.VISIBLE
            (holder.contentImage.parent as View).visibility = View.GONE
            holder.contentImage.visibility = View.GONE
            holder.uploadProgress?.visibility = View.GONE
            holder.contentText.text = message.content + if (message.edited) " (edited)" else ""
            holder.contentText.setTextColor(Color.BLACK)
            holder.contentText.setTypeface(null, Typeface.NORMAL)
        }

        if (message.replyToId != null) {
            val repliedMessage = messages.find { it.id == message.replyToId }
            holder.replyText?.visibility = View.VISIBLE
            holder.replyText?.text = repliedMessage?.content ?: "Xabar topilmadi"
        } else {
            holder.replyText?.visibility = View.GONE
        }

        // Uzun bosish bilan dialog ochish
        holder.itemView.setOnLongClickListener {
            if (message.sender == currentUser && !message.deleted) { // Faqat o‘z xabarimiz va o‘chirilmagan bo‘lsa
                showEditDeleteDialog(holder.itemView.context, message, position)
            }
            true
        }
    }
    private fun showEditDeleteDialog(context: Context, message: Message, position: Int) {
        val options = arrayOf("Edit", "Delete")
        AlertDialog.Builder(context)
            .setItems(options) { _, which ->
                when (which) {
                    0 -> showEditDialog(context, message, position) // Tahrirlash
                    1 -> deleteMessage(message, position) // O‘chirish
                }
            }
            .setNegativeButton("Cancel", null)
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

        // Serverga yangilangan xabarni yuborish
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

    private fun deleteMessage(message: Message, position: Int) {
        message.deleted = true
        message.content = "This message was deleted"
        notifyItemChanged(position)

        // Serverga o‘chirish so‘rovini yuborish
        val activity = context as? ChatActivity
        activity?.let {
            val json = JSONObject().apply {
                put("msg_id", message.id)
                put("action", "delete")
            }
            it.sendMessage(json.toString())
        }
    }



    override fun getItemCount() = messages.size

    override fun getItemViewType(position: Int): Int {
        return if (messages[position].sender == currentUser) 0 else 1
    }

    fun addMessage(message: Message) {
        messages.add(message)
        notifyItemInserted(messages.size - 1)
    }

    fun updateUsers(users: List<User>) {}

    private fun showReactionDialog(context: Context, message: Message, position: Int) {
        val reactions = arrayOf("👍", "❤️", "😂", "😮", "😢", "😡")
        AlertDialog.Builder(context)
            .setTitle("Reaksiya qo‘shish")
            .setItems(reactions) { _, which ->
                message.reaction = reactions[which]
                notifyItemChanged(position)
                val json = JSONObject().apply {
                    put("content", message.content)
                    put("action", "react")
                    put("msg_id", message.id)
                    put("reaction", message.reaction)
                }
                (context as? ChatActivity)?.sendMessage(json.toString())
            }
            .setNegativeButton("Bekor qilish", null)
            .show()
    }

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

    inner class MessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val senderText: TextView? = itemView.findViewById(R.id.senderText)
        val contentText: TextView = itemView.findViewById(R.id.contentText)
        val contentImage: ImageView = itemView.findViewById(R.id.contentImage)
        val uploadProgress: ProgressBar? = itemView.findViewById(R.id.uploadProgress)
        val timestampText: TextView = itemView.findViewById(R.id.timestampText)
        val reactionText: TextView = itemView.findViewById(R.id.messageReaction)
        val replyText: TextView? = itemView.findViewById(R.id.replyContent)
    }
}