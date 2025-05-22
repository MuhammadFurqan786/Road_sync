package com.roadsync.auth.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class InvitedUser(
    val userId: String = "",
    val name: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val key: String = ""  // Can be "invited", "notAccepted", "accepted", etc.
):Parcelable