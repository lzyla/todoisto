package pl.media30.todoisto.desktop

import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.toComposeImageBitmap
import java.awt.BasicStroke
import java.awt.Color
import java.awt.GradientPaint
import java.awt.RenderingHints
import java.awt.geom.AffineTransform
import java.awt.geom.Path2D
import java.awt.geom.RoundRectangle2D
import java.awt.image.BufferedImage

/**
 * Ikona aplikacji rysowana 1:1 z `ic_launcher_background/foreground.xml` Androida
 * (gradient #B558F6 → #5B2BE0, biały „ptaszek" i trzy linie listy). Używana jako
 * ikona okna, ikona w Docku i logo na ekranie startowym.
 */
object AppIcon {
    fun image(size: Int, rounded: Boolean = true): BufferedImage {
        val img = BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB)
        val g = img.createGraphics()
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE)
        val pad = if (rounded) size * 0.04 else 0.0
        val s = size - 2 * pad
        val r = if (rounded) s * 0.225 else 0.0
        g.paint = GradientPaint(pad.toFloat(), pad.toFloat(), Color(0xB558F6), (pad + s).toFloat(), (pad + s).toFloat(), Color(0x5B2BE0))
        g.fill(RoundRectangle2D.Double(pad, pad, s, s, r, r))
        val at = AffineTransform()
        at.translate(pad, pad); at.scale(s / 512.0, s / 512.0)
        at.translate(256.0, 256.0); at.scale(0.62, 0.62); at.translate(-256.0, -256.0)
        g.transform(at)
        g.color = Color.WHITE
        g.stroke = BasicStroke(38f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND)
        val check = Path2D.Double().apply { moveTo(112.0, 240.0); lineTo(176.0, 304.0); lineTo(304.0, 166.0) }
        g.draw(check)
        g.fill(RoundRectangle2D.Double(305.0, 217.0, 115.0, 30.0, 30.0, 30.0))
        g.fill(RoundRectangle2D.Double(260.0, 297.0, 160.0, 30.0, 30.0, 30.0))
        g.fill(RoundRectangle2D.Double(175.0, 377.0, 245.0, 30.0, 30.0, 30.0))
        g.dispose()
        return img
    }

    /** Sam biały znak (bez tła) — do splash screenu na gradientowym tle. */
    fun foreground(size: Int): BufferedImage {
        val img = BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB)
        val g = img.createGraphics()
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        val at = AffineTransform()
        at.scale(size / 512.0, size / 512.0)
        at.translate(256.0, 256.0); at.scale(0.62, 0.62); at.translate(-256.0, -256.0)
        g.transform(at)
        g.color = Color.WHITE
        g.stroke = BasicStroke(38f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND)
        g.draw(Path2D.Double().apply { moveTo(112.0, 240.0); lineTo(176.0, 304.0); lineTo(304.0, 166.0) })
        g.fill(RoundRectangle2D.Double(305.0, 217.0, 115.0, 30.0, 30.0, 30.0))
        g.fill(RoundRectangle2D.Double(260.0, 297.0, 160.0, 30.0, 30.0, 30.0))
        g.fill(RoundRectangle2D.Double(175.0, 377.0, 245.0, 30.0, 30.0, 30.0))
        g.dispose()
        return img
    }

    val painter: Painter by lazy { BitmapPainter(image(256).toComposeImageBitmap()) }
    val foregroundPainter: Painter by lazy { BitmapPainter(foreground(512).toComposeImageBitmap()) }

    /** Ikona w Docku macOS (przy `gradle run` Java pokazałaby swoją). */
    fun installDockIcon() {
        runCatching {
            if (java.awt.Taskbar.isTaskbarSupported()) {
                val tb = java.awt.Taskbar.getTaskbar()
                if (tb.isSupported(java.awt.Taskbar.Feature.ICON_IMAGE)) tb.iconImage = image(512)
            }
        }
    }
}
