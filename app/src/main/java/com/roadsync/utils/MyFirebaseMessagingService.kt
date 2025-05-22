package com.roadsync.utils

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.roadsync.R

class MyFirebaseMessagingService : FirebaseMessagingService() {

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel() // 👈 Very important
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d("FCM", "New Token: $token")
        // Here you can save the token to your server if needed
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        // Handle notification (UI) payload
        remoteMessage.notification?.let {
            showNotification(it.title, it.body)
        }

        // Handle data payload manually
        if (remoteMessage.data.isNotEmpty()) {
            val title = remoteMessage.data["title"]
            val message = remoteMessage.data["message"]
            showNotification(title, message)
        }
    }


    private fun showNotification(title: String?, body: String?) {
        val builder =
            NotificationCompat.Builder(this, "default_channel_id") // 👈 Use your channel id
                .setSmallIcon(R.drawable.ic_notification) // Make sure you have this icon
                .setContentTitle(title ?: "RoadSync")
                .setContentText(body ?: "You have a new notification")
                .setPriority(NotificationCompat.PRIORITY_HIGH) // High priority for heads-up notifications
                .setAutoCancel(true)

        val notificationManager = NotificationManagerCompat.from(this)

        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED || Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU
        ) {
            notificationManager.notify(System.currentTimeMillis().toInt(), builder.build())
        } else {
            Log.e("FCM", "No POST_NOTIFICATIONS permission")
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "default_channel_id",   // 👈 Same as in Builder
                "RoadSync Notifications",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Channel for RoadSync notifications"
            }
            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    companion object {
        fun getDeviceToken(onSuccess: (String) -> Unit, onFailure: (Exception) -> Unit) {
            FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    task.result?.let {
                        onSuccess(it)
                    }
                } else {
                    onFailure(task.exception ?: Exception("Token retrieval failed"))
                }
            }
        }
    }
}
