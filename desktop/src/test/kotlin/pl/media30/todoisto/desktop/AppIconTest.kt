package pl.media30.todoisto.desktop

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.nio.ByteBuffer

/**
 * Generuje ikonę we wszystkich rozmiarach i pakuje `desktop/icons/Todoisto.icns`
 * (ikona .app) oraz `src/main/resources/icon.png`. Uruchamiane z testami, żeby
 * ikona zawsze była zgodna z rysunkiem w `AppIcon`.
 */
class AppIconTest {
    @Test fun `ikona renderuje sie i powstaje icns`() {
        val types = mapOf(16 to "icp4", 32 to "icp5", 64 to "icp6", 128 to "ic07", 256 to "ic08", 512 to "ic09", 1024 to "ic10")
        val chunks = java.io.ByteArrayOutputStream()
        types.forEach { (size, type) ->
            val png = java.io.ByteArrayOutputStream().also { javax.imageio.ImageIO.write(AppIcon.image(size), "png", it) }.toByteArray()
            chunks.write(type.toByteArray(Charsets.US_ASCII))
            chunks.write(ByteBuffer.allocate(4).putInt(8 + png.size).array())
            chunks.write(png)
        }
        val body = chunks.toByteArray()
        val icns = File("icons/Todoisto.icns").apply { parentFile.mkdirs() }
        icns.outputStream().use { out ->
            out.write("icns".toByteArray(Charsets.US_ASCII)); out.write(ByteBuffer.allocate(4).putInt(8 + body.size).array()); out.write(body)
        }
        AppIcon.writePng(512, File("src/main/resources/icon.png"))
        AppIcon.writePng(256, File("build/previews/icon_256.png").apply { parentFile.mkdirs() })
        assertTrue(icns.length() > 50_000)
    }
}
