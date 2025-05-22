package com.roadsync.home.presentation.trip

import android.annotation.SuppressLint
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.AppCompatButton
import androidx.appcompat.widget.AppCompatTextView
import androidx.recyclerview.widget.RecyclerView
import com.roadsync.R
import com.roadsync.home.models.Trip

class TripsAdapter(
    private val trips: List<Trip>,
    private val listener: OnTripClickListener
) : RecyclerView.Adapter<TripsAdapter.TripViewHolder>() {

    interface OnTripClickListener {
        fun onDetailsClicked(tripId: String)
        fun onLiveLocationClicked(tripId: String, departure: String, destination: String)
    }

    class TripViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tripName: AppCompatTextView = itemView.findViewById(R.id.tripName)
        val tripRoute: AppCompatTextView = itemView.findViewById(R.id.tripRoute)
        val tripStatus: AppCompatTextView = itemView.findViewById(R.id.tripStatus)
        val detailButton: AppCompatButton = itemView.findViewById(R.id.detailButton)
        val liveLocation: AppCompatButton = itemView.findViewById(R.id.live_location)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TripViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_trip, parent, false)
        return TripViewHolder(view)
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: TripViewHolder, position: Int) {
        val trip = trips[position]
        holder.tripName.text = trip.tripName
        holder.tripRoute.text = "Route: ${trip.departure} - ${trip.destination}"
        holder.tripStatus.text = if (trip.active) "Active" else "Inactive"

        if (!trip.active) {
            holder.tripStatus.setTextColor(Color.RED)
            holder.liveLocation.visibility = View.GONE
        } else {
            holder.tripStatus.setTextColor(Color.GREEN)
            holder.liveLocation.visibility = View.VISIBLE
        }

        holder.detailButton.setOnClickListener {
            listener.onDetailsClicked(trip.inviteCode)
        }

        holder.liveLocation.setOnClickListener {
            listener.onLiveLocationClicked(trip.tripId,trip.departure, trip.destination)
        }
    }

    override fun getItemCount(): Int = trips.size
}
