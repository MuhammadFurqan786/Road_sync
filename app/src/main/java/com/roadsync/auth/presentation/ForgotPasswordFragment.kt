package com.roadsync.auth.presentation

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.google.android.material.snackbar.Snackbar
import com.roadsync.auth.viewmodel.AuthViewModel
import com.roadsync.databinding.FragmentForgotPasswordBinding
import com.roadsync.utils.GlobalUtils

class ForgotPasswordFragment : Fragment() {
    private lateinit var _binding: FragmentForgotPasswordBinding
    private val binding get() = _binding

    private val viewModel: AuthViewModel by viewModels()

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
        initObserver()

        return binding.root
    }

    private fun initObserver() {

        viewModel.authResult.observe(viewLifecycleOwner) { result ->
            result.fold(
                onSuccess = {
                    binding.loadingView.visibility = View.GONE
                    GlobalUtils.showMessage(
                        binding.root,
                        "Reset link sent to email",
                        Snackbar.LENGTH_SHORT
                    )
                    Handler(Looper.getMainLooper()).postDelayed({
                        findNavController().navigateUp()
                    }, 1500)
                },
                onFailure = { exception ->
                    // Handle error
                    binding.loadingView.visibility = View.GONE
                    GlobalUtils.showMessage(
                        binding.root,
                        "Error: ${exception.message}",
                        Snackbar.LENGTH_SHORT
                    )
                }
            )
        }

    }

    private fun setupClicks() {
        binding.apply {
            back.setOnClickListener {
                findNavController().navigateUp()
            }
            sendButton.setOnClickListener {
                sendResetLink()
            }
        }
    }

    private fun sendResetLink() {
        val email = binding.edEmail.text.toString()
        if (email.isEmpty()) {
            binding.edEmail.error = "Email address is required"
            return
        }
        viewModel.forgotPassword(email)
        binding.loadingView.visibility = View.VISIBLE
    }
}