package com.example.flora

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.flora.data.AppDatabase
import com.example.flora.data.BowelMovement
import com.example.flora.data.BowelMovementRepository
import com.example.flora.data.Meal
import com.example.flora.model.FoodCorrelation
import com.example.flora.model.HealthStats
import com.example.flora.util.toLocalDate
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.temporal.ChronoUnit

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

    val healthStats: StateFlow<HealthStats?> = allMovements.combine(allMeals) { movements, _ ->
        if (movements.isEmpty()) return@combine null
        
        val total = movements.size
        val now = Instant.now()
        val last7Days = movements.count { 
            Instant.ofEpochMilli(it.timestamp).isAfter(now.minus(7, ChronoUnit.DAYS))
        }
        
        val today = LocalDate.now()
        val thisMonth = movements.count {
            val date = it.timestamp.toLocalDate()
            date.month == today.month && date.year == today.year
        }

        val ideal = (movements.count { it.bristolType in 3..4 }.toFloat() / total * 100).toInt()
        val attention = (movements.count { it.bristolType in listOf(2, 5, 6) }.toFloat() / total * 100).toInt()
        val warning = (movements.count { it.bristolType in listOf(1, 7) }.toFloat() / total * 100).toInt()

        HealthStats(total, last7Days, thisMonth, ideal, attention, warning)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val foodCorrelations: StateFlow<List<FoodCorrelation>> = combine(allMovements, allMeals) { movements, meals ->
        if (movements.isEmpty() || meals.isEmpty()) return@combine emptyList()

        val analysis = mutableMapOf<String, Pair<Int, Int>>() // Name -> (TotalScore, AssociationCount)
        
        meals.forEach { meal ->
            val mealName = meal.description.trim().lowercase()
            if (mealName.isNotEmpty()) {
                val nextMovements = movements.filter { 
                    it.timestamp >= meal.timestamp &&
                    it.timestamp < meal.timestamp + 24 * 60 * 60 * 1000 // 24h window
                }
                
                if (nextMovements.isNotEmpty()) {
                    var mealScore = 0
                    nextMovements.forEach { move ->
                        mealScore += when (move.bristolType) {
                            3, 4 -> 10 // Ideal
                            2, 5 -> 5  // Okay
                            6 -> -5    // Warning
                            1, 7 -> -20 // Bad
                            else -> 0
                        }
                    }
                    val current = analysis.getOrDefault(mealName, Pair(0, 0))
                    analysis[mealName] = Pair(current.first + mealScore, current.second + nextMovements.size)
                }
            }
        }
        
        analysis.map { (name, stats) ->
            val avgScore = stats.first.toFloat() / stats.second
            FoodCorrelation(name.replaceFirstChar { it.uppercase() }, avgScore, stats.second)
        }.filter { it.count >= 1 }
        .sortedByDescending { it.score }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

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
