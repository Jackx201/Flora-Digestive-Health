package com.example.flora.ui.components

import androidx.compose.ui.graphics.Color
import com.example.flora.R
import com.example.flora.ui.theme.BristolAttention
import com.example.flora.ui.theme.BristolIdeal
import com.example.flora.ui.theme.BristolUnknown
import com.example.flora.ui.theme.BristolWarning

fun getBristolColor(type: Int?): Color {
    return when (type) {
        1, 7 -> BristolWarning
        2, 5, 6 -> BristolAttention
        3, 4 -> BristolIdeal
        else -> BristolUnknown
    }
}

fun getBristolIcon(type: Int?): Int {
    return when (type) {
        1 -> R.drawable.bristol_1
        2 -> R.drawable.bristol_2
        3 -> R.drawable.bristol_3
        4 -> R.drawable.bristol_4
        5 -> R.drawable.bristol_5
        6 -> R.drawable.bristol_6
        7 -> R.drawable.bristol_7
        else -> R.drawable.ic_launcher_foreground
    }
}

fun getBristolDescription(type: Int?): String {
    return when (type) {
        1 -> "Trozos duros y separados (estreñimiento)"
        2 -> "Forma de cilindro alargado con bultos"
        3 -> "Como un cilindro alargado con grietas"
        4 -> "Suave y lisa (ideal)"
        5 -> "Trozos blandos con bordes definidos"
        6 -> "Trozos blandos con bordes deshechos"
        7 -> "Acuosa, sin trozos (diarrea)"
        else -> "Selecciona un tipo"
    }
}
