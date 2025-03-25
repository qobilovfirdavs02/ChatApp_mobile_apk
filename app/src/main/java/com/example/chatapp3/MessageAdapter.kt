package com.example.chatapp3

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView

class MessageAdapter(private val currentUser: String) : RecyclerView.Adapter<MessageAdapter.MessageViewHolder>() {
    private val messages = mutableListOf<Message>()
    var onReplyRequested: ((Message) -> Unit)? = null // Javob berish uchun callback

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageViewHolder {
        val layout = if (viewType == 0) R.layout.item_message_sent else R.layout.item_message_received
        val view = LayoutInflater.from(parent.context).inflate(layout, parent, false)
        return MessageViewHolder(view)
    }

    override fun onBindViewHolder(holder: MessageViewHolder, position: Int) {
        val message = messages[position]
        holder.senderText.text = if (message.sender == currentUser) "" else message.sender // O‘z xabarida sender ko‘rinmaydi
        holder.contentText.text = message.content + if (message.edited) " (tahrirlangan)" else ""
        holder.timestampText.text = message.timestamp
        holder.reactionText.text = message.reaction ?: ""

        // Javob berilgan xabar
        if (message.replyToId != null) {
            val repliedMessage = messages.find { it.id == message.replyToId }
            holder.replyText.visibility = View.VISIBLE
            holder.replyText.text = repliedMessage?.content ?: "Xabar topilmadi"
        } else {
            holder.replyText.visibility = View.GONE
        }

        // Uzoq bosish bilan reaksiya
        holder.itemView.setOnLongClickListener {
            showReactionDialog(holder.itemView.context, message, position)
            true
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

    fun updateUsers(users: List<User>) {
        // Bu funksiya MainActivity uchun edi, bu yerda kerak emas, lekin xato chiqmasligi uchun saqlab qoldim
    }

    private fun showReactionDialog(context: Context, message: Message, position: Int) {
        val reactions = arrayOf("👍", "❤️", "😂", "😮", "😢", "😡")
        AlertDialog.Builder(context)
            .setTitle("Reaksiya qo‘shish")
            .setItems(reactions) { _, which ->
                message.reaction = reactions[which]
                notifyItemChanged(position)
                // Serverga reaksiyani yuborish uchun ChatActivity'dan callback qo‘shish mumkin
            }
            .setNegativeButton("Bekor qilish", null)
            .show()
    }

    // Xabarni o‘ngga surish uchun ItemTouchHelper
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
                notifyItemChanged(position) // Swipedan keyin pozitsiyani qaytarish
            }
        })
        itemTouchHelper.attachToRecyclerView(recyclerView)
    }

    inner class MessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val senderText: TextView = itemView.findViewById(R.id.senderText)
        val contentText: TextView = itemView.findViewById(R.id.contentText)
        val timestampText: TextView = itemView.findViewById(R.id.timestampText)
        val reactionText: TextView = itemView.findViewById(R.id.messageReaction)
        val replyText: TextView = itemView.findViewById(R.id.replyContent)
    }
}