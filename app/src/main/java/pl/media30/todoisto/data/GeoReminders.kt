package pl.media30.todoisto.data

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.Worker
import androidx.work.WorkerParameters
import java.util.concurrent.TimeUnit

/**
 * Przypomnienia w miejscu (geofencing „lekki", bez Play Services):
 * co ~15 min (WorkManager) i przy starcie apki porównujemy ostatnią znaną
 * lokalizację z zadaniami, które mają ustawione miejsce (promień 300 m).
 * Trafienie → lokalna notyfikacja; ponawiamy najwcześniej po 6 h.
 */
object GeoReminders {

    const val RADIUS_M = 300.0
    private const val CHANNEL = "geo_reminders"
    private const val RENOTIFY_MS = 6 * 60 * 60 * 1000L

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < 26) return
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL, "Przypomnienia w miejscu", NotificationManager.IMPORTANCE_HIGH)
        )
    }

    /** Porównuje lokalizację z zadaniami i wysyła notyfikacje. Bezpieczne bez zgód (nic nie robi). */
    fun check(context: Context, tasks: List<Task>) {
        val here = LocationHelper.lastKnown(context) ?: return
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return
        ensureChannel(context)
        val prefs = context.getSharedPreferences("todoisto_geo", Context.MODE_PRIVATE)
        val now = System.currentTimeMillis()
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        tasks.forEach { t ->
            val lat = t.locLat ?: return@forEach
            val lon = t.locLon ?: return@forEach
            if (t.isCompleted) return@forEach
            val dist = LocationHelper.distanceMeters(here.first, here.second, lat, lon)
            if (dist > RADIUS_M) return@forEach
            val key = "notif_${t.id}"
            if (now - prefs.getLong(key, 0L) < RENOTIFY_MS) return@forEach
            prefs.edit().putLong(key, now).apply()
            val open = context.packageManager.getLaunchIntentForPackage(context.packageName)
            val pi = PendingIntent.getActivity(
                context, t.id.toInt(), open ?: Intent(), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
            val notif = NotificationCompat.Builder(context, CHANNEL)
                .setSmallIcon(android.R.drawable.ic_menu_mylocation)
                .setContentTitle("Jesteś w pobliżu: ${t.locName ?: "miejsce zadania"}")
                .setContentText(t.title)
                .setContentIntent(pi)
                .setAutoCancel(true)
                .build()
            runCatching { nm.notify(t.id.toInt(), notif) }
        }
    }

    /** Harmonogram cyklicznego sprawdzania (min. interwał WorkManagera = 15 min). */
    fun schedule(context: Context) {
        val req = PeriodicWorkRequestBuilder<GeoWorker>(15, TimeUnit.MINUTES).build()
        androidx.work.WorkManager.getInstance(context)
            .enqueueUniquePeriodicWork("geo_reminders", ExistingPeriodicWorkPolicy.KEEP, req)
    }
}

/** Worker tła: czyta zadania z lokalizacją i odpala sprawdzenie. */
class GeoWorker(context: Context, params: WorkerParameters) : Worker(context, params) {
    override fun doWork(): Result {
        runCatching {
            val db = TodoDatabase.getInstance(applicationContext)
            val tasks = kotlinx.coroutines.runBlocking { db.taskDao().getWithLocation() }
            GeoReminders.check(applicationContext, tasks)
        }
        return Result.success()
    }
}
