package com.roadsync.auth.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Participant(
    val userId: String = "",
    val name: String = "",
    val lat: Double = 0.0,
    val lon: Double = 0.0,
    val pickup: String = "",
    val role:String ="",
    val isOutsideGeofence: Boolean = false
):Parcelable
