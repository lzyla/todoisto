package pl.media30.todoisto.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.Logout
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import pl.media30.todoisto.data.Account
import pl.media30.todoisto.ui.components.bouncy
import pl.media30.todoisto.ui.components.glassFieldColors
import pl.media30.todoisto.ui.theme.GlassAccent
import pl.media30.todoisto.ui.theme.GlassBackground
import pl.media30.todoisto.ui.theme.GlassTextPrimary
import pl.media30.todoisto.ui.theme.GlassTextSecondary
import pl.media30.todoisto.ui.theme.ThemePalettes
import pl.media30.todoisto.ui.theme.glass

/** Ekran „Konto" — profil, motyw kolorów, tryb nocny i wylogowanie. */
@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun AccountScreen(
    account: Account?,
    avatar: String,
    onPickAvatar: (android.net.Uri) -> Unit,
    dark: Boolean,
    themeId: String,
    onSelectTheme: (String) -> Unit,
    onToggleDark: () -> Unit,
    onLogout: () -> Unit,
    cloud: pl.media30.todoisto.ui.TodoViewModel.CloudState,
    onCloudConfig: (String, String) -> Unit,
    onCloudSignIn: (String, String) -> Unit,
    onCloudSignUp: (String, String) -> Unit,
    onCloudSignOut: () -> Unit,
    onCloudBackup: () -> Unit,
    onCloudRestore: () -> Unit,
    gmailUser: String,
    gmail: pl.media30.todoisto.ui.TodoViewModel.GmailState,
    onSaveGmail: (String, String) -> Unit,
    onSyncGmail: () -> Unit,
    aiPromptTokens: Long,
    aiCompletionTokens: Long,
    aiImageCount: Long,
    onResetAiUsage: () -> Unit,
    adminKeySet: Boolean,
    aiCost: pl.media30.todoisto.ui.AiCostState?,
    onSetAdminKey: (String) -> Unit,
    onRefreshCost: () -> Unit,
    onBack: () -> Unit
) {
    BackHandler { onBack() }
    var showAdminKey by remember { mutableStateOf(false) }
    // Auto-odświeżenie realnego kosztu przy wejściu, gdy klucz Admin ustawiony.
    androidx.compose.runtime.LaunchedEffect(adminKeySet) { if (adminKeySet && aiCost == null) onRefreshCost() }
    GlassBackground {
        Column(Modifier.fillMaxSize()) {
            // Górny pasek
            Row(
                Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(Modifier.size(40.dp).clip(CircleShape).bouncy(0.9f, onBack), contentAlignment = Alignment.Center) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Wstecz", tint = GlassTextPrimary, modifier = Modifier.size(22.dp))
                }
                Spacer(Modifier.width(6.dp))
                Text("Konto", fontSize = 26.sp, fontWeight = FontWeight.W900, color = GlassTextPrimary)
            }

            Column(
                Modifier.fillMaxWidth().weight(1f).verticalScroll(rememberScrollState())
                    .padding(horizontal = 18.dp).padding(top = 6.dp, bottom = 28.dp)
            ) {
                // Profil
                val hasReg = androidx.activity.compose.LocalActivityResultRegistryOwner.current != null
                val avatarPicker = if (hasReg) androidx.activity.compose.rememberLauncherForActivityResult(
                    androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia()
                ) { uri -> if (uri != null) onPickAvatar(uri) } else null
                Row(
                    Modifier.fillMaxWidth().glass(RoundedCornerShape(24.dp)).padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val initial = (account?.name?.trim()?.firstOrNull() ?: '?').uppercaseChar().toString()
                    Box(
                        Modifier.size(60.dp).clip(CircleShape).background(GlassAccent)
                            .bouncy(0.9f) { avatarPicker?.launch(androidx.activity.result.PickVisualMediaRequest(androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                        contentAlignment = Alignment.Center
                    ) {
                        if (avatar.isNotBlank()) {
                            coil.compose.AsyncImage(
                                model = java.io.File(avatar), contentDescription = "Zdjęcie profilowe",
                                contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                                modifier = Modifier.size(60.dp).clip(CircleShape)
                            )
                        } else {
                            Text(initial, fontSize = 24.sp, fontWeight = FontWeight.W900, color = Color.White)
                        }
                    }
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(account?.name ?: "Użytkownik", fontSize = 18.sp, fontWeight = FontWeight.W800, color = GlassTextPrimary)
                        Text(account?.email ?: "", fontSize = 13.sp, color = GlassTextSecondary)
                        Spacer(Modifier.height(3.dp))
                        Text("Dotknij zdjęcie, aby zmienić", fontSize = 11.sp, color = GlassAccent, fontWeight = FontWeight.W700)
                    }
                }
                Spacer(Modifier.height(24.dp))

                // Motyw kolorów
                Text("MOTYW KOLORÓW", fontSize = 11.5.sp, fontWeight = FontWeight.W800, color = GlassAccent, modifier = Modifier.padding(start = 6.dp, bottom = 10.dp))
                Column(Modifier.fillMaxWidth().glass(RoundedCornerShape(24.dp)).padding(16.dp)) {
                    androidx.compose.foundation.layout.FlowRow(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        ThemePalettes.all.forEach { pal ->
                            val selected = pal.id == themeId
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Box(
                                    Modifier.size(48.dp).clip(CircleShape).background(pal.swatch)
                                        .then(if (selected) Modifier.border(3.dp, GlassTextPrimary, CircleShape) else Modifier)
                                        .bouncy(0.9f) { onSelectTheme(pal.id) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (selected) Icon(Icons.Filled.Check, null, tint = Color.White, modifier = Modifier.size(22.dp))
                                }
                                Spacer(Modifier.height(6.dp))
                                Text(pal.name, fontSize = 11.5.sp, fontWeight = if (selected) FontWeight.W800 else FontWeight.W600, color = if (selected) GlassTextPrimary else GlassTextSecondary)
                            }
                        }
                    }
                }
                Spacer(Modifier.height(24.dp))

                // Wygląd
                Text("WYGLĄD", fontSize = 11.5.sp, fontWeight = FontWeight.W800, color = GlassAccent, modifier = Modifier.padding(start = 6.dp, bottom = 10.dp))
                Row(
                    Modifier.fillMaxWidth().glass(RoundedCornerShape(22.dp)).padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(Modifier.size(38.dp).clip(RoundedCornerShape(11.dp)).background(GlassAccent.copy(alpha = 0.18f)), contentAlignment = Alignment.Center) {
                        Icon(Icons.Outlined.DarkMode, null, tint = GlassAccent, modifier = Modifier.size(20.dp))
                    }
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Tryb ciemny", fontSize = 15.sp, fontWeight = FontWeight.W700, color = GlassTextPrimary)
                        Text(if (dark) "Włączony" else "Wyłączony", fontSize = 12.5.sp, color = GlassTextSecondary)
                    }
                    Switch(
                        checked = dark, onCheckedChange = { onToggleDark() },
                        colors = SwitchDefaults.colors(checkedTrackColor = GlassAccent, checkedThumbColor = Color.White)
                    )
                }
                Spacer(Modifier.height(24.dp))

                // Chmura (Supabase) — synchronizacja między urządzeniami
                Text("CHMURA (SYNCHRONIZACJA)", fontSize = 11.5.sp, fontWeight = FontWeight.W800, color = GlassAccent, modifier = Modifier.padding(start = 6.dp, bottom = 10.dp))
                CloudSection(cloud, onCloudConfig, onCloudSignIn, onCloudSignUp, onCloudSignOut, onCloudBackup, onCloudRestore)
                Spacer(Modifier.height(24.dp))

                // Gmail → zadania: maile z gwiazdką stają się zadaniami w Skrzynce.
                Text("GMAIL (MAILE Z GWIAZDKĄ → ZADANIA)", fontSize = 11.5.sp, fontWeight = FontWeight.W800, color = GlassAccent, modifier = Modifier.padding(start = 6.dp, bottom = 10.dp))
                GmailSection(gmailUser, gmail, onSaveGmail, onSyncGmail)
                Spacer(Modifier.height(24.dp))

                // Zużycie AI: tokeny, szacowany i realny koszt — rozliczenia przy koncie.
                AiUsageCard(
                    aiPromptTokens, aiCompletionTokens, aiImageCount, onResetAiUsage,
                    adminKeySet, aiCost, onRefreshCost, onOpenAdminKey = { showAdminKey = true }
                )
                Spacer(Modifier.height(28.dp))

                // Wyloguj
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Color(0x1AEB4034))
                        .bouncy(0.97f, onLogout).padding(vertical = 15.dp),
                    horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Outlined.Logout, null, tint = Color(0xFFEB4034), modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Wyloguj się (lokalnie)", fontSize = 14.sp, fontWeight = FontWeight.W800, color = Color(0xFFEB4034))
                }
                Spacer(Modifier.navigationBarsPadding())
            }
        }
    }
    if (showAdminKey) {
        AdminKeyDialog(adminKeySet, onDismiss = { showAdminKey = false }) { k ->
            onSetAdminKey(k); showAdminKey = false; onRefreshCost()
        }
    }
}

/** Sekcja synchronizacji z chmurą (Supabase): konfiguracja → logowanie → kopia. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CloudSection(
    cloud: pl.media30.todoisto.ui.TodoViewModel.CloudState,
    onCloudConfig: (String, String) -> Unit,
    onCloudSignIn: (String, String) -> Unit,
    onCloudSignUp: (String, String) -> Unit,
    onCloudSignOut: () -> Unit,
    onCloudBackup: () -> Unit,
    onCloudRestore: () -> Unit
) {
    var url by remember { mutableStateOf("") }
    var anon by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    Column(Modifier.fillMaxWidth().glass(RoundedCornerShape(24.dp)).padding(18.dp)) {
        when {
            // 1) Brak konfiguracji projektu → wklej URL + klucz anon.
            !cloud.configured -> {
                Text("Podłącz swój darmowy projekt Supabase, aby synchronizować zadania między urządzeniami.", fontSize = 12.5.sp, color = GlassTextSecondary)
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(url, { url = it }, label = { Text("URL projektu (https://…supabase.co)") }, singleLine = true, colors = glassFieldColors(), shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(anon, { anon = it }, label = { Text("Klucz anon (public)") }, singleLine = true, colors = glassFieldColors(), shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(14.dp))
                CloudButton("Zapisz projekt", primary = true, enabled = !cloud.busy) { onCloudConfig(url, anon) }
                Spacer(Modifier.height(8.dp))
                Text("Znajdziesz je w Supabase → Project Settings → API (Project URL i anon public).", fontSize = 11.sp, color = GlassTextSecondary.copy(alpha = 0.85f))
            }
            // 2) Skonfigurowane, ale niezalogowane → e-mail + hasło.
            !cloud.signedIn -> {
                Text("Projekt podłączony. Zaloguj się lub załóż konto w chmurze.", fontSize = 12.5.sp, color = GlassTextSecondary)
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(email, { email = it }, label = { Text("E-mail") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email), colors = glassFieldColors(), shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(password, { password = it }, label = { Text("Hasło") }, singleLine = true, visualTransformation = PasswordVisualTransformation(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password), colors = glassFieldColors(), shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(14.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(Modifier.weight(1f)) { CloudButton("Zaloguj", primary = true, enabled = !cloud.busy) { onCloudSignIn(email, password) } }
                    Box(Modifier.weight(1f)) { CloudButton("Załóż konto", primary = false, enabled = !cloud.busy) { onCloudSignUp(email, password) } }
                }
            }
            // 3) Zalogowany → kopia / przywracanie.
            else -> {
                Text("Zalogowano w chmurze:", fontSize = 12.5.sp, color = GlassTextSecondary)
                Text(cloud.email, fontSize = 14.sp, fontWeight = FontWeight.W800, color = GlassTextPrimary)
                Spacer(Modifier.height(14.dp))
                CloudButton("Wyślij kopię do chmury", primary = true, enabled = !cloud.busy, onClick = onCloudBackup)
                Spacer(Modifier.height(8.dp))
                CloudButton("Pobierz z chmury", primary = false, enabled = !cloud.busy, onClick = onCloudRestore)
                Spacer(Modifier.height(8.dp))
                Text("Wyloguj z chmury", fontSize = 12.5.sp, fontWeight = FontWeight.W700, color = GlassTextSecondary, modifier = Modifier.bouncy(1f, onCloudSignOut).padding(top = 4.dp))
            }
        }
        if (cloud.busy) {
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(color = GlassAccent, strokeWidth = 2.5.dp, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(10.dp)); Text("Łączę z chmurą…", fontSize = 12.5.sp, color = GlassTextSecondary)
            }
        }
        cloud.message?.let {
            Spacer(Modifier.height(10.dp))
            Text(it, fontSize = 12.5.sp, fontWeight = FontWeight.W700, color = Color(0xFF1F8A5B))
        }
        cloud.error?.let {
            Spacer(Modifier.height(10.dp))
            Text(it, fontSize = 12.5.sp, fontWeight = FontWeight.W700, color = Color(0xFFEB4034))
        }
    }
}

/** Gmail przez hasło do aplikacji: zapis danych + ręczna synchronizacja. */
@Composable
private fun GmailSection(
    savedUser: String,
    state: pl.media30.todoisto.ui.TodoViewModel.GmailState,
    onSave: (String, String) -> Unit,
    onSync: () -> Unit
) {
    var user by remember { mutableStateOf(savedUser) }
    var pass by remember { mutableStateOf("") }
    val connected = savedUser.isNotBlank()
    Column(Modifier.fillMaxWidth().glass(RoundedCornerShape(24.dp)).padding(18.dp)) {
        if (!connected) {
            Text(
                "Oznacz maila gwiazdką w Gmailu, a Todoisto zamieni go w zadanie (z linkiem do wątku). " +
                    "Potrzebne HASŁO DO APLIKACJI: włącz weryfikację dwuetapową, wejdź na myaccount.google.com/apppasswords i wygeneruj hasło dla poczty.",
                fontSize = 12.5.sp, lineHeight = 18.sp, color = GlassTextSecondary
            )
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(user, { user = it }, label = { Text("Adres Gmail") }, singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                colors = glassFieldColors(), shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(pass, { pass = it }, label = { Text("Hasło do aplikacji (16 znaków)") }, singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                colors = glassFieldColors(), shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(14.dp))
            CloudButton("Połącz z Gmailem", primary = true, enabled = !state.loading) { onSave(user, pass) }
        } else {
            Text("Połączono:", fontSize = 12.5.sp, color = GlassTextSecondary)
            Text(savedUser, fontSize = 14.sp, fontWeight = FontWeight.W800, color = GlassTextPrimary)
            Spacer(Modifier.height(14.dp))
            CloudButton(if (state.loading) "Pobieram…" else "Pobierz maile z gwiazdką", primary = true, enabled = !state.loading, onClick = onSync)
            Spacer(Modifier.height(8.dp))
            Text(
                "Rozłącz", fontSize = 12.5.sp, fontWeight = FontWeight.W700, color = GlassTextSecondary,
                modifier = Modifier.bouncy(1f) { onSave("", "") }.padding(top = 4.dp)
            )
        }
        if (state.loading) {
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(color = GlassAccent, strokeWidth = 2.5.dp, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(10.dp)); Text("Łączę z Gmailem…", fontSize = 12.5.sp, color = GlassTextSecondary)
            }
        }
        state.info?.let { Spacer(Modifier.height(10.dp)); Text(it, fontSize = 12.5.sp, fontWeight = FontWeight.W700, color = Color(0xFF1F8A5B)) }
        state.error?.let { Spacer(Modifier.height(10.dp)); Text(it, fontSize = 12.5.sp, lineHeight = 18.sp, fontWeight = FontWeight.W700, color = Color(0xFFEB4034)) }
    }
}

@Composable
private fun CloudButton(label: String, primary: Boolean, enabled: Boolean, onClick: () -> Unit) {
    val bg = if (primary) GlassAccent else GlassAccent.copy(alpha = 0.14f)
    val fg = if (primary) Color.White else GlassAccent
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(bg)
            .bouncy(0.97f) { if (enabled) onClick() }.padding(vertical = 13.dp),
        horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically
    ) { Text(label, color = fg, fontSize = 13.5.sp, fontWeight = FontWeight.W800) }
}
