package com.roadsync.auth.presentation

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.roadsync.auth.model.User
import com.roadsync.databinding.ItemUsersBinding

class UsersAdapter(
    private val users: List<User>,
    private val onInviteClick: (User) -> Unit,
    private val onSendNotificationClick: (User) -> Unit
) : RecyclerView.Adapter<UsersAdapter.UserViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        val binding = ItemUsersBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return UserViewHolder(binding)
    }

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        val user = users[position]
        holder.bind(user)
    }

    override fun getItemCount(): Int = users.size

    inner class UserViewHolder(private val binding: ItemUsersBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(user: User) {
            binding.apply {
                tvUserName.text = user.name

                // Show green dot for online users
                if (user.onlineStatus) {
                    onlineStatusDot.setBackgroundColor(Color.GREEN)
                    inviteButton.visibility = View.VISIBLE
                    sendNotification.visibility = View.GONE
                } else {
                    onlineStatusDot.setBackgroundColor(Color.RED)
                    inviteButton.visibility = View.GONE
                    sendNotification.visibility = View.VISIBLE
                }

                inviteButton.setOnClickListener {
                    onInviteClick(user)
                }
                sendNotification.setOnClickListener {
                    onSendNotificationClick(user)
                }
            }
        }
    }
}
