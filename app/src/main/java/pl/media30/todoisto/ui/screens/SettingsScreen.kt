package pl.media30.todoisto.ui.screens

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.Gradient
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material.icons.outlined.Calculate
import androidx.compose.material.icons.outlined.CalendarViewWeek
import androidx.compose.material.icons.outlined.CloudDownload
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.DataUsage
import androidx.compose.material.icons.outlined.Paid
import androidx.compose.material.icons.outlined.RestartAlt
import androidx.compose.material.icons.outlined.Shuffle
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.Palette
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState
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
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
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
    aiPromptTokens: Long,
    aiCompletionTokens: Long,
    aiImageCount: Long,
    onResetAiUsage: () -> Unit,
    adminKeySet: Boolean,
    aiCost: pl.media30.todoisto.ui.AiCostState?,
    onSetAdminKey: (String) -> Unit,
    onRefreshCost: () -> Unit,
    customPhotos: List<String>,
    activeCustomBg: String,
    usePhaseBg: Boolean,
    aiImages: pl.media30.todoisto.ui.AiImagesState?,
    bgBusy: Boolean,
    onSelectGradient: () -> Unit,
    onSelectScene: () -> Unit,
    onAddCustomPhoto: (android.net.Uri) -> Unit,
    onSetActiveCustom: (String) -> Unit,
    onRemoveCustom: (String) -> Unit,
    onReorderCustom: (List<String>) -> Unit,
    onGenerateAi: () -> Unit,
    onGeneratePhaseAi: () -> Unit,
    onAddPresets: () -> Unit,
    onDismissAiImages: () -> Unit,
    themeId: String,
    onSelectTheme: (String) -> Unit,
    onOpenAccount: () -> Unit,
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
    var showAdminKey by remember { mutableStateOf(false) }

    // Auto-odświeżenie realnego kosztu przy wejściu, gdy klucz Admin jest ustawiony.
    androidx.compose.runtime.LaunchedEffect(adminKeySet) { if (adminKeySet && aiCost == null) onRefreshCost() }

    BackHandler { if (route != 0) route = 0 else onBack() }

    GlassBackground {
        Column(Modifier.fillMaxSize()) {
            TopBar(when (route) { 1 -> "Ogólne"; 2 -> "Tło"; 3 -> "Motyw"; else -> "Ustawienia" }) {
                if (route != 0) route = 0 else onBack()
            }
            Column(
                Modifier.fillMaxWidth().weight(1f).verticalScroll(rememberScrollState())
                    .padding(horizontal = 18.dp).padding(top = 6.dp, bottom = 28.dp)
            ) {
                if (route == 2) BackgroundSettings(
                    dark, photo, activeCustomBg, usePhaseBg, customPhotos, bgBusy,
                    onSelectGradient, onSelectScene, onAddCustomPhoto, onSetActiveCustom, onRemoveCustom,
                    onReorderCustom, onAddPresets
                ) else if (route == 3) ThemeSettings(themeId, onSelectTheme)
                else if (route == 0) MainSettings(
                    dark, photo, activeCustomBg, hasApiKey, dailyGoal, weeklyGoal,
                    aiPromptTokens, aiCompletionTokens, aiImageCount, onResetAiUsage,
                    adminKeySet, aiCost, onRefreshCost, onOpenAdminKey = { showAdminKey = true },
                    onOpenGeneral = { route = 1 }, onOpenBackground = { route = 2 },
                    onToggleDark, themeName = pl.media30.todoisto.ui.theme.ThemePalettes.byId(themeId).name,
                    onOpenTheme = { route = 3 }, onOpenAccount = onOpenAccount,
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
    if (showAdminKey) AdminKeyDialog(adminKeySet, { showAdminKey = false }) { k -> onSetAdminKey(k); showAdminKey = false }
}

@Composable
private fun MainSettings(
    dark: Boolean, photo: Boolean, activeCustomBg: String, hasApiKey: Boolean, dailyGoal: Int, weeklyGoal: Int,
    aiPromptTokens: Long, aiCompletionTokens: Long, aiImageCount: Long, onResetAiUsage: () -> Unit,
    adminKeySet: Boolean, aiCost: pl.media30.todoisto.ui.AiCostState?, onRefreshCost: () -> Unit, onOpenAdminKey: () -> Unit,
    onOpenGeneral: () -> Unit, onOpenBackground: () -> Unit,
    onToggleDark: () -> Unit,
    themeName: String, onOpenTheme: () -> Unit, onOpenAccount: () -> Unit,
    onOpenKey: () -> Unit, onOpenGoals: () -> Unit,
    onOpenPool: () -> Unit, onOpenImport: () -> Unit
) {
    SettingsHero()
    Spacer(Modifier.height(22.dp))

    SettingsSection("Ustawienia")
    SettingsCard {
        NavRow(Icons.Outlined.AccountCircle, Color(0xFF9B6BFF), "Konto", "Profil, motyw, wylogowanie", onClick = onOpenAccount)
        RowDivider()
        NavRow(Icons.Outlined.Tune, Color(0xFF8AA0FF), "Ogólne", "Widok startowy, daty, tydzień, przesuwanie", onClick = onOpenGeneral)
    }
    Spacer(Modifier.height(22.dp))

    SettingsSection("Personalizacja")
    SettingsCard {
        NavRow(Icons.Outlined.Palette, Color(0xFFFB7185), "Motyw", themeName, onClick = onOpenTheme)
        RowDivider()
        ToggleRow(Icons.Outlined.DarkMode, Color(0xFF7C6BFF), "Tryb ciemny", if (dark) "Włączony" else "Wyłączony", dark, onToggleDark)
        RowDivider()
        val bgSub = when {
            activeCustomBg.isNotBlank() -> "Własne zdjęcie"
            photo -> "Scena wg pory dnia"
            else -> "Gradient (mesh)"
        }
        NavRow(Icons.Outlined.Wallpaper, Color(0xFF2DD4BF), "Tło", bgSub, onClick = onOpenBackground)
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
        val verCtx = LocalContext.current
        val versionName = remember {
            runCatching { verCtx.packageManager.getPackageInfo(verCtx.packageName, 0).versionName }.getOrNull() ?: "1.0"
        }
        NavRow(Icons.Outlined.Info, Color(0xFF9AA6FF), "Wersja", "Todoisto v$versionName", trailing = {})
        RowDivider()
        NavRow(Icons.Outlined.Lock, Color(0xFF34D399), "Dane", "Przechowywane lokalnie · offline, konto tylko na urządzeniu", trailing = {})
    }
    Spacer(Modifier.height(18.dp))
    Text(
        "Zrobione z ♥ dla spokojnego dnia.",
        fontSize = 11.5.sp, color = GlassTextSecondary.copy(alpha = 0.7f),
        modifier = Modifier.fillMaxWidth().padding(top = 2.dp)
    )
}

@Composable
internal fun AiUsageCard(
    promptTokens: Long, completionTokens: Long, imageCount: Long, onReset: () -> Unit,
    adminKeySet: Boolean, aiCost: pl.media30.todoisto.ui.AiCostState?, onRefreshCost: () -> Unit, onOpenAdminKey: () -> Unit
) {
    val totalTokens = promptTokens + completionTokens
    val textCost = promptTokens / 1_000_000.0 * 0.15 + completionTokens / 1_000_000.0 * 0.60
    val imageCost = imageCount * 0.08
    val totalCost = textCost + imageCost
    val costPln = totalCost * 4.0
    val blue = Color(0xFF6B8AFF); val gold = Color(0xFFF4B740); val teal = Color(0xFF2DD4BF); val violet = Color(0xFFB99CFF)

    SettingsSection("Zużycie AI")
    SettingsCard {
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            // Nagłówek: duży koszt + tokeny/obrazy
            Row(verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f)) {
                    Text("SZACOWANY KOSZT", fontSize = 10.5.sp, fontWeight = FontWeight.W800, letterSpacing = 1.2.sp, color = GlassTextSecondary)
                    Spacer(Modifier.height(3.dp))
                    Text("$" + "%.4f".format(totalCost), fontSize = 26.sp, fontWeight = FontWeight.W800, color = GlassTextPrimary)
                    Text("≈ %.2f zł".format(costPln), fontSize = 12.sp, color = GlassTextSecondary)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(fmt(totalTokens), fontSize = 18.sp, fontWeight = FontWeight.W800, color = GlassTextPrimary)
                    Text("tokeny", fontSize = 11.sp, color = GlassTextSecondary)
                    Spacer(Modifier.height(4.dp))
                    Text("$imageCount", fontSize = 18.sp, fontWeight = FontWeight.W800, color = GlassTextPrimary)
                    Text("obrazy AI", fontSize = 11.sp, color = GlassTextSecondary)
                }
            }
            Spacer(Modifier.height(16.dp))

            // Wizualizacja 1 — rozkład kosztu: tekst vs obrazy
            Text("ROZKŁAD KOSZTU", fontSize = 10.sp, fontWeight = FontWeight.W800, letterSpacing = 1.1.sp, color = GlassTextSecondary)
            Spacer(Modifier.height(7.dp))
            UsageBar(listOf(textCost.toFloat() to blue, imageCost.toFloat() to gold))
            Spacer(Modifier.height(8.dp))
            Row {
                LegendItem(blue, "Tekst", "$" + "%.4f".format(textCost))
                Spacer(Modifier.width(18.dp))
                LegendItem(gold, "Obrazy", "$" + "%.2f".format(imageCost))
            }
            Spacer(Modifier.height(16.dp))

            // Wizualizacja 2 — tokeny wejście vs wyjście
            Text("TOKENY — WEJŚCIE / WYJŚCIE", fontSize = 10.sp, fontWeight = FontWeight.W800, letterSpacing = 1.1.sp, color = GlassTextSecondary)
            Spacer(Modifier.height(7.dp))
            UsageBar(listOf(promptTokens.toFloat() to teal, completionTokens.toFloat() to violet))
            Spacer(Modifier.height(8.dp))
            Row {
                LegendItem(teal, "Wejście", fmt(promptTokens))
                Spacer(Modifier.width(18.dp))
                LegendItem(violet, "Wyjście", fmt(completionTokens))
            }
        }
        RowDivider()
        // Realny koszt z OpenAI (Costs API) — jeśli podano klucz Admin.
        val costSub = when {
            !adminKeySet -> "Dodaj klucz Admin OpenAI, aby zobaczyć realny koszt"
            aiCost?.loading == true -> "Pobieram z OpenAI…"
            aiCost?.error != null -> aiCost.error
            aiCost?.amountUsd != null -> "Ten miesiąc · odśwież dotknięciem"
            else -> "Dotknij, aby pobrać"
        }
        SettingRowScaffold(
            Icons.Outlined.Paid, Color(0xFF34D399), "Realny koszt (OpenAI)", costSub,
            trailing = {
                when {
                    !adminKeySet -> Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = GlassTextSecondary.copy(alpha = 0.6f), modifier = Modifier.size(20.dp))
                    aiCost?.amountUsd != null -> Text("$" + "%.2f".format(aiCost.amountUsd), fontSize = 15.sp, fontWeight = FontWeight.W800, color = GlassTextPrimary)
                    else -> Text("—", fontSize = 15.sp, fontWeight = FontWeight.W800, color = GlassTextSecondary)
                }
            },
            onClick = if (adminKeySet) onRefreshCost else onOpenAdminKey
        )
        if (adminKeySet) {
            RowDivider()
            NavRow(Icons.Outlined.Key, Color(0xFFB99CFF), "Klucz Admin", "Ustawiony — dotknij, aby zmienić/usunąć", trailing = { StatusDot(true) }, onClick = onOpenAdminKey)
        }
        if (totalTokens > 0L || imageCount > 0L) {
            RowDivider()
            NavRow(Icons.Outlined.RestartAlt, Color(0xFFFB7185), "Wyzeruj liczniki", "Zeruje lokalne zużycie i koszt", trailing = {}, onClick = onReset)
        }
    }
    Text(
        "Szacunek liczony lokalnie z tokenów (gpt-4o-mini) i liczby obrazów (dall-e-3, ~0,08 $/szt.). Realny koszt pochodzi z Costs API OpenAI (klucz Admin) — orientacyjnie.",
        fontSize = 11.5.sp, color = GlassTextSecondary.copy(alpha = 0.85f),
        modifier = Modifier.padding(start = 6.dp, top = 8.dp, end = 6.dp)
    )
}

@Composable
private fun UsageBar(parts: List<Pair<Float, Color>>) {
    Box(
        Modifier.fillMaxWidth().height(12.dp).clip(RoundedCornerShape(6.dp))
            .background(GlassTextSecondary.copy(alpha = 0.16f))
    ) {
        if (parts.any { it.first > 0f }) {
            Row(Modifier.fillMaxWidth().fillMaxHeight()) {
                parts.filter { it.first > 0f }.forEach { (v, c) ->
                    Box(Modifier.weight(v).fillMaxHeight().background(c))
                }
            }
        }
    }
}

@Composable
private fun LegendItem(color: Color, label: String, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(9.dp).clip(CircleShape).background(color))
        Spacer(Modifier.width(6.dp))
        Text("$label · $value", fontSize = 11.5.sp, fontWeight = FontWeight.W600, color = GlassTextSecondary)
    }
}

@Composable
internal fun AdminKeyDialog(hasKey: Boolean, onDismiss: () -> Unit, onSave: (String) -> Unit) {
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
        title = { Text("Klucz Admin (koszty)") },
        text = {
            Column {
                Text(
                    (if (hasKey) "Klucz Admin jest ustawiony. Wklej nowy, aby zmienić.\n\n" else "") +
                        "Utwórz na platform.openai.com → Settings → Organization → Admin keys. " +
                        "Ma szersze uprawnienia niż zwykły klucz — zostaje tylko na tym urządzeniu.",
                    fontSize = 12.5.sp
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(value = key, onValueChange = { key = it }, singleLine = true, placeholder = { Text("sk-admin-…") }, modifier = Modifier.fillMaxWidth())
            }
        }
    )
}

private fun fmt(n: Long): String = "%,d".format(n).replace(',', ' ')

@Composable
private fun BackgroundSettings(
    dark: Boolean, photo: Boolean, activeCustomBg: String, usePhaseBg: Boolean, customPhotos: List<String>,
    bgBusy: Boolean,
    onSelectGradient: () -> Unit, onSelectScene: () -> Unit, onAddCustomPhoto: (android.net.Uri) -> Unit,
    onSetActiveCustom: (String) -> Unit, onRemoveCustom: (String) -> Unit,
    onReorderCustom: (List<String>) -> Unit,
    onAddPresets: () -> Unit
) {
    val customActive = activeCustomBg.isNotBlank() && !usePhaseBg
    val hasRegistry = androidx.activity.compose.LocalActivityResultRegistryOwner.current != null
    val picker = if (hasRegistry) rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) onAddCustomPhoto(uri)
    } else null

    SettingsSection("Rodzaj tła")
    SettingsCard {
        BgOptionRow(Icons.Outlined.Gradient, Color(0xFFB99CFF), "Gradient (mesh)", "Domyślne, animowane", !photo && !customActive && !usePhaseBg, onSelectGradient)
        RowDivider()
        BgOptionRow(Icons.Outlined.WbSunny, Color(0xFFF4B740), "Scena wg pory dnia", "Malowane: rano / dzień / wieczór", photo && !customActive && !usePhaseBg, onSelectScene)
    }
    Spacer(Modifier.height(22.dp))

    SettingsSection("Zdjęcie w tle")
    SettingsCard {
        Column(Modifier.fillMaxWidth().padding(14.dp)) {
            Text("Wrzuć do 3 własnych zdjęć lub wylosuj gotowe. Dotknij kafelka, aby ustawić jako tło; przytrzymaj i przeciągnij, aby zmienić kolejność.", fontSize = 12.5.sp, color = GlassTextSecondary)
            Spacer(Modifier.height(12.dp))
            CustomPhotoTiles(customPhotos, activeCustomBg, onSetActiveCustom, onRemoveCustom, onReorderCustom) {
                picker?.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            }
            Spacer(Modifier.height(14.dp))
            // GŁÓWNY przycisk — losuje 3 prawdziwe zdjęcia z kolorową maską apki.
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(15.dp)).background(GlassAccent)
                    .bouncy(0.97f) { if (!bgBusy) onAddPresets() }.padding(vertical = 14.dp),
                horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically
            ) {
                if (bgBusy) {
                    CircularProgressIndicator(color = Color.White, strokeWidth = 2.5.dp, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(10.dp))
                    Text("Losuję zdjęcia…", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.W800)
                } else {
                    Icon(Icons.Outlined.Shuffle, null, tint = Color.White, modifier = Modifier.size(17.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Losuj zdjęcia w tle", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.W800)
                }
            }
            Spacer(Modifier.height(6.dp))
            Text(
                "Losowe zdjęcia z kolorową maską w barwach apki. Klikaj do skutku — za każdym razem inny zestaw.",
                fontSize = 11.5.sp, color = GlassTextSecondary
            )
        }
    }
    Text(
        "Zdjęcia pobierają się z internetu i zapisują tylko na urządzeniu. Gdy brak sieci, losujemy gradient.",
        fontSize = 11.5.sp, color = GlassTextSecondary.copy(alpha = 0.85f),
        modifier = Modifier.padding(start = 6.dp, top = 8.dp, end = 6.dp)
    )
}

/** Wybór motywu kolorystycznego (siatka próbek). */
@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
private fun ThemeSettings(themeId: String, onSelectTheme: (String) -> Unit) {
    SettingsSection("Kolor akcentu")
    SettingsCard {
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            Text("Zmienia kolor przycisków, akcentów i kart w całej apce.", fontSize = 12.5.sp, color = GlassTextSecondary)
            Spacer(Modifier.height(14.dp))
            androidx.compose.foundation.layout.FlowRow(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                pl.media30.todoisto.ui.theme.ThemePalettes.all.forEach { pal ->
                    val selected = pal.id == themeId
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            Modifier.size(52.dp).clip(CircleShape).background(pal.swatch)
                                .then(if (selected) Modifier.border(3.dp, GlassTextPrimary, CircleShape) else Modifier)
                                .bouncy(0.9f) { onSelectTheme(pal.id) },
                            contentAlignment = Alignment.Center
                        ) { if (selected) Icon(Icons.Filled.Check, null, tint = Color.White, modifier = Modifier.size(24.dp)) }
                        Spacer(Modifier.height(6.dp))
                        Text(pal.name, fontSize = 12.sp, fontWeight = if (selected) FontWeight.W800 else FontWeight.W600, color = if (selected) GlassTextPrimary else GlassTextSecondary)
                    }
                }
            }
        }
    }
}

@Composable
private fun BgOptionRow(icon: ImageVector, tint: Color, title: String, subtitle: String, selected: Boolean, onClick: () -> Unit) {
    SettingRowScaffold(
        icon, tint, title, subtitle,
        trailing = {
            if (selected) Box(Modifier.size(22.dp).clip(CircleShape).background(GlassAccent), contentAlignment = Alignment.Center) {
                Icon(Icons.Filled.Check, null, tint = Color.White, modifier = Modifier.size(14.dp))
            } else Box(Modifier.size(22.dp).clip(CircleShape).background(GlassTextSecondary.copy(alpha = 0.18f)))
        },
        onClick = onClick
    )
}

/**
 * Kafelki własnych zdjęć z obsługą drag-and-drop (przytrzymaj i przeciągnij, by
 * zmienić kolejność). Dotknięcie ustawia zdjęcie jako aktywne tło; „+" dodaje
 * nowe (do 3). Kolejność podczas przeciągania trzymamy lokalnie dla płynnej
 * animacji, a zapisujemy po puszczeniu.
 */
@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
private fun CustomPhotoTiles(
    customPhotos: List<String>,
    activeCustomBg: String,
    onSetActive: (String) -> Unit,
    onRemove: (String) -> Unit,
    onReorder: (List<String>) -> Unit,
    onAdd: () -> Unit
) {
    var order by remember(customPhotos) { mutableStateOf(customPhotos) }
    val lazyState = rememberLazyListState()
    val reorderState = rememberReorderableLazyListState(lazyState) { from, to ->
        val list = order.toMutableList()
        val f = from.index; val t = to.index
        if (f in list.indices && t in list.indices) {
            list.add(t, list.removeAt(f))
            order = list
        }
    }
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val spacing = 10.dp
        val tileW = (maxWidth - spacing * 2) / 3
        LazyRow(
            state = lazyState,
            horizontalArrangement = Arrangement.spacedBy(spacing),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(order, key = { it }) { path ->
                ReorderableItem(reorderState, key = path) { _ ->
                    val isActive = path == activeCustomBg
                    Box(
                        Modifier.width(tileW).aspectRatio(0.62f).clip(RoundedCornerShape(16.dp))
                            .background(GlassTextSecondary.copy(alpha = 0.12f))
                            .then(if (isActive) Modifier.border(2.5.dp, GlassAccent, RoundedCornerShape(16.dp)) else Modifier)
                    ) {
                        coil.compose.AsyncImage(
                            model = java.io.File(path), contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(16.dp))
                                .longPressDraggableHandle(onDragStopped = { onReorder(order) })
                                .bouncy(0.95f) { onSetActive(path) }
                        )
                        Box(
                            Modifier.align(Alignment.TopEnd).padding(4.dp).size(22.dp).clip(CircleShape)
                                .background(Color(0x99000000)).bouncy(0.9f) { onRemove(path) },
                            contentAlignment = Alignment.Center
                        ) { Icon(Icons.Filled.Close, "Usuń", tint = Color.White, modifier = Modifier.size(13.dp)) }
                    }
                }
            }
            if (order.size < 3) {
                item(key = "add") {
                    Box(
                        Modifier.width(tileW).aspectRatio(0.62f).clip(RoundedCornerShape(16.dp))
                            .background(GlassTextSecondary.copy(alpha = 0.12f))
                            .bouncy(0.95f) { onAdd() },
                        contentAlignment = Alignment.Center
                    ) { Icon(Icons.Filled.Add, "Dodaj zdjęcie", tint = GlassTextSecondary, modifier = Modifier.size(24.dp)) }
                }
            }
        }
    }
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
