package pl.media30.todoisto.data

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import java.io.ByteArrayOutputStream

/** Rysuje wbudowane, spokojne tła-gradienty (bez AI, bez kosztów). */
object PresetBackgrounds {

    /**
     * Bank kolorowych palet (3 przystanki każda) — w rodzinie kolorów apki:
     * fiolet, lawenda, róż, brzoskwinia, błękit, mięta, złoto. Losujemy z niego
     * tła bez AI, więc jest z czego wybierać i za każdym razem wychodzi inaczej.
     */
    val palettes: List<IntArray> = listOf(
        intArrayOf(0xFFF3E7FF.toInt(), 0xFFFFE3F1.toInt(), 0xFFFFF0D9.toInt()), // fiolet→róż→żółty
        intArrayOf(0xFFDDF3FF.toInt(), 0xFFE7ECFF.toInt(), 0xFFF6E7FF.toInt()), // błękit→lawenda
        intArrayOf(0xFFFFE9D6.toInt(), 0xFFFFD9E3.toInt(), 0xFFE9D9FF.toInt()), // brzoskwinia→róż→fiolet
        intArrayOf(0xFFEDE3FF.toInt(), 0xFFD9E4FF.toInt(), 0xFFD9F5EE.toInt()), // lawenda→błękit→mięta
        intArrayOf(0xFFFFE7F0.toInt(), 0xFFF0E0FF.toInt(), 0xFFDCE7FF.toInt()), // róż→fiolet→błękit
        intArrayOf(0xFFFFF2D9.toInt(), 0xFFFFE0CC.toInt(), 0xFFFFD6E6.toInt()), // złoto→brzoskwinia→róż
        intArrayOf(0xFFE0F7F0.toInt(), 0xFFDCEEFF.toInt(), 0xFFEDE3FF.toInt()), // mięta→błękit→lawenda
        intArrayOf(0xFFF6E7FF.toInt(), 0xFFEAD9FF.toInt(), 0xFFDCD1FF.toInt()), // odcienie fioletu
        intArrayOf(0xFFFFEDE0.toInt(), 0xFFFFE0EC.toInt(), 0xFFF3E0FF.toInt()), // ciepły róż→fiolet
        intArrayOf(0xFFE7ECFF.toInt(), 0xFFF0E7FF.toInt(), 0xFFFFEAF3.toInt()), // błękit→lawenda→róż
        intArrayOf(0xFFDFF6FF.toInt(), 0xFFE5F0FF.toInt(), 0xFFF7E9FF.toInt()), // cyjan→błękit→fiolet
        intArrayOf(0xFFFFF0E0.toInt(), 0xFFFCE3F0.toInt(), 0xFFEDE0FF.toInt())  // krem→róż→lawenda
    )

    /** Zwraca [n] różnych, losowych palet z banku. */
    fun randomPalettes(n: Int = 3): List<IntArray> = palettes.shuffled().take(n)

    /** Miękki gradient pionowy + delikatny rozświetlony „blob" — zwraca PNG jako bajty. */
    fun gradientPng(w: Int, h: Int, colors: IntArray): ByteArray {
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        // Baza: pionowy gradient przez podane kolory.
        paint.shader = LinearGradient(0f, 0f, w * 0.4f, h.toFloat(), colors, null, Shader.TileMode.CLAMP)
        canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), paint)

        // Dwa rozmyte rozświetlenia, żeby tło „oddychało".
        paint.shader = RadialGradient(
            w * 0.25f, h * 0.28f, w * 0.75f,
            intArrayOf(0x33FFFFFF, 0x00FFFFFF), null, Shader.TileMode.CLAMP
        )
        canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), paint)
        paint.shader = RadialGradient(
            w * 0.8f, h * 0.75f, w * 0.7f,
            intArrayOf(colors.last() and 0x40FFFFFF, 0x00FFFFFF), null, Shader.TileMode.CLAMP
        )
        canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), paint)

        val out = ByteArrayOutputStream()
        bmp.compress(Bitmap.CompressFormat.PNG, 100, out)
        bmp.recycle()
        return out.toByteArray()
    }
}
