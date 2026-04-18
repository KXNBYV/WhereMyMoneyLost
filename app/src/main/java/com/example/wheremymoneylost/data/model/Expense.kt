package com.example.wheremymoneylost.data.model

data class Expense(
    val id: Int = 0,
    val amount: Double,
    val categoryId: Int,
    val timestamp: Long = System.currentTimeMillis()
)
