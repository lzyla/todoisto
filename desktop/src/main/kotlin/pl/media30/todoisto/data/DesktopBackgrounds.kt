package pl.media30.todoisto.data

import java.awt.AlphaComposite
import java.awt.Color
import java.awt.GradientPaint
import java.awt.RadialGradientPaint
import java.awt.RenderingHints
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URL
import javax.imageio.ImageIO

/**
 * Tła na desktopie (odpowiednik `PhotoBackgrounds` + `PresetBackgrounds` z Androida,
 * ale na Java2D): losowe zdjęcie z Lorem Picsum z nałożoną kolorową maską w barwach
 * apki, a gdy brak sieci — lokalny gradient.
 */
object DesktopBackgrounds {
    private val masks: List<Pair<Color, Color>> = listOf(
        Color(0xB558F6) to Color(0x5B2BE0),
        Color(0x2563C9) to Color(0x6B3FE0),
        Color(0x1F9D6B) to Color(0x2DD4BF),
        Color(0xE0632C) to Color(0xD6417F),
        Color(0x5A5A72) to Color(0x241844)
    )
    val maskCount: Int get() = masks.size

    fun fetchRandomPhoto(w: Int, h: Int, seed: Int): BufferedImage {
        val conn = (URL("https://picsum.photos/seed/$seed/$w/$h").openConnection() as HttpURLConnection).apply {
            connectTimeout = 12000; readTimeout = 15000; instanceFollowRedirects = true
        }
        return conn.inputStream.use { ImageIO.read(it) } ?: error("Nie udało się odczytać zdjęcia")
    }

    fun withColorMask(photo: BufferedImage, maskIdx: Int): ByteArray {
        val (c1, c2) = masks[maskIdx.coerceIn(0, masks.size - 1)]
        val out = BufferedImage(photo.width, photo.height, BufferedImage.TYPE_INT_RGB)
        val g = out.createGraphics()
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR)
        g.drawImage(photo, 0, 0, null)
        g.composite = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.55f)
        g.paint = GradientPaint(0f, 0f, c1, photo.width.toFloat(), photo.height.toFloat(), c2)
        g.fillRect(0, 0, photo.width, photo.height)
        g.dispose()
        return ByteArrayOutputStream().also { ImageIO.write(out, "jpg", it) }.toByteArray()
    }

    /** Lokalny gradient (fallback offline). */
    fun gradientPng(w: Int, h: Int, seed: Int): ByteArray {
        val (c1, c2) = masks[(seed % masks.size + masks.size) % masks.size]
        val img = BufferedImage(w, h, BufferedImage.TYPE_INT_RGB)
        val g = img.createGraphics()
        g.paint = GradientPaint(0f, 0f, c1, w.toFloat(), h.toFloat(), c2)
        g.fillRect(0, 0, w, h)
        g.paint = RadialGradientPaint(w * 0.7f, h * 0.25f, w * 0.6f, floatArrayOf(0f, 1f), arrayOf(Color(255, 255, 255, 90), Color(255, 255, 255, 0)))
        g.fillRect(0, 0, w, h)
        g.dispose()
        return ByteArrayOutputStream().also { ImageIO.write(img, "png", it) }.toByteArray()
    }

    /** Skalowanie własnego zdjęcia do max 1600 px (jak `NoteScan.bytesFromUri`). */
    fun scaledJpeg(file: java.io.File, maxDim: Int = 1600): ByteArray? {
        val src = runCatching { ImageIO.read(file) }.getOrNull() ?: return null
        val scale = minOf(1.0, maxDim.toDouble() / maxOf(src.width, src.height))
        val w = (src.width * scale).toInt().coerceAtLeast(1); val h = (src.height * scale).toInt().coerceAtLeast(1)
        val out = BufferedImage(w, h, BufferedImage.TYPE_INT_RGB)
        val g = out.createGraphics()
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR)
        g.drawImage(src, 0, 0, w, h, null); g.dispose()
        return ByteArrayOutputStream().also { ImageIO.write(out, "jpg", it) }.toByteArray()
    }
}
