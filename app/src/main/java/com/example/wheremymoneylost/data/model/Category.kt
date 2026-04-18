package com.example.wheremymoneylost.data.model

data class Category(
    val id: Int,
    val name: String,
    val iconName: String,
    val colorHex: String,
    val budgetLimit: Double = 0.0  // 0 = ไม่จำกัด
)
