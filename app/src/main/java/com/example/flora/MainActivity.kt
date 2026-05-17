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
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
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
import com.example.flora.data.Meal
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
                0 -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.surface)
                    ) {
                        CalendarHeader(selectedDate, movements, meals) { selectedDate = it }
                        
                        val filteredMovements = movements.filter {
                            val moveDate = Instant.ofEpochMilli(it.timestamp)
                                .atZone(ZoneId.systemDefault())
                                .toLocalDate()
                            moveDate == selectedDate
                        }

                        val filteredMeals = meals.filter {
                            val mealDate = Instant.ofEpochMilli(it.timestamp)
                                .atZone(ZoneId.systemDefault())
                                .toLocalDate()
                            mealDate == selectedDate
                        }
                        
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
                1 -> {
                    StatsScreen(movements, meals)
                }
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

fun exportStatsAsText(context: android.content.Context, movements: List<BowelMovement>, meals: List<Meal>) {
    val totalMovements = movements.size
    val totalMeals = meals.size
    
    val report = buildString {
        appendLine("📊 Reporte de Salud Flora")
        appendLine("Generado el: ${LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))}")
        appendLine("---------------------------")
        appendLine("🔹 Resumen Digestivo:")
        appendLine("- Registros totales: $totalMovements")
        if (totalMovements > 0) {
            val ideal = movements.count { it.bristolType in 3..4 }
            appendLine("- Salud ideal (Bristol 3-4): ${(ideal * 100 / totalMovements)}%")
        }
        appendLine()
        appendLine("🔹 Resumen Alimenticio:")
        appendLine("- Comidas registradas: $totalMeals")
        if (totalMeals > 0) {
            val lastMeal = meals.firstOrNull()
            appendLine("- Última comida: ${lastMeal?.description} (${lastMeal?.mealType})")
        }
        appendLine()
        appendLine("🔸 Últimos Eventos:")
        // Merge and take last 5
        val combined = (movements.map { it.timestamp to "Evacuación Tipo ${it.bristolType}" } + 
                        meals.map { it.timestamp to "Comida: ${it.description}" })
                        .sortedByDescending { it.first }
                        .take(5)
        
        combined.forEach { (ts, desc) ->
            val date = Instant.ofEpochMilli(ts).atZone(ZoneId.systemDefault()).toLocalDate()
            appendLine("- $date: $desc")
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
    allMeals: List<Meal>,
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
                    MonthView(selectedDate, displayedMonth, allMovements, allMeals, onDateSelected)
                } else {
                    WeekView(selectedDate, allMovements, allMeals, onDateSelected)
                }
            }
        }
    }
}

@Composable
fun WeekView(
    selectedDate: LocalDate,
    allMovements: List<BowelMovement>,
    allMeals: List<Meal>,
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
            DayItem(date, selectedDate, allMovements, allMeals, onDateSelected)
        }
    }
}

@Composable
fun MonthView(
    selectedDate: LocalDate,
    currentMonth: YearMonth,
    allMovements: List<BowelMovement>,
    allMeals: List<Meal>,
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
                                allMeals = allMeals,
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
    allMeals: List<Meal>,
    onDateSelected: (LocalDate) -> Unit,
    compact: Boolean = false
) {
    val isSelected = date == selectedDate
    val movementsForDay = allMovements.filter {
        Instant.ofEpochMilli(it.timestamp)
            .atZone(ZoneId.systemDefault())
            .toLocalDate() == date
    }
    
    val mealsForDay = allMeals.filter {
        Instant.ofEpochMilli(it.timestamp)
            .atZone(ZoneId.systemDefault())
            .toLocalDate() == date
    }
    
    val indicatorColor = if (movementsForDay.isEmpty()) {
        if (mealsForDay.isNotEmpty()) MaterialTheme.colorScheme.tertiary else Color.Transparent
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
            if (movementsForDay.isNotEmpty() || mealsForDay.isNotEmpty()) {
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

@Composable
fun MealItem(
    meal: Meal,
    onDelete: (Meal) -> Unit
) {
    val date = Instant.ofEpochMilli(meal.timestamp)
        .atZone(ZoneId.systemDefault())
        .toLocalDateTime()
    val formatter = DateTimeFormatter.ofPattern("HH:mm")
    var showDeleteConfirm by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.4f)
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
                    .background(MaterialTheme.colorScheme.tertiary.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Restaurant, 
                    contentDescription = null, 
                    tint = MaterialTheme.colorScheme.tertiary
                )
            }
            
            Spacer(Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = meal.mealType,
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
                Spacer(Modifier.height(4.dp))
                Text(
                    text = meal.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
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
            title = { Text("¿Eliminar registro de comida?") },
            text = { Text("Esta acción no se puede deshacer.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDelete(meal)
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
fun AddEntrySheetContent(
    recentMeals: List<String>,
    onAddMovement: (Int?, String) -> Unit,
    onAddMeal: (String, String) -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .padding(bottom = 32.dp)
    ) {
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = Color.Transparent,
            divider = {},
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                    color = MaterialTheme.colorScheme.primary
                )
            }
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("Evacuación") }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text("Comida") }
            )
        }
        
        Spacer(Modifier.height(24.dp))

        if (selectedTab == 0) {
            AddMovementForm(onConfirm = onAddMovement)
        } else {
            AddMealForm(
                suggestions = recentMeals,
                onConfirm = onAddMeal
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddMovementForm(onConfirm: (Int?, String) -> Unit) {
    var notes by remember { mutableStateOf("") }
    var bristolType by remember { mutableStateOf<Int?>(null) }

    val bristolDescription = when (bristolType) {
        1 -> "Trozos duros y separados (estreñimiento)"
        2 -> "Forma de cilindro alargado con bultos"
        3 -> "Como un cilindro alargado con grietas"
        4 -> "Suave y lisa (ideal)"
        5 -> "Trozos blandos con bordes definidos"
        6 -> "Trozos blandos con bordes deshechos"
        7 -> "Acuosa, sin trozos (diarrea)"
        else -> "Selecciona un tipo"
    }

    Column {
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

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun AddMealForm(suggestions: List<String>, onConfirm: (String, String) -> Unit) {
    var description by remember { mutableStateOf("") }
    var mealType by remember { mutableStateOf("Almuerzo") }
    val mealTypes = listOf("Desayuno", "Almuerzo", "Cena", "Snack")

    Column {
        Text(
            "¿Qué comiste?",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.height(16.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            mealTypes.forEach { type ->
                FilterChip(
                    selected = mealType == type,
                    onClick = { mealType = type },
                    label = { Text(type) }
                )
            }
        }
        
        Spacer(Modifier.height(16.dp))
        
        OutlinedTextField(
            value = description,
            onValueChange = { description = it },
            label = { Text("Descripción de la comida") },
            placeholder = { Text("Ej: Ensalada de pollo y arroz") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            leadingIcon = { Icon(Icons.Default.Restaurant, null) }
        )

        if (suggestions.isNotEmpty()) {
            Spacer(Modifier.height(12.dp))
            Text("Comidas recientes:", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.outline)
            Spacer(Modifier.height(8.dp))
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                suggestions.forEach { suggestion ->
                    SuggestionChip(
                        onClick = { description = suggestion },
                        label = { Text(suggestion) },
                        shape = RoundedCornerShape(8.dp)
                    )
                }
            }
        }
        
        Spacer(Modifier.height(32.dp))
        
        Button(
            onClick = { onConfirm(description, mealType) },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(16.dp),
            enabled = description.isNotEmpty()
        ) {
            Text("Guardar Comida", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }
    }
}

@Composable
fun StatsScreen(movements: List<BowelMovement>, meals: List<Meal>) {
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
            FoodCorrelationSection(movements, meals)
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
fun FoodCorrelationSection(movements: List<BowelMovement>, meals: List<Meal>) {
    if (movements.isEmpty() || meals.isEmpty()) return

    val correlations = remember(movements, meals) {
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
            Triple(name.replaceFirstChar { it.uppercase() }, avgScore, stats.second)
        }.filter { it.third >= 1 }
        .sortedByDescending { it.second }
    }

    val bestFoods = correlations.filter { it.second > 0f }.take(3)
    val worstFoods = correlations.filter { it.second <= 0f }.sortedBy { it.second }.take(3)

    if (bestFoods.isEmpty() && worstFoods.isEmpty()) return

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Restaurant, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("Correlación Alimento-Salud", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(16.dp))

            if (bestFoods.isNotEmpty()) {
                Text("Mejor digestión con:", style = MaterialTheme.typography.labelMedium, color = Color(0xFF81C784))
                bestFoods.forEach { (name, score, _) ->
                    val quality = when {
                        score >= 8f -> "Excelente"
                        else -> "Buena"
                    }
                    CorrelationRow(name, quality, Color(0xFF81C784), Icons.Default.ThumbUp)
                }
            }

            if (worstFoods.isNotEmpty()) {
                if (bestFoods.isNotEmpty()) Spacer(Modifier.height(12.dp))
                Text("Menor digestión con:", style = MaterialTheme.typography.labelMedium, color = Color(0xFFE57373))
                worstFoods.forEach { (name, score, _) ->
                    val quality = when {
                        score <= -15f -> "Muy Pesada"
                        else -> "Pesada"
                    }
                    CorrelationRow(name, quality, Color(0xFFE57373), Icons.Default.ThumbDown)
                }
            }
        }
    }
}

@Composable
fun CorrelationRow(name: String, quality: String, color: Color, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = color.copy(alpha = 0.6f), modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(8.dp))
        Text(name, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        Text(
            quality,
            style = MaterialTheme.typography.labelLarge, 
            fontWeight = FontWeight.Bold,
            color = color
        )
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
