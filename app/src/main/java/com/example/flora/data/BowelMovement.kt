package com.example.flora.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDateTime

@Entity(tableName = "bowel_movements")
data class BowelMovement(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val notes: String = "",
    val bristolType: Int? = null // For digestive health tracking
)
