package com.roadsync.home.presentation.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.roadsync.R
import com.roadsync.home.models.NotificationDataModel


class NotificationService : Service() {

    private val database = FirebaseDatabase.getInstance().reference
    private var userId: String? = null
    private var notificationListener: ChildEventListener? = null

    override fun onCreate() {
        super.onCreate()
        startForegroundService()
        startListeningForNotifications()
    }

    private fun startForegroundService() {
        val channelId = "notification_service_channel"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Notification Service Channel",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("RoadSync")
            .setContentText("Listening for new notifications...")
            .setSmallIcon(R.drawable.ic_notification) // Replace with your app's notification icon
            .build()

        startForeground(1, notification)
    }

    private fun startListeningForNotifications() {
        userId = getCurrentUserId() // 👈 Get current logged-in user's ID

        userId?.let { uid ->
            val ref = database.child("notifications").child(uid)

            notificationListener = object : ChildEventListener {
                override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                    snapshot.getValue(NotificationDataModel::class.java)?.let { notificationModel ->
                        showNotification(notificationModel)
                    }
                }

                override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {}
                override fun onChildRemoved(snapshot: DataSnapshot) {}
                override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}
                override fun onCancelled(error: DatabaseError) {}
            }

            ref.addChildEventListener(notificationListener as ChildEventListener)
        }
    }

    private fun showNotification(notificationModel: NotificationDataModel) {
        // Using helper class to create notification
        NotificationUtils.showNotification(
            context = this,
            title = notificationModel.title,
            message = notificationModel.body
        )
    }

    override fun onDestroy() {
        super.onDestroy()
        // Clean up listener
        userId?.let {
            database.child("notifications").child(it)
                .removeEventListener(notificationListener ?: return)
        }
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    private fun getCurrentUserId(): String? {
        return FirebaseAuth.getInstance().currentUser?.uid
    }
}

