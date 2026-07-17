package pl.media30.todoisto.data

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat

/**
 * Przypomnienia CZASOWE (pole Task.reminderAt, millis): dokładny alarm systemowy
 * (AlarmManager) → notyfikacja o wskazanej godzinie, także gdy apka jest zamknięta.
 * Po restarcie telefonu alarmy odtwarza BootReceiver.
 */
object Reminders {

    private const val CHANNEL = "time_reminders"

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < 26) return
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL, "Przypomnienia", NotificationManager.IMPORTANCE_HIGH)
        )
    }

    private fun pending(context: Context, taskId: Long, title: String): PendingIntent {
        val i = Intent(context, ReminderReceiver::class.java)
            .putExtra("taskId", taskId)
            .putExtra("title", title)
        return PendingIntent.getBroadcast(
            context, taskId.toInt(), i, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    /** Ustawia (lub nadpisuje) alarm; czasy z przeszłości są ignorowane. */
    fun schedule(context: Context, taskId: Long, title: String, atMillis: Long) {
        if (atMillis <= System.currentTimeMillis()) return
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pi = pending(context, taskId, title)
        val exactAllowed = Build.VERSION.SDK_INT < 31 || runCatching { am.canScheduleExactAlarms() }.getOrDefault(false)
        runCatching {
            if (exactAllowed) am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, atMillis, pi)
            else am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, atMillis, pi)
        }
    }

    fun cancel(context: Context, taskId: Long) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        runCatching { am.cancel(pending(context, taskId, "")) }
    }

    fun notifyNow(context: Context, taskId: Long, title: String) {
        ensureChannel(context)
        val open = context.packageManager.getLaunchIntentForPackage(context.packageName)
        val pi = PendingIntent.getActivity(
            context, taskId.toInt(), open ?: Intent(), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val notif = NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle("Przypomnienie")
            .setContentText(title)
            .setContentIntent(pi)
            .setAutoCancel(true)
            .build()
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        runCatching { nm.notify(200_000 + taskId.toInt(), notif) }
    }

    /** Odtwarza wszystkie przyszłe alarmy (po restarcie telefonu / starcie apki). */
    suspend fun rescheduleAll(context: Context) {
        val db = TodoDatabase.getInstance(context)
        val now = System.currentTimeMillis()
        db.taskDao().getWithReminder().forEach { t ->
            val at = t.reminderAt ?: return@forEach
            if (!t.isCompleted && at > now) schedule(context, t.id, t.title, at)
        }
    }
}

/** Odbiera alarm i pokazuje notyfikację. */
class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getLongExtra("taskId", -1L)
        val title = intent.getStringExtra("title").orEmpty().ifBlank { "Zadanie czeka" }
        if (id >= 0) Reminders.notifyNow(context, id, title)
    }
}

/** Po restarcie telefonu alarmy systemowe znikają — ustawiamy je ponownie. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        val pending = goAsync()
        Thread {
            runCatching { kotlinx.coroutines.runBlocking { Reminders.rescheduleAll(context) } }
            pending.finish()
        }.start()
    }
}
