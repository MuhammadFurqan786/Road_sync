package com.roadsync.auth.presentation

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Patterns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.roadsync.R
import com.roadsync.admin.presentation.DashboardActivity
import com.roadsync.auth.viewmodel.AuthViewModel
import com.roadsync.databinding.FragmentLoginBinding
import com.roadsync.home.presentation.DashboardFragment
import com.roadsync.home.presentation.MainActivity
import com.roadsync.pref.PreferenceHelper
import com.roadsync.utils.GlobalUtils


class LoginFragment : Fragment() {

    private lateinit var _binding: FragmentLoginBinding
    private val binding get() = _binding

    private val viewModel: AuthViewModel by viewModels()
    private lateinit var helper: PreferenceHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        helper = PreferenceHelper.getPref(requireContext())
        setupClicks()
        initObserver()
        GlobalUtils.setupPasswordToggle(
            context = requireContext(),
            editText = binding.edPassword,
            showPasswordIcon = R.drawable.ic_show_password,
            hidePasswordIcon = R.drawable.ic_hide_password
        )
        return binding.root
    }

    private fun initObserver() {
        viewModel.authResult.observe(viewLifecycleOwner) { result ->
            result.fold(
                onSuccess = {
                    val userId = FirebaseAuth.getInstance().currentUser?.uid
                    if (userId != null) {
                        viewModel.fetchUserData(userId)
                    } else {
                        binding.loadingView.visibility = View.GONE
                        GlobalUtils.showMessage(
                            binding.root, "User not found",
                            Snackbar.LENGTH_SHORT
                        )
                    }
                }, onFailure = {
                    it.message?.let { it1 ->
                        binding.loadingView.visibility = View.GONE
                        GlobalUtils.showMessage(
                            binding.root,
                            it1,
                            Snackbar.LENGTH_SHORT
                        )
                    }
                }
            )
        }
        viewModel.userData.observe(viewLifecycleOwner) { result ->
            result.fold(
                onSuccess = { user ->
                    // Update UI with user data
                    if (user.userType == "admin") {
                        helper.saveCurrentUser(user)
                        helper.setUserLogin(true)
                        binding.loadingView.visibility = View.GONE
                        GlobalUtils.showMessage(
                            binding.root,
                            "Login Successful",
                            Snackbar.LENGTH_SHORT
                        )
                        Handler(Looper.getMainLooper()).postDelayed({
                            startActivity(Intent(requireContext(), DashboardActivity::class.java))
                            activity?.finish()
                        }, 1500)

                    } else {
                        if (user.active) {
                            helper.saveCurrentUser(user)
                            helper.setUserLogin(true)
                            binding.loadingView.visibility = View.GONE
                            GlobalUtils.showMessage(
                                binding.root,
                                "Login Successful",
                                Snackbar.LENGTH_SHORT
                            )

                            Handler(Looper.getMainLooper()).postDelayed({
                                startActivity(Intent(requireContext(), MainActivity::class.java))
                                activity?.finish()
                            }, 1500)
                        } else {
                            binding.loadingView.visibility = View.GONE
                            GlobalUtils.showMessage(
                                binding.root,
                                "User Account is not active",
                                Snackbar.LENGTH_SHORT
                            )
                        }
                    }

                },
                onFailure = { exception ->
                    binding.loadingView.visibility = View.GONE
                    Toast.makeText(
                        requireContext(),
                        "Error: ${exception.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            )
        }

    }


    private fun setupClicks() {
        binding.apply {
            loginButton.setOnClickListener {
                loginUser()
            }

            signup.setOnClickListener {
                findNavController().navigate(R.id.action_loginFragment_to_registerFragment)
            }
            forgotPassword.setOnClickListener {
                findNavController().navigate(R.id.action_loginFragment_to_forgotPasswordFragment)
            }
        }
    }

    private fun loginUser() {
        val email = binding.edEmail.text.toString()
        val password = binding.edPassword.text.toString()

        if (email.isEmpty()) {
            binding.edEmail.error = "Email address is required"
            return
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email)
                .matches()
        ) {
            binding.edEmail.error = "Invalid email"
            return
        }

        if (password.isEmpty()) {
            binding.edPassword.error = "Password is required"
            return
        }
        if (password.length < 6) {
            binding.edPassword.error = "Password must be at least 6 characters"
            return
        }

        viewModel.loginUser(email, password)
        binding.loadingView.visibility = View.VISIBLE

    }

}