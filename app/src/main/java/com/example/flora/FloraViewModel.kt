package com.example.flora

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.flora.data.AppDatabase
import com.example.flora.data.BowelMovement
import com.example.flora.data.BowelMovementRepository
import com.example.flora.data.Meal
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class FloraViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: BowelMovementRepository
    val allMovements: StateFlow<List<BowelMovement>>
    val allMeals: StateFlow<List<Meal>>

    init {
        val db = AppDatabase.getDatabase(application)
        repository = BowelMovementRepository(db.bowelMovementDao(), db.mealDao())
        
        allMovements = repository.allMovements.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        allMeals = repository.allMeals.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
    }

    fun addMovement(bristolType: Int? = null, notes: String = "", timestamp: Long = System.currentTimeMillis()) {
        viewModelScope.launch {
            repository.insertMovement(BowelMovement(bristolType = bristolType, notes = notes, timestamp = timestamp))
        }
    }

    fun deleteMovement(bowelMovement: BowelMovement) {
        viewModelScope.launch {
            repository.deleteMovement(bowelMovement)
        }
    }

    fun addMeal(description: String, mealType: String, timestamp: Long = System.currentTimeMillis()) {
        viewModelScope.launch {
            repository.insertMeal(Meal(description = description, mealType = mealType, timestamp = timestamp))
        }
    }

    fun deleteMeal(meal: Meal) {
        viewModelScope.launch {
            repository.deleteMeal(meal)
        }
    }
}
