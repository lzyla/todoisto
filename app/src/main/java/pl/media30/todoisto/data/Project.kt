package pl.media30.todoisto.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "projects")
data class Project(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val colorArgb: Long = 0xFF9B6BFF,
    val position: Int = 0,
    val isFavorite: Boolean = false,
    val isArchived: Boolean = false,
    /** Typ wysiłku tego projektu — do doboru w Puli aktywności (null = nieoznaczony → MENTAL). */
    val workEffortType: EffortType? = null,
    /** Obszar, do którego należy projekt (null = nieprzypisany, widoczny w „Wszystko"). */
    val areaId: Long? = null
)

@Entity(tableName = "sections")
data class Section(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val projectId: Long,
    val name: String,
    val position: Int = 0
)

@Entity(tableName = "labels")
data class Label(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val colorArgb: Long = 0xFF4D6BFF,
    val isFavorite: Boolean = false
)

/** Preset colours offered when creating projects/labels. */
object PaletteColors {
    val options: List<Long> = listOf(
        0xFF9B6BFF, // violet
        0xFFC24DFF, // magenta
        0xFF4D6BFF, // blue
        0xFF2DD4BF, // teal
        0xFF34D399, // green
        0xFFFBBF24, // amber
        0xFFFB7185, // rose
        0xFFF87171  // red
    )
}
