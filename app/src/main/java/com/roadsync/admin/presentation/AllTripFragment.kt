package com.roadsync.admin.presentation

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.roadsync.R
import com.roadsync.databinding.FragmentAllTripBinding
import com.roadsync.home.models.Trip

class AllTripFragment : Fragment() {

    private lateinit var binding: FragmentAllTripBinding
    private lateinit var tripAdapter: TripAdapter
    private val tripViewModel: AllTripViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentAllTripBinding.inflate(inflater, container, false)

        setupRecyclerView()
        binding.progressBar.visibility = View.VISIBLE
        tripViewModel.fetchAllTrips()

        tripViewModel.trips.observe(viewLifecycleOwner) { trips ->
            if (trips.isEmpty()) {
                binding.rvTrips.visibility = View.GONE
                binding.progressBar.visibility = View.GONE
                binding.noData.visibility = View.VISIBLE
            } else {
                binding.rvTrips.visibility = View.VISIBLE
                binding.progressBar.visibility = View.GONE
                tripAdapter.updateList(trips)
                binding.noData.visibility = View.GONE
            }

        }

        return binding.root
    }

    private fun setupRecyclerView() {
        tripAdapter = TripAdapter(
            tripList = listOf(),
            onDeleteClick = { trip ->
                tripViewModel.deleteTrip(trip.tripId)
            },
            onDetailClick = { trip ->
                showTripDetailsDialog(trip)
            }
        )

        binding.rvTrips.apply {
            adapter = tripAdapter
            layoutManager = LinearLayoutManager(requireContext())
        }
    }

    private fun showTripDetailsDialog(trip: Trip) {
        val bundle = Bundle().apply {
            putParcelable("trip", trip)
        }
        findNavController().navigate(R.id.action_nav_trips_to_tripDetailFragment, bundle)

    }
//        AlertDialog.Builder(requireContext())
//            .setTitle(trip.tripName)
//            .setMessage(
//                "From: ${trip.departure}\n" +
//                        "To: ${trip.destination}\n" +
//                        "Cost: ${trip.cost}\n" +
//                        "Stay Option: ${trip.stayOption}\n" +
//                        "Invite Code: ${trip.inviteCode}\n" +
//                        "Active: ${trip.isActive}"
//            )
//            .setPositiveButton("OK", null)
//            .show()
//    }
}
