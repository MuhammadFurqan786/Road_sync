package com.roadsync.admin.presentation

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.roadsync.R
import com.roadsync.auth.model.User
import com.roadsync.auth.viewmodel.AuthViewModel
import com.roadsync.databinding.FragmentUsersBinding
import de.hdodenhof.circleimageview.CircleImageView

class UsersFragment : Fragment() {

    private lateinit var binding: FragmentUsersBinding
    private val viewModel: AuthViewModel by viewModels()
    private lateinit var userAdapter: UserAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentUsersBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        setupRecyclerView()

        binding.progressBar.visibility = View.VISIBLE
        viewModel.fetchAllUsers()
        viewModel.allUsers.observe(viewLifecycleOwner) { users ->
            if (users.isEmpty()) {
                binding.progressBar.visibility = View.GONE
                binding.rvUsers.visibility = View.GONE
                binding.noData.visibility = View.VISIBLE

            } else {
                binding.progressBar.visibility = View.GONE
                binding.rvUsers.visibility = View.VISIBLE
                binding.noData.visibility = View.GONE
                userAdapter.updateList(users)
            }

        }
    }

    private fun setupRecyclerView() {
        userAdapter = UserAdapter(
            users = listOf(),
            onStatusChange = { user, isActive ->
                changeUserStatus(user, isActive)
            },
            onDetailsClick = { user ->
                showUserDetails(user)
            }
        )
        binding.rvUsers.apply {
            adapter = userAdapter
            layoutManager = LinearLayoutManager(requireContext())
        }
    }

    private fun changeUserStatus(user: User, isActive: Boolean) {
        val userRef = FirebaseDatabase.getInstance().getReference("users").child(user.userId)
        userRef.child("active").setValue(isActive)
    }

    private fun showUserDetails(user: User) {

        showUserDetails(requireContext(), user)

    }

    @SuppressLint("MissingInflatedId")
    private fun showUserDetails(context: Context, user: User) {
        val view = LayoutInflater.from(context).inflate(R.layout.dialog_user_details, null)
        val dialog = AlertDialog.Builder(context).setView(view).create()

        val profileImage = view.findViewById<CircleImageView>(R.id.profileImageView)
        val nameText = view.findViewById<TextView>(R.id.nameTextView)
        val emailText = view.findViewById<TextView>(R.id.emailTextView)
        val phoneText = view.findViewById<TextView>(R.id.phoneTextView)
        val emergencyText = view.findViewById<TextView>(R.id.emergencyTextView)
        val userTypeText = view.findViewById<TextView>(R.id.userTypeTextView)
        val statusText = view.findViewById<TextView>(R.id.statusTextView)
        val closeBtn = view.findViewById<Button>(R.id.closeButton)

        // Set text
        nameText.text = user.name
        emailText.text = user.email
        phoneText.text = "📞 Phone: ${user.phone}"
        emergencyText.text = "🚨 Emergency: ${user.emergencyNumber}"
        userTypeText.text = "🧑‍💼 Type: ${user.userType}"
        statusText.text = if (user.active) "✅ Status: Active" else "❌ Status: Inactive"

        // Load image (if using Glide or Coil)
        if (user.profileImage.isNotEmpty()) {
            Glide.with(context)
                .load(user.profileImage)
                .placeholder(R.drawable.user_image)
                .into(profileImage)
        }

        closeBtn.setOnClickListener { dialog.dismiss() }

        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.show()
    }

}
