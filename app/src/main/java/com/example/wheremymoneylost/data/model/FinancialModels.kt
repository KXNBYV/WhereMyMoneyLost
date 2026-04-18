package com.example.wheremymoneylost.data.model

data class Bill(
    val id: Int,
    val name: String,
    val amount: Double,
    val dueDate: Long, // Timestamp for the day
    val isPaid: Boolean = false,
    val categoryId: Int = 4 // Default to "Bills"
)

data class SavingGoal(
    val id: Int,
    val name: String,
    val targetAmount: Double,
    val currentAmount: Double = 0.0,
    val deadline: Long? = null
)
