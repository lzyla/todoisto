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
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import pl.media30.todoisto.data.Account
import pl.media30.todoisto.ui.components.bouncy
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
    dark: Boolean,
    themeId: String,
    onSelectTheme: (String) -> Unit,
    onToggleDark: () -> Unit,
    onLogout: () -> Unit,
    onBack: () -> Unit
) {
    BackHandler { onBack() }
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
                Row(
                    Modifier.fillMaxWidth().glass(RoundedCornerShape(24.dp)).padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val initial = (account?.name?.trim()?.firstOrNull() ?: '?').uppercaseChar().toString()
                    Box(
                        Modifier.size(56.dp).clip(CircleShape).background(GlassAccent),
                        contentAlignment = Alignment.Center
                    ) { Text(initial, fontSize = 24.sp, fontWeight = FontWeight.W900, color = Color.White) }
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(account?.name ?: "Użytkownik", fontSize = 18.sp, fontWeight = FontWeight.W800, color = GlassTextPrimary)
                        Text(account?.email ?: "", fontSize = 13.sp, color = GlassTextSecondary)
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
                Spacer(Modifier.height(28.dp))

                // Wyloguj
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Color(0x1AEB4034))
                        .bouncy(0.97f, onLogout).padding(vertical = 15.dp),
                    horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Outlined.Logout, null, tint = Color(0xFFEB4034), modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Wyloguj się", fontSize = 14.sp, fontWeight = FontWeight.W800, color = Color(0xFFEB4034))
                }
                Spacer(Modifier.navigationBarsPadding())
            }
        }
    }
}
