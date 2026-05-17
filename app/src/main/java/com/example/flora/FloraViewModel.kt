package com.example.flora

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.flora.data.AppDatabase
import com.example.flora.data.BowelMovement
import com.example.flora.data.BowelMovementRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class FloraViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: BowelMovementRepository
    val allMovements: StateFlow<List<BowelMovement>>

    init {
        val dao = AppDatabase.getDatabase(application).bowelMovementDao()
        repository = BowelMovementRepository(dao)
        allMovements = repository.allMovements.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
    }

    fun addMovement(bristolType: Int? = null, notes: String = "", timestamp: Long = System.currentTimeMillis()) {
        viewModelScope.launch {
            repository.insert(BowelMovement(bristolType = bristolType, notes = notes, timestamp = timestamp))
        }
    }

    fun deleteMovement(bowelMovement: BowelMovement) {
        viewModelScope.launch {
            repository.delete(bowelMovement)
        }
    }
}
