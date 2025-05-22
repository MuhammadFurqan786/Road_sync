package com.roadsync.home.presentation

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.ConnectivityManager
import android.os.Bundle
import android.telephony.SmsManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.AppCompatButton
import androidx.appcompat.widget.AppCompatEditText
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.roadsync.R
import com.roadsync.databinding.FragmentDashboardBinding
import com.roadsync.home.presentation.trip.ActiveTripActivity
import com.roadsync.home.presentation.trip.TripViewModel
import com.roadsync.home.presentation.trip.TripsAdapter
import com.roadsync.pref.PreferenceHelper


class DashboardFragment : Fragment(), TripsAdapter.OnTripClickListener {

    private lateinit var _binding: FragmentDashboardBinding
    private val binding get() = _binding

    private lateinit var helper :PreferenceHelper

    private val viewModel: TripViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        _binding = FragmentDashboardBinding.inflate(inflater, container, false)
        binding.loadingView.visibility = View.VISIBLE
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        helper =  PreferenceHelper.getPref(requireContext())
        if (userId != null) {
            viewModel.getActiveTripsForUser(
                userId = userId,
                onResult = { trips ->
                    // Show trips in RecyclerView
                    if (trips.isEmpty()) {
                        binding.loadingView.visibility = View.GONE
                        binding.activeTripsLayout.visibility = View.GONE
                        binding.noActiveTripLayout.visibility = View.VISIBLE
                    } else {
                        val adapter = TripsAdapter(trips, this)
                        binding.activeTrips.adapter = adapter
                        binding.activeTripsLayout.visibility = View.VISIBLE
                        binding.noActiveTripLayout.visibility = View.GONE
                        binding.loadingView.visibility = View.GONE
                    }
                },
                onError = { error ->
                    binding.noActiveTripLayout.visibility = View.GONE
                    binding.loadingView.visibility = View.GONE
                    Toast.makeText(context, error, Toast.LENGTH_SHORT).show()
                }
            )
        }

        binding.createTripButton.setOnClickListener {
            findNavController().navigate(R.id.action_dashboard_to_createTripFragment)
        }

        binding.tripHistory.setOnClickListener {
            val bottomNav = requireActivity().findViewById<BottomNavigationView>(R.id.nav_view)
            bottomNav.selectedItemId = R.id.trips
        }

        binding.settingButton.setOnClickListener {
            val bottomNav = requireActivity().findViewById<BottomNavigationView>(R.id.nav_view)
            bottomNav.selectedItemId = R.id.account
        }

        binding.sosMessageButton.setOnClickListener {
            checkSmsPermission()
        }

        checkAndShowEmergencyDialog()

        return binding.root
    }

    private fun checkSmsPermission() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.SEND_SMS)
            != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(
                requireActivity(),
                arrayOf(Manifest.permission.SEND_SMS),
                1001
            )
        } else {
            sendSosMessage()
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == 1001 && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            sendSosMessage()
        } else {
            Toast.makeText(requireContext(), "SMS permission denied", Toast.LENGTH_SHORT).show()
        }
    }


    private fun checkAndShowEmergencyDialog() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val database = FirebaseDatabase.getInstance().getReference("users/$userId/emergencyNumber")

        database.get().addOnSuccessListener { snapshot ->
            val emergencyNumber = snapshot.getValue(String::class.java)

            if (emergencyNumber.isNullOrBlank()) {
                showEmergencyDialog(database)
            }
        }
    }



    override fun onDetailsClicked(inviteCode: String) {
        val bundle = Bundle()
        bundle.putString("inviteCode", inviteCode)
        findNavController().navigate(R.id.action_dashboard_to_tripDetailsFragment, bundle)
    }

    override fun onLiveLocationClicked(tripId: String, departure: String, destination: String) {
        val intent = Intent(requireContext(), ActiveTripActivity::class.java)
        intent.putExtra("tripId", tripId)
        intent.putExtra("departure", departure)
        intent.putExtra("destination", destination)
        startActivity(intent)

    }

    private fun showEmergencyDialog(databaseRef: DatabaseReference) {
        val dialogView =
            LayoutInflater.from(requireContext()).inflate(R.layout.dialog_emergency_number, null)
        val editText = dialogView.findViewById<AppCompatEditText>(R.id.editEmergencyNumber)

        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .setCancelable(false) // prevent skipping
            .create()

        dialogView.findViewById<AppCompatButton>(R.id.btnSave).setOnClickListener {
            val number = editText.text.toString().trim()
            if (number.isEmpty()) {
                editText.error = "Please enter a number"
                return@setOnClickListener
            }

            databaseRef.setValue(number).addOnSuccessListener {
                helper.getCurrentUser()?.emergencyNumber = number
                Toast.makeText(requireContext(), "Emergency number saved", Toast.LENGTH_SHORT)
                    .show()
                dialog.dismiss()
            }.addOnFailureListener {
                Toast.makeText(requireContext(), "Failed to save", Toast.LENGTH_SHORT).show()
            }
        }
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.show()
    }

    fun isNetworkAvailable(context: Context): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        return cm.activeNetworkInfo?.isConnectedOrConnecting == true
    }



    private fun sendSosMessage() {
        val context = requireContext()
        val emergencyNumber = helper.getCurrentUser()?.emergencyNumber

        if (!emergencyNumber.isNullOrEmpty()) {
            sendSms(emergencyNumber)
        } else {
            // Try to fetch from Firebase if connected to the internet
            if (!isNetworkAvailable(context)) {
                Toast.makeText(context, "No emergency number and no internet", Toast.LENGTH_SHORT).show()
                return
            }

            val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
            val database = FirebaseDatabase.getInstance().getReference("users/$userId/emergencyNumber")

            database.get().addOnSuccessListener { snapshot ->
                val numberFromDb = snapshot.getValue(String::class.java)
                if (!numberFromDb.isNullOrEmpty()) {
                    // Save to helper and prefs
                   helper.getCurrentUser()?.emergencyNumber = numberFromDb
                    sendSms(numberFromDb)
                } else {
                    Toast.makeText(context, "No emergency number found in Firebase", Toast.LENGTH_SHORT).show()
                }
            }.addOnFailureListener {
                Toast.makeText(context, "Error fetching emergency number", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun sendSms(number: String) {
        val message = "🚨 SOS! I need help. This is an emergency. Please contact me ASAP."

        try {
            val smsManager = SmsManager.getDefault()
            smsManager.sendTextMessage(number, null, message, null, null)
            Toast.makeText(requireContext(), "SOS message sent", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Failed to send message: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }





}