package com.roadsync.admin.presentation

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView

import com.roadsync.auth.model.InvitedUser
import com.roadsync.databinding.ItemInvitedUserListBinding

class InvitedUserAdapter(private val invitedUsers: List<InvitedUser>) :
    RecyclerView.Adapter<InvitedUserAdapter.InvitedUserViewHolder>() {

    inner class InvitedUserViewHolder(val binding: ItemInvitedUserListBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): InvitedUserViewHolder {
        val binding = ItemInvitedUserListBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return InvitedUserViewHolder(binding)
    }

    override fun onBindViewHolder(holder: InvitedUserViewHolder, position: Int) {
        val invitedUser = invitedUsers[position]
        holder.binding.invitedName.text = invitedUser.name
    }

    override fun getItemCount() = invitedUsers.size
}
