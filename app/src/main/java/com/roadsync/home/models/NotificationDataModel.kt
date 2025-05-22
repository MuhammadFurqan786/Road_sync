package com.roadsync.home.models

data class NotificationDataModel(
    val notificationId: String = "",
    val title: String = "",
    val body: String = "",
    val type: String = "",
    val tripId: String? = null,
    val inviteCode: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)