package com.roadsync.home.presentation.trip.newtrip

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.AppCompatButton
import androidx.appcompat.widget.AppCompatTextView
import androidx.recyclerview.widget.RecyclerView
import com.roadsync.R
import com.roadsync.auth.model.InvitedUser
import com.roadsync.auth.model.Participant

class InvitedUserAdapter(
    private val currentUserId: String,
    private val users: MutableList<InvitedUser>,
    private val onRemoveClick: (InvitedUser) -> Unit
) : RecyclerView.Adapter<InvitedUserAdapter.UserViewHolder>() {


    inner class UserViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvUserName: AppCompatTextView = itemView.findViewById(R.id.userName)
        val btnRemove: AppCompatButton = itemView.findViewById(R.id.btnRemove)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_trip_participant, parent, false)
        return UserViewHolder(view)
    }

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        val user = users[position]
        holder.tvUserName.text = user.name
        // Show "Remove" button only if current user is creator and not removing themselves

        holder.btnRemove.visibility = View.VISIBLE

        holder.btnRemove.setOnClickListener {
            onRemoveClick(user)
        }
    }

    fun removeUser(userId: String) {
        val index = users.indexOfFirst { it.userId == userId }
        if (index != -1) {
            users.removeAt(index)
            notifyItemRemoved(index)
        }
    }


    override fun getItemCount(): Int = users.size
}
