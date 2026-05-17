package com.example.flora.data

import kotlinx.coroutines.flow.Flow

class BowelMovementRepository(
    private val bowelMovementDao: BowelMovementDao,
    private val mealDao: MealDao
) {
    val allMovements: Flow<List<BowelMovement>> = bowelMovementDao.getAll()
    val allMeals: Flow<List<Meal>> = mealDao.getAll()

    suspend fun insertMovement(bowelMovement: BowelMovement) {
        bowelMovementDao.insert(bowelMovement)
    }

    suspend fun deleteMovement(bowelMovement: BowelMovement) {
        bowelMovementDao.delete(bowelMovement)
    }

    suspend fun insertMeal(meal: Meal) {
        mealDao.insert(meal)
    }

    suspend fun deleteMeal(meal: Meal) {
        mealDao.delete(meal)
    }
}
