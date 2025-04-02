package com.example.chatapp3

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.example.chatapp3.chat.ChatActivity
import com.example.chatapp3.message.TextMessage
import com.example.chatapp3.message.VoiceMessage

class MessageAdapter(private val currentUser: String, private val context: Context) : RecyclerView.Adapter<MessageAdapter.MessageViewHolder>() {
    val messages = mutableListOf<Message>()
    var onReplyRequested: ((Message) -> Unit)? = null
    private val dialogAdapter = DialogAdapter(context, currentUser)
    private val textMessageHandler = TextMessage(this, currentUser)
    private val voiceMessageHandler = VoiceMessage(this, currentUser)

    companion object {
        private const val TYPE_TEXT_SENT = 0
        private const val TYPE_TEXT_RECEIVED = 1
        private const val TYPE_VOICE_SENT = 2
        private const val TYPE_VOICE_RECEIVED = 3
    }

    override fun getItemViewType(position: Int): Int {
        val message = messages[position]
        return when {
            message.type == "voice" && message.sender == currentUser -> TYPE_VOICE_SENT
            message.type == "voice" -> TYPE_VOICE_RECEIVED
            message.sender == currentUser -> TYPE_TEXT_SENT
            else -> TYPE_TEXT_RECEIVED
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageViewHolder {
        val inflater = LayoutInflater.from(context)
        return when (viewType) {
            TYPE_TEXT_SENT -> TextMessage.TextSentViewHolder(inflater.inflate(R.layout.item_message_sent, parent, false), this)
            TYPE_TEXT_RECEIVED -> TextMessage.TextReceivedViewHolder(inflater.inflate(R.layout.item_message_received, parent, false), this)
            TYPE_VOICE_SENT -> VoiceMessage.VoiceSentViewHolder(inflater.inflate(R.layout.item_voice_message_sent, parent, false), this)
            TYPE_VOICE_RECEIVED -> VoiceMessage.VoiceReceivedViewHolder(inflater.inflate(R.layout.item_voice_message_received, parent, false), this)
            else -> throw IllegalArgumentException("Noto‘g‘ri view turi")
        }
    }

    override fun onBindViewHolder(holder: MessageViewHolder, position: Int) {
        holder.bind(messages[position], position)
    }

    override fun getItemCount() = messages.size

    fun addMessage(message: Message) {
        messages.add(message)
        notifyItemInserted(messages.size - 1)
    }

    fun attachSwipeToReply(recyclerView: RecyclerView) {
        val itemTouchHelper = ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.RIGHT) {
            override fun onMove(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder): Boolean = false
            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.adapterPosition
                onReplyRequested?.invoke(messages[position])
                notifyItemChanged(position)
            }
        })
        itemTouchHelper.attachToRecyclerView(recyclerView)
    }

    // Dialog funksiyalari
    fun showOtherUserOptionsDialog(message: Message, position: Int) {
        dialogAdapter.showOtherUserOptionsDialog(message, position, { json -> (context as? ChatActivity)?.sendMessage(json.toString()) }, { removeMessage(position) })
    }

    fun showEditDeleteDialog(message: Message, position: Int) {
        dialogAdapter.showEditDeleteDialog(message, position, { json -> (context as? ChatActivity)?.sendMessage(json.toString()) }, { removeMessage(position) })
    }

    fun showImageOptionsDialog(message: Message, position: Int) {
        dialogAdapter.showImageOptionsDialog(message, position, { json -> (context as? ChatActivity)?.sendMessage(json.toString()) }, { removeMessage(position) })
    }

    fun showPermanentDeleteDialog(message: Message, position: Int) {
        dialogAdapter.showPermanentDeleteDialog(message) { json -> (context as? ChatActivity)?.sendMessage(json.toString()); removeMessage(position) }
    }

    private fun removeMessage(position: Int) {
        messages.removeAt(position)
        notifyItemRemoved(position)
    }

    abstract class MessageViewHolder(itemView: View, val adapter: MessageAdapter) : RecyclerView.ViewHolder(itemView) {
        abstract fun bind(message: Message, position: Int)
    }
}