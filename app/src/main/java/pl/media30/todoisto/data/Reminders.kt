package pl.media30.todoisto.data

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * Przypomnienia CZASOWE (pole Task.reminderAt, millis): dokładny alarm systemowy
 * (AlarmManager) → notyfikacja o wskazanej godzinie, także gdy apka jest zamknięta.
 *
 * Zasady niezawodności:
 *  - jeden punkt prawdy: [sync] po KAŻDYM zapisie zadania (hak w TaskRepository),
 *    po synchronizacji, starcie aplikacji i restarcie telefonu ([rescheduleAll]);
 *  - tożsamość alarmu = Uri zadania (nie `id.toInt()`, które przy 53-bitowych id
 *    dawało kolizje i nadpisywanie cudzych alarmów);
 *  - odbiornik sprawdza w bazie, czy przypomnienie nadal jest aktualne (zadanie
 *    mogło zostać ukończone, usunięte albo przełożone na innym urządzeniu);
 *  - każde dostarczenie trafia do dziennika z opóźnieniem — do testu tygodniowego.
 */
object Reminders {

    private const val CHANNEL = "time_reminders"
    private const val EXTRA_ID = "taskId"
    private const val EXTRA_AT = "at"
    private const val EXTRA_TITLE = "title"
    private const val EXTRA_TEST = "test"
    const val TEST_ID = -42L

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < 26) return
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL, "Przypomnienia", NotificationManager.IMPORTANCE_HIGH)
        )
    }

    private fun intentFor(context: Context, taskId: Long): Intent =
        Intent(context, ReminderReceiver::class.java).setData(Uri.parse("todoisto://reminder/$taskId"))

    private fun pending(context: Context, taskId: Long, extras: (Intent) -> Unit = {}, flags: Int = PendingIntent.FLAG_UPDATE_CURRENT): PendingIntent? {
        val i = intentFor(context, taskId).also(extras)
        return PendingIntent.getBroadcast(context, 0, i, PendingIntent.FLAG_IMMUTABLE or flags)
    }

    fun canScheduleExact(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < 31) return true
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        return runCatching { am.canScheduleExactAlarms() }.getOrDefault(false)
    }

    /** Ustawia (lub nadpisuje) alarm; czasy z przeszłości są ignorowane. */
    fun schedule(context: Context, taskId: Long, title: String, atMillis: Long, test: Boolean = false) {
        if (atMillis <= System.currentTimeMillis()) return
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pi = pending(context, taskId, {
            it.putExtra(EXTRA_ID, taskId).putExtra(EXTRA_AT, atMillis).putExtra(EXTRA_TITLE, title).putExtra(EXTRA_TEST, test)
        }) ?: return
        runCatching {
            if (canScheduleExact(context)) am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, atMillis, pi)
            else am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, atMillis, pi)
        }
        ReminderLog.scheduled(context, taskId, atMillis, exact = canScheduleExact(context))
    }

    fun cancel(context: Context, taskId: Long) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        runCatching { pending(context, taskId)?.let { am.cancel(it) } }
        cancelLegacy(context, taskId)
    }

    /** Alarmy ustawione przez wersje ≤ v119 (requestCode = id.toInt(), bez Uri). */
    private fun cancelLegacy(context: Context, taskId: Long) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val legacy = PendingIntent.getBroadcast(
            context, taskId.toInt(), Intent(context, ReminderReceiver::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_NO_CREATE
        )
        if (legacy != null) runCatching { am.cancel(legacy); legacy.cancel() }
    }

    /** Doprowadza alarm do stanu zgodnego z zadaniem (ustaw / przesuń / odwołaj). */
    fun sync(context: Context, task: Task) {
        val at = task.reminderAt
        if (at != null && at > System.currentTimeMillis() && !task.isCompleted && !task.deleted) {
            schedule(context, task.id, task.title, at)
        } else cancel(context, task.id)
    }

    fun notifyNow(context: Context, taskId: Long, title: String, test: Boolean = false) {
        ensureChannel(context)
        val open = context.packageManager.getLaunchIntentForPackage(context.packageName)
        val pi = PendingIntent.getActivity(
            context, taskId.hashCode(), open ?: Intent(), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val notif = NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle(if (test) "Test przypomnienia" else "Przypomnienie")
            .setContentText(title)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pi)
            .setAutoCancel(true)
            .build()
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        runCatching { nm.notify(200_000 + taskId.hashCode(), notif) }
    }

    /** Odtwarza wszystkie przyszłe alarmy (po restarcie telefonu / starcie apki / synchronizacji). */
    suspend fun rescheduleAll(context: Context) {
        val db = TodoDatabase.getInstance(context)
        db.taskDao().getWithReminder().forEach { t ->
            cancelLegacy(context, t.id)
            sync(context, t)
        }
    }

    /** Test diagnostyczny: notyfikacja za [minutes] minut, przez tę samą ścieżkę co zwykłe przypomnienie. */
    fun scheduleTest(context: Context, minutes: Int = 2) {
        schedule(context, TEST_ID, "Jeśli to widzisz o czasie — przypomnienia działają.", System.currentTimeMillis() + minutes * 60_000L, test = true)
    }

    internal suspend fun handleFired(context: Context, intent: Intent) {
        val id = intent.getLongExtra(EXTRA_ID, Long.MIN_VALUE)
        val at = intent.getLongExtra(EXTRA_AT, 0L)
        val test = intent.getBooleanExtra(EXTRA_TEST, false)
        val title = intent.getStringExtra(EXTRA_TITLE).orEmpty().ifBlank { "Zadanie czeka" }
        if (id == Long.MIN_VALUE) return
        if (test) {
            ReminderLog.fired(context, id, at, shown = true, note = "test")
            notifyNow(context, id, title, test = true)
            return
        }
        val task = TodoDatabase.getInstance(context).taskDao().getTaskById(id)
        val stale = task == null || task.deleted || task.isCompleted || task.reminderAt == null ||
            (at != 0L && task.reminderAt != at)
        if (stale) {
            ReminderLog.fired(context, id, at, shown = false, note = "nieaktualne")
            return
        }
        ReminderLog.fired(context, id, at, shown = true, note = null)
        notifyNow(context, id, task!!.title)
    }

    /**
     * Przesuwa przypomnienie razem z terminem: gdy dueDate zmienia się o N dni,
     * reminderAt przesuwa się o N dni kalendarzowych (z zachowaniem godziny, także
     * przy zmianie czasu letni/zimowy).
     */
    fun shiftWithDueDate(reminderAt: Long?, oldDue: Long?, newDue: Long?, zone: ZoneId = ZoneId.systemDefault()): Long? {
        if (reminderAt == null || oldDue == null || newDue == null || oldDue == newDue) return reminderAt
        return Instant.ofEpochMilli(reminderAt).atZone(zone).plusDays(newDue - oldDue).toInstant().toEpochMilli()
    }

    /** Moment „dzień + minuty od północy" w LOKALNEJ strefie (nie UTC). */
    fun localMillis(epochDay: Long, minutes: Int, zone: ZoneId = ZoneId.systemDefault()): Long =
        LocalDate.ofEpochDay(epochDay).atStartOfDay(zone).plusMinutes(minutes.toLong()).toInstant().toEpochMilli()
}

/** Stan uprawnień i ustawień systemu, od których zależy dostarczenie przypomnień. */
object ReminderHealth {
    data class Status(
        val notificationsAllowed: Boolean,
        val channelEnabled: Boolean,
        val exactAlarms: Boolean,
        val batteryUnrestricted: Boolean
    ) {
        val allGood get() = notificationsAllowed && channelEnabled && exactAlarms && batteryUnrestricted
    }

    fun check(context: Context): Status {
        val nmc = NotificationManagerCompat.from(context)
        val postGranted = Build.VERSION.SDK_INT < 33 ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        val channelOk = Build.VERSION.SDK_INT < 26 || run {
            Reminders.ensureChannel(context)
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            (nm.getNotificationChannel("time_reminders")?.importance ?: NotificationManager.IMPORTANCE_DEFAULT) != NotificationManager.IMPORTANCE_NONE
        }
        val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        return Status(
            notificationsAllowed = postGranted && nmc.areNotificationsEnabled(),
            channelEnabled = channelOk,
            exactAlarms = Reminders.canScheduleExact(context),
            batteryUnrestricted = pm.isIgnoringBatteryOptimizations(context.packageName)
        )
    }

    fun notificationSettingsIntent(context: Context): Intent =
        if (Build.VERSION.SDK_INT >= 26) Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
        else appDetails(context)

    fun exactAlarmIntent(context: Context): Intent =
        if (Build.VERSION.SDK_INT >= 31) Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:${context.packageName}"))
        else appDetails(context)

    /** Lista optymalizacji baterii — wybór „Bez ograniczeń" robi użytkownik (bez uprawnienia REQUEST_IGNORE…). */
    fun batteryIntent(): Intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)

    private fun appDetails(context: Context) =
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}"))
}

/** Dziennik ustawionych i dostarczonych przypomnień (ostatnie 60 wpisów) — do testu tygodniowego. */
object ReminderLog {
    data class Entry(val taskId: Long, val scheduledFor: Long, val firedAt: Long, val shown: Boolean, val note: String?) {
        val delayMs get() = firedAt - scheduledFor
    }

    private const val PREFS = "todoisto_reminder_log"
    private const val KEY = "entries"
    private const val MAX = 60

    fun scheduled(context: Context, taskId: Long, at: Long, exact: Boolean) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putBoolean("last_exact", exact).putLong("last_scheduled_at", System.currentTimeMillis()).apply()
    }

    @Synchronized
    fun fired(context: Context, taskId: Long, scheduledFor: Long, shown: Boolean, note: String?) {
        val p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val line = listOf(taskId, scheduledFor, System.currentTimeMillis(), if (shown) 1 else 0, note.orEmpty()).joinToString("|")
        val all = (listOf(line) + p.getString(KEY, "").orEmpty().split('\n').filter { it.isNotBlank() }).take(MAX)
        p.edit().putString(KEY, all.joinToString("\n")).apply()
    }

    fun entries(context: Context): List<Entry> =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, "").orEmpty()
            .split('\n').mapNotNull { l ->
                val f = l.split('|'); if (f.size < 5) return@mapNotNull null
                runCatching { Entry(f[0].toLong(), f[1].toLong(), f[2].toLong(), f[3] == "1", f[4].ifBlank { null }) }.getOrNull()
            }

    fun clear(context: Context) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().remove(KEY).apply()
}

/** Odbiera alarm, sprawdza aktualność w bazie i pokazuje notyfikację. */
class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try { runCatching { Reminders.handleFired(context.applicationContext, intent) } } finally { pending.finish() }
        }
    }
}

/** Po restarcie telefonu (i po aktualizacji aplikacji) alarmy systemowe znikają — ustawiamy je ponownie. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED && intent.action != Intent.ACTION_MY_PACKAGE_REPLACED) return
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try { runCatching { Reminders.rescheduleAll(context.applicationContext) } } finally { pending.finish() }
        }
    }
}
