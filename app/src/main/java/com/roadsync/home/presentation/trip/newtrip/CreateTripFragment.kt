package com.roadsync.home.presentation.trip.newtrip

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.AppCompatEditText
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.google.firebase.database.FirebaseDatabase
import com.roadsync.R
import com.roadsync.auth.model.User
import com.roadsync.databinding.FragmentCreateTripBinding
import com.roadsync.home.presentation.trip.TripViewModel
import com.roadsync.pref.PreferenceHelper


class CreateTripFragment : Fragment() {

    private lateinit var _binding: FragmentCreateTripBinding
    private val binding get() = _binding
    private lateinit var tripViewModel: TripViewModel
    private lateinit var helper: PreferenceHelper


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        _binding = FragmentCreateTripBinding.inflate(layoutInflater, container, false)
        tripViewModel = ViewModelProvider(this)[TripViewModel::class.java]
        helper = PreferenceHelper.getPref(requireContext())
        binding.createTripButton.setOnClickListener {
            createTrip()
        }
        binding.joinTripButton.setOnClickListener {
            val user = helper.getCurrentUser()
            if (user != null) {
                showJoinTripDialog(user)
            } else {
                Toast.makeText(requireContext(), "User not logged in", Toast.LENGTH_SHORT).show()
            }
        }

        binding.back.setOnClickListener {
            findNavController().navigateUp()
        }

        initObserver()
        return binding.root

    }

    private fun initObserver() {
        tripViewModel.tripStatus.observe(viewLifecycleOwner, Observer { status ->
            Toast.makeText(requireContext(), status, Toast.LENGTH_SHORT).show()
        })


    }

    private fun createTrip() {
        val tripName = binding.edTripName.text.toString()
        val departure = binding.edDeparture.text.toString()
        val destination = binding.destination.text.toString()
        val pickUpPoint = binding.edPickUp.text.toString()
        val stayOption = binding.edStay.text.toString()

        if (tripName.isEmpty() || departure.isEmpty() || destination.isEmpty() || pickUpPoint.isEmpty() || stayOption.isEmpty()) {
            Toast.makeText(requireContext(), "Please fill all fields", Toast.LENGTH_SHORT).show()
            return
        }
        binding.loadingView.visibility = View.VISIBLE

        val user = helper.getCurrentUser()

        if (user != null) {
            tripViewModel.createTrip(
                tripName = tripName,
                departure = departure,
                destination = destination,
                user = user,
                pickup = pickUpPoint,
                stayOption = stayOption,
                onCreated = { inviteCode ->
                    // ✅ Trip created successfully
                    Toast.makeText(requireContext(), "Trip created", Toast.LENGTH_LONG).show()

                    binding.edTripName.text?.clear()
                    binding.edDeparture.text?.clear()
                    binding.destination.text?.clear()

                    val bundle = Bundle()
                    bundle.putString("inviteCode", inviteCode)
                    binding.loadingView.visibility = View.GONE
                    findNavController().navigate(
                        R.id.action_createTripFragment_to_tripDetailsFragment,
                        bundle
                    )

                },
                onError = { errorMessage ->
                    // ❌ Something went wrong
                    binding.loadingView.visibility = View.GONE
                    Toast.makeText(requireContext(), "Failed: $errorMessage", Toast.LENGTH_LONG)
                        .show()
                }
            )
        }
    }

    private fun showJoinTripDialog(user: User) {
        val dialogView =
            LayoutInflater.from(requireContext()).inflate(R.layout.dialog_join_trip, null)
        val inputField = dialogView.findViewById<AppCompatEditText>(R.id.inviteCodeInput)
        val inputFieldPickup = dialogView.findViewById<AppCompatEditText>(R.id.edPickUp)

        AlertDialog.Builder(requireContext())
            .setTitle("Join Trip")
            .setView(dialogView)
            .setPositiveButton("Join") { dialog, _ ->
                val inviteCode = inputField.text.toString().trim()
                val pickUpLocation = inputFieldPickup.text.toString().trim()
                if (inviteCode.isNotEmpty() || pickUpLocation.isNotEmpty()) {
                    binding.loadingView.visibility = View.VISIBLE
                    checkAndJoinTrip(inviteCode, user,pickUpLocation)
                } else {
                    binding.loadingView.visibility = View.GONE
                    Toast.makeText(requireContext(), "Please enter invite code", Toast.LENGTH_SHORT)
                        .show()
                }
                dialog.dismiss()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun checkAndJoinTrip(inviteCode: String, user: User, pickupLocation: String) {
        // Trigger check (realtime fetch)

        tripViewModel.getTripByInviteCode(inviteCode)

        tripViewModel.tripData.observe(viewLifecycleOwner) { snapshot ->
            val userAlreadyJoined = snapshot?.child("users")?.hasChild(user.userId) == true
            if (userAlreadyJoined) {
                Toast.makeText(requireContext(), "You already joined this trip", Toast.LENGTH_SHORT)
                    .show()
            } else {
                tripViewModel.joinTripByInvite(inviteCode, user, pickupLocation)
            }
        }
    }


}