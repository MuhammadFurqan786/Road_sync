package com.roadsync.home.presentation.profile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.roadsync.auth.model.User
import com.roadsync.auth.viewmodel.AuthViewModel
import com.roadsync.databinding.FragmentProfileBinding
import com.roadsync.pref.PreferenceHelper


class ProfileFragment : Fragment() {

    private lateinit var _binding: FragmentProfileBinding
    private val binding get() = _binding

    private val viewModel: AuthViewModel by viewModels()

    private lateinit var helper: PreferenceHelper


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        _binding = FragmentProfileBinding.inflate(layoutInflater, container, false)
        helper = PreferenceHelper.getPref(requireContext())
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val userId = helper.getCurrentUser()?.userId
        if (userId != null) {
            binding.loadingView.visibility = View.VISIBLE
            viewModel.getUserData(userId)
        }

        binding.updateButton.setOnClickListener {
            val name =
                binding.edFirstName.text.toString() + " " + binding.edLastName.text.toString()
            val phone = binding.edPhone.text.toString()
            val emergencyNumber = binding.edEmergencyContact.text.toString()

            if (name.isNotEmpty() && phone.isNotEmpty() && emergencyNumber.isNotEmpty()) {
                binding.loadingView.visibility = View.VISIBLE
                viewModel.updateUserData(name, phone, emergencyNumber)
            } else {
                Toast.makeText(requireContext(), "Please fill all the fields", Toast.LENGTH_SHORT)
                    .show()
            }

        }
        binding.cancelButton.setOnClickListener {
            findNavController().navigateUp()
        }

        initObserver()
    }

    private fun initObserver() {
        viewModel.userData.observe(viewLifecycleOwner) { result ->
            result.onSuccess { user ->
                binding.loadingView.visibility = View.GONE
                showData(user)
            }.onFailure { exception ->
                binding.loadingView.visibility = View.GONE
                Toast.makeText(
                    requireContext(),
                    "Failed to load user: ${exception.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        viewModel.updateStatus.observe(viewLifecycleOwner) { result ->
            result.onSuccess { success ->
                if (success) {
                    // Hide loader
                    binding.loadingView.visibility = View.GONE
                    Toast.makeText(requireContext(), "Update successful", Toast.LENGTH_SHORT).show()
                    findNavController().navigateUp()
                } else {
                    // Show loader (initial state)
                    binding.loadingView.visibility = View.VISIBLE
                }
            }.onFailure { exception ->
                binding.loadingView.visibility = View.GONE
                Toast.makeText(
                    requireContext(),
                    "Update failed: ${exception.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }


    }

    private fun showData(user: User) {
        val nameParts = user.name.trim().split("\\s+".toRegex())
        val firstName = nameParts.firstOrNull() ?: ""
        val lastName = if (nameParts.size > 1) {
            nameParts.subList(1, nameParts.size).joinToString(" ")
        } else {
            ""
        }
        binding.edFirstName.setText(firstName)
        binding.edLastName.setText(lastName)
        binding.edPhone.setText(user.phone)
        binding.edEmail.setText(user.email)
        binding.edEmergencyContact.setText(user.emergencyNumber)
        val isActive = user.active
        if (isActive) {
            binding.edStatus.setText("Active")
        } else {
            binding.edStatus.setText("In Active")
        }
    }


}