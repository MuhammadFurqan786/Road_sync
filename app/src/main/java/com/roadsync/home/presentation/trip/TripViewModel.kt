package com.roadsync.home.presentation.trip

import android.annotation.SuppressLint
import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.location.Location
import android.os.Build
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.GenericTypeIndicator
import com.google.firebase.database.ServerValue
import com.google.firebase.database.ValueEventListener
import com.roadsync.R
import com.roadsync.auth.model.InvitedUser
import com.roadsync.auth.model.Participant
import com.roadsync.auth.model.User
import com.roadsync.home.models.NotificationDataModel
import com.roadsync.home.models.Trip

class TripViewModel(application: Application) : AndroidViewModel(application) {

    private val context = application.applicationContext
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(application)
    private val db = FirebaseDatabase.getInstance().reference

    private val _tripData = MutableLiveData<DataSnapshot?>()
    val tripData: LiveData<DataSnapshot?> = _tripData

    private val _userLocation = MutableLiveData<Location>()
    val userLocation: LiveData<Location> = _userLocation

    private val _tripStatus = MutableLiveData<String>()
    val tripStatus: LiveData<String> = _tripStatus

    private var locationCallback: LocationCallback? = null
    private var geoCenter: LatLng? = null
    private val geofenceRadius = 1000 // meters
    var tripId: String? = null
    private var lastNotificationTime: Long = 0


    // Map to store markers for each user
    private val userMarkers = mutableMapOf<String, Marker?>()

    // Reference to the GoogleMap instance
    var googleMap: GoogleMap? = null

    fun clearTripStatus() {
        _tripStatus.value = ""
    }


    // =============== CREATE TRIP ==================
    fun createTrip(
        tripName: String,
        departure: String,
        destination: String,
        pickup: String,
        stayOption: String,
        user: User,
        onCreated: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        generateUniqueInviteCode { inviteCode ->
            val newTripId = db.child("trips").push().key
            if (newTripId == null) {
                onError("Failed to generate trip ID")
                _tripStatus.postValue("Failed to generate trip ID")
                return@generateUniqueInviteCode
            }

            val userData = mapOf(
                "name" to user.name,
                "userId" to user.userId,
                "lat" to 0.0,
                "lon" to 0.0,
                "pickup" to pickup,
                "role" to "creator",
                "isOutsideGeofence" to false
            )

            val tripData = mapOf(
                "tripId" to newTripId,
                "tripName" to tripName,
                "departure" to departure,
                "destination" to destination,
                "inviteCode" to inviteCode,
                "timestamp" to ServerValue.TIMESTAMP,
                "cost" to "",
                "stayOption" to stayOption,
                "active" to false,
                "users" to mapOf(user.userId to userData),
                "invitedUsers" to emptyMap<String, InvitedUser>()
            )

            db.child("trips").child(newTripId).setValue(tripData)
                .addOnSuccessListener {
                    tripId = newTripId
                    listenToTrip(newTripId)
                    _tripStatus.postValue("Trip created successfully")
                    onCreated(inviteCode)
                }
                .addOnFailureListener { e ->
                    _tripStatus.postValue("Trip creation failed: ${e.message}")
                    onError(e.message ?: "Unknown error")
                }
        }
    }


    // =============== JOIN TRIP USING INVITE CODE ==================
    fun joinTripByInvite(inviteCode: String, user: User, pickup: String) {
        db.child("trips").orderByChild("inviteCode").equalTo(inviteCode)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.childrenCount > 0) {
                        val tripSnapshot = snapshot.children.first()
                        val id = tripSnapshot.key ?: return

                        // Check if the user is already in the trip
                        val usersSnapshot = tripSnapshot.child("users")
                        val alreadyJoined = usersSnapshot.hasChild(user.userId)

                        if (alreadyJoined) {
                            // User is already part of the trip
                            _tripStatus.postValue("You have already joined this trip.")
                        } else {
                            // User is not part of the trip, join the trip
                            joinTrip(id, user, pickup)
                        }
                    } else {
                        // Invalid invite code
                        _tripStatus.postValue("Invalid invite code.")
                    }
                }
                override fun onCancelled(error: DatabaseError) {
                    _tripStatus.postValue("Error: ${error.message}")
                }
            })
    }

    private fun joinTrip(id: String, user: User, pickup: String) {
        val userData = mapOf(
            "name" to user.name,
            "userId" to user.userId,
            "lat" to 0.0,
            "lon" to 0.0,
            "pickup" to pickup,
            "role" to "member",
            "isOutsideGeofence" to false
        )

        db.child("trips").child(id).child("users").child(user.userId).setValue(userData)
            .addOnSuccessListener {
                tripId = id
                listenToTrip(id)
                _tripStatus.postValue("Successfully joined the trip!")
            }
            .addOnFailureListener { error ->
                _tripStatus.postValue("Error: ${error.message}")
            }
    }


    // =============== START TRIP ==================

    fun startTrip() {
        tripId?.let {
            db.child("trips").child(it).child("active").setValue(true)
                .addOnSuccessListener {
                    _tripStatus.postValue("Trip started successfully.")
                }
                .addOnFailureListener { error ->
                    _tripStatus.postValue("Failed to start trip: ${error.message}")
                }
        }

    }

    fun endTrip() {
        tripId?.let {
            db.child("trips").child(it).child("active").setValue(false)
                .addOnSuccessListener {
                    _tripStatus.postValue("Trip Ended successfully.")
                }
                .addOnFailureListener { error ->
                    _tripStatus.postValue("Failed to end trip: ${error.message}")
                }
        }

    }


    // =============== LISTEN TO TRIP ==================
    fun listenToTrip(id: String) {
        db.child("trips").child(id).addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                _tripData.postValue(snapshot)

                val usersSnap = snapshot.child("users")
                if (geoCenter == null && usersSnap.exists()) {
                    usersSnap.children.firstOrNull()?.let { user ->
                        val lat = user.child("lat").getValue(Double::class.java) ?: 0.0
                        val lon = user.child("lon").getValue(Double::class.java) ?: 0.0
                        geoCenter = LatLng(lat, lon)
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                _tripStatus.postValue(error.toString())
            }
        })
    }


    fun getTripByInviteCode(inviteCode: String) {
        db.child("trips").orderByChild("inviteCode").equalTo(inviteCode)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        val tripSnapshot = snapshot.children.first()
                        _tripData.postValue(tripSnapshot)
                    } else {
                        _tripStatus.postValue("Trip not found")
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    _tripStatus.postValue("Error: ${error.message}")
                }
            })
    }


    // =============== LOCATION TRACKING ==================
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
                _userLocation.postValue(location)
                updateUserLocation(location)
                checkGeofence(location)
                checkDynamicGeofence(location)
            }
        }


        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            locationCallback!!,
            Looper.getMainLooper()
        )
    }

    fun stopLocationUpdates() {
        locationCallback?.let {
            fusedLocationClient.removeLocationUpdates(it)
        }
    }

    private fun updateUserLocation(location: Location) {
        val userId = auth.currentUser?.uid ?: return
        tripId?.let {
            db.child("trips").child(it).child("users").child(userId).apply {
                child("lat").setValue(location.latitude)
                child("lon").setValue(location.longitude)
            }
        }
    }


    private fun checkGeofence(location: Location) {
        val userId = auth.currentUser?.uid ?: return
        geoCenter?.let {
            val distance = FloatArray(1)
            Location.distanceBetween(
                location.latitude, location.longitude,
                it.latitude, it.longitude,
                distance
            )

            val isOutside = distance[0] > geofenceRadius
            tripId?.let { trip ->
                db.child("trips").child(trip).child("users")
                    .child(userId).child("isOutsideGeofence").setValue(isOutside)
            }
        }
    }

    // =============== GENERATE UNIQUE INVITE CODE ==================
    private fun generateUniqueInviteCode(callback: (String) -> Unit) {
        val code = generateCode()
        db.child("trips").orderByChild("inviteCode").equalTo(code)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        generateUniqueInviteCode(callback) // retry
                    } else {
                        callback(code)
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    _tripStatus.postValue(error.toString())
                }
            })
    }

    private fun generateCode(): String {
        val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
        return (1..6).map { chars.random() }.joinToString("")
    }

    // Method to get all users in a trip
    fun getAllUsersInTrip(tripId: String, onUsersFetched: (Map<String, Participant>) -> Unit) {
        db.child("trips").child(tripId).child("users")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val users = mutableMapOf<String, Participant>()
                    for (userSnapshot in snapshot.children) {
                        val userId = userSnapshot.key ?: continue
                        val userName = userSnapshot.child("name").getValue(String::class.java)
                        val userLat = userSnapshot.child("lat").getValue(Double::class.java) ?: 0.0
                        val userLon = userSnapshot.child("lon").getValue(Double::class.java) ?: 0.0
                        val userPickup = userSnapshot.child("pickup").getValue(String::class.java)
                        val isOutsideGeofence = userSnapshot.child("isOutsideGeofence")
                            .getValue(Boolean::class.java) ?: false
                        val userRole =
                            userSnapshot.child("role").getValue(String::class.java) ?: "member"

                        val userData =
                            Participant(
                                userId,
                                userName.toString(),
                                userLat.toDouble(),
                                userLon.toDouble(),
                                pickup = userPickup.toString(),
                                userRole,
                                isOutsideGeofence
                            )
                        users[userId] = userData
                    }

                    onUsersFetched(users) // Return the list of users
                }

                override fun onCancelled(error: DatabaseError) {
                    // Handle any errors here
                }
            })
    }


    private fun checkDynamicGeofence(location: Location) {
        val userId = auth.currentUser?.uid ?: return
        val tripKey = tripId ?: return

        db.child("trips").child(tripKey).child("users")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val currentUserLat = location.latitude
                    val currentUserLon = location.longitude

                    var isOutsideGroup = false

                    for (userSnap in snapshot.children) {
                        val otherUserId = userSnap.key ?: continue
                        if (otherUserId == userId) continue

                        val otherLat = userSnap.child("lat").getValue(Double::class.java) ?: continue
                        val otherLon = userSnap.child("lon").getValue(Double::class.java) ?: continue

                        val result = FloatArray(1)
                        Location.distanceBetween(
                            currentUserLat, currentUserLon,
                            otherLat, otherLon,
                            result
                        )

                        if (result[0] > geofenceRadius) {
                            isOutsideGroup = true
                            break
                        }
                    }

                    val previousStatus = snapshot.child(userId)
                        .child("isOutsideGeofence")
                        .getValue(Boolean::class.java) ?: false

                    if (previousStatus != isOutsideGroup) {
                        val currentTime = System.currentTimeMillis()

                        // Check if at least 1 minute has passed since last notification
                        if (currentTime - lastNotificationTime >= 60000) {
                            // Update geofence status
                            db.child("trips").child(tripKey)
                                .child("users").child(userId)
                                .child("isOutsideGeofence")
                                .setValue(isOutsideGroup)

                            val entered = !isOutsideGroup
                            val userName = auth.currentUser?.displayName ?: "A user"
                            val message = if (entered) {
                                "$userName is back within 1 km of the group."
                            } else {
                                "$userName has moved more than 1 km away from the group!"
                            }

                            // Notify others
                            saveNotificationToFirebase(tripKey, userId, message)

                            // Update last notification time
                            lastNotificationTime = currentTime
                        }
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("GeoFence", "Database error: ${error.message}")
                }
            })
    }


    fun saveNotificationToFirebase(
        tripId: String,
        senderId: String,
        message: String
    ) {
        val timestamp = System.currentTimeMillis()

        db.child("trips").child(tripId).child("users")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    for (userSnap in snapshot.children) {
                        val userId = userSnap.key ?: continue
                        if (userId == senderId) continue

                        val notificationData = mapOf(
                            "message" to message,
                            "timestamp" to timestamp,
                            "from" to senderId,
                            "tripId" to tripId,
                            "read" to false
                        )

                        db.child("users").child(userId)
                            .child("notifications")
                            .push()
                            .setValue(notificationData)
                    }
                }

                override fun onCancelled(error: DatabaseError) {}
            })
    }

    fun listenForNotifications() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        db.child("users").child(userId).child("notifications")
            .orderByChild("read").equalTo(false)
            .addChildEventListener(object : ChildEventListener {
                override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                    val data = snapshot.getValue(NotificationDataModel::class.java) ?: return

                    showLocalNotification(data.body)
                    Log.d("Notification", "New unread notification: ${data.body}")
                    // Optionally mark as read
                    snapshot.ref.child("read").setValue(true)
                }

                override fun onCancelled(error: DatabaseError) {}
                override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {}
                override fun onChildRemoved(snapshot: DataSnapshot) {}
                override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}
            })
    }

    private fun showLocalNotification(message: String) {
        val channelId = "road_sync_channel"
        val notificationManager = getApplication<Application>()
            .getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Create channel if Android 8+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Road Sync Alerts",
                NotificationManager.IMPORTANCE_HIGH
            )
            notificationManager.createNotificationChannel(channel)
            Log.d("Notification", "Showing notification: $message")
        }

        val builder = NotificationCompat.Builder(getApplication(), channelId)
            .setSmallIcon(R.drawable.ic_notification) // Replace with your icon
            .setContentTitle("Road Sync Alert")
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)

        notificationManager.notify(System.currentTimeMillis().toInt(), builder.build())
    }

    fun listenToUserLocations() {
        val tripId = this.tripId ?: return

        db.child("trips").child(tripId).child("users")
            .addChildEventListener(object : ChildEventListener {
                override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                    // When a new user is added, create a new marker
                    updateUserMarker(snapshot)
                }

                override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {
                    // When a user's location changes, update their marker
                    updateUserMarker(snapshot)
                }

                override fun onChildRemoved(snapshot: DataSnapshot) {
                    // If a user leaves, remove their marker
                    removeUserMarker(snapshot)
                }

                override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}
                override fun onCancelled(error: DatabaseError) {}
            })
    }

    // Update user location on the map
    private fun updateUserMarker(snapshot: DataSnapshot) {
        val userId = snapshot.key ?: return
        val lat = snapshot.child("lat").getValue(Double::class.java) ?: return
        val lon = snapshot.child("lon").getValue(Double::class.java) ?: return

        val userLocation = LatLng(lat, lon)

        googleMap?.let {
            val marker = userMarkers[userId]
            if (marker == null) {
                val icon = getColoredCarBitmapDescriptor(userId)
                if (icon != null) {
                    val newMarker = it.addMarker(
                        MarkerOptions()
                            .position(userLocation)
                            .title(userId)
                            .icon(icon) // ✅ Use custom icon
                    )
                    userMarkers[userId] = newMarker
                } else {
                    Log.e("TripViewModel", "Failed to get custom icon for $userId")
                }
            } else {
                // Update existing marker
                marker.position = userLocation
            }
        }
    }

    private fun removeUserMarker(snapshot: DataSnapshot) {
        val userId = snapshot.key ?: return
        val marker = userMarkers[userId]
        marker?.remove()
        userMarkers.remove(userId)
    }


    override fun onCleared() {
        super.onCleared()
        stopLocationUpdates()
    }

    fun removeUserFromTrip(tripId: String, userId: String, onComplete: (Boolean, String) -> Unit) {
        db.child("trips").child(tripId).child("users").child(userId)
            .removeValue()
            .addOnSuccessListener {
                onComplete(true, "User removed successfully")
            }
            .addOnFailureListener { e ->
                onComplete(false, "Failed to remove user: ${e.localizedMessage}")
            }
    }

    fun removeInvitedUserFromTrip(
        tripId: String,
        userId: String,
        callback: (Boolean, String) -> Unit
    ) {
        val ref =
            FirebaseDatabase.getInstance().getReference("trips").child(tripId).child("invitedUsers")
                .child(userId)

        ref.removeValue()
            .addOnSuccessListener {
                callback(true, "Invited user removed successfully")
            }
            .addOnFailureListener { e ->
                callback(false, "Failed to remove invited user: ${e.message}")
            }
    }


    fun getActiveTripsForUser(
        userId: String,
        onResult: (List<Trip>) -> Unit,
        onError: (String) -> Unit
    ) {
        db.child("trips").orderByChild("active").equalTo(true)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val userTrips = mutableListOf<Trip>()
                    for (tripSnapshot in snapshot.children) {
                        val participantsSnapshot = tripSnapshot.child("users")
                        if (participantsSnapshot.hasChild(userId)) {
                            val tripId = tripSnapshot.key ?: continue

                            val tripName =
                                tripSnapshot.child("tripName").getValue(String::class.java)
                                    ?: "Unnamed Trip"
                            val departure =
                                tripSnapshot.child("departure").getValue(String::class.java) ?: ""
                            val destination =
                                tripSnapshot.child("destination").getValue(String::class.java) ?: ""
                            val inviteCode =
                                tripSnapshot.child("inviteCode").getValue(String::class.java) ?: ""
                            val timestamp =
                                tripSnapshot.child("timestamp").getValue(Long::class.java) ?: 0L
                            val isActive =
                                tripSnapshot.child("active").getValue(Boolean::class.java)
                                    ?: false
                            val usersMap = tripSnapshot.child("users").getValue(object :
                                GenericTypeIndicator<Map<String, Participant>>() {}) ?: emptyMap()

                            val invitedUser = tripSnapshot.child("invitedUsers").getValue(object :
                                GenericTypeIndicator<Map<String, InvitedUser>>() {}) ?: emptyMap()

                            val trip = Trip(
                                tripId = tripId,
                                tripName = tripName,
                                departure = departure,
                                destination = destination, inviteCode = inviteCode,
                                timestamp = timestamp,
                                active = isActive,
                                users = usersMap,
                                invitedUsers = invitedUser
                            )
                            userTrips.add(trip)
                        }
                    }
                    onResult(userTrips)
                }

                override fun onCancelled(error: DatabaseError) {
                    onError("Failed to load user trips: ${error.message}")
                }
            })
    }

    fun getInActiveTripsForUser(
        userId: String,
        onResult: (List<Trip>) -> Unit,
        onError: (String) -> Unit
    ) {
        db.child("trips").orderByChild("active").equalTo(false)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val userTrips = mutableListOf<Trip>()
                    for (tripSnapshot in snapshot.children) {
                        val participantsSnapshot = tripSnapshot.child("users")
                        if (participantsSnapshot.hasChild(userId)) {
                            val tripId = tripSnapshot.key ?: continue

                            val tripName =
                                tripSnapshot.child("tripName").getValue(String::class.java)
                                    ?: "Unnamed Trip"
                            val departure =
                                tripSnapshot.child("departure").getValue(String::class.java) ?: ""
                            val destination =
                                tripSnapshot.child("destination").getValue(String::class.java) ?: ""
                            val inviteCode =
                                tripSnapshot.child("inviteCode").getValue(String::class.java) ?: ""
                            val timestamp =
                                tripSnapshot.child("timestamp").getValue(Long::class.java) ?: 0L
                            val isActive =
                                tripSnapshot.child("active").getValue(Boolean::class.java)
                                    ?: false
                            val usersMap = tripSnapshot.child("users").getValue(object :
                                GenericTypeIndicator<Map<String, Participant>>() {}) ?: emptyMap()

                            val trip = Trip(
                                tripId = tripId,
                                tripName = tripName,
                                departure = departure,
                                destination = destination, inviteCode = inviteCode,
                                timestamp = timestamp,
                                active = isActive,
                                users = usersMap
                            )
                            userTrips.add(trip)
                        }
                    }
                    onResult(userTrips)
                }

                override fun onCancelled(error: DatabaseError) {
                    onError("Failed to load user trips: ${error.message}")
                }
            })
    }

    fun getUserLocation(userId: String, onResult: (lat: Double, lon: Double) -> Unit) {
        val userRef = FirebaseDatabase.getInstance().getReference("users").child(userId)

        userRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    val latitude = snapshot.child("latitude").getValue(Double::class.java) ?: 0.0
                    val longitude = snapshot.child("longitude").getValue(Double::class.java) ?: 0.0
                    onResult(latitude, longitude)
                } else {
                    onResult(0.0, 0.0)  // Default if not found
                }
            }

            override fun onCancelled(error: DatabaseError) {
                onResult(0.0, 0.0)  // Handle error
            }
        })
    }

    fun updateTripCost(updatedTrip: Trip) {
        FirebaseDatabase.getInstance().getReference("trips").child(updatedTrip.tripId).child("cost")
            .setValue(updatedTrip.cost)
    }

    private fun getColoredCarBitmapDescriptor(userId: String): BitmapDescriptor {
        val baseBitmap = vectorToBitmap(R.drawable.ic_car_top_view)


        // Create a blank bitmap to paint on
        val resultBitmap = Bitmap.createBitmap(
            baseBitmap.width,
            baseBitmap.height,
            Bitmap.Config.ARGB_8888
        )

        val canvas = Canvas(resultBitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        // Generate a unique color from userId
        val hash = userId.hashCode()
        val red = (hash shr 16 and 0xFF)
        val green = (hash shr 8 and 0xFF)
        val blue = (hash and 0xFF)

        paint.colorFilter =
            PorterDuffColorFilter(Color.rgb(red, green, blue), PorterDuff.Mode.SRC_ATOP)

        // Draw the base bitmap onto the canvas using the color filter
        canvas.drawBitmap(baseBitmap, 0f, 0f, paint)

        return BitmapDescriptorFactory.fromBitmap(resultBitmap)
    }

    private fun vectorToBitmap(drawableId: Int): Bitmap {
        val drawable = ContextCompat.getDrawable(context, drawableId)!!
        val bitmap = Bitmap.createBitmap(
            drawable.intrinsicWidth,
            drawable.intrinsicHeight,
            Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        return bitmap
    }

}
