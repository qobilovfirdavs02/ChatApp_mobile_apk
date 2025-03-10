package com.example.chatapp3

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

data class User(val username: String)

class UserAdapter(private val currentUser: String) : RecyclerView.Adapter<UserAdapter.UserViewHolder>() {
    private val users = mutableListOf<User>()

    class UserViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        // android.R.layout.simple_list_item_1 da 'text1' ID’si ishlatiladi
        val usernameText: TextView = itemView.findViewById(android.R.id.text1)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(android.R.layout.simple_list_item_1, parent, false)
        return UserViewHolder(view)
    }

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        val user = users[position]
        holder.usernameText.text = user.username
        holder.itemView.setOnClickListener {
            val intent = Intent(holder.itemView.context, ChatActivity::class.java)
            intent.putExtra("username", currentUser)
            intent.putExtra("receiver", user.username)
            holder.itemView.context.startActivity(intent)
        }
    }

    override fun getItemCount() = users.size

    fun updateUsers(newUsers: List<User>) {
        users.clear()
        users.addAll(newUsers)
        notifyDataSetChanged()
    }
}