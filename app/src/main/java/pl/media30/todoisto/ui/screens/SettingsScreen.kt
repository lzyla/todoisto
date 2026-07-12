package pl.media30.todoisto.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.CalendarViewWeek
import androidx.compose.material.icons.outlined.CloudDownload
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.SwipeLeft
import androidx.compose.material.icons.outlined.TextFields
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.VolumeUp
import androidx.compose.material.icons.outlined.Wallpaper
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import pl.media30.todoisto.ui.components.bouncy
import pl.media30.todoisto.ui.theme.GlassAccent
import pl.media30.todoisto.ui.theme.GlassBackground
import pl.media30.todoisto.ui.theme.GlassHair
import pl.media30.todoisto.ui.theme.GlassTextPrimary
import pl.media30.todoisto.ui.theme.GlassTextSecondary
import pl.media30.todoisto.ui.theme.GlassTheme
import pl.media30.todoisto.ui.theme.glass

/**
 * Pełnoekranowe „Ustawienia" (liquid glass) w układzie jak w referencji: lista
 * z ikonami i podsekcjami. Ekran główny + podekran „Ogólne" z realnymi,
 * zapisywanymi opcjami (widok startowy, rozpoznawanie dat, początek tygodnia,
 * dźwięk ukończenia, czynności przesuwania).
 */
@Composable
fun SettingsScreen(
    dark: Boolean,
    photo: Boolean,
    hasApiKey: Boolean,
    dailyGoal: Int,
    weeklyGoal: Int,
    startViewToday: Boolean,
    dateRecognition: Boolean,
    weekStartMonday: Boolean,
    completionSound: Boolean,
    swipeRightCompletes: Boolean,
    onBack: () -> Unit,
    onToggleDark: () -> Unit,
    onTogglePhoto: () -> Unit,
    onSetApiKey: (String) -> Unit,
    onSetGoals: (Int, Int) -> Unit,
    onSetStartView: (Boolean) -> Unit,
    onSetDateRecognition: (Boolean) -> Unit,
    onSetWeekStartMonday: (Boolean) -> Unit,
    onSetCompletionSound: (Boolean) -> Unit,
    onSetSwipeRightCompletes: (Boolean) -> Unit,
    onOpenPool: () -> Unit,
    onOpenImport: () -> Unit
) {
    var route by remember { mutableStateOf(0) } // 0 = główny, 1 = Ogólne
    var showKey by remember { mutableStateOf(false) }
    var showGoals by remember { mutableStateOf(false) }

    BackHandler { if (route != 0) route = 0 else onBack() }

    GlassBackground {
        Column(Modifier.fillMaxSize()) {
            TopBar(if (route == 0) "Ustawienia" else "Ogólne") {
                if (route != 0) route = 0 else onBack()
            }
            Column(
                Modifier.fillMaxWidth().weight(1f).verticalScroll(rememberScrollState())
                    .padding(horizontal = 18.dp).padding(top = 6.dp, bottom = 28.dp)
            ) {
                if (route == 0) MainSettings(
                    dark, photo, hasApiKey, dailyGoal, weeklyGoal,
                    onOpenGeneral = { route = 1 },
                    onToggleDark, onTogglePhoto,
                    onOpenKey = { showKey = true }, onOpenGoals = { showGoals = true },
                    onOpenPool, onOpenImport
                ) else GeneralSettings(
                    startViewToday, dateRecognition, weekStartMonday, completionSound, swipeRightCompletes,
                    onSetStartView, onSetDateRecognition, onSetWeekStartMonday, onSetCompletionSound, onSetSwipeRightCompletes
                )
                Spacer(Modifier.navigationBarsPadding())
            }
        }
    }

    if (showKey) KeyDialog(hasApiKey, { showKey = false }) { k -> onSetApiKey(k); showKey = false }
    if (showGoals) GoalsEditDialog(dailyGoal, weeklyGoal, { showGoals = false }) { d, w -> onSetGoals(d, w); showGoals = false }
}

@Composable
private fun MainSettings(
    dark: Boolean, photo: Boolean, hasApiKey: Boolean, dailyGoal: Int, weeklyGoal: Int,
    onOpenGeneral: () -> Unit,
    onToggleDark: () -> Unit, onTogglePhoto: () -> Unit,
    onOpenKey: () -> Unit, onOpenGoals: () -> Unit,
    onOpenPool: () -> Unit, onOpenImport: () -> Unit
) {
    SettingsHero()
    Spacer(Modifier.height(22.dp))

    SettingsSection("Ustawienia")
    SettingsCard {
        NavRow(Icons.Outlined.Tune, Color(0xFF8AA0FF), "Ogólne", "Widok startowy, daty, tydzień, przesuwanie", onClick = onOpenGeneral)
    }
    Spacer(Modifier.height(22.dp))

    SettingsSection("Personalizacja")
    SettingsCard {
        ToggleRow(Icons.Outlined.DarkMode, Color(0xFF7C6BFF), "Tryb ciemny", if (dark) "Włączony" else "Wyłączony", dark, onToggleDark)
        RowDivider()
        ToggleRow(Icons.Outlined.Wallpaper, Color(0xFF2DD4BF), "Tło zdjęciowe", if (photo) "Scena wg pory dnia" else "Gradient (mesh)", photo, onTogglePhoto)
    }
    Spacer(Modifier.height(22.dp))

    SettingsSection("Asystent AI")
    SettingsCard {
        NavRow(Icons.Outlined.Key, Color(0xFFB99CFF), "Klucz API (OpenAI)", if (hasApiKey) "Ustawiony — Zapytaj AI działa" else "Nie ustawiony", trailing = { StatusDot(hasApiKey) }, onClick = onOpenKey)
    }
    Text(
        "Klucz zostaje tylko na tym urządzeniu — nigdy nie trafia do repozytorium ani do nas.",
        fontSize = 11.5.sp, color = GlassTextSecondary.copy(alpha = 0.85f),
        modifier = Modifier.padding(start = 6.dp, top = 8.dp, end = 6.dp)
    )
    Spacer(Modifier.height(22.dp))

    SettingsSection("Produktywność")
    SettingsCard {
        NavRow(Icons.Outlined.EmojiEvents, Color(0xFFF4B740), "Cele", "Dzień $dailyGoal · Tydzień $weeklyGoal", onClick = onOpenGoals)
        RowDivider()
        NavRow(Icons.Outlined.Bolt, Color(0xFFFB7185), "Pula aktywności", "Twoje pomysły na wolny czas", onClick = onOpenPool)
        RowDivider()
        NavRow(Icons.Outlined.CloudDownload, Color(0xFF6B8AFF), "Import z arkusza", "Wczytaj aktywności z linku CSV", onClick = onOpenImport)
    }
    Spacer(Modifier.height(22.dp))

    SettingsSection("O aplikacji")
    SettingsCard {
        NavRow(Icons.Outlined.Info, Color(0xFF9AA6FF), "Wersja", "Todoisto 1.0", trailing = {})
        RowDivider()
        NavRow(Icons.Outlined.Lock, Color(0xFF34D399), "Dane", "Przechowywane lokalnie · offline, bez konta", trailing = {})
    }
    Spacer(Modifier.height(18.dp))
    Text(
        "Zrobione z ♥ dla spokojnego dnia.",
        fontSize = 11.5.sp, color = GlassTextSecondary.copy(alpha = 0.7f),
        modifier = Modifier.fillMaxWidth().padding(top = 2.dp)
    )
}

@Composable
private fun GeneralSettings(
    startViewToday: Boolean, dateRecognition: Boolean, weekStartMonday: Boolean,
    completionSound: Boolean, swipeRightCompletes: Boolean,
    onSetStartView: (Boolean) -> Unit, onSetDateRecognition: (Boolean) -> Unit,
    onSetWeekStartMonday: (Boolean) -> Unit, onSetCompletionSound: (Boolean) -> Unit,
    onSetSwipeRightCompletes: (Boolean) -> Unit
) {
    SettingsSection("Widok")
    SettingsCard {
        NavRow(
            Icons.Outlined.Home, Color(0xFF8AA0FF), "Widok główny",
            if (startViewToday) "Dziś" else "Nadchodzące"
        ) { onSetStartView(!startViewToday) }
    }
    Spacer(Modifier.height(22.dp))

    SettingsSection("Wprowadzanie")
    SettingsCard {
        ToggleRow(
            Icons.Outlined.TextFields, Color(0xFF2DD4BF), "Rozpoznawanie dat",
            "Automatyczne wykrywanie terminów w zadaniach", dateRecognition, { onSetDateRecognition(!dateRecognition) }
        )
    }
    Spacer(Modifier.height(22.dp))

    SettingsSection("Data i godzina")
    SettingsCard {
        NavRow(
            Icons.Outlined.CalendarViewWeek, Color(0xFFF4B740), "Zacznij tydzień od",
            if (weekStartMonday) "Poniedziałek" else "Niedziela"
        ) { onSetWeekStartMonday(!weekStartMonday) }
    }
    Spacer(Modifier.height(22.dp))

    SettingsSection("Czynności przesuwania")
    SettingsCard {
        NavRow(
            Icons.Outlined.SwipeLeft, Color(0xFFFB7185), "Przesuwanie kafelka",
            if (swipeRightCompletes) "W prawo: Ukończ · W lewo: Na jutro" else "W prawo: Na jutro · W lewo: Ukończ"
        ) { onSetSwipeRightCompletes(!swipeRightCompletes) }
    }
    Text(
        "Dotknij, aby zamienić strony przesuwania.",
        fontSize = 11.5.sp, color = GlassTextSecondary.copy(alpha = 0.85f),
        modifier = Modifier.padding(start = 6.dp, top = 8.dp, end = 6.dp)
    )
    Spacer(Modifier.height(22.dp))

    SettingsSection("Dźwięk")
    SettingsCard {
        ToggleRow(
            Icons.Outlined.VolumeUp, Color(0xFF9AA6FF), "Dźwięk ukończenia zadania",
            if (completionSound) "Włączony" else "Wyłączony", completionSound, { onSetCompletionSound(!completionSound) }
        )
    }
}

@Composable
private fun TopBar(title: String, onBack: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(42.dp).glass(CircleShape).bouncy(0.9f, onBack), contentAlignment = Alignment.Center) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Wróć", tint = GlassTextPrimary, modifier = Modifier.size(19.dp))
        }
        Spacer(Modifier.width(14.dp))
        Text(title, fontSize = 24.sp, fontWeight = FontWeight.W800, color = GlassTextPrimary)
    }
}

@Composable
private fun SettingsHero() {
    Row(Modifier.fillMaxWidth().glass(RoundedCornerShape(26.dp)).padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier.size(52.dp).clip(RoundedCornerShape(17.dp))
                .background(Brush.linearGradient(listOf(Color(0xFFA47CFF), Color(0xFF6B3FE0)))),
            contentAlignment = Alignment.Center
        ) { Icon(Icons.Filled.Check, null, tint = Color.White, modifier = Modifier.size(26.dp)) }
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text("Todoisto", fontSize = 20.sp, fontWeight = FontWeight.W800, color = GlassTextPrimary)
            Spacer(Modifier.height(3.dp))
            Text("Twój dzień, uporządkowany", fontSize = 13.sp, color = GlassTextSecondary)
        }
        Text(
            "1.0", fontSize = 12.sp, fontWeight = FontWeight.W800, color = GlassAccent,
            modifier = Modifier.clip(RoundedCornerShape(50))
                .background(GlassAccent.copy(alpha = if (GlassTheme.dark) 0.22f else 0.12f))
                .padding(horizontal = 12.dp, vertical = 6.dp)
        )
    }
}

@Composable
private fun SettingsSection(title: String) {
    Text(
        title.uppercase(),
        fontSize = 11.sp, fontWeight = FontWeight.W800, letterSpacing = 1.3.sp, color = GlassAccent,
        modifier = Modifier.padding(start = 8.dp, bottom = 10.dp)
    )
}

@Composable
private fun SettingsCard(content: @Composable () -> Unit) {
    Column(Modifier.fillMaxWidth().glass(RoundedCornerShape(22.dp))) { content() }
}

@Composable
private fun RowDivider() {
    Box(Modifier.fillMaxWidth().padding(start = 66.dp, end = 16.dp).height(1.dp).background(GlassHair))
}

@Composable
private fun SettingRowScaffold(
    icon: ImageVector, tint: Color, title: String, subtitle: String?,
    trailing: @Composable () -> Unit, onClick: (() -> Unit)?
) {
    val base = Modifier.fillMaxWidth()
    Row(
        (if (onClick != null) base.bouncy(0.98f, onClick) else base).padding(horizontal = 14.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(38.dp).clip(RoundedCornerShape(12.dp)).background(tint.copy(alpha = 0.16f)), contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = tint, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(title, fontSize = 15.sp, fontWeight = FontWeight.W700, color = GlassTextPrimary)
            if (subtitle != null) {
                Spacer(Modifier.height(2.dp))
                Text(subtitle, fontSize = 12.5.sp, color = GlassTextSecondary)
            }
        }
        Spacer(Modifier.width(10.dp))
        trailing()
    }
}

@Composable
private fun NavRow(
    icon: ImageVector, tint: Color, title: String, subtitle: String? = null,
    trailing: (@Composable () -> Unit)? = null, onClick: (() -> Unit)? = null
) {
    SettingRowScaffold(
        icon, tint, title, subtitle,
        trailing = trailing ?: { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = GlassTextSecondary.copy(alpha = 0.6f), modifier = Modifier.size(20.dp)) },
        onClick = onClick
    )
}

@Composable
private fun ToggleRow(icon: ImageVector, tint: Color, title: String, subtitle: String?, on: Boolean, onToggle: () -> Unit) {
    SettingRowScaffold(icon, tint, title, subtitle, trailing = { GlassToggle(on) }, onClick = onToggle)
}

@Composable
private fun GlassToggle(on: Boolean) {
    val knob by animateDpAsState(if (on) 19.dp else 3.dp, spring(dampingRatio = 0.6f), label = "knob")
    val track by animateColorAsState(if (on) GlassAccent else GlassTextSecondary.copy(alpha = 0.32f), tween(250), label = "track")
    Box(Modifier.size(40.dp, 24.dp).clip(RoundedCornerShape(12.dp)).background(track)) {
        Box(Modifier.offset(x = knob, y = 3.dp).size(18.dp).clip(CircleShape).background(Color.White))
    }
}

@Composable
private fun StatusDot(on: Boolean) {
    Box(Modifier.size(10.dp).clip(CircleShape).background(if (on) Color(0xFF34D399) else GlassTextSecondary.copy(alpha = 0.4f)))
}

@Composable
private fun KeyDialog(hasKey: Boolean, onDismiss: () -> Unit, onSave: (String) -> Unit) {
    var key by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = { onSave(key) }, enabled = key.isNotBlank()) { Text("Zapisz") } },
        dismissButton = {
            Row {
                if (hasKey) TextButton(onClick = { onSave("") }) { Text("Usuń") }
                TextButton(onClick = onDismiss) { Text("Anuluj") }
            }
        },
        title = { Text("Klucz AI (OpenAI)") },
        text = {
            Column {
                Text(
                    (if (hasKey) "Klucz jest ustawiony. Wklej nowy, aby zmienić.\n\n" else "") +
                        "Klucz utworzysz na platform.openai.com w sekcji API keys. Zostaje tylko na tym urządzeniu.",
                    fontSize = 12.5.sp
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(value = key, onValueChange = { key = it }, singleLine = true, placeholder = { Text("sk-…") }, modifier = Modifier.fillMaxWidth())
            }
        }
    )
}

@Composable
private fun GoalsEditDialog(daily: Int, weekly: Int, onDismiss: () -> Unit, onConfirm: (Int, Int) -> Unit) {
    var d by remember { mutableStateOf(daily.toString()) }
    var w by remember { mutableStateOf(weekly.toString()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = { onConfirm(d.toIntOrNull()?.coerceIn(1, 99) ?: daily, w.toIntOrNull()?.coerceIn(1, 999) ?: weekly) }) { Text("Zapisz") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Anuluj") } },
        title = { Text("Cele produktywności") },
        text = {
            Column {
                Text("Ile zadań chcesz domykać?", fontSize = 12.5.sp)
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(value = d, onValueChange = { d = it.filter { c -> c.isDigit() }.take(2) }, singleLine = true, label = { Text("Dziennie") }, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(value = w, onValueChange = { w = it.filter { c -> c.isDigit() }.take(3) }, singleLine = true, label = { Text("Tygodniowo") }, modifier = Modifier.fillMaxWidth())
            }
        }
    )
}
