package com.example.flora.data

import kotlinx.coroutines.flow.Flow

class BowelMovementRepository(private val bowelMovementDao: BowelMovementDao) {
    val allMovements: Flow<List<BowelMovement>> = bowelMovementDao.getAll()

    suspend fun insert(bowelMovement: BowelMovement) {
        bowelMovementDao.insert(bowelMovement)
    }

    suspend fun delete(bowelMovement: BowelMovement) {
        bowelMovementDao.delete(bowelMovement)
    }
}
