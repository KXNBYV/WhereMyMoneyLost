package com.example.wheremymoneylost.data.model

data class RecurringExpense(
    val id: Int,
    val amount: Double,
    val categoryId: Int,
    val name: String,
    val dayOfMonth: Int // 1-31
)
