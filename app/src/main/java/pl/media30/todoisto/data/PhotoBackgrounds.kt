package pl.media30.todoisto.data

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URL

/**
 * Pobiera losowe, prawdziwe zdjęcia (Lorem Picsum — darmowe, bez klucza) i
 * nakłada na nie kolorową maskę w barwach apki, żeby tła były spójne
 * stylistycznie i tekst pozostawał czytelny. Bez AI i bez kosztów.
 */
object PhotoBackgrounds {

    // Kolorowe maski (górny → dolny) w rodzinie apki. Alfa dobrana tak, by
    // zdjęcie było widoczne, ale utrzymane w fiolecie/różu/turkusie/złocie.
    private val masks: List<Pair<Int, Int>> = listOf(
        0xB36E45D9.toInt() to 0xCC44239E.toInt(), // fiolet
        0xB3C24DA0.toInt() to 0xCC5B35C4.toInt(), // magenta → fiolet
        0xB32EA68C.toInt() to 0xCC3B2E8C.toInt(), // turkus → fiolet
        0xB3FF6E8E.toInt() to 0xCCC06BE0.toInt(), // róż → fiolet
        0xB3F4B740.toInt() to 0xCCC24DA0.toInt(), // złoto → magenta
        0xB34E80C8.toInt() to 0xCC44239E.toInt()  // błękit → fiolet
    )

    val maskCount: Int get() = masks.size

    /** Pobiera losowe zdjęcie o zadanym rozmiarze i zwraca surowe bajty. */
    fun fetchRandomPhoto(w: Int, h: Int, seed: Int): ByteArray {
        val conn = (URL("https://picsum.photos/seed/$seed/$w/$h").openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            instanceFollowRedirects = true
            connectTimeout = 15000
            readTimeout = 30000
        }
        val code = conn.responseCode
        if (code !in 200..299) throw RuntimeException("Nie udało się pobrać zdjęcia (kod $code)")
        return conn.inputStream.use { it.readBytes() }
    }

    /** Nakłada kolorową maskę (pionowy gradient w barwach apki) i zwraca JPEG. */
    fun withColorMask(photoBytes: ByteArray, maskIndex: Int): ByteArray {
        val src = BitmapFactory.decodeByteArray(photoBytes, 0, photoBytes.size)
            ?: throw RuntimeException("Uszkodzone zdjęcie")
        val bmp = if (src.isMutable) src else src.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(bmp)
        val (top, bot) = masks[maskIndex % masks.size]
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.shader = LinearGradient(0f, 0f, 0f, bmp.height.toFloat(), top, bot, Shader.TileMode.CLAMP)
        canvas.drawRect(0f, 0f, bmp.width.toFloat(), bmp.height.toFloat(), paint)
        val out = ByteArrayOutputStream()
        bmp.compress(Bitmap.CompressFormat.JPEG, 88, out)
        if (bmp !== src) src.recycle()
        bmp.recycle()
        return out.toByteArray()
    }
}
