package com.roadsync.auth

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.roadsync.MainActivity
import com.roadsync.R
import com.roadsync.databinding.FragmentRegisterBinding


class RegisterFragment : Fragment() {
    private lateinit var _binding: FragmentRegisterBinding
    private val binding get() = _binding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        _binding = FragmentRegisterBinding.inflate(inflater, container, false)
        setupClicks()
        return binding.root
    }

    private fun setupClicks() {
        binding.apply {
            signupButton.setOnClickListener {
                registerUser()
            }

            loginButton.setOnClickListener {
                findNavController().navigate(R.id.action_registerFragment_to_loginFragment)
            }

        }
    }

    private fun registerUser() {
        startActivity(Intent(requireContext(), MainActivity::class.java))
    }


}