package pl.media30.todoisto.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Obszar (kontekst życiowy) — jeden poziom nad projektami: Osobiste, Vocative,
 * Praca, Projekty… Zadanie należy do obszaru przez swój projekt, a zadania bez
 * projektu (Skrzynka) mają [Task.areaId] bezpośrednio. Przełącznik u góry filtruje
 * całą appkę; brak wybranego obszaru = „Wszystko".
 */
@Entity(tableName = "areas")
data class Area(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val colorArgb: Long = 0xFF9B6BFF,
    val position: Int = 0
)
