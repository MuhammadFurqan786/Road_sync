package com.roadsync.home.presentation.notification

import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.roadsync.home.models.NotificationDataModel

object NotificationRepository {

    private val database = FirebaseDatabase.getInstance().reference

    fun sendNotification(userId: String, notification: NotificationDataModel) {
        val notificationId = database.child("notifications").child(userId).push().key ?: return
        database.child("notifications").child(userId).child(notificationId).setValue(notification.copy(notificationId = notificationId))
    }

    fun getAllNotifications(userId: String, onNotificationsFetched: (List<NotificationDataModel>) -> Unit) {
        val ref = database.child("notifications").child(userId)
        ref.get().addOnSuccessListener { snapshot ->
            val notifications = snapshot.children.mapNotNull { it.getValue(NotificationDataModel::class.java) }
            onNotificationsFetched(notifications)
        }
    }


    fun listenForNotifications(userId: String, onNewNotification: (NotificationDataModel) -> Unit) {
        val ref = database.child("notifications").child(userId)
        ref.addChildEventListener(object : ChildEventListener {
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                snapshot.getValue(NotificationDataModel::class.java)?.let {
                    onNewNotification(it)
                }
            }
            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {}
            override fun onChildRemoved(snapshot: DataSnapshot) {}
            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}
            override fun onCancelled(error: DatabaseError) {}
        })
    }
}
