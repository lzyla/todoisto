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
