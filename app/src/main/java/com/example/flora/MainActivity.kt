package com.example.flora

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.flora.ui.screens.HomeScreen
import com.example.flora.ui.screens.StatsScreen
import com.example.flora.ui.sheets.AddEntrySheetContent
import com.example.flora.ui.theme.FloraTheme
import com.example.flora.util.exportStatsAsText
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            FloraTheme {
                FloraApp()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FloraApp(viewModel: FloraViewModel = viewModel()) {
    val movements by viewModel.allMovements.collectAsState()
    val meals by viewModel.allMeals.collectAsState()
    var showSheet by remember { mutableStateOf(false) }
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }
    var currentTab by remember { mutableIntStateOf(0) }
    val sheetState = rememberModalBottomSheetState()
    val context = LocalContext.current

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { 
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            if (currentTab == 0) Icons.Rounded.CalendarMonth else Icons.Rounded.BarChart, 
                            contentDescription = null, 
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(if (currentTab == 0) "Flora" else "Estadísticas", fontWeight = FontWeight.Bold)
                    }
                },
                actions = {
                    if (currentTab == 0) {
                        var showDatePicker by remember { mutableStateOf(false) }
                        IconButton(onClick = { showDatePicker = true }) {
                            Icon(Icons.Rounded.CalendarMonth, "Seleccionar fecha")
                        }
                        if (showDatePicker) {
                            val datePickerState = rememberDatePickerState(
                                initialSelectedDateMillis = selectedDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
                            )
                            DatePickerDialog(
                                onDismissRequest = { showDatePicker = false },
                                confirmButton = {
                                    TextButton(onClick = {
                                        datePickerState.selectedDateMillis?.let {
                                            selectedDate = Instant.ofEpochMilli(it)
                                                .atZone(ZoneId.systemDefault())
                                                .toLocalDate()
                                        }
                                        showDatePicker = false
                                    }) { Text("OK") }
                                },
                                dismissButton = {
                                    TextButton(onClick = { showDatePicker = false }) { Text("Cancelar") }
                                }
                            ) {
                                DatePicker(state = datePickerState)
                            }
                        }
                    } else {
                        IconButton(onClick = { exportStatsAsText(context, movements, meals) }) {
                            Icon(Icons.Default.Share, "Compartir estadísticas")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp
            ) {
                NavigationBarItem(
                    icon = { Icon(Icons.Rounded.Home, null) },
                    label = { Text("Diario") },
                    selected = currentTab == 0,
                    onClick = { currentTab = 0 }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Rounded.BarChart, null) },
                    label = { Text("Estadísticas") },
                    selected = currentTab == 1,
                    onClick = { currentTab = 1 }
                )
            }
        },
        floatingActionButton = {
            if (currentTab == 0) {
                ExtendedFloatingActionButton(
                    onClick = { showSheet = true },
                    icon = { Icon(Icons.Default.Add, "Agregar") },
                    text = { Text("Registrar") },
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    ) { innerPadding ->
        Crossfade(targetState = currentTab, modifier = Modifier.padding(innerPadding), label = "TabTransition") { tab ->
            when (tab) {
                0 -> HomeScreen(viewModel, selectedDate) { selectedDate = it }
                1 -> StatsScreen(viewModel)
            }
        }

        if (showSheet) {
            ModalBottomSheet(
                onDismissRequest = { showSheet = false },
                sheetState = sheetState,
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
            ) {
                AddEntrySheetContent(
                    recentMeals = meals.map { it.description }.distinct().take(8),
                    onAddMovement = { bristolType, notes ->
                        val now = LocalTime.now()
                        val timestamp = selectedDate.atTime(now)
                            .atZone(ZoneId.systemDefault())
                            .toInstant()
                            .toEpochMilli()
                        
                        viewModel.addMovement(bristolType, notes, timestamp)
                        showSheet = false
                    },
                    onAddMeal = { description, mealType ->
                        val now = LocalTime.now()
                        val timestamp = selectedDate.atTime(now)
                            .atZone(ZoneId.systemDefault())
                            .toInstant()
                            .toEpochMilli()
                        
                        viewModel.addMeal(description, mealType, timestamp)
                        showSheet = false
                    }
                )
            }
        }
    }
}
