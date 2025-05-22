package com.roadsync.admin.presentation

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.roadsync.databinding.ItemTripListBinding
import com.roadsync.home.models.Trip


class TripAdapter(
    private var tripList: List<Trip>,
    private val onDeleteClick: (Trip) -> Unit,
    private val onDetailClick: (Trip) -> Unit
) : RecyclerView.Adapter<TripAdapter.TripViewHolder>() {

    inner class TripViewHolder(val binding: ItemTripListBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(trip: Trip) {
            binding.tripName.text = trip.tripName
            binding.departure.text = "From: ${trip.departure}"
            binding.destination.text = "To: ${trip.destination}"
            binding.cost.text = "Cost: ${trip.cost}"
            binding.stayOption.text = "Stay: ${trip.stayOption}"
            val activeStatus = if (trip.active) "Active" else "Inactive"
            binding.activeStatus.text = "Status: $activeStatus"
            if (trip.active) {
                binding.activeStatus.setTextColor(Color.GREEN)
            } else {
                binding.activeStatus.setTextColor(Color.RED)
            }

            binding.deleteButton.setOnClickListener {
                onDeleteClick(trip)
            }

            binding.detailsButton.setOnClickListener {
                onDetailClick(trip)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TripViewHolder {
        val binding =
            ItemTripListBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return TripViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TripViewHolder, position: Int) {
        holder.bind(tripList[position])
    }

    override fun getItemCount(): Int = tripList.size

    fun updateList(newList: List<Trip>) {
        tripList = newList
        notifyDataSetChanged()
    }
}
