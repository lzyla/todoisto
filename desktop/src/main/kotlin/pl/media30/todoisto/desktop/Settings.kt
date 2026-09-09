package pl.media30.todoisto.desktop

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import pl.media30.todoisto.ui.theme.ThemePalettes
import java.io.File
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

// ─── Ustawienia (route 0–3) — jak `SettingsScreen` ──────────────────────────
@Composable
fun SettingsScreen(st: AppState) {
    val g = LocalGlass.current
    var route by remember { mutableStateOf(0) }
    st.settingsRev
    val s = st.settings
    FullScreen {
        Column(Modifier.fillMaxSize()) {
            ScreenTopBar(listOf("Ustawienia", "Ogólne", "Tło", "Motyw")[route], onBack = { if (route != 0) route = 0 else st.showSettings = false })
            Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 18.dp).padding(bottom = 28.dp)) {
                Box(Modifier.widthIn(max = 720.dp).align(Alignment.CenterHorizontally)) {
                    when (route) {
                        0 -> MainSettings(st) { route = it }
                        1 -> GeneralSettings(st)
                        2 -> BackgroundSettings(st)
                        3 -> ThemeSettings(st)
                    }
                }
            }
        }
    }
    Unit.let { s; g }
}

@Composable
private fun SettingsCard(content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxWidth().glass(RoundedCornerShape(22.dp))) { content() }
}

@Composable
private fun Section(title: String) { SectionHeader(title, modifier = Modifier.padding(start = 8.dp, bottom = 10.dp)) }

@Composable
private fun Chevron() { val g = LocalGlass.current; Icon(Icons.Filled.KeyboardArrowRight, null, tint = g.textSecondary, modifier = Modifier.size(20.dp)) }

@Composable
private fun StatusDot(on: Boolean) { val g = LocalGlass.current; Dot(if (on) Color(0xFF34D399) else g.textSecondary.copy(alpha = 0.4f), 10.dp) }

@Composable
private fun MainSettings(st: AppState, go: (Int) -> Unit) {
    val g = LocalGlass.current
    val s = st.settings
    Column {
        // Hero
        Row(Modifier.fillMaxWidth().glass(RoundedCornerShape(26.dp)).padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(52.dp).clip(RoundedCornerShape(17.dp)).background(Brush.linearGradient(listOf(Color(0xFFA47CFF), Color(0xFF6B3FE0)))), contentAlignment = Alignment.Center) { Icon(Icons.Filled.Check, null, tint = Color.White, modifier = Modifier.size(26.dp)) }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text("Todoisto", fontSize = 20.sp, fontWeight = FontWeight.W800, color = g.textPrimary, fontFamily = Sora)
                Text("Twój dzień, uporządkowany", fontSize = 13.sp, color = g.textSecondary, fontFamily = Manrope)
            }
            Box(Modifier.clip(RoundedCornerShape(50)).background(g.accent.copy(alpha = 0.12f)).padding(horizontal = 10.dp, vertical = 4.dp)) { Text("Mac 1.1", fontSize = 12.sp, fontWeight = FontWeight.W800, color = g.accent, fontFamily = Manrope) }
        }
        Spacer(Modifier.height(22.dp))

        Section("Ustawienia")
        SettingsCard {
            SettingRow(Icons.Outlined.AccountCircle, Color(0xFF9B6BFF), "Konto", "Profil, chmura, Gmail, zużycie AI", onClick = { st.showAccount = true }) { Chevron() }
            RowDivider()
            SettingRow(Icons.Outlined.Tune, Color(0xFF8AA0FF), "Ogólne", "Widok startowy, daty, tydzień, przesuwanie", onClick = { go(1) }) { Chevron() }
            RowDivider()
            SettingRow(Icons.Outlined.Sync, Color(0xFF2DD4BF), "Synchronizacja automatyczna", if (s.autoSync) "Włączona — start, co 2 min i po zmianie" else "Wyłączona (⌘S synchronizuje ręcznie)") { GlassToggle(s.autoSync) { s.autoSync = it; st.settingsChanged() } }
        }
        Spacer(Modifier.height(22.dp))

        Section("Personalizacja")
        SettingsCard {
            SettingRow(Icons.Outlined.Palette, Color(0xFFFB7185), "Motyw", ThemePalettes.byId(s.themeId).name, onClick = { go(3) }) { Chevron() }
            RowDivider()
            SettingRow(Icons.Outlined.DarkMode, Color(0xFF7C6BFF), "Tryb ciemny", if (s.darkTheme) "Włączony" else "Wyłączony") { GlassToggle(s.darkTheme) { s.darkTheme = it; st.settingsChanged() } }
            RowDivider()
            val bgName = when { s.activeCustomBg.isNotBlank() -> "Własne zdjęcie"; s.photoBackground -> "Scena wg pory dnia"; else -> "Gradient (mesh)" }
            SettingRow(Icons.Outlined.Wallpaper, Color(0xFF2DD4BF), "Tło", bgName, onClick = { go(2) }) { Chevron() }
            RowDivider()
            SettingRow(Icons.Outlined.ViewSidebar, Color(0xFF8AA0FF), "Menu boczne", if (st.drawerOpen) "Widoczne" else "Ukryte (☰ lub ⌘B)") { GlassToggle(st.drawerOpen) { st.setDrawer(it) } }
            RowDivider()
            SettingRow(Icons.Outlined.Dock, Color(0xFF2DD4BF), "Dock na dole zawsze widoczny", if (s.dockAlwaysVisible) "Zawsze" else "Wysuwa się po najechaniu myszą na dół okna") { GlassToggle(s.dockAlwaysVisible) { s.dockAlwaysVisible = it; st.settingsChanged() } }
        }
        Spacer(Modifier.height(22.dp))

        Section("Asystent AI")
        SettingsCard {
            SettingRow(Icons.Outlined.AutoAwesome, g.accent, "Klucz API (OpenAI)", if (s.openAiKey.isNotBlank()) "Ustawiony — Zapytaj AI działa" else "Nie ustawiony", onClick = { st.dialog = DialogKind.ApiKey }) { StatusDot(s.openAiKey.isNotBlank()) }
        }
        Text("Klucz zostaje tylko na tym komputerze — nigdy nie trafia do repozytorium ani do nas.", fontSize = 11.5.sp, color = g.textSecondary.copy(alpha = 0.85f), fontFamily = Manrope, modifier = Modifier.padding(start = 8.dp, top = 8.dp))
        Spacer(Modifier.height(22.dp))

        Section("Produktywność")
        SettingsCard {
            SettingRow(Icons.Outlined.EmojiEvents, Color(0xFFF4B740), "Cele", "Dzień ${s.dailyGoal} · Tydzień ${s.weeklyGoal}", onClick = { st.dialog = DialogKind.Goals }) { Chevron() }
            RowDivider()
            SettingRow(Icons.Outlined.Bolt, Color(0xFFFB7185), "Pula aktywności", "Twoje pomysły na wolny czas", onClick = { st.showSettings = false; st.showPool = true }) { Chevron() }
            RowDivider()
            SettingRow(Icons.Outlined.CloudDownload, Color(0xFF6B8AFF), "Import z arkusza", "Wczytaj aktywności z linku CSV", onClick = { st.showSettings = false; st.showImport = true }) { Chevron() }
            RowDivider()
            SettingRow(Icons.Outlined.Insights, Color(0xFF34D399), "Statystyki", "Passa, wykresy, przesunięcia", onClick = { st.showStats = true }) { Chevron() }
        }
        Spacer(Modifier.height(22.dp))

        Section("Porządki")
        SettingsCard {
            SettingRow(Icons.Outlined.DeleteSweep, Color(0xFFFB7185), "Usuń ukończone", "Czyści listę Ukończone (na wszystkich urządzeniach po synchronizacji)", onClick = { st.deleteCompleted(); st.toast = "Ukończone usunięte" }) { Chevron() }
        }
        Spacer(Modifier.height(22.dp))

        Section("O aplikacji")
        SettingsCard {
            SettingRow(Icons.Outlined.Info, Color(0xFF9AA6FF), "Wersja", "Todoisto na macOS · dane w ~/.todoisto", null) {}
            RowDivider()
            SettingRow(Icons.Outlined.Lock, Color(0xFF34D399), "Dane", "Przechowywane lokalnie · synchronizacja tylko przez Twój projekt Supabase", null) {}
        }
        Text("Zrobione z ♥ dla spokojnego dnia.", fontSize = 11.5.sp, color = g.textSecondary.copy(alpha = 0.7f), fontFamily = Manrope, modifier = Modifier.padding(start = 8.dp, top = 10.dp))
    }
}

@Composable
private fun GeneralSettings(st: AppState) {
    val g = LocalGlass.current
    val s = st.settings
    Column {
        Section("Widok")
        SettingsCard { SettingRow(Icons.Outlined.Home, Color(0xFF8AA0FF), "Widok główny", if (s.startView == "today") "Dziś" else "Nadchodzące", onClick = { s.startView = if (s.startView == "today") "upcoming" else "today"; st.settingsChanged() }) { Chevron() } }
        Spacer(Modifier.height(22.dp))
        Section("Wprowadzanie")
        SettingsCard { SettingRow(Icons.Outlined.TextFields, Color(0xFF2DD4BF), "Rozpoznawanie dat", "Automatyczne wykrywanie terminów w zadaniach") { GlassToggle(s.dateRecognition) { s.dateRecognition = it; st.settingsChanged() } } }
        Spacer(Modifier.height(22.dp))
        Section("Data i godzina")
        SettingsCard { SettingRow(Icons.Outlined.CalendarViewWeek, Color(0xFFF4B740), "Zacznij tydzień od", if (s.weekStartMonday) "Poniedziałek" else "Niedziela", onClick = { s.weekStartMonday = !s.weekStartMonday; st.settingsChanged() }) { Chevron() } }
        Spacer(Modifier.height(22.dp))
        Section("Czynności przesuwania (telefon)")
        SettingsCard { SettingRow(Icons.Outlined.SwipeLeft, Color(0xFFFB7185), "Przesuwanie kafelka", if (s.swipeRightCompletes) "W prawo: Ukończ · W lewo: Na jutro" else "W prawo: Na jutro · W lewo: Ukończ", onClick = { s.swipeRightCompletes = !s.swipeRightCompletes; st.settingsChanged() }) { Chevron() } }
        Text("Dotknij, aby zamienić strony przesuwania. Na Macu odpowiednikiem są ikony przy zadaniu.", fontSize = 11.5.sp, color = g.textSecondary, fontFamily = Manrope, modifier = Modifier.padding(start = 8.dp, top = 8.dp))
        Spacer(Modifier.height(22.dp))
        Section("Dźwięk")
        SettingsCard { SettingRow(Icons.Outlined.VolumeUp, Color(0xFF9AA6FF), "Dźwięk ukończenia zadania", if (s.completionSound) "Włączony" else "Wyłączony") { GlassToggle(s.completionSound) { s.completionSound = it; st.settingsChanged() } } }
    }
}

@Composable
private fun BackgroundSettings(st: AppState) {
    val g = LocalGlass.current
    val s = st.settings
    val customActive = s.activeCustomBg.isNotBlank()
    Column {
        Section("Rodzaj tła")
        SettingsCard {
            BgOption(Icons.Outlined.Gradient, Color(0xFFB99CFF), "Gradient (mesh)", "Domyślne", !s.photoBackground && !customActive) { s.activeCustomBg = ""; s.photoBackground = false; st.settingsChanged() }
            RowDivider()
            BgOption(Icons.Outlined.WbSunny, Color(0xFFF4B740), "Scena wg pory dnia", "Barwy: rano / dzień / wieczór", s.photoBackground && !customActive) { s.activeCustomBg = ""; s.photoBackground = true; st.settingsChanged() }
        }
        Spacer(Modifier.height(22.dp))
        Section("Zdjęcie w tle")
        Column(Modifier.fillMaxWidth().glass(RoundedCornerShape(22.dp)).padding(14.dp)) {
            Text("Wrzuć do 3 własnych zdjęć lub wylosuj gotowe. Kliknij kafelek, aby ustawić jako tło.", fontSize = 13.sp, color = g.textSecondary, fontFamily = Manrope)
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                s.customPhotos.forEach { path ->
                    val bmp = remember(path) { runCatching { javax.imageio.ImageIO.read(File(path)).toComposeImageBitmap() }.getOrNull() }
                    Box(Modifier.width(110.dp).height(170.dp).clip(RoundedCornerShape(16.dp)).background(g.field).then(if (s.activeCustomBg == path) Modifier.border(2.5.dp, g.accent, RoundedCornerShape(16.dp)) else Modifier).clickable { st.setActiveCustomBg(path) }) {
                        if (bmp != null) Image(bmp, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                        Box(Modifier.align(Alignment.TopEnd).padding(6.dp).size(22.dp).clip(CircleShape).background(Color(0x99000000)).clickable { st.removeCustomPhoto(path) }, contentAlignment = Alignment.Center) { Icon(Icons.Filled.Close, "Usuń", tint = Color.White, modifier = Modifier.size(13.dp)) }
                    }
                }
                if (s.customPhotos.size < 3) Box(Modifier.width(110.dp).height(170.dp).clip(RoundedCornerShape(16.dp)).background(g.field).border(1.dp, g.hair, RoundedCornerShape(16.dp)).clickable { pickImageFile()?.let { st.addCustomPhoto(it) } }, contentAlignment = Alignment.Center) {
                    Icon(Icons.Filled.Add, "Dodaj zdjęcie", tint = g.accent, modifier = Modifier.size(26.dp))
                }
            }
            Spacer(Modifier.height(12.dp))
            PrimaryBtn(if (st.bgBusy) "Losuję zdjęcia…" else "Losuj zdjęcia w tle", icon = Icons.Outlined.Shuffle, enabled = !st.bgBusy) { st.addPresetBackgrounds() }
            Spacer(Modifier.height(8.dp))
            Text("Losowe zdjęcia z kolorową maską w barwach apki. Klikaj do skutku — za każdym razem inny zestaw.", fontSize = 11.5.sp, color = g.textSecondary, fontFamily = Manrope)
        }
        Text("Zdjęcia pobierają się z internetu i zapisują tylko na tym komputerze. Gdy brak sieci, losujemy gradient.", fontSize = 11.5.sp, color = g.textSecondary.copy(alpha = 0.85f), fontFamily = Manrope, modifier = Modifier.padding(start = 8.dp, top = 8.dp))
    }
}

@Composable
private fun BgOption(icon: androidx.compose.ui.graphics.vector.ImageVector, tint: Color, title: String, sub: String, selected: Boolean, onClick: () -> Unit) {
    val g = LocalGlass.current
    SettingRow(icon, tint, title, sub, onClick = onClick) {
        Box(Modifier.size(22.dp).clip(CircleShape).background(if (selected) g.accent else Color.Transparent).border(2.dp, if (selected) g.accent else g.textSecondary.copy(alpha = 0.4f), CircleShape), contentAlignment = Alignment.Center) {
            if (selected) Icon(Icons.Filled.Check, null, tint = Color.White, modifier = Modifier.size(13.dp))
        }
    }
}

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
private fun ThemeSettings(st: AppState) {
    val g = LocalGlass.current
    Column {
        Section("Kolor akcentu")
        Column(Modifier.fillMaxWidth().glass(RoundedCornerShape(22.dp)).padding(16.dp)) {
            Text("Zmienia kolor przycisków, akcentów i kart w całej apce.", fontSize = 13.sp, color = g.textSecondary, fontFamily = Manrope)
            Spacer(Modifier.height(14.dp))
            ThemeSwatches(st, 52.dp)
        }
    }
}

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
private fun ThemeSwatches(st: AppState, size: androidx.compose.ui.unit.Dp) {
    val g = LocalGlass.current
    FlowRow(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        ThemePalettes.all.forEach { p ->
            val sel = p.id == st.settings.themeId
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { st.settings.themeId = p.id; st.settingsChanged() }) {
                Box(Modifier.size(size).clip(CircleShape).background(p.swatch).then(if (sel) Modifier.border(3.dp, g.textPrimary, CircleShape) else Modifier), contentAlignment = Alignment.Center) {
                    if (sel) Icon(Icons.Filled.Check, null, tint = Color.White, modifier = Modifier.size(24.dp))
                }
                Text(p.name, fontSize = 12.sp, fontWeight = if (sel) FontWeight.W800 else FontWeight.W600, color = g.textPrimary, fontFamily = Manrope)
            }
        }
    }
}

// ─── Konto — jak `AccountScreen` ────────────────────────────────────────────
@Composable
fun AccountScreen(st: AppState) {
    val g = LocalGlass.current
    st.settingsRev
    val s = st.settings
    var url by remember { mutableStateOf(s.url) }
    var anon by remember { mutableStateOf(s.anonKey) }
    var email by remember { mutableStateOf(s.email) }
    var password by remember { mutableStateOf("") }
    var gUser by remember { mutableStateOf(s.gmailUser) }
    var gPass by remember { mutableStateOf("") }
    var editName by remember { mutableStateOf(false) }
    var nameDraft by remember { mutableStateOf(s.userName) }
    LaunchedEffect(Unit) { if (s.openAiAdminKey.isNotBlank() && st.aiCost == null) st.refreshAiCost() }
    FullScreen {
        Column(Modifier.fillMaxSize()) {
            ScreenTopBar("Konto", onBack = { st.showAccount = false })
            Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 18.dp).padding(bottom = 28.dp)) {
                Column(Modifier.widthIn(max = 720.dp).align(Alignment.CenterHorizontally)) {
                    // Profil
                    Row(Modifier.fillMaxWidth().glass(RoundedCornerShape(24.dp)).padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.clickable { pickImageFile()?.let { f -> s.avatarPath = f.absolutePath; st.settingsChanged() } }) { Avatar(st, 60.dp) }
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            if (editName) Row(verticalAlignment = Alignment.CenterVertically) {
                                GlassField(nameDraft, { nameDraft = it }, "Imię", Modifier.weight(1f), minHeight = 40.dp, onEnter = { s.userName = nameDraft; st.settingsChanged(); editName = false })
                                Spacer(Modifier.width(8.dp)); SecondaryBtn("Zapisz") { s.userName = nameDraft; st.settingsChanged(); editName = false }
                            } else Text(s.userName.ifBlank { "Użytkownik" }, fontSize = 18.sp, fontWeight = FontWeight.W800, color = g.textPrimary, fontFamily = Sora, modifier = Modifier.clickable { editName = true })
                            Text(s.email.ifBlank { "Niezalogowany w chmurze" }, fontSize = 13.sp, color = g.textSecondary, fontFamily = Manrope)
                            Text("Kliknij zdjęcie, aby zmienić · kliknij imię, aby edytować", fontSize = 11.sp, fontWeight = FontWeight.W700, color = g.accent, fontFamily = Manrope)
                        }
                    }
                    Spacer(Modifier.height(24.dp))

                    Section("Motyw kolorów")
                    Column(Modifier.fillMaxWidth().glass(RoundedCornerShape(22.dp)).padding(16.dp)) { ThemeSwatches(st, 48.dp) }
                    Spacer(Modifier.height(24.dp))

                    Section("Wygląd")
                    SettingsCard { SettingRow(Icons.Outlined.DarkMode, g.accent, "Tryb ciemny", if (s.darkTheme) "Włączony" else "Wyłączony") { GlassToggle(s.darkTheme) { s.darkTheme = it; st.settingsChanged() } } }
                    Spacer(Modifier.height(24.dp))

                    // Chmura
                    Section("Chmura (synchronizacja)")
                    Column(Modifier.fillMaxWidth().glass(RoundedCornerShape(22.dp)).padding(16.dp)) {
                        val c = st.cloud
                        when {
                            !s.configured -> {
                                Text("Podłącz swój darmowy projekt Supabase, aby synchronizować zadania między urządzeniami.", fontSize = 13.sp, color = g.textSecondary, fontFamily = Manrope)
                                Spacer(Modifier.height(10.dp))
                                GlassField(url, { url = it }, "URL projektu (https://…supabase.co)")
                                Spacer(Modifier.height(8.dp))
                                GlassField(anon, { anon = it }, "Klucz anon (public)", password = true)
                                Spacer(Modifier.height(10.dp))
                                PrimaryBtn("Zapisz projekt", enabled = !c.busy && url.isNotBlank() && anon.isNotBlank()) { st.setCloudConfig(url, anon) }
                                Spacer(Modifier.height(6.dp))
                                Text("Znajdziesz je w Supabase → Project Settings → API (Project URL i anon public). Nigdy nie wklejaj klucza „secret”.", fontSize = 11.5.sp, color = g.textSecondary, fontFamily = Manrope)
                            }
                            !s.signedIn -> {
                                Text("Projekt podłączony. Zaloguj się lub załóż konto w chmurze — tym samym e-mailem i hasłem co na telefonie.", fontSize = 13.sp, color = g.textSecondary, fontFamily = Manrope)
                                Spacer(Modifier.height(10.dp))
                                GlassField(email, { email = it }, "E-mail")
                                Spacer(Modifier.height(8.dp))
                                GlassField(password, { password = it }, "Hasło", password = true, onEnter = { st.cloudSignIn(email, password) })
                                Spacer(Modifier.height(10.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Box(Modifier.weight(1f)) { PrimaryBtn("Zaloguj", enabled = !c.busy) { st.cloudSignIn(email, password) } }
                                    SecondaryBtn("Załóż konto", Modifier.weight(1f)) { st.cloudSignUp(email, password) }
                                }
                                Spacer(Modifier.height(8.dp))
                                Text("Zmień projekt", fontSize = 12.sp, fontWeight = FontWeight.W700, color = g.textSecondary, fontFamily = Manrope, modifier = Modifier.clickable { s.url = ""; s.anonKey = ""; st.settingsChanged() })
                            }
                            else -> {
                                Text("Zalogowano w chmurze:", fontSize = 13.sp, color = g.textSecondary, fontFamily = Manrope)
                                Text(s.email, fontSize = 14.sp, fontWeight = FontWeight.W800, color = g.textPrimary, fontFamily = Manrope)
                                Spacer(Modifier.height(10.dp))
                                PrimaryBtn("Synchronizuj teraz (⌘S)", enabled = !c.busy && !st.syncing, icon = Icons.Outlined.Sync) { st.runSync(auto = false) }
                                Spacer(Modifier.height(8.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    SecondaryBtn("Wyślij kopię do chmury", Modifier.weight(1f)) { st.cloudBackup() }
                                    SecondaryBtn("Pobierz z chmury", Modifier.weight(1f)) { st.cloudRestore() }
                                }
                                Spacer(Modifier.height(10.dp))
                                Text("Wyloguj z chmury", fontSize = 12.5.sp, fontWeight = FontWeight.W700, color = g.textSecondary, fontFamily = Manrope, modifier = Modifier.clickable { st.cloudSignOut() })
                            }
                        }
                        if (c.busy || st.syncing) { Spacer(Modifier.height(8.dp)); Row(verticalAlignment = Alignment.CenterVertically) { CircularProgressIndicator(Modifier.size(18.dp), color = g.accent, strokeWidth = 2.dp); Spacer(Modifier.width(8.dp)); Text("Łączę z chmurą…", fontSize = 12.5.sp, color = g.textSecondary, fontFamily = Manrope) } }
                        c.message?.let { Spacer(Modifier.height(8.dp)); Text(it, fontSize = 12.5.sp, fontWeight = FontWeight.W700, color = g.success, fontFamily = Manrope) }
                        c.error?.let { Spacer(Modifier.height(8.dp)); Text(it, fontSize = 12.5.sp, fontWeight = FontWeight.W700, color = g.danger, fontFamily = Manrope) }
                    }
                    Spacer(Modifier.height(24.dp))

                    // Gmail
                    Section("Gmail (maile z gwiazdką → zadania)")
                    Column(Modifier.fillMaxWidth().glass(RoundedCornerShape(22.dp)).padding(16.dp)) {
                        val gm = st.gmail
                        if (s.gmailUser.isBlank()) {
                            Text("Oznacz maila gwiazdką w Gmailu, a Todoisto zamieni go w zadanie (z linkiem do wątku). Potrzebne HASŁO DO APLIKACJI: włącz weryfikację dwuetapową, wejdź na myaccount.google.com/apppasswords i wygeneruj hasło dla poczty.", fontSize = 13.sp, lineHeight = 18.sp, color = g.textSecondary, fontFamily = Manrope)
                            Spacer(Modifier.height(10.dp))
                            GlassField(gUser, { gUser = it }, "Adres Gmail")
                            Spacer(Modifier.height(8.dp))
                            GlassField(gPass, { gPass = it }, "Hasło do aplikacji (16 znaków)", password = true)
                            Spacer(Modifier.height(10.dp))
                            PrimaryBtn("Połącz z Gmailem", enabled = gUser.isNotBlank() && gPass.isNotBlank()) { st.setGmailCreds(gUser, gPass.replace(" ", "")) }
                        } else {
                            Text("Połączono:", fontSize = 13.sp, color = g.textSecondary, fontFamily = Manrope)
                            Text(s.gmailUser, fontSize = 14.sp, fontWeight = FontWeight.W800, color = g.textPrimary, fontFamily = Manrope)
                            Spacer(Modifier.height(10.dp))
                            PrimaryBtn(if (gm.loading) "Pobieram…" else "Pobierz maile z gwiazdką", enabled = !gm.loading) { st.syncGmail() }
                            Spacer(Modifier.height(8.dp))
                            Text("Rozłącz", fontSize = 12.5.sp, fontWeight = FontWeight.W700, color = g.textSecondary, fontFamily = Manrope, modifier = Modifier.clickable { st.setGmailCreds("", ""); gUser = ""; gPass = "" })
                        }
                        gm.info?.let { Spacer(Modifier.height(8.dp)); Text(it, fontSize = 12.5.sp, fontWeight = FontWeight.W700, color = g.success, fontFamily = Manrope) }
                        gm.error?.let { Spacer(Modifier.height(8.dp)); Text(it, fontSize = 12.5.sp, fontWeight = FontWeight.W700, color = g.danger, fontFamily = Manrope) }
                    }
                    Spacer(Modifier.height(24.dp))

                    AiUsageCard(st)
                }
            }
        }
    }
}

/** Zużycie AI — jak `AiUsageCard`. */
@Composable
private fun AiUsageCard(st: AppState) {
    val g = LocalGlass.current
    val s = st.settings
    val pt = s.aiPromptTokens; val ct = s.aiCompletionTokens; val img = s.aiImageCount
    val textCost = pt / 1e6 * 0.15 + ct / 1e6 * 0.60
    val imageCost = img * 0.08
    val total = textCost + imageCost
    fun fmt(n: Long) = "%,d".format(n).replace(',', ' ')
    Section("Zużycie AI")
    Column(Modifier.fillMaxWidth().glass(RoundedCornerShape(22.dp)).padding(16.dp)) {
        Row {
            Column(Modifier.weight(1f)) {
                Text("SZACOWANY KOSZT", fontSize = 10.5.sp, fontWeight = FontWeight.W800, letterSpacing = 1.2.sp, color = g.textSecondary, fontFamily = Manrope)
                Text("$%.4f".format(total), fontSize = 26.sp, fontWeight = FontWeight.W800, color = g.textPrimary, fontFamily = Sora)
                Text("≈ %.2f zł".format(total * 4.0), fontSize = 12.sp, color = g.textSecondary, fontFamily = Manrope)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(fmt(pt + ct), fontSize = 16.sp, fontWeight = FontWeight.W800, color = g.textPrimary, fontFamily = Sora); Text("tokeny", fontSize = 11.sp, color = g.textSecondary, fontFamily = Manrope)
                Text(fmt(img), fontSize = 16.sp, fontWeight = FontWeight.W800, color = g.textPrimary, fontFamily = Sora); Text("obrazy AI", fontSize = 11.sp, color = g.textSecondary, fontFamily = Manrope)
            }
        }
        Spacer(Modifier.height(12.dp))
        UsageBar("ROZKŁAD KOSZTU", textCost.toFloat(), imageCost.toFloat(), Color(0xFF6B8AFF), Color(0xFFF4B740), "Tekst · $%.4f".format(textCost), "Obrazy · $%.2f".format(imageCost))
        Spacer(Modifier.height(10.dp))
        UsageBar("TOKENY — WEJŚCIE / WYJŚCIE", pt.toFloat(), ct.toFloat(), Color(0xFF2DD4BF), Color(0xFFB99CFF), "Wejście · ${fmt(pt)}", "Wyjście · ${fmt(ct)}")
        Spacer(Modifier.height(12.dp))
        val adminSet = s.openAiAdminKey.isNotBlank(); val cost = st.aiCost
        SettingRow(Icons.Outlined.Paid, Color(0xFF34D399), "Realny koszt (OpenAI)", when {
            !adminSet -> "Dodaj klucz Admin OpenAI, aby zobaczyć realny koszt"; cost?.loading == true -> "Pobieram z OpenAI…"
            cost?.error != null -> cost.error; cost?.amountUsd != null -> "Ten miesiąc · odśwież kliknięciem"; else -> "Kliknij, aby pobrać"
        }, onClick = { if (adminSet) st.refreshAiCost() else st.dialog = DialogKind.AdminKey }) {
            when { !adminSet -> Chevron(); cost?.amountUsd != null -> Text("$%.2f".format(cost.amountUsd), fontSize = 14.sp, fontWeight = FontWeight.W800, color = g.textPrimary, fontFamily = Manrope); else -> Text("—", color = g.textSecondary) }
        }
        if (adminSet) { RowDivider(); SettingRow(Icons.Outlined.Key, g.accent, "Klucz Admin", "Ustawiony — kliknij, aby zmienić/usunąć", onClick = { st.dialog = DialogKind.AdminKey }) { StatusDot(true) } }
        if (pt + ct + img > 0) { RowDivider(); SettingRow(Icons.Outlined.RestartAlt, Color(0xFFFB7185), "Wyzeruj liczniki", "Zeruje lokalne zużycie i koszt", onClick = { s.resetAiUsage(); st.settingsChanged() }) { Chevron() } }
    }
    Text("Szacunek liczony lokalnie z tokenów (gpt-4o-mini) i liczby obrazów (~0,08 $/szt.). Realny koszt pochodzi z Costs API OpenAI (klucz Admin) — orientacyjnie.", fontSize = 11.5.sp, color = g.textSecondary.copy(alpha = 0.85f), fontFamily = Manrope, modifier = Modifier.padding(start = 8.dp, top = 8.dp))
}

@Composable
private fun UsageBar(label: String, a: Float, b: Float, ca: Color, cb: Color, la: String, lb: String) {
    val g = LocalGlass.current
    val sum = (a + b).coerceAtLeast(0.0001f)
    Text(label, fontSize = 10.sp, fontWeight = FontWeight.W800, letterSpacing = 1.sp, color = g.textSecondary, fontFamily = Manrope)
    Spacer(Modifier.height(6.dp))
    Row(Modifier.fillMaxWidth().height(12.dp).clip(RoundedCornerShape(50)).background(g.hair)) {
        if (a > 0) Box(Modifier.weight(a / sum).fillMaxHeight().background(ca))
        if (b > 0) Box(Modifier.weight(b / sum).fillMaxHeight().background(cb))
    }
    Spacer(Modifier.height(6.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) { Dot(ca, 8.dp); Spacer(Modifier.width(6.dp)); Text(la, fontSize = 11.5.sp, color = g.textSecondary, fontFamily = Manrope) }
        Row(verticalAlignment = Alignment.CenterVertically) { Dot(cb, 8.dp); Spacer(Modifier.width(6.dp)); Text(lb, fontSize = 11.5.sp, color = g.textSecondary, fontFamily = Manrope) }
    }
}

// ─── Statystyki — jak `StatsScreen` ─────────────────────────────────────────
@Composable
fun StatsScreen(st: AppState) {
    val g = LocalGlass.current
    st.rev; st.settingsRev
    val tasks = st.repo.tasks
    val zone = ZoneId.systemDefault()
    val today = st.today
    val completed = tasks.filter { it.isCompleted && (it.completedAt ?: 0L) > 0L }
    val weekday = IntArray(7); completed.forEach { weekday[Instant.ofEpochMilli(it.completedAt!!).atZone(zone).dayOfWeek.value - 1]++ }
    val prio = IntArray(4); tasks.filter { !it.isCompleted && it.parentId == null }.forEach { prio[(it.priority.drop(1).toIntOrNull() ?: 4) - 1]++ }
    val avgDays = completed.filter { it.createdAt > 0 && it.completedAt!! >= it.createdAt }.map { (it.completedAt!! - it.createdAt) / 86_400_000.0 }.average().takeIf { !it.isNaN() } ?: 0.0
    val hours = st.settings.openHours
    val parts = listOf("Noc" to (0..5), "Rano" to (6..11), "Popoł." to (12..17), "Wiecz." to (18..23)).map { (l, r) -> l to r.sumOf { hours[it] } }
    val doneDays = completed.map { Instant.ofEpochMilli(it.completedAt!!).atZone(zone).toLocalDate().toEpochDay() }.toSet()
    val last30 = (0 until 30).map { off -> doneDays.count { it == today - 29 + off } }
    var streak = 0
    if (doneDays.contains(today) || doneDays.contains(today - 1)) { var d = if (doneDays.contains(today)) today else today - 1; while (doneDays.contains(d)) { streak++; d-- } }
    var best = 0; var run = 0; var prev: Long? = null
    doneDays.sorted().forEach { d -> run = if (prev != null && d == prev!! + 1) run + 1 else 1; best = maxOf(best, run); prev = d }
    val deferCount = st.settings.deferCount

    FullScreen {
        Column(Modifier.fillMaxSize()) {
            ScreenTopBar("Statystyki", onBack = { st.showStats = false })
            Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 18.dp).padding(bottom = 28.dp)) {
                Column(Modifier.widthIn(max = 760.dp).align(Alignment.CenterHorizontally)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        StatTile("Ukończone", "${completed.size}", Color(0xFF34D399), Modifier.weight(1f))
                        StatTile("Przesunięcia", "$deferCount", Color(0xFFFB7185), Modifier.weight(1f))
                        StatTile("Śr. czas", if (avgDays > 0) "%.1f d".format(avgDays) else "—", Color(0xFF6B8AFF), Modifier.weight(1f))
                    }
                    Spacer(Modifier.height(14.dp))
                    Row(Modifier.fillMaxWidth().glass(RoundedCornerShape(22.dp)).padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("🔥", fontSize = 30.sp); Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            Text("Passa ukończeń", fontSize = 13.sp, fontWeight = FontWeight.W800, color = g.textPrimary, fontFamily = Manrope)
                            Text(if (streak > 0) "Domykasz zadania od $streak ${if (streak == 1) "dnia" else "dni"}" else "Zacznij dziś — domknij jedno zadanie", fontSize = 12.5.sp, color = g.textSecondary, fontFamily = Manrope)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("$streak", fontSize = 26.sp, fontWeight = FontWeight.W800, color = g.accent, fontFamily = Sora)
                            Text("rekord $best", fontSize = 11.sp, color = g.textSecondary, fontFamily = Manrope)
                        }
                    }
                    Spacer(Modifier.height(14.dp))
                    ChartCard("Ostatnie 30 dni") {
                        Row(Modifier.fillMaxWidth().height(90.dp), horizontalArrangement = Arrangement.spacedBy(2.dp), verticalAlignment = Alignment.Bottom) {
                            val max = (last30.maxOrNull() ?: 0).coerceAtLeast(1)
                            last30.forEach { v -> Box(Modifier.weight(1f).height((90 * v / max).dp.coerceAtLeast(3.dp)).clip(RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp)).background(Brush.verticalGradient(listOf(Color(0xFFA47CFF), g.accent)))) }
                        }
                        Row(Modifier.fillMaxWidth().padding(top = 6.dp), horizontalArrangement = Arrangement.SpaceBetween) { Text("30 dni temu", fontSize = 10.5.sp, color = g.textSecondary, fontFamily = Manrope); Text("dziś", fontSize = 10.5.sp, color = g.textSecondary, fontFamily = Manrope) }
                    }
                    Spacer(Modifier.height(14.dp))
                    ChartCard("Ukończone wg dnia tygodnia") { VBars(listOf("Pn", "Wt", "Śr", "Cz", "Pt", "So", "Nd").zip(weekday.toList()), List(7) { Brush.verticalGradient(listOf(Color(0xFFA47CFF), g.accent)) }) }
                    Spacer(Modifier.height(14.dp))
                    ChartCard("Priorytety (aktywne)") { VBars(listOf("P1", "P2", "P3", "P4").zip(prio.toList()), listOf(0xFFD1453B, 0xFFEB8909, 0xFF246FE0, 0xFF9E9E9E).map { Brush.verticalGradient(listOf(Color(it), Color(it))) }) }
                    Spacer(Modifier.height(14.dp))
                    ChartCard("Kiedy otwierasz zadania") { VBars(parts, List(4) { Brush.verticalGradient(listOf(Color(0xFF5EE7D0), Color(0xFF2DD4BF))) }) }
                }
            }
        }
    }
}

@Composable
private fun StatTile(label: String, value: String, dot: Color, modifier: Modifier) {
    val g = LocalGlass.current
    Column(modifier.glass(RoundedCornerShape(20.dp)).padding(14.dp)) {
        Dot(dot, 8.dp); Spacer(Modifier.height(8.dp))
        Text(value, fontSize = 22.sp, fontWeight = FontWeight.W800, color = g.textPrimary, fontFamily = Sora)
        Text(label, fontSize = 11.5.sp, color = g.textSecondary, fontFamily = Manrope)
    }
}

@Composable
private fun ChartCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    val g = LocalGlass.current
    Column(Modifier.fillMaxWidth().glass(RoundedCornerShape(22.dp)).padding(16.dp)) {
        Text(title, fontSize = 13.sp, fontWeight = FontWeight.W800, color = g.textPrimary, fontFamily = Manrope)
        Spacer(Modifier.height(14.dp))
        content()
    }
}

@Composable
private fun VBars(entries: List<Pair<String, Int>>, brushes: List<Brush>) {
    val g = LocalGlass.current
    val max = (entries.maxOfOrNull { it.second } ?: 0).coerceAtLeast(1).toFloat()
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Bottom) {
        entries.forEachIndexed { i, (label, v) ->
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("$v", fontSize = 10.sp, fontWeight = FontWeight.W700, color = g.textSecondary, fontFamily = Manrope)
                Spacer(Modifier.height(4.dp))
                Box(Modifier.fillMaxWidth(0.66f).height((110 * v / max).dp.coerceAtLeast(4.dp)).clip(RoundedCornerShape(topStart = 7.dp, topEnd = 7.dp)).background(brushes[i % brushes.size]))
                Spacer(Modifier.height(6.dp))
                Text(label, fontSize = 10.5.sp, fontWeight = FontWeight.W700, color = g.textSecondary, fontFamily = Manrope)
            }
        }
    }
}
