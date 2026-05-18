package com.example.flora.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.ThumbDown
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.CalendarViewWeek
import androidx.compose.material.icons.rounded.History
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.flora.FloraViewModel
import com.example.flora.data.BowelMovement
import com.example.flora.model.FoodCorrelation
import com.example.flora.model.HealthStats
import com.example.flora.ui.theme.BristolAttention
import com.example.flora.ui.theme.BristolIdeal
import com.example.flora.ui.theme.BristolWarning
import com.example.flora.util.toLocalDate
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun StatsScreen(viewModel: FloraViewModel) {
    val stats by viewModel.healthStats.collectAsState()
    val correlations by viewModel.foodCorrelations.collectAsState()
    val movements by viewModel.allMovements.collectAsState()

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

        stats?.let {
            item { OverviewSection(it) }
            item { HealthDistributionSection(it) }
        }

        if (correlations.isNotEmpty()) {
            item { FoodCorrelationSection(correlations) }
        }

        if (movements.isNotEmpty()) {
            item { HistoricalPatternsSection(movements) }
        }
        
        item { Spacer(Modifier.height(16.dp)) }
    }
}

@Composable
fun OverviewSection(stats: HealthStats) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        StatCard("Total", stats.totalMovements.toString(), Icons.Rounded.History, Modifier.weight(1f))
        StatCard("7 días", stats.last7Days.toString(), Icons.Rounded.CalendarViewWeek, Modifier.weight(1f))
        StatCard("Mes", stats.thisMonth.toString(), Icons.Rounded.CalendarMonth, Modifier.weight(1f))
    }
}

@Composable
fun StatCard(label: String, value: String, icon: ImageVector, modifier: Modifier = Modifier) {
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
fun HealthDistributionSection(stats: HealthStats) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.1f))
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text("Distribución Bristol", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(16.dp))
            
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(24.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest)
            ) {
                if (stats.warningPercentage > 0) Box(Modifier.weight(stats.warningPercentage.toFloat().coerceAtLeast(1f)).fillMaxHeight().background(BristolWarning))
                if (stats.attentionPercentage > 0) Box(Modifier.weight(stats.attentionPercentage.toFloat().coerceAtLeast(1f)).fillMaxHeight().background(BristolAttention))
                if (stats.idealPercentage > 0) Box(Modifier.weight(stats.idealPercentage.toFloat().coerceAtLeast(1f)).fillMaxHeight().background(BristolIdeal))
            }
            
            Spacer(Modifier.height(16.dp))
            
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                LegendItem("Ideal (Tipo 3-4)", BristolIdeal, stats.idealPercentage)
                LegendItem("Atención (Tipo 2, 5, 6)", BristolAttention, stats.attentionPercentage)
                LegendItem("Advertencia (Tipo 1, 7)", BristolWarning, stats.warningPercentage)
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
fun FoodCorrelationSection(correlations: List<FoodCorrelation>) {
    val bestFoods = correlations.filter { it.score > 0f }.take(3)
    val worstFoods = correlations.filter { it.score <= 0f }.sortedBy { it.score }.take(3)

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
                Text("Mejor digestión con:", style = MaterialTheme.typography.labelMedium, color = BristolIdeal)
                bestFoods.forEach { correlation ->
                    val quality = if (correlation.score >= 8f) "Excelente" else "Buena"
                    CorrelationRow(correlation.name, quality, BristolIdeal, Icons.Default.ThumbUp)
                }
            }

            if (worstFoods.isNotEmpty()) {
                if (bestFoods.isNotEmpty()) Spacer(Modifier.height(12.dp))
                Text("Menor digestión con:", style = MaterialTheme.typography.labelMedium, color = BristolWarning)
                worstFoods.forEach { correlation ->
                    val quality = if (correlation.score <= -15f) "Muy Pesada" else "Pesada"
                    CorrelationRow(correlation.name, quality, BristolWarning, Icons.Default.ThumbDown)
                }
            }
        }
    }
}

@Composable
fun CorrelationRow(name: String, quality: String, color: Color, icon: ImageVector) {
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
fun HistoricalPatternsSection(movements: List<BowelMovement>) {
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
                    val count = movements.count { it.timestamp.toLocalDate() == date }
                    
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
