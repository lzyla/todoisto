package pl.media30.todoisto.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import pl.media30.todoisto.ui.components.bouncy
import pl.media30.todoisto.ui.components.glassFieldColors
import pl.media30.todoisto.ui.theme.GlassAccent
import pl.media30.todoisto.ui.theme.GlassTextPrimary
import pl.media30.todoisto.ui.theme.GlassTextSecondary
import pl.media30.todoisto.ui.theme.glass

/**
 * Bramka logowania/rejestracji (liquid glass). Renderowana zamiast aplikacji,
 * gdy użytkownik nie jest zalogowany. Backend jest lokalny (AccountStore),
 * ale przepływ jest pełny: rejestracja → logowanie → wejście do apki.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthScreen(
    hasAccount: Boolean,
    onLogin: (email: String, password: String) -> String?,
    onRegister: (name: String, email: String, password: String) -> String?
) {
    var registerMode by remember { mutableStateOf(!hasAccount) }
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    Box(Modifier.fillMaxSize().imePadding(), contentAlignment = Alignment.Center) {
        Column(
            Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 26.dp, vertical = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                Modifier.size(64.dp).clip(RoundedCornerShape(20.dp)).background(GlassAccent),
                contentAlignment = Alignment.Center
            ) { Icon(Icons.Outlined.CheckCircle, null, tint = androidx.compose.ui.graphics.Color.White, modifier = Modifier.size(34.dp)) }
            Spacer(Modifier.height(16.dp))
            Text("Todoisto", fontSize = 28.sp, fontWeight = FontWeight.W900, color = GlassTextPrimary)
            Text(
                if (registerMode) "Załóż konto, by zacząć" else "Zaloguj się, aby kontynuować",
                fontSize = 13.5.sp, color = GlassTextSecondary
            )
            Spacer(Modifier.height(26.dp))

            Column(Modifier.fillMaxWidth().glass(RoundedCornerShape(26.dp)).padding(20.dp)) {
                if (registerMode) {
                    OutlinedTextField(
                        value = name, onValueChange = { name = it; error = null },
                        label = { Text("Imię") }, singleLine = true,
                        colors = glassFieldColors(), shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(12.dp))
                }
                OutlinedTextField(
                    value = email, onValueChange = { email = it; error = null },
                    label = { Text("E-mail") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    colors = glassFieldColors(), shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = password, onValueChange = { password = it; error = null },
                    label = { Text("Hasło") }, singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    colors = glassFieldColors(), shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                if (error != null) {
                    Spacer(Modifier.height(10.dp))
                    Text(error!!, fontSize = 12.5.sp, fontWeight = FontWeight.W700, color = androidx.compose.ui.graphics.Color(0xFFEB4034))
                }
                Spacer(Modifier.height(18.dp))
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(15.dp)).background(GlassAccent)
                        .bouncy(0.97f) {
                            error = if (registerMode) onRegister(name, email, password) else onLogin(email, password)
                        }
                        .padding(vertical = 14.dp),
                    horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        if (registerMode) "Załóż konto" else "Zaloguj się",
                        color = androidx.compose.ui.graphics.Color.White, fontSize = 15.sp, fontWeight = FontWeight.W800
                    )
                }
            }

            Spacer(Modifier.height(18.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    if (registerMode) "Masz już konto?" else "Nie masz konta?",
                    fontSize = 13.sp, color = GlassTextSecondary
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    if (registerMode) "Zaloguj się" else "Zarejestruj się",
                    fontSize = 13.sp, fontWeight = FontWeight.W800, color = GlassAccent,
                    modifier = Modifier.bouncy(1f) { registerMode = !registerMode; error = null }
                )
            }
            Spacer(Modifier.height(14.dp))
            Text(
                "Konto działa lokalnie na tym urządzeniu — dane i hasło nie opuszczają telefonu.",
                fontSize = 11.sp, color = GlassTextSecondary.copy(alpha = 0.8f),
                modifier = Modifier.padding(horizontal = 8.dp)
            )
        }
    }
}
