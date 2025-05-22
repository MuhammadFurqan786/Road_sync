package com.roadsync.home.presentation.trip

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.google.firebase.auth.FirebaseAuth
import com.roadsync.R
import com.roadsync.databinding.FragmentInActiveTripBinding


class InActiveTripFragment : Fragment(),TripsAdapter.OnTripClickListener {
    private lateinit var _binding: FragmentInActiveTripBinding
    private val binding get() = _binding
    private val viewModel: TripViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentInActiveTripBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId != null) {
            viewModel.getInActiveTripsForUser(
                userId = userId,
                onResult = { trips ->
                    // Show trips in RecyclerView
                    if (trips.isEmpty()) {
                        binding.loadingView.visibility = View.GONE
                        binding.rvInactiveTrip.visibility = View.GONE
                        binding.noTripFound.visibility = View.VISIBLE
                    } else {
                        val adapter = TripsAdapter(trips,this)
                        binding.rvInactiveTrip.adapter = adapter
                        binding.rvInactiveTrip.visibility = View.VISIBLE
                        binding.noTripFound.visibility = View.GONE
                        binding.loadingView.visibility = View.GONE
                    }
                },
                onError = { error ->
                    binding.noTripFound.visibility = View.VISIBLE
                    binding.loadingView.visibility = View.GONE
                    binding.noTripFound.text = "Oops, Something went wrong"
                    Toast.makeText(context, error, Toast.LENGTH_SHORT).show()
                }
            )
        }



    }

    override fun onDetailsClicked(inviteCode: String) {
        val bundle = Bundle()
        bundle.putString("inviteCode", inviteCode)
        findNavController().navigate(R.id.action_trips_to_tripDetailsFragment,bundle)
    }

    override fun onLiveLocationClicked(tripId: String, departure: String, destination: String) {
        val intent  =  Intent(requireContext(), ActiveTripActivity::class.java)
        intent.putExtra("tripId", tripId)
        intent.putExtra("departure", departure)
        intent.putExtra("destination", destination)
        startActivity(intent)
    }
}