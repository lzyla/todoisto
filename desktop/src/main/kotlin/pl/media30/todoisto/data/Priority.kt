package pl.media30.todoisto.data

import androidx.compose.ui.graphics.Color

/**
 * Task priority levels, in the style of Todoist (P1 highest -> P4 lowest/none).
 */
enum class Priority(val label: String, val color: Color) {
    P1("Priorytet 1", Color(0xFFD1453B)),
    P2("Priorytet 2", Color(0xFFEB8909)),
    P3("Priorytet 3", Color(0xFF246FE0)),
    P4("Brak priorytetu", Color(0xFF9E9E9E));

    companion object {
        fun fromOrdinalSafe(value: Int): Priority =
            entries.getOrElse(value) { P4 }
    }
}
