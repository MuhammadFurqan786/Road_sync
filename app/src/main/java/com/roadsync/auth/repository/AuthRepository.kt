package com.roadsync.auth.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.roadsync.auth.model.User
import com.roadsync.utils.GlobalKeys

class AuthRepository {
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val database: DatabaseReference =
        FirebaseDatabase.getInstance().getReference(GlobalKeys.USER_TABLE)

    // Register User
    fun registerUser(
        email: String,
        password: String,
        name: String,
        phone: String,
        deviceToken: String,
        emergencyNumber: String,
        latitude: Double,
        longitude: Double,
        isOnline: Boolean,
        userType: String,
        profileImage: String,
        isActive: Boolean,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val userId = auth.currentUser?.uid
                    if (userId != null) {
                        val user = User(
                            userId = userId,
                            name = name,
                            email = email,
                            phone = phone,
                            emergencyNumber = emergencyNumber,
                            deviceToken = deviceToken,
                            latitude = latitude,
                            longitude = longitude,
                            onlineStatus = isOnline,
                            profileImage = profileImage,
                            userType = userType,
                            active = isActive
                        )
                        database.child(userId).setValue(user)
                            .addOnSuccessListener { onSuccess() }
                            .addOnFailureListener { onFailure(it.message ?: "Unknown error") }
                    }
                } else {
                    onFailure(task.exception?.message ?: "Registration failed")
                }
            }
    }

    // Login User
    fun loginUser(
        email: String,
        password: String,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    onSuccess()
                } else {
                    onFailure(task.exception?.message ?: "Login failed")
                }
            }
    }

    // Forgot Password
    fun forgotPassword(
        email: String,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        auth.sendPasswordResetEmail(email)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onFailure(it.message ?: "Failed to send password reset email") }
    }


    // Fetch User Data
    fun getUserData(userId: String, onSuccess: (User) -> Unit, onFailure: (String) -> Unit) {
        database.child(userId).get()
            .addOnSuccessListener {
                if (it.exists()) {
                    val user = it.getValue(User::class.java)
                    user?.let { onSuccess(it) } ?: onFailure("User not found")
                } else {
                    onFailure("User not found")
                }
            }
            .addOnFailureListener { onFailure(it.message ?: "Failed to fetch user data") }
    }

    // Check If User is Logged In
    fun isUserLoggedIn(): Boolean {
        return auth.currentUser != null
    }

    // Logout User
    fun logoutUser() {
        auth.signOut()
    }
}
