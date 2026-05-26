package com.example.splitify.model

import com.google.firebase.firestore.DocumentId

data class User(
    @DocumentId val id: String = "",
    val name: String = "",
    val email: String = "",
    val balance: Double = 0.0
)

data class Group(
    @DocumentId val id: String = "",
    val name: String = "",
    val members: List<String> = emptyList(), // List of User IDs
    val balances: Map<String, Double> = emptyMap() // User ID to net balance
)

data class Expense(
    @DocumentId val id: String = "",
    val description: String = "",
    val amount: Double = 0.0,
    val date: Long = System.currentTimeMillis(),
    val paidBy: String = "", // User ID
    val splitType: SplitType = SplitType.EQUALLY,
    val splitDetails: Map<String, Double> = emptyMap(), // User ID to amount
    val involvedUserIds: List<String> = emptyList(),
    val groupId: String? = null,
    val isSettled: Boolean = false,
    val isSettlement: Boolean = false
)

enum class SplitType {
    EQUALLY, EXACT, PERCENTAGE
}

data class Friend(
    val user: User = User(),
    val balanceWithMe: Double = 0.0
)

data class Settlement(
    val fromUserId: String,
    val toUserId: String,
    val amount: Double
)
