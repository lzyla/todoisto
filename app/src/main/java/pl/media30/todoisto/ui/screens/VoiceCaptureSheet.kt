package pl.media30.todoisto.ui.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import pl.media30.todoisto.ui.components.bouncy
import pl.media30.todoisto.ui.theme.GlassAccent
import pl.media30.todoisto.ui.theme.GlassRim
import pl.media30.todoisto.ui.theme.GlassSurface
import pl.media30.todoisto.ui.theme.GlassTextPrimary
import pl.media30.todoisto.ui.theme.GlassTextSecondary

/**
 * Animowana nakładka nagrywania głosu — własny [SpeechRecognizer] z pulsującym
 * mikrofonem i tekstem na żywo (zamiast surowego okna systemowego). Po
 * rozpoznaniu zwraca tekst przez [onResult]; [onDismiss] zamyka bez dodawania.
 */
@Composable
fun VoiceCaptureOverlay(onResult: (String) -> Unit, onDismiss: () -> Unit) {
    val context = LocalContext.current
    var partial by remember { mutableStateOf("") }
    var status by remember { mutableStateOf("Przygotowuję…") }
    var error by remember { mutableStateOf<String?>(null) }
    var listening by remember { mutableStateOf(false) }
    var granted by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED)
    }

    val permLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
        granted = ok
        if (!ok) error = "Brak zgody na mikrofon. Włącz ją w ustawieniach systemu."
    }
    LaunchedEffect(Unit) { if (!granted) permLauncher.launch(Manifest.permission.RECORD_AUDIO) }

    // Uruchomienie i sprzątanie rozpoznawania mowy powiązane z uprawnieniem.
    DisposableEffect(granted) {
        if (!granted || !SpeechRecognizer.isRecognitionAvailable(context)) {
            if (granted) error = "Rozpoznawanie mowy niedostępne na tym urządzeniu."
            return@DisposableEffect onDispose { }
        }
        val recognizer = SpeechRecognizer.createSpeechRecognizer(context)
        recognizer.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) { status = "Słucham…"; listening = true }
            override fun onBeginningOfSpeech() { listening = true }
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() { listening = false; status = "Przetwarzam…" }
            override fun onError(errorCode: Int) {
                error = when (errorCode) {
                    SpeechRecognizer.ERROR_NO_MATCH, SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Nie usłyszałem — spróbuj ponownie."
                    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Brak zgody na mikrofon."
                    SpeechRecognizer.ERROR_NETWORK, SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Problem z siecią."
                    else -> "Nie udało się rozpoznać mowy."
                }
                listening = false
            }
            override fun onResults(results: Bundle?) {
                val text = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()?.trim()
                if (!text.isNullOrBlank()) onResult(text) else onDismiss()
            }
            override fun onPartialResults(partialResults: Bundle?) {
                partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()?.let { partial = it }
            }
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "pl-PL")
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        }
        recognizer.startListening(intent)
        onDispose { runCatching { recognizer.stopListening() }; runCatching { recognizer.destroy() } }
    }

    // Pulsujący mikrofon.
    val pulse = rememberInfiniteTransition(label = "voice")
    val scale by pulse.animateFloat(
        1f, if (listening) 1.14f else 1.05f,
        infiniteRepeatable(tween(700, easing = androidx.compose.animation.core.FastOutSlowInEasing), RepeatMode.Reverse),
        label = "micScale"
    )
    val ring by pulse.animateFloat(
        1f, 1.9f, infiniteRepeatable(tween(1400, easing = androidx.compose.animation.core.LinearEasing), RepeatMode.Restart), label = "ring"
    )
    val ringAlpha by pulse.animateFloat(
        0.35f, 0f, infiniteRepeatable(tween(1400, easing = androidx.compose.animation.core.LinearEasing), RepeatMode.Restart), label = "ringA"
    )

    Box(
        Modifier.fillMaxSize().background(Color(0x66000000))
            .clickable(remember { MutableInteractionSource() }, null, onClick = onDismiss),
        contentAlignment = Alignment.Center
    ) {
        Column(
            Modifier.fillMaxWidth(0.82f)
                .clip(RoundedCornerShape(30.dp)).background(GlassSurface)
                .clickable(remember { MutableInteractionSource() }, null) {}
                .padding(vertical = 30.dp, horizontal = 22.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(contentAlignment = Alignment.Center) {
                // Rozchodzący się pierścień (tylko gdy słucha).
                if (listening && error == null) {
                    Box(Modifier.size(92.dp).scale(ring).clip(CircleShape).background(GlassAccent.copy(alpha = ringAlpha)))
                }
                Box(
                    Modifier.size(92.dp).scale(if (error == null) scale else 1f).clip(CircleShape)
                        .background(if (error == null) GlassAccent else Color(0xFFEB4034)),
                    contentAlignment = Alignment.Center
                ) { Icon(Icons.Filled.Mic, null, tint = Color.White, modifier = Modifier.size(40.dp)) }
            }
            Spacer(Modifier.height(20.dp))
            Text(
                error ?: status, fontSize = 16.sp, fontWeight = FontWeight.W800,
                color = if (error == null) GlassTextPrimary else Color(0xFFEB4034)
            )
            Spacer(Modifier.height(8.dp))
            Text(
                if (partial.isNotBlank()) partial else "Powiedz zadanie, np. raport jutro o 15 #praca",
                fontSize = 13.sp, color = GlassTextSecondary, textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Spacer(Modifier.height(22.dp))
            Text(
                "Anuluj", fontSize = 13.sp, fontWeight = FontWeight.W800, color = GlassAccent,
                modifier = Modifier.clip(RoundedCornerShape(50)).bouncy(0.95f, onDismiss).padding(horizontal = 22.dp, vertical = 10.dp)
            )
        }
    }
}
