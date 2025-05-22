package com.roadsync.auth.viewmodel

import android.annotation.SuppressLint
import android.app.Application
import android.location.Location
import android.os.Looper
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.roadsync.auth.model.User
import com.roadsync.auth.repository.AuthRepository
import com.roadsync.utils.GlobalKeys

class AuthViewModel(application: Application) : AndroidViewModel(application) {
    private val authRepository = AuthRepository()
    private val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(application)
    private val _authResult = MutableLiveData<Result<Boolean>>()
    val authResult: LiveData<Result<Boolean>> get() = _authResult

    private val _userData = MutableLiveData<Result<User>>()
    val userData: LiveData<Result<User>> get() = _userData

    private var locationCallback: LocationCallback? = null


    private val _allUsers = MutableLiveData<List<User>>()
    val allUsers: LiveData<List<User>> get() = _allUsers

    private val _updateStatus = MutableLiveData<Result<Boolean>>()
    val updateStatus: LiveData<Result<Boolean>> get() = _updateStatus



    fun fetchAllUsers() {
        val dbRef = FirebaseDatabase.getInstance().getReference("users")
        dbRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val users = mutableListOf<User>()
                for (userSnapshot in snapshot.children) {
                    val user = userSnapshot.getValue(User::class.java)
                    user?.let { users.add(it) }
                }
                _allUsers.value = users
            }

            override fun onCancelled(error: DatabaseError) {
                _allUsers.value = emptyList()
            }
        })
    }


    fun registerUser(
        email: String,
        password: String,
        name: String,
        phone: String,
        emergencyNumber: String,
        deviceToken: String,
        latitude: Double,
        longitude: Double,
        isOnline: Boolean

    ) {
        authRepository.registerUser(
            email,
            password,
            name,
            phone,
            emergencyNumber,
            deviceToken,
            latitude,
            longitude,
            isOnline,
            userType = "user",
            profileImage = "",
            isActive = true,
            onSuccess = { _authResult.value = Result.success(true) },
            onFailure = { error -> _authResult.value = Result.failure(Exception(error)) }
        )
    }

    fun loginUser(email: String, password: String) {
        authRepository.loginUser(
            email, password,
            onSuccess = { _authResult.value = Result.success(true) },
            onFailure = { error -> _authResult.value = Result.failure(Exception(error)) }
        )
    }

    fun fetchUserData(userId: String) {
        authRepository.getUserData(
            userId,
            onSuccess = { user -> _userData.value = Result.success(user) },
            onFailure = { error -> _userData.value = Result.failure(Exception(error)) }
        )
    }

    fun forgotPassword(email: String) {
        authRepository.forgotPassword(
            email,
            onSuccess = { _authResult.value = Result.success(true) },
            onFailure = { error -> _authResult.value = Result.failure(Exception(error)) }
        )
    }

    fun getUserData(userId: String) {
        val dbRef = FirebaseDatabase.getInstance().getReference("users").child(userId)
        dbRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val user = snapshot.getValue(User::class.java)
                if (user != null) {
                    _userData.value = Result.success(user)
                } else {
                    _userData.value = Result.failure(Exception("User data is null"))
                }
            }

            override fun onCancelled(error: DatabaseError) {
                _userData.value = Result.failure(error.toException())
            }
        })
    }


    private fun updateUserLocation(location: Location) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        FirebaseDatabase.getInstance().getReference(GlobalKeys.USER_TABLE).child(userId).apply {
            child("latitude").setValue(location.latitude)
            child("longitude").setValue(location.longitude)
        }
    }

    fun updateUserData(name: String, phone: String, emergencyNumber: String) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        _updateStatus.value = Result.success(false) // Trigger loading state

        val userRef = FirebaseDatabase.getInstance().getReference(GlobalKeys.USER_TABLE).child(userId)
        val updates = mapOf(
            "name" to name,
            "emergencyNumber" to emergencyNumber,
            "phone" to phone
        )

        userRef.updateChildren(updates)
            .addOnSuccessListener {
                _updateStatus.value = Result.success(true)
            }
            .addOnFailureListener { e ->
                _updateStatus.value = Result.failure(e)
            }
    }


    @SuppressLint("MissingPermission")
    fun startLocationUpdates() {
        val locationRequest = LocationRequest.create().apply {
            interval = 5000
            fastestInterval = 2000
            priority = Priority.PRIORITY_HIGH_ACCURACY
        }
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                val location = result.lastLocation ?: return
                updateUserLocation(location)
            }
        }

        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            locationCallback!!,
            Looper.getMainLooper()
        )
    }

    fun setUserOnlineStatus(isOnline: Boolean) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val userRef = FirebaseDatabase.getInstance().getReference("users/$userId")

        if (isOnline) {
            // Check if the user is connected
            val presenceRef = FirebaseDatabase.getInstance().getReference(".info/connected")
            presenceRef.addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.value == true) {
                        // Set user online status in Firebase
                        userRef.child("onlineStatus").onDisconnect()
                            .setValue(false)  // Will set to false when disconnected
                        userRef.child("onlineStatus").setValue(true)  // Set to true when connected
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    // Handle error if needed
                }
            })
        } else {
            userRef.child("onlineStatus").setValue(false)
        }
    }


    fun isUserLoggedIn(): Boolean = authRepository.isUserLoggedIn()
    fun logoutUser() = authRepository.logoutUser()
}
