package com.roadsync.home.models

import android.os.Parcelable
import com.roadsync.auth.model.InvitedUser
import com.roadsync.auth.model.Participant
import kotlinx.parcelize.Parcelize

@Parcelize
data class Trip(
    val tripId: String = "",
    val tripName: String = "",
    val departure: String = "",
    val destination: String = "",
    val inviteCode: String = "",
    val timestamp: Long = 0L,
    val cost: String = "",
    val stayOption: String = "",
    val active: Boolean = false,
    val users: Map<String, Participant> = emptyMap(),
    val invitedUsers: Map<String, InvitedUser> = emptyMap(),
    val images: Map<String, String> = emptyMap()
) : Parcelable

