package com.example.flora.ui.sheets

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.flora.ui.components.getBristolColor
import com.example.flora.ui.components.getBristolDescription
import com.example.flora.ui.components.getBristolIcon

@OptIn(ExperimentalMaterial3Api::class)
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

    Column {
        Text(
            "¿Cómo fue la consistencia?",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            getBristolDescription(bristolType),
            style = MaterialTheme.typography.bodyMedium,
            color = if (bristolType != null) getBristolColor(bristolType) else MaterialTheme.colorScheme.outline
        )
        Spacer(Modifier.height(16.dp))
        
        if (bristolType != null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(getBristolColor(bristolType).copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(getBristolIcon(bristolType)),
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = getBristolColor(bristolType)
                )
            }
            Spacer(Modifier.height(16.dp))
        }

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
        
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
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
