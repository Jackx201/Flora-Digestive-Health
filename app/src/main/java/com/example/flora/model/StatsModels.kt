package com.example.flora.model

data class FoodCorrelation(
    val name: String,
    val score: Float,
    val count: Int
)

data class HealthStats(
    val totalMovements: Int,
    val last7Days: Int,
    val thisMonth: Int,
    val idealPercentage: Int,
    val attentionPercentage: Int,
    val warningPercentage: Int
)
