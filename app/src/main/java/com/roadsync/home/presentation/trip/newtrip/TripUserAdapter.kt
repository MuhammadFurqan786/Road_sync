package com.roadsync.home.presentation.trip.newtrip

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.AppCompatButton
import androidx.appcompat.widget.AppCompatTextView
import androidx.recyclerview.widget.RecyclerView
import com.roadsync.R
import com.roadsync.auth.model.Participant

class TripUserAdapter(
    private val currentUserId: String,
    private val users: MutableList<Participant>,
    private val onRemoveClick: (Participant) -> Unit
) : RecyclerView.Adapter<TripUserAdapter.UserViewHolder>() {


    inner class UserViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvUserName: AppCompatTextView = itemView.findViewById(R.id.userName)
        val pickup: AppCompatTextView = itemView.findViewById(R.id.pickup)
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
        holder.pickup.text = "Pick-up Point : ${user.pickup}"

        val creatorUser = users.find { it.role == "creator" } // Find the creator in the list
        val creatorId = creatorUser?.userId

        if (creatorId == currentUserId && user.userId != currentUserId) {
            // Current user is creator, and it's not himself
            holder.btnRemove.visibility = View.VISIBLE
            holder.btnRemove.setOnClickListener {
                onRemoveClick(user)
            }
        } else {
            holder.btnRemove.visibility = View.GONE
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
