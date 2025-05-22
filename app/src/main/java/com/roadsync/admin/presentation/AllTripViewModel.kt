package com.roadsync.admin.presentation

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.database.*
import com.roadsync.home.models.Trip

class AllTripViewModel : ViewModel() {

    private val _trips = MutableLiveData<List<Trip>>()
    val trips: LiveData<List<Trip>> = _trips

    private val dbRef = FirebaseDatabase.getInstance().getReference("trips")

    fun fetchAllTrips() {
        dbRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val tripList = mutableListOf<Trip>()
                for (tripSnapshot in snapshot.children) {
                    val trip = tripSnapshot.getValue(Trip::class.java)
                    trip?.let { tripList.add(it) }
                }
                _trips.value = tripList
            }

            override fun onCancelled(error: DatabaseError) {

            }
        })
    }

    fun deleteTrip(tripId: String) {
        dbRef.child(tripId).removeValue()
    }
}
