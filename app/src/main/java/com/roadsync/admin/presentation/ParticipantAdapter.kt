package com.roadsync.admin.presentation

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.roadsync.auth.model.Participant
import com.roadsync.databinding.ItemParticipantListBinding

class ParticipantAdapter(private val members: List<Participant>) :
    RecyclerView.Adapter<ParticipantAdapter.ParticipantViewHolder>() {

    inner class ParticipantViewHolder(val binding: ItemParticipantListBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ParticipantViewHolder {
        val binding = ItemParticipantListBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ParticipantViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ParticipantViewHolder, position: Int) {
        val participant = members[position]
        holder.binding.memberName.text = participant.name
        holder.binding.role.text = "Role: ${participant.role}"

        if (participant.isOutsideGeofence) {
            holder.binding.status.text = "Status: Outside Geofence"
            holder.binding.status.setTextColor(Color.RED)
        } else {
            holder.binding.status.text = "Status: Inside Geofence"
            holder.binding.status.setTextColor(Color.GREEN)
        }
    }

    override fun getItemCount() = members.size
}
