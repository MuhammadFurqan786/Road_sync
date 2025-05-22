package com.roadsync.home.presentation

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.navigation.findNavController
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.shape.CornerFamily
import com.google.android.material.shape.MaterialShapeDrawable
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.messaging.FirebaseMessaging
import com.roadsync.R
import com.roadsync.auth.viewmodel.AuthViewModel
import com.roadsync.databinding.ActivityMainBinding
import com.roadsync.home.presentation.notification.NotificationService
import com.roadsync.utils.GlobalKeys
import com.roadsync.utils.MyFirebaseMessagingService.Companion.getDeviceToken

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private val viewModel: AuthViewModel by viewModels()
    private val locationPermissions = arrayOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION
    )


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        checkAndRequestPermissions()

        val navView: BottomNavigationView = binding.navView

        val navController = findNavController(R.id.nav_host_fragment_activity_home)

        navView.setupWithNavController(navController)

        val radius = resources.getDimension(R.dimen.radius_small)
        val bottomNavigationViewBackground = navView.background as MaterialShapeDrawable
        bottomNavigationViewBackground.shapeAppearanceModel =
            bottomNavigationViewBackground.shapeAppearanceModel.toBuilder()
                .setTopRightCorner(CornerFamily.ROUNDED, radius)
                .setTopLeftCorner(CornerFamily.ROUNDED, radius)
                .build()

        navController.addOnDestinationChangedListener { controller, destination, arguments ->
            when (destination.id) {
                R.id.dashboard -> binding.navView.visibility = View.VISIBLE
                R.id.trips -> binding.navView.visibility = View.VISIBLE
                R.id.notifications -> binding.navView.visibility = View.VISIBLE
                R.id.account -> binding.navView.visibility = View.VISIBLE
                else -> binding.navView.visibility = View.GONE
            }
        }


        navView.itemIconTintList = null

        getDeviceToken(
            onSuccess = { token ->
                // Save the token to Firebase after the user is logged in or created
                saveTokenToFirebase(token)
            },
            onFailure = { exception ->
                Log.e("FCM", "Failed to retrieve token", exception)
            }
        )

        viewModel.startLocationUpdates()
        viewModel.setUserOnlineStatus(true)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val intent = Intent(this, NotificationService::class.java)
            startForegroundService(intent)
        } else {
            val intent = Intent(this, NotificationService::class.java)
            startService(intent)
        }


    }

    private fun saveTokenToFirebase(token: String) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return

        // Save token to Firebase Realtime Database (or Firestore)
        val userRef =
            FirebaseDatabase.getInstance().getReference(GlobalKeys.USER_TABLE).child(userId)
        userRef.child("deviceToken").setValue(token)
            .addOnSuccessListener {
                Log.d("FCM", "Token saved to Firebase")
            }
            .addOnFailureListener { exception ->
                Log.e("FCM", "Error saving token to Firebase", exception)
            }

        FirebaseMessaging.getInstance().subscribeToTopic(userId)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Log.d("FCM", "Subscribed to topic: $userId")
                } else {
                    Log.e("FCM", "Failed to subscribe: ${task.exception?.message}")
                }
            }


    }


    private fun checkAndRequestPermissions() {
        if (!hasLocationPermission()) {
            ActivityCompat.requestPermissions(this, locationPermissions, LOCATION_PERMISSION_CODE)
        } else {
            Toast.makeText(this, "Location permission already granted", Toast.LENGTH_SHORT).show()
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissions(
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    NOTIFICATION_PERMISSION_CODE
                )
            } else {
                Toast.makeText(this, "Notification permission already granted", Toast.LENGTH_SHORT)
                    .show()
            }
        }
    }


    override fun dispatchTouchEvent(ev: MotionEvent?): Boolean {
        if (currentFocus != null) {
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(currentFocus!!.windowToken, 0)
        }
        return super.dispatchTouchEvent(ev)
    }

    private fun hasLocationPermission(): Boolean {
        return locationPermissions.all {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        when (requestCode) {
            LOCATION_PERMISSION_CODE -> {
                if (grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                    Toast.makeText(this, "Location permission granted", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Location permission denied", Toast.LENGTH_SHORT).show()
                }
            }

            NOTIFICATION_PERMISSION_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, "Notification permission granted", Toast.LENGTH_SHORT)
                        .show()
                } else {
                    Toast.makeText(this, "Notification permission denied", Toast.LENGTH_SHORT)
                        .show()
                }
            }
        }
    }

    override fun onStop() {
        super.onStop()
        viewModel.setUserOnlineStatus(false)
    }

    companion object {
        const val LOCATION_PERMISSION_CODE = 1001
        const val NOTIFICATION_PERMISSION_CODE = 1002
    }


}