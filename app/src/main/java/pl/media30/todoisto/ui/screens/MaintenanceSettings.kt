package pl.media30.todoisto.ui.screens

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BatterySaver
import androidx.compose.material.icons.outlined.CloudDownload
import androidx.compose.material.icons.outlined.CloudUpload
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.RestartAlt
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import pl.media30.todoisto.data.DbSafety
import pl.media30.todoisto.data.ReminderHealth
import pl.media30.todoisto.data.ReminderLog
import pl.media30.todoisto.data.Reminders
import pl.media30.todoisto.ui.theme.GlassTextSecondary
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val OK = Color(0xFF34D399)
private val BAD = Color(0xFFFB7185)

@Composable
private fun Hint(text: String) = Text(
    text, fontSize = 11.5.sp, color = GlassTextSecondary.copy(alpha = 0.85f),
    modifier = Modifier.padding(start = 6.dp, top = 8.dp, end = 6.dp)
)

/**
 * Diagnostyka przypomnień: czy system pozwoli je dostarczyć, test „za 2 min"
 * i dziennik faktycznych opóźnień — narzędzie do tygodniowego testu z checklisty.
 */
@Composable
internal fun ReminderSettings() {
    val ctx = LocalContext.current
    var tick by remember { mutableIntStateOf(0) }
    // Odświeżenie po powrocie z ustawień systemu.
    val owner = LocalLifecycleOwner.current
    androidx.compose.runtime.DisposableEffect(owner) {
        val obs = LifecycleEventObserver { _, e -> if (e == Lifecycle.Event.ON_RESUME) tick++ }
        owner.lifecycle.addObserver(obs)
        onDispose { owner.lifecycle.removeObserver(obs) }
    }
    val status = remember(tick) { ReminderHealth.check(ctx) }
    val log = remember(tick) { ReminderLog.entries(ctx) }
    val permLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { tick++ }
    fun open(i: Intent) = runCatching { ctx.startActivity(i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }

    SettingsSection("Stan")
    SettingsCard {
        NavRow(
            Icons.Outlined.Notifications, if (status.notificationsAllowed && status.channelEnabled) OK else BAD, "Powiadomienia",
            when {
                !status.notificationsAllowed -> "Zablokowane — dotknij, aby zezwolić"
                !status.channelEnabled -> "Kanał „Przypomnienia” wyłączony"
                else -> "Dozwolone"
            },
            trailing = { StatusDot(status.notificationsAllowed && status.channelEnabled) }
        ) {
            if (!status.notificationsAllowed && android.os.Build.VERSION.SDK_INT >= 33)
                permLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
            else open(ReminderHealth.notificationSettingsIntent(ctx))
        }
        RowDivider()
        NavRow(
            Icons.Outlined.Schedule, if (status.exactAlarms) OK else BAD, "Dokładne alarmy",
            if (status.exactAlarms) "Dozwolone — przypomnienie o czasie" else "Brak zgody — przypomnienia mogą się spóźniać",
            trailing = { StatusDot(status.exactAlarms) }
        ) { open(ReminderHealth.exactAlarmIntent(ctx)) }
        RowDivider()
        NavRow(
            Icons.Outlined.BatterySaver, if (status.batteryUnrestricted) OK else BAD, "Optymalizacja baterii",
            if (status.batteryUnrestricted) "Bez ograniczeń" else "Ograniczona — ustaw „Bez ograniczeń” dla Todoisto",
            trailing = { StatusDot(status.batteryUnrestricted) }
        ) { open(ReminderHealth.batteryIntent()) }
    }
    Hint(if (status.allGood) "System pozwala dostarczać przypomnienia na czas." else "Czerwone pozycje mogą opóźniać lub blokować przypomnienia. Dotknij, aby poprawić.")
    Spacer(Modifier.height(22.dp))

    SettingsSection("Test")
    SettingsCard {
        NavRow(Icons.Outlined.NotificationsActive, Color(0xFFEB8909), "Wyślij test za 2 minuty", "Zablokuj telefon i poczekaj", trailing = {}) {
            Reminders.scheduleTest(ctx, 2); tick++
        }
    }
    Hint("Test idzie tą samą ścieżką co zwykłe przypomnienie (alarm systemowy). Wynik zobaczysz w dzienniku poniżej.")
    Spacer(Modifier.height(22.dp))

    SettingsSection("Dziennik dostarczeń")
    SettingsCard {
        if (log.isEmpty()) {
            NavRow(Icons.Outlined.History, GlassTextSecondary, "Brak wpisów", "Pojawią się po pierwszym przypomnieniu", trailing = {})
        } else {
            val fmt = DateTimeFormatter.ofPattern("dd.MM HH:mm").withZone(ZoneId.systemDefault())
            log.take(20).forEachIndexed { i, e ->
                if (i > 0) RowDivider()
                val delayMin = e.delayMs / 60_000.0
                val late = e.shown && e.scheduledFor > 0 && e.delayMs > 60_000
                NavRow(
                    Icons.Outlined.History, if (!e.shown) GlassTextSecondary else if (late) BAD else OK,
                    fmt.format(Instant.ofEpochMilli(e.firedAt)) + if (e.note == "test") " · test" else "",
                    when {
                        !e.shown -> "Pominięte: ${e.note ?: "nieaktualne"}"
                        e.scheduledFor <= 0 -> "Dostarczone"
                        else -> "Opóźnienie: %.1f min".format(delayMin)
                    },
                    trailing = {}
                )
            }
            RowDivider()
            NavRow(Icons.Outlined.RestartAlt, BAD, "Wyczyść dziennik", null, trailing = {}) { ReminderLog.clear(ctx); tick++ }
        }
    }
    Hint("Opóźnienie > 1 min oznacza zwykle brak zgody na dokładne alarmy albo agresywne oszczędzanie baterii.")
}

/** Eksport/import pliku i kopie lokalne (codzienne JSON + kopie bazy sprzed migracji). */
@Composable
internal fun DataSettings(vm: pl.media30.todoisto.ui.TodoViewModel?) {
    val ctx = LocalContext.current
    if (vm == null) { Hint("Niedostępne w tym widoku."); return }
    val op by vm.dataOp.collectAsState()
    var confirmFile by remember { mutableStateOf<java.io.File?>(null) }
    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) vm.exportToUri(uri)
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) vm.importFromUri(uri)
    }
    LaunchedEffect(Unit) { vm.clearDataOp() }
    val jsonBackups = remember(op) { DbSafety.listJsonBackups(ctx) }
    val dbBackups = remember(op) { DbSafety.listDbBackups(ctx) }

    SettingsSection("Plik")
    SettingsCard {
        NavRow(Icons.Outlined.CloudUpload, Color(0xFF34D399), "Eksportuj do pliku", "Pełna kopia JSON (np. na Dysk lub do Plików)", trailing = {}) {
            exportLauncher.launch("todoisto-${java.time.LocalDate.now()}.json")
        }
        RowDivider()
        NavRow(Icons.Outlined.CloudDownload, Color(0xFF6B8AFF), "Importuj z pliku", "Scala z bieżącymi danymi — nic nie znika", trailing = {}) {
            importLauncher.launch(arrayOf("application/json", "text/plain", "*/*"))
        }
    }
    when {
        op.busy -> Hint("Pracuję…")
        op.error != null -> Hint("Błąd: ${op.error}")
        op.message != null -> Hint(op.message!!)
        else -> Hint("Eksport chroni przed utratą telefonu. Import nigdy nie kasuje lokalnych zadań — scala je z kopią.")
    }
    Spacer(Modifier.height(22.dp))

    SettingsSection("Kopie automatyczne (na tym telefonie)")
    SettingsCard {
        if (jsonBackups.isEmpty()) NavRow(Icons.Outlined.History, GlassTextSecondary, "Brak kopii dziennych", "Powstają raz dziennie przy starcie", trailing = {})
        jsonBackups.take(7).forEachIndexed { i, f ->
            if (i > 0) RowDivider()
            NavRow(Icons.Outlined.History, Color(0xFF9AA6FF), f.nameWithoutExtension.removePrefix("todoisto-"), "${f.length() / 1024} KB · dotknij, aby scalić", trailing = {}) {
                confirmFile = f
            }
        }
    }
    Hint("Kopie bazy sprzed aktualizacji schematu: ${dbBackups.size}" + (dbBackups.firstOrNull()?.let { " (ostatnia: ${it.name})" } ?: "") + ".")

    confirmFile?.let { f ->
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { confirmFile = null },
            title = { Text("Scalić kopię?") },
            text = { Text("Dane z ${f.nameWithoutExtension.removePrefix("todoisto-")} zostaną scalone z obecnymi. Nowsze wersje zadań zostają, usunięte nie wracają.") },
            confirmButton = { androidx.compose.material3.TextButton(onClick = { vm.importLocalBackup(f); confirmFile = null }) { Text("Scal") } },
            dismissButton = { androidx.compose.material3.TextButton(onClick = { confirmFile = null }) { Text("Anuluj") } }
        )
    }
}
