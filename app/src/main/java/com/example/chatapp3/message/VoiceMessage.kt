package com.example.chatapp3.message

import android.media.MediaPlayer
import android.view.View
import android.widget.Button
import android.widget.TextView
import com.example.chatapp3.Message
import com.example.chatapp3.MessageAdapter
import com.example.chatapp3.R

class VoiceMessage(private val adapter: MessageAdapter, private val currentUser: String) {

    class VoiceSentViewHolder(itemView: View, adapter: MessageAdapter) : MessageAdapter.MessageViewHolder(itemView, adapter) {
        private val playButton: Button = itemView.findViewById(R.id.playButton)
        private val durationText: TextView = itemView.findViewById(R.id.voiceDuration)
        private val timestampText: TextView = itemView.findViewById(R.id.timestampText)
        private val reactionText: TextView = itemView.findViewById(R.id.messageReaction)
        private val replyText: TextView? = itemView.findViewById(R.id.replyContent)
        private var mediaPlayer: MediaPlayer? = null

        override fun bind(message: Message, position: Int) {
            timestampText.text = message.timestamp
            reactionText.text = message.reaction ?: ""
            durationText.text = "0:00"
            
            if (message.type == "voice") {
                setupVoicePlayer(message) // URL dan foydalanadi
            }

            setupVoicePlayer(message)
            setupReply(message)
            itemView.setOnLongClickListener {
                adapter.showImageOptionsDialog(message, position)
                true
            }
        }



        private fun setupVoicePlayer(message: Message) {
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
                        setDataSource(message.content)
                        prepare()
                        start()
                        playButton.text = "Pause"
                        durationText.text = String.format("%d:%02d", duration / 1000 / 60, duration / 1000 % 60)
                        setOnCompletionListener {
                            playButton.text = "Play"
                            reset()
                            mediaPlayer = null
                        }
                    }
                }
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

    class VoiceReceivedViewHolder(itemView: View, adapter: MessageAdapter) : MessageAdapter.MessageViewHolder(itemView, adapter) {
        private val senderText: TextView = itemView.findViewById(R.id.senderText)
        private val playButton: Button = itemView.findViewById(R.id.playButton)
        private val durationText: TextView = itemView.findViewById(R.id.voiceDuration)
        private val timestampText: TextView = itemView.findViewById(R.id.timestampText)
        private val reactionText: TextView = itemView.findViewById(R.id.messageReaction)
        private val replyText: TextView? = itemView.findViewById(R.id.replyContent)
        private var mediaPlayer: MediaPlayer? = null

        override fun bind(message: Message, position: Int) {
            senderText.text = message.sender
            timestampText.text = message.timestamp
            reactionText.text = message.reaction ?: ""
            durationText.text = "0:00"

            setupVoicePlayer(message)
            setupReply(message)
            itemView.setOnLongClickListener {
                adapter.showOtherUserOptionsDialog(message, position)
                true
            }
        }

        private fun setupVoicePlayer(message: Message) {
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
                        setDataSource(message.content)
                        prepare()
                        start()
                        playButton.text = "Pause"
                        durationText.text = String.format("%d:%02d", duration / 1000 / 60, duration / 1000 % 60)
                        setOnCompletionListener {
                            playButton.text = "Play"
                            reset()
                            mediaPlayer = null
                        }
                    }
                }
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