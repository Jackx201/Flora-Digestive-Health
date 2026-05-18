package com.example.flora.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.flora.data.BowelMovement
import com.example.flora.data.Meal
import com.example.flora.util.toLocalDate
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

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
    val firstDayOfWeek = firstDayOfMonth.dayOfWeek.value
    val emptySlots = firstDayOfWeek - 1

    Column(modifier = Modifier.padding(bottom = 8.dp)) {
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
    val movementsForDay = allMovements.filter { it.timestamp.toLocalDate() == date }
    val mealsForDay = allMeals.filter { it.timestamp.toLocalDate() == date }
    
    val indicatorColor = if (movementsForDay.isEmpty()) {
        if (mealsForDay.isNotEmpty()) MaterialTheme.colorScheme.tertiary else Color.Transparent
    } else {
        val worstType = movementsForDay.mapNotNull { it.bristolType }.maxByOrNull { 
            when(it) {
                1, 7 -> 3
                2, 5, 6 -> 2
                else -> 1
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
