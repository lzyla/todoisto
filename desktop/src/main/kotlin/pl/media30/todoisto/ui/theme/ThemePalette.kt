package pl.media30.todoisto.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Motyw kolorystyczny apki — steruje kolorem akcentu (przyciski, chipy,
 * checkboxy, linki, aktywne stany) oraz odcieniem kart. Tło-gradient dobiera
 * się dalej wg pory dnia, ale akcent i karty przyjmują barwę motywu.
 */
data class ThemePalette(
    val id: String,
    val name: String,
    val swatch: Color,          // próbka do wyboru w UI
    val accentLight: Color,
    val accentDark: Color,
    val tintLight: Color,
    val tintDark: Color
)

object ThemePalettes {
    val all: List<ThemePalette> = listOf(
        ThemePalette("violet", "Fiolet", Color(0xFF6E45D9), Color(0xFF6E45D9), Color(0xFFB99CFF), Color(0xFFEAE2FB), Color(0xFF7160B8)),
        ThemePalette("ocean", "Ocean", Color(0xFF2563C9), Color(0xFF2563C9), Color(0xFF7FB0FF), Color(0xFFE0EAFB), Color(0xFF5A6FA8)),
        ThemePalette("forest", "Las", Color(0xFF1F9D6B), Color(0xFF1F9D6B), Color(0xFF74E0B0), Color(0xFFDCF2E8), Color(0xFF4F9E80)),
        ThemePalette("sunset", "Zachód", Color(0xFFE0632C), Color(0xFFE0632C), Color(0xFFFFA477), Color(0xFFFCE6D9), Color(0xFFB8785A)),
        ThemePalette("rose", "Róż", Color(0xFFD6417F), Color(0xFFD6417F), Color(0xFFFF9CC4), Color(0xFFFBE2EE), Color(0xFFB86088)),
        ThemePalette("graphite", "Grafit", Color(0xFF5A5A72), Color(0xFF5A5A72), Color(0xFFB9B9CC), Color(0xFFE8E8EF), Color(0xFF6E6E80))
    )

    fun byId(id: String): ThemePalette = all.firstOrNull { it.id == id } ?: all.first()
}
