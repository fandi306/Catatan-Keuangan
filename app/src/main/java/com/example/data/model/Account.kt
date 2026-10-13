package com.example.data.model

import com.google.firebase.Timestamp

data class Account(
    val id: String = "",
    val userId: String = "",
    val name: String = "",
    val type: String = "liquid", // "liquid" (Liquid Aset) or "non_liquid" (Non-Liquid Aset)
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
) {
    val isLiquid: Boolean
        get() = type == "liquid"
}
