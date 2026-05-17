package com.example.flora

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.flora.data.BowelMovement
import com.example.flora.ui.theme.FloraTheme
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

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
    var showDialog by remember { mutableStateOf(false) }
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Flora - Registro Intestinal") })
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Agregar Registro")
            }
        }
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
            CalendarHeader(selectedDate, movements) { selectedDate = it }
            val filteredMovements = movements.filter {
                val moveDate = Instant.ofEpochMilli(it.timestamp)
                    .atZone(ZoneId.systemDefault())
                    .toLocalDate()
                moveDate == selectedDate
            }
            MovementList(filteredMovements)
        }

        if (showDialog) {
            AddMovementDialog(
                onDismiss = { showDialog = false },
                onConfirm = { bristolType, notes ->
                    viewModel.addMovement(bristolType, notes)
                    showDialog = false
                }
            )
        }
    }
}

@Composable
fun CalendarHeader(
    selectedDate: LocalDate,
    allMovements: List<BowelMovement>,
    onDateSelected: (LocalDate) -> Unit
) {
    val days = remember(selectedDate) {
        (-3..3).map { selectedDate.plusDays(it.toLong()) }
    }

    Row(
        modifier = Modifier.fillMaxWidth().padding(8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        days.forEach { date ->
            val isSelected = date == selectedDate
            val hasMovements = allMovements.any {
                Instant.ofEpochMilli(it.timestamp)
                    .atZone(ZoneId.systemDefault())
                    .toLocalDate() == date
            }

            Column(
                modifier = Modifier
                    .padding(4.dp)
                    .width(45.dp)
                    .height(70.dp)
                    .let {
                        if (isSelected) it.background(
                            MaterialTheme.colorScheme.primaryContainer,
                            MaterialTheme.shapes.small
                        )
                        else it
                    }
                    .clickable { onDateSelected(date) },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = date.dayOfWeek.name.take(3),
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = date.dayOfMonth.toString(),
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                )
                if (hasMovements) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .background(
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.primary,
                                shape = androidx.compose.foundation.shape.CircleShape
                            )
                    )
                }
            }
        }
    }
}

@Composable
fun MovementList(movements: List<BowelMovement>) {
    LazyColumn {
        items(movements) { movement ->
            MovementItem(movement)
        }
    }
}

@Composable
fun MovementItem(movement: BowelMovement) {
    val date = Instant.ofEpochMilli(movement.timestamp)
        .atZone(ZoneId.systemDefault())
        .toLocalDateTime()
    val formatter = DateTimeFormatter.ofPattern("MMM dd, yyyy - HH:mm")

    Card(
        modifier = Modifier.fillMaxWidth().padding(8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = date.format(formatter), style = MaterialTheme.typography.titleMedium)
            if (movement.bristolType != null) {
                Text(text = "Escala de las heces: ${movement.bristolType}")
            }
            if (movement.notes.isNotEmpty()) {
                Text(text = "Notas: ${movement.notes}")
            }
        }
    }
}

@Composable
fun AddMovementDialog(onDismiss: () -> Unit, onConfirm: (Int?, String) -> Unit) {
    var notes by remember { mutableStateOf("") }
    var bristolType by remember { mutableStateOf<Int?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Registrar evacuación") },
        text = {
            Column {
                TextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notas") }
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text("Bristol Type (1-7):")
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    (1..4).forEach { type ->
                        FilterChip(
                            selected = bristolType == type,
                            onClick = { bristolType = type },
                            label = { Text(type.toString()) }
                        )
                    }
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    (5..7).forEach { type ->
                        FilterChip(
                            selected = bristolType == type,
                            onClick = { bristolType = type },
                            label = { Text(type.toString()) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(bristolType, notes) }) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
