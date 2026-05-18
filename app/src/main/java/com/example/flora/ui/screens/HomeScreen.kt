package com.example.flora.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.History
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.flora.FloraViewModel
import com.example.flora.data.BowelMovement
import com.example.flora.data.Meal
import com.example.flora.ui.components.CalendarHeader
import com.example.flora.ui.components.MealItem
import com.example.flora.ui.components.MovementItem
import com.example.flora.util.toLocalDate
import java.time.LocalDate

@Composable
fun HomeScreen(
    viewModel: FloraViewModel,
    selectedDate: LocalDate,
    onDateSelected: (LocalDate) -> Unit
) {
    val movements by viewModel.allMovements.collectAsState()
    val meals by viewModel.allMeals.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        CalendarHeader(selectedDate, movements, meals, onDateSelected)
        
        val filteredMovements = movements.filter { it.timestamp.toLocalDate() == selectedDate }
        val filteredMeals = meals.filter { it.timestamp.toLocalDate() == selectedDate }
        
        if (filteredMovements.isEmpty() && filteredMeals.isEmpty()) {
            EmptyState()
        } else {
            EntryList(
                movements = filteredMovements,
                meals = filteredMeals,
                onDeleteMovement = { viewModel.deleteMovement(it) },
                onDeleteMeal = { viewModel.deleteMeal(it) }
            )
        }
    }
}

@Composable
fun EmptyState() {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Rounded.History,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.outlineVariant
        )
        Spacer(Modifier.height(16.dp))
        Text(
            "No hay registros hoy",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.outline
        )
    }
}

@Composable
fun EntryList(
    movements: List<BowelMovement>,
    meals: List<Meal>,
    onDeleteMovement: (BowelMovement) -> Unit,
    onDeleteMeal: (Meal) -> Unit
) {
    val allEntries = remember(movements, meals) {
        val entryList = mutableListOf<Pair<Long, Any>>()
        movements.forEach { entryList.add(it.timestamp to it) }
        meals.forEach { entryList.add(it.timestamp to it) }
        entryList.sortByDescending { it.first }
        entryList
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(allEntries) { (_, entry) ->
            when (entry) {
                is BowelMovement -> MovementItem(entry, onDeleteMovement)
                is Meal -> MealItem(entry, onDeleteMeal)
            }
        }
    }
}
