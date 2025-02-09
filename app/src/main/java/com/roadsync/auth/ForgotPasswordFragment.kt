package com.roadsync.auth

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.roadsync.databinding.FragmentForgotPasswordBinding

class ForgotPasswordFragment : Fragment() {
    private lateinit var _binding: FragmentForgotPasswordBinding
    private val binding get() = _binding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        _binding = FragmentForgotPasswordBinding.inflate(layoutInflater)

        setupClicks()

        return binding.root
    }

    private fun setupClicks() {
        binding.apply {
            back.setOnClickListener {
                findNavController().navigateUp()
            }
            sendButton.setOnClickListener {
                findNavController().navigateUp()
            }
        }
    }
}