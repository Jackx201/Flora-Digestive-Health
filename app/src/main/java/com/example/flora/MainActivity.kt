package com.example.flora

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.CalendarViewWeek
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.flora.data.BowelMovement
import com.example.flora.ui.theme.FloraTheme
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.time.temporal.ChronoUnit
import java.util.Locale

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

fun getBristolColor(type: Int?): Color {
    return when (type) {
        1, 7 -> Color(0xFFE57373) // Red (Stressed/Warning)
        2, 5, 6 -> Color(0xFFFFB74D) // Orange/Yellow (Attention)
        3, 4 -> Color(0xFF81C784) // Green (Ideal)
        else -> Color(0xFFBDBDBD) // Grey (Unknown)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FloraApp(viewModel: FloraViewModel = viewModel()) {
    val movements by viewModel.allMovements.collectAsState()
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
                        IconButton(onClick = { exportStatsAsText(context, movements) }) {
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
                0 -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.surface)
                    ) {
                        CalendarHeader(selectedDate, movements) { selectedDate = it }
                        
                        val filteredMovements = movements.filter {
                            val moveDate = Instant.ofEpochMilli(it.timestamp)
                                .atZone(ZoneId.systemDefault())
                                .toLocalDate()
                            moveDate == selectedDate
                        }
                        
                        if (filteredMovements.isEmpty()) {
                            EmptyState()
                        } else {
                            MovementList(
                                movements = filteredMovements,
                                onDelete = { viewModel.deleteMovement(it) }
                            )
                        }
                    }
                }
                1 -> {
                    StatsScreen(movements)
                }
            }
        }

        if (showSheet) {
            ModalBottomSheet(
                onDismissRequest = { showSheet = false },
                sheetState = sheetState,
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
            ) {
                AddMovementSheetContent(
                    onConfirm = { bristolType, notes ->
                        val now = LocalTime.now()
                        val timestamp = selectedDate.atTime(now)
                            .atZone(ZoneId.systemDefault())
                            .toInstant()
                            .toEpochMilli()
                        
                        viewModel.addMovement(bristolType, notes, timestamp)
                        showSheet = false
                    }
                )
            }
        }
    }
}

fun exportStatsAsText(context: android.content.Context, movements: List<BowelMovement>) {
    val total = movements.size
    val last7Days = movements.count { 
        Instant.ofEpochMilli(it.timestamp).isAfter(Instant.now().minus(7, ChronoUnit.DAYS))
    }
    
    val ideal = movements.count { it.bristolType in 3..4 }
    val attention = movements.count { it.bristolType in listOf(2, 5, 6) }
    val warning = movements.count { it.bristolType in listOf(1, 7) }
    
    val report = buildString {
        appendLine("📊 Reporte de Salud Flora")
        appendLine("Generado el: ${LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))}")
        appendLine("---------------------------")
        appendLine("🔹 Resumen General:")
        appendLine("- Registros totales: $total")
        appendLine("- Últimos 7 días: $last7Days")
        appendLine()
        appendLine("🔹 Distribución Bristol:")
        if (total > 0) {
            appendLine("- Ideal (Tipo 3-4): $ideal (${(ideal * 100 / total)}%)")
            appendLine("- Atención (Tipo 2, 5, 6): $attention (${(attention * 100 / total)}%)")
            appendLine("- Advertencia (Tipo 1, 7): $warning (${(warning * 100 / total)}%)")
        } else {
            appendLine("Sin registros aún.")
        }
        appendLine()
        appendLine("🔸 Últimas Notas:")
        movements.take(5).forEach { move ->
            val date = Instant.ofEpochMilli(move.timestamp).atZone(ZoneId.systemDefault()).toLocalDate()
            val type = move.bristolType ?: "?"
            appendLine("- $date: Tipo $type ${if (move.notes.isNotEmpty()) "(${move.notes})" else ""}")
        }
    }

    val sendIntent: Intent = Intent().apply {
        action = Intent.ACTION_SEND
        putExtra(Intent.EXTRA_TEXT, report)
        type = "text/plain"
    }
    val shareIntent = Intent.createChooser(sendIntent, "Enviar reporte de Flora")
    context.startActivity(shareIntent)
}

@Composable
fun EmptyState() {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Default.History,
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
fun CalendarHeader(
    selectedDate: LocalDate,
    allMovements: List<BowelMovement>,
    onDateSelected: (LocalDate) -> Unit
) {
    var isExpanded by remember { mutableStateOf(false) }
    var displayedMonth by remember(selectedDate) { mutableStateOf(YearMonth.from(selectedDate)) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        )
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            // Header: Month Name and Toggle
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (isExpanded) {
                        IconButton(onClick = { displayedMonth = displayedMonth.minusMonths(1) }) {
                            Icon(Icons.Rounded.ChevronLeft, "Mes anterior")
                        }
                    }
                    
                    Text(
                        text = displayedMonth.month.getDisplayName(TextStyle.FULL, Locale("es")).replaceFirstChar { it.uppercase() } + " ${displayedMonth.year}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )

                    if (isExpanded) {
                        IconButton(onClick = { displayedMonth = displayedMonth.plusMonths(1) }) {
                            Icon(Icons.Rounded.ChevronRight, "Mes siguiente")
                        }
                    }
                }
                
                IconButton(onClick = { isExpanded = !isExpanded }) {
                    Icon(
                        if (isExpanded) Icons.Rounded.KeyboardArrowUp else Icons.Rounded.KeyboardArrowDown,
                        contentDescription = if (isExpanded) "Colapsar" else "Expandir",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            AnimatedContent(
                targetState = isExpanded,
                transitionSpec = {
                    expandVertically() + fadeIn() togetherWith shrinkVertically() + fadeOut()
                },
                label = "CalendarExpansion"
            ) { expanded ->
                if (expanded) {
                    MonthView(selectedDate, displayedMonth, allMovements, onDateSelected)
                } else {
                    WeekView(selectedDate, allMovements, onDateSelected)
                }
            }
        }
    }
}

@Composable
fun WeekView(
    selectedDate: LocalDate,
    allMovements: List<BowelMovement>,
    onDateSelected: (LocalDate) -> Unit
) {
    val days = remember(selectedDate) {
        (-3..3).map { selectedDate.plusDays(it.toLong()) }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        days.forEach { date ->
            DayItem(date, selectedDate, allMovements, onDateSelected)
        }
    }
}

@Composable
fun MonthView(
    selectedDate: LocalDate,
    currentMonth: YearMonth,
    allMovements: List<BowelMovement>,
    onDateSelected: (LocalDate) -> Unit
) {
    val daysInMonth = currentMonth.lengthOfMonth()
    val firstDayOfMonth = currentMonth.atDay(1)
    val firstDayOfWeek = firstDayOfMonth.dayOfWeek.value // 1 (Mon) to 7 (Sun)
    
    // Adjust for Monday start: value - 1
    val emptySlots = firstDayOfWeek - 1

    Column(modifier = Modifier.padding(bottom = 8.dp)) {
        // Day names header
        Row(modifier = Modifier.fillMaxWidth()) {
            listOf("L", "M", "X", "J", "V", "S", "D").forEach { dayName ->
                Text(
                    text = dayName,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }
        
        Spacer(Modifier.height(8.dp))

        // Grid of days (simplified using Rows instead of LazyVerticalGrid to avoid nested scroll issues in some contexts)
        val totalDays = (1..daysInMonth).map { currentMonth.atDay(it) }
        val chunks = (List(emptySlots) { null } + totalDays).chunked(7)

        chunks.forEach { week ->
            Row(modifier = Modifier.fillMaxWidth()) {
                week.forEach { date ->
                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        if (date != null) {
                            DayItem(
                                date = date,
                                selectedDate = selectedDate,
                                allMovements = allMovements,
                                onDateSelected = onDateSelected,
                                compact = true
                            )
                        }
                    }
                }
                // Fill the last row if needed
                if (week.size < 7) {
                    repeat(7 - week.size) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
            Spacer(Modifier.height(4.dp))
        }
    }
}

@Composable
fun DayItem(
    date: LocalDate,
    selectedDate: LocalDate,
    allMovements: List<BowelMovement>,
    onDateSelected: (LocalDate) -> Unit,
    compact: Boolean = false
) {
    val isSelected = date == selectedDate
    val movementsForDay = allMovements.filter {
        Instant.ofEpochMilli(it.timestamp)
            .atZone(ZoneId.systemDefault())
            .toLocalDate() == date
    }
    
    val indicatorColor = if (movementsForDay.isEmpty()) {
        Color.Transparent
    } else {
        val worstType = movementsForDay.mapNotNull { it.bristolType }.maxByOrNull { 
            when(it) {
                1, 7 -> 3 // Priority Red
                2, 5, 6 -> 2 // Priority Orange
                else -> 1 // Priority Green
            }
        } ?: 4
        getBristolColor(worstType)
    }

    Surface(
        onClick = { onDateSelected(date) },
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
        modifier = Modifier
            .width(if (compact) 40.dp else 48.dp)
            .height(if (compact) 48.dp else 75.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(2.dp)
        ) {
            if (!compact) {
                Text(
                    text = date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale("es")).take(1).uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = date.dayOfMonth.toString(),
                style = if (compact) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
            )
            if (movementsForDay.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .padding(top = 2.dp)
                        .size(if (compact) 4.dp else 6.dp)
                        .background(
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else indicatorColor,
                            shape = CircleShape
                        )
                )
            }
        }
    }
}

@Composable
fun MovementList(
    movements: List<BowelMovement>,
    onDelete: (BowelMovement) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(movements) { movement ->
            MovementItem(movement, onDelete)
        }
    }
}

@Composable
fun MovementItem(
    movement: BowelMovement,
    onDelete: (BowelMovement) -> Unit
) {
    val date = Instant.ofEpochMilli(movement.timestamp)
        .atZone(ZoneId.systemDefault())
        .toLocalDateTime()
    val formatter = DateTimeFormatter.ofPattern("HH:mm")
    var showDeleteConfirm by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(getBristolColor(movement.bristolType).copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = movement.bristolType?.toString() ?: "?",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = getBristolColor(movement.bristolType)
                )
            }
            
            Spacer(Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Tipo ${movement.bristolType ?: '?'}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.weight(1f))
                    Text(
                        text = date.format(formatter),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
                if (movement.notes.isNotEmpty()) {
                    Spacer(Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.Top) {
                        Icon(
                            Icons.AutoMirrored.Filled.Notes, 
                            contentDescription = null, 
                            modifier = Modifier.size(14.dp).padding(top = 2.dp),
                            tint = MaterialTheme.colorScheme.outline
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = movement.notes,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            IconButton(onClick = { showDeleteConfirm = true }) {
                Icon(
                    Icons.Default.DeleteOutline,
                    contentDescription = "Borrar",
                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
                )
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("¿Eliminar registro?") },
            text = { Text("Esta acción no se puede deshacer.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDelete(movement)
                        showDeleteConfirm = false
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Eliminar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddMovementSheetContent(onConfirm: (Int?, String) -> Unit) {
    var notes by remember { mutableStateOf("") }
    var bristolType by remember { mutableStateOf<Int?>(null) }

    val bristolDescription = when (bristolType) {
        1 -> "Trozos duros y separados (estreñimiento)"
        2 -> "Forma de salchicha con bultos"
        3 -> "Como una salchicha con grietas"
        4 -> "Suave y lisa (ideal)"
        5 -> "Trozos blandos con bordes definidos"
        6 -> "Trozos blandos con bordes deshechos"
        7 -> "Acuosa, sin trozos (diarrea)"
        else -> "Selecciona un tipo"
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .padding(bottom = 32.dp)
    ) {
        Text(
            "Nuevo Registro",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(24.dp))
        
        Text(
            "¿Cómo fue la consistencia?",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            bristolDescription,
            style = MaterialTheme.typography.bodyMedium,
            color = if (bristolType != null) getBristolColor(bristolType) else MaterialTheme.colorScheme.outline
        )
        Spacer(Modifier.height(16.dp))
        
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            (1..7).forEach { type ->
                FilterChip(
                    selected = bristolType == type,
                    onClick = { bristolType = type },
                    label = { Text(type.toString()) },
                    shape = CircleShape,
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = getBristolColor(type),
                        selectedLabelColor = Color.White
                    )
                )
            }
        }
        
        Spacer(Modifier.height(24.dp))
        
        OutlinedTextField(
            value = notes,
            onValueChange = { notes = it },
            label = { Text("Notas o síntomas") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            leadingIcon = { Icon(Icons.AutoMirrored.Filled.Notes, null) }
        )
        
        Spacer(Modifier.height(32.dp))
        
        Button(
            onClick = { onConfirm(bristolType, notes) },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(16.dp),
            enabled = bristolType != null
        ) {
            Text("Guardar Registro", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }
    }
}

@Composable
fun StatsScreen(movements: List<BowelMovement>) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                "Resumen de Salud",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }

        item {
            OverviewSection(movements)
        }

        item {
            HealthDistributionSection(movements)
        }

        item {
            HistoricalPatternsSection(movements)
        }
        
        item {
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
fun OverviewSection(movements: List<BowelMovement>) {
    val total = movements.size
    val last7Days = movements.count { 
        Instant.ofEpochMilli(it.timestamp).isAfter(Instant.now().minus(7, ChronoUnit.DAYS))
    }
    val thisMonth = movements.count {
        val date = Instant.ofEpochMilli(it.timestamp).atZone(ZoneId.systemDefault()).toLocalDate()
        date.month == LocalDate.now().month && date.year == LocalDate.now().year
    }

    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        StatCard("Total", total.toString(), Icons.Rounded.History, Modifier.weight(1f))
        StatCard("7 días", last7Days.toString(), Icons.Rounded.CalendarViewWeek, Modifier.weight(1f))
        StatCard("Mes", thisMonth.toString(), Icons.Rounded.CalendarMonth, Modifier.weight(1f))
    }
}

@Composable
fun StatCard(label: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Icon(icon, null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(8.dp))
            Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.outline)
        }
    }
}

@Composable
fun HealthDistributionSection(movements: List<BowelMovement>) {
    if (movements.isEmpty()) return

    val total = movements.size.toFloat()
    val ideal = movements.count { it.bristolType in 3..4 } / total
    val attention = movements.count { it.bristolType in listOf(2, 5, 6) } / total
    val warning = movements.count { it.bristolType in listOf(1, 7) } / total

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.1f))
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text("Distribución Bristol", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(16.dp))
            
            // Custom Bar Chart
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(24.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest)
            ) {
                if (warning > 0) Box(Modifier.weight(warning.coerceAtLeast(0.01f)).fillMaxHeight().background(Color(0xFFE57373)))
                if (attention > 0) Box(Modifier.weight(attention.coerceAtLeast(0.01f)).fillMaxHeight().background(Color(0xFFFFB74D)))
                if (ideal > 0) Box(Modifier.weight(ideal.coerceAtLeast(0.01f)).fillMaxHeight().background(Color(0xFF81C784)))
            }
            
            Spacer(Modifier.height(16.dp))
            
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                LegendItem("Ideal (Tipo 3-4)", Color(0xFF81C784), (ideal * 100).toInt())
                LegendItem("Atención (Tipo 2, 5, 6)", Color(0xFFFFB74D), (attention * 100).toInt())
                LegendItem("Advertencia (Tipo 1, 7)", Color(0xFFE57373), (warning * 100).toInt())
            }
        }
    }
}

@Composable
fun LegendItem(label: String, color: Color, percentage: Int) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(12.dp).clip(CircleShape).background(color))
        Spacer(Modifier.width(8.dp))
        Text(label, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
        Text("$percentage%", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun HistoricalPatternsSection(movements: List<BowelMovement>) {
    if (movements.isEmpty()) return

    val last7DaysMovements = movements.filter { 
        Instant.ofEpochMilli(it.timestamp).isAfter(Instant.now().minus(7, ChronoUnit.DAYS))
    }.sortedBy { it.timestamp }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text("Actividad últimos 7 días", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(24.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth().height(100.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                val today = LocalDate.now()
                (6 downTo 0).forEach { daysAgo ->
                    val date = today.minusDays(daysAgo.toLong())
                    val count = last7DaysMovements.count { 
                        Instant.ofEpochMilli(it.timestamp).atZone(ZoneId.systemDefault()).toLocalDate() == date 
                    }
                    
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier
                                .width(12.dp)
                                .height((count * 20).coerceAtMost(100).dp)
                                .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                .background(MaterialTheme.colorScheme.primary)
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale("es")).take(1),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            }
        }
    }
}
