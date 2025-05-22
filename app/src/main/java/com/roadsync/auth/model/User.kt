package com.roadsync.auth.model

data class User(
    val userId: String = "",
    val name: String = "",
    val email: String = "",
    val phone: String = "",
    var emergencyNumber: String = "",
    var deviceToken: String = "",
    var latitude: Double = 0.0,
    var longitude: Double = 0.0,
    var onlineStatus: Boolean = false,
    var profileImage: String = "",
    var userType: String = "",
    var active: Boolean = false
)