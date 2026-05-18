package com.example.flora.util

import android.content.Context
import android.content.Intent
import com.example.flora.data.BowelMovement
import com.example.flora.data.Meal
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

fun exportStatsAsText(context: Context, movements: List<BowelMovement>, meals: List<Meal>) {
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
