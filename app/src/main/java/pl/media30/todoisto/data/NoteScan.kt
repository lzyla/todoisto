package pl.media30.todoisto.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.ByteArrayOutputStream
import java.io.File

/** Pomocnik do skanu kartki: tworzenie pliku na zdjęcie + wczytanie/skalowanie. */
object NoteScan {

    /** Tymczasowy plik w cache + jego FileProvider Uri (do pełnowymiarowego zdjęcia z aparatu). */
    fun newCaptureUri(context: Context): Pair<Uri, File> {
        val dir = File(context.cacheDir, "scans").apply { mkdirs() }
        val file = File(dir, "scan_${System.currentTimeMillis()}.jpg")
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        return uri to file
    }

    /**
     * Wczytuje obraz z [uri], skaluje do maks. [maxDim] px (dłuższy bok) i zwraca JPEG.
     * Ograniczenie rozmiaru trzyma payload do AI w ryzach, a tekst pozostaje czytelny.
     */
    fun bytesFromUri(context: Context, uri: Uri, maxDim: Int = 1600): ByteArray? = runCatching {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        val longSide = maxOf(bounds.outWidth, bounds.outHeight).coerceAtLeast(1)
        var sample = 1
        while (longSide / sample > maxDim * 2) sample *= 2
        val opts = BitmapFactory.Options().apply { inSampleSize = sample }
        val bmp = context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opts) }
            ?: return null
        val scale = maxDim.toFloat() / maxOf(bmp.width, bmp.height)
        val out = if (scale < 1f) Bitmap.createScaledBitmap(bmp, (bmp.width * scale).toInt(), (bmp.height * scale).toInt(), true) else bmp
        val baos = ByteArrayOutputStream()
        out.compress(Bitmap.CompressFormat.JPEG, 85, baos)
        baos.toByteArray()
    }.getOrNull()
}
