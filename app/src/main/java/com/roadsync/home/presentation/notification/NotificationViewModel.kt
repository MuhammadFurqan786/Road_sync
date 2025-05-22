package com.roadsync.home.presentation.notification

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.roadsync.home.models.NotificationDataModel

class NotificationViewModel : ViewModel() {

    private val _notifications = MutableLiveData<List<NotificationDataModel>>()
    val notifications: LiveData<List<NotificationDataModel>> get() = _notifications

    private val notificationList = mutableListOf<NotificationDataModel>()

    fun startListening(userId: String) {
        // Clear existing notifications when starting the listener to avoid duplication
        notificationList.clear()

        // Fetch existing notifications from the database
        NotificationRepository.getAllNotifications(userId) { existingNotifications ->
            Log.d("NotificationViewModel", "Fetched ${existingNotifications.size} notifications")
            notificationList.addAll(existingNotifications) // Add all fetched notifications
            _notifications.value = notificationList.toList() // Update LiveData
        }

        // Listen for new notifications in real-time
        NotificationRepository.listenForNotifications(userId) { notification ->
            // Check if the notification already exists in the list to avoid duplication
            if (!notificationList.contains(notification)) {
                Log.d("NotificationViewModel", "New notification added: ${notification.notificationId}")
                notificationList.add(0, notification) // Add new notification at top
                _notifications.value = notificationList.toList() // Update LiveData
            }
        }
    }

    fun sendNotification(toUserId: String, notification: NotificationDataModel) {
        NotificationRepository.sendNotification(toUserId, notification)
    }
}


