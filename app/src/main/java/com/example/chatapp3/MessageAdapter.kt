package com.example.chatapp3

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

data class Message(
    val id: Int,
    val sender: String,
    val content: String,
    val timestamp: String,
    val edited: Boolean
)

class MessageAdapter(private val currentUser: String) : RecyclerView.Adapter<MessageAdapter.MessageViewHolder>() {
    private val messages = mutableListOf<Message>()
    var onEditClick: ((Message) -> Unit)? = null

    class MessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val senderText: TextView = itemView.findViewById(R.id.senderText)
        val contentText: TextView = itemView.findViewById(R.id.contentText)
        val timestampText: TextView = itemView.findViewById(R.id.timestampText)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageViewHolder {
        val layout = if (viewType == 0) R.layout.item_message_sent else R.layout.item_message_received
        val view = LayoutInflater.from(parent.context).inflate(layout, parent, false)
        return MessageViewHolder(view)
    }

    override fun onBindViewHolder(holder: MessageViewHolder, position: Int) {
        val message = messages[position]
        holder.senderText.text = message.sender
        holder.contentText.text = message.content + if (message.edited) " (tahrirlangan)" else ""
        holder.timestampText.text = message.timestamp
        if (message.sender == currentUser) {
            holder.itemView.setOnLongClickListener {
                onEditClick?.invoke(message)
                true
            }
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
}