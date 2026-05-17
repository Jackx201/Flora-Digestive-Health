package com.example.flora.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "meals")
data class Meal(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val description: String = "",
    val mealType: String = "Comida" // Desayuno, Almuerzo, Cena, Snack
)
