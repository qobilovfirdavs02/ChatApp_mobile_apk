package com.example.chatapp3

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class StickerAdapter(
    private val stickers: List<String>,
    private val onStickerClick: (String) -> Unit
) : RecyclerView.Adapter<StickerAdapter.StickerViewHolder>() {

    class StickerViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val stickerText: TextView = itemView.findViewById(android.R.id.text1)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StickerViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(android.R.layout.simple_list_item_1, parent, false)
        return StickerViewHolder(view)
    }

    override fun onBindViewHolder(holder: StickerViewHolder, position: Int) {
        val sticker = stickers[position]
        holder.stickerText.text = sticker
        holder.itemView.setOnClickListener { onStickerClick(sticker) }
    }

    override fun getItemCount() = stickers.size
}