package com.example.data.model

import com.google.firebase.Timestamp

data class UserProfile(
    val userId: String = "",
    val displayName: String = "",
    val email: String = "",
    val photoUri: String = "",
    val bio: String = "",
    val savingsTarget: Long = 50_000_000L,
    val updatedAt: Timestamp? = null
)
