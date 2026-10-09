package com.example.data.model

import com.google.firebase.Timestamp

data class Transaction(
    val id: String = "",
    val userId: String = "",
    val type: String = "out", // "in" or "out"
    val description: String = "",
    val amount: Long = 0L,
    val account: String = "",
    val category: String = "",
    val date: String = "", // "YYYY-MM-DD"
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
) {
    val isIncome: Boolean
        get() = type == "in"
}
