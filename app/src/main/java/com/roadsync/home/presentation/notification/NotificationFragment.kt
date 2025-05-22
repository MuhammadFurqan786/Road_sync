package com.roadsync.home.presentation.notification

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.AppCompatEditText
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.FirebaseAuth
import com.roadsync.R
import com.roadsync.auth.model.User
import com.roadsync.databinding.FragmentNotificationBinding
import com.roadsync.home.presentation.trip.TripViewModel
import com.roadsync.pref.PreferenceHelper


class NotificationFragment : Fragment() {

    private lateinit var _binding: FragmentNotificationBinding
    private val binding get() = _binding

    private val viewModel: NotificationViewModel by viewModels()
    private val tripViewModel: TripViewModel by viewModels()
    private lateinit var notificationsAdapter: NotificationAdapter
    private lateinit var userId: String
    private lateinit var helper: PreferenceHelper
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        _binding = FragmentNotificationBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        helper = PreferenceHelper.getPref(requireContext())


        notificationsAdapter = NotificationAdapter { notification ->
            Toast.makeText(requireContext()
            , notification.body, Toast.LENGTH_SHORT).show()
            val user = helper.getCurrentUser()
            if (user != null) {
                notification.inviteCode?.let { showJoinTripDialog(it, user) }
            }
        }
        binding.notificationRecyclerView.layoutManager = LinearLayoutManager(context)
        binding.notificationRecyclerView.adapter = notificationsAdapter

        userId = FirebaseAuth.getInstance().currentUser?.uid.toString()

        viewModel.startListening(userId)

        viewModel.notifications.observe(viewLifecycleOwner, Observer { notifications ->
            // Update RecyclerView with new notifications
            notificationsAdapter.submitList(notifications)
        })


    }

    @SuppressLint("MissingInflatedId")
    private fun showJoinTripDialog(inviteCode: String, user: User) {
        val dialogView =
            LayoutInflater.from(requireContext()).inflate(R.layout.dialog_join_trip, null)
        val inputField = dialogView.findViewById<AppCompatEditText>(R.id.inviteCodeInput)
        val inputFieldPickup = dialogView.findViewById<AppCompatEditText>(R.id.edPickUp)
        inputField.setText(inviteCode)
        Toast.makeText(requireContext()
            , inviteCode, Toast.LENGTH_SHORT).show()
        AlertDialog.Builder(requireContext())
            .setTitle("Join Trip")
            .setView(dialogView)
            .setPositiveButton("Join") { dialog, _ ->
                val inviteCode = inputField.text.toString().trim()
                val pickupLocation = inputFieldPickup.text.toString().trim()
                if (inviteCode.isNotEmpty() || pickupLocation.isNotEmpty()) {
                    checkAndJoinTrip(inviteCode, user,pickupLocation)
                } else {
                    Toast.makeText(requireContext(), "Please enter all fields", Toast.LENGTH_SHORT)
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