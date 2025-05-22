package com.roadsync.admin.presentation

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.roadsync.databinding.FragmentTripDetailBinding
import com.roadsync.home.models.Trip
import com.roadsync.utils.GlobalUtils.toReadableDate

class TripDetailFragment : Fragment() {

    private var _binding: FragmentTripDetailBinding? = null
    private val binding get() = _binding!!

    private lateinit var trip: Trip


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        trip = arguments?.getParcelable("trip") ?: Trip()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTripDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupUI()
    }

    @SuppressLint("SetTextI18n")
    private fun setupUI() {
        binding.tripName.text = trip.tripName
        binding.departure.text = "Departure: ${trip.departure}"
        binding.destination.text = "Destination: ${trip.destination}"
        binding.inviteCode.text = "Invite Code: ${trip.inviteCode}"
        binding.timestamp.text = "Created On: ${trip.timestamp.toReadableDate()}"
        binding.cost.text = "Cost: ${trip.cost}"
        binding.stayOption.text = "Stay Option: ${trip.stayOption}"
        binding.activeStatus.text = if (trip.active) "Status: Active" else "Status: Inactive"
        binding.usersCount.text = "Participants: ${trip.users.size}"
        binding.invitedUsersCount.text = "Invited Users: ${trip.invitedUsers.size}"

        // Setup members RecyclerView
        binding.membersRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.membersRecyclerView.adapter = ParticipantAdapter(trip.users.values.toList())

        // Setup invited users RecyclerView
        binding.invitedRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.invitedRecyclerView.adapter = InvitedUserAdapter(trip.invitedUsers.values.toList())
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
