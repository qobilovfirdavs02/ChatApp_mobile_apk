package com.example.chatapp3.message

import android.graphics.Color
import android.graphics.Typeface
import android.view.View
import android.widget.TextView
import com.example.chatapp3.Message
import com.example.chatapp3.MessageAdapter
import com.example.chatapp3.R

class TextMessage(private val adapter: MessageAdapter, private val currentUser: String) {

    class TextSentViewHolder(itemView: View, adapter: MessageAdapter) : MessageAdapter.MessageViewHolder(itemView, adapter) {
        private val contentText: TextView = itemView.findViewById(R.id.contentText)
        private val timestampText: TextView = itemView.findViewById(R.id.timestampText)
        private val reactionText: TextView = itemView.findViewById(R.id.messageReaction)
        private val replyText: TextView? = itemView.findViewById(R.id.replyContent)

        override fun bind(message: Message, position: Int) {
            timestampText.text = message.timestamp
            reactionText.text = message.reaction ?: ""
            contentText.text = if (message.deleted) "This message was deleted" else "${message.content}${if (message.edited) " (edited)" else ""}"
            contentText.setTextColor(if (message.deleted) Color.GRAY else Color.BLACK)
            contentText.setTypeface(null, if (message.deleted) Typeface.ITALIC else Typeface.NORMAL)

            setupReply(message)
            itemView.setOnLongClickListener {
                if (message.deleted) adapter.showPermanentDeleteDialog(message, position)
                else adapter.showEditDeleteDialog(message, position)
                true
            }
        }

        private fun setupReply(message: Message) {
            replyText?.let {
                if (message.replyToId != null) {
                    val repliedMessage = adapter.messages.find { m -> m.id == message.replyToId }
                    it.visibility = View.VISIBLE
                    it.text = repliedMessage?.content ?: "Xabar topilmadi"
                } else {
                    it.visibility = View.GONE
                }
            }
        }
    }

    class TextReceivedViewHolder(itemView: View, adapter: MessageAdapter) : MessageAdapter.MessageViewHolder(itemView, adapter) {
        private val senderText: TextView = itemView.findViewById(R.id.senderText)
        private val contentText: TextView = itemView.findViewById(R.id.contentText)
        private val timestampText: TextView = itemView.findViewById(R.id.timestampText)
        private val reactionText: TextView = itemView.findViewById(R.id.messageReaction)
        private val replyText: TextView? = itemView.findViewById(R.id.replyContent)

        override fun bind(message: Message, position: Int) {
            senderText.text = message.sender
            timestampText.text = message.timestamp
            reactionText.text = message.reaction ?: ""
            contentText.text = if (message.deleted) "This message was deleted" else message.content
            contentText.setTextColor(if (message.deleted) Color.GRAY else Color.BLACK)
            contentText.setTypeface(null, if (message.deleted) Typeface.ITALIC else Typeface.NORMAL)

            setupReply(message)
            itemView.setOnLongClickListener {
                if (message.deleted) adapter.showPermanentDeleteDialog(message, position)
                else adapter.showOtherUserOptionsDialog(message, position)
                true
            }
        }

        private fun setupReply(message: Message) {
            replyText?.let {
                if (message.replyToId != null) {
                    val repliedMessage = adapter.messages.find { m -> m.id == message.replyToId }
                    it.visibility = View.VISIBLE
                    it.text = repliedMessage?.content ?: "Xabar topilmadi"
                } else {
                    it.visibility = View.GONE
                }
            }
        }
    }
}