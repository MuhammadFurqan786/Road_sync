package com.roadsync.auth.presentation

import android.location.Location
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.roadsync.auth.model.InvitedUser
import com.roadsync.auth.model.User
import com.roadsync.databinding.DialogUserListBinding
import com.roadsync.home.models.NotificationDataModel
import com.roadsync.home.presentation.notification.NotificationViewModel
import com.roadsync.utils.GlobalKeys

class InviteUserDialogFragment : DialogFragment() {

    private lateinit var binding: DialogUserListBinding
    private val usersInRange = mutableListOf<User>()
    private lateinit var adapter: UsersAdapter
    private val viewModel: NotificationViewModel by viewModels()

    private var currentLat = 0.0
    private var currentLon = 0.0
    private lateinit var tripId: String
    private lateinit var inviteCode: String
    private lateinit var invitedUserIds: Set<String>

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = DialogUserListBinding.inflate(inflater, container, false)

        arguments?.let {
            currentLat = it.getDouble("currentLat")
            currentLon = it.getDouble("currentLon")
            tripId = it.getString("tripId", "")
            inviteCode = it.getString("inviteCode", "")
        }

        adapter = UsersAdapter(
            usersInRange,
            onInviteClick = { user -> onInviteClicked(user) },
            onSendNotificationClick = { user -> sendInviteNotification(user) }
        )
        binding.rvUsers.layoutManager = LinearLayoutManager(context)
        binding.rvUsers.adapter = adapter

        fetchAlreadyInvitedUsers()

        return binding.root
    }

    private fun fetchAlreadyInvitedUsers() {
        val tripRef =
            FirebaseDatabase.getInstance().getReference("trips").child(tripId).child("invitedUsers")
        tripRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                invitedUserIds = snapshot.children.mapNotNull { it.key }.toSet()
                fetchUsersAndCheckDistance(currentLat, currentLon)
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(context, "Failed to load invited users", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun fetchUsersAndCheckDistance(currentLat: Double, currentLon: Double) {
        val usersRef = FirebaseDatabase.getInstance().getReference("users")
        usersRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                usersInRange.clear()
                for (userSnapshot in snapshot.children) {
                    val user = userSnapshot.getValue(User::class.java)
                    user?.let {
                        val userLat = user.latitude
                        val userLon = user.longitude
                        val distance = calculateDistance(currentLat, currentLon, userLat, userLon)
                        if (distance <= 30000) {  // 1 KM
                            if (user.userId != FirebaseAuth.getInstance().currentUser?.uid) {
                                val isInvited = invitedUserIds.contains(user.userId)
                                if (!isInvited) {
                                    usersInRange.add(user)
                                }
                            }
                        }
                    }
                }
                adapter.notifyDataSetChanged()
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(context, "Failed to fetch users", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Float {
        val results = FloatArray(1)
        Location.distanceBetween(lat1, lon1, lat2, lon2, results)
        return results[0]
    }

    private fun onInviteClicked(user: User) {

        val tripRef = FirebaseDatabase.getInstance().getReference("trips").child(tripId)
        val invitedUser = InvitedUser(
            userId = user.userId,
            name = user.name,
            latitude = user.latitude,
            longitude = user.longitude,
            key = "invited"
        )
        tripRef.child("invitedUsers").child(user.userId).setValue(invitedUser)
            .addOnSuccessListener {
                Toast.makeText(context, "Invitation sent", Toast.LENGTH_SHORT).show()
                sendInviteNotification(user)
                adapter.notifyDataSetChanged()

            }
            .addOnFailureListener {
                Toast.makeText(context, "Failed to send invitation", Toast.LENGTH_SHORT).show()
            }
    }

    private fun sendInviteNotification(user: User) {

        val testNotification = NotificationDataModel(
            type = GlobalKeys.INVITE,
            tripId = tripId,
            title = "Trip Invite Notification",
            body = "You've been invited to a trip! Join trip using $inviteCode",
            inviteCode = inviteCode,
            timestamp = System.currentTimeMillis()
        )
        viewModel.sendNotification(user.userId, testNotification)
    }


}
