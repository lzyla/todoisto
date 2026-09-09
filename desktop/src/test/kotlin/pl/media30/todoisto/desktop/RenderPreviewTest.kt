package pl.media30.todoisto.desktop

import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.unit.Density
import androidx.compose.ui.use
import org.jetbrains.skia.EncodedImageFormat
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import pl.media30.todoisto.shared.CloudTask
import java.io.File
import java.time.LocalDate

/**
 * Renderuje główne ekrany desktopu do PNG (build/previews) — do wizualnej
 * weryfikacji bez uruchamiania okna. Sprawdza też, że nic nie wywala kompozycji.
 */
class RenderPreviewTest {
    private lateinit var home: File
    private val out = File("build/previews").apply { mkdirs() }

    @Before fun tempHome() {
        home = File.createTempFile("todoisto-preview", "").apply { delete(); mkdirs() }
        System.setProperty("user.home", home.absolutePath)
        // Ustawienia żyją w java.util.prefs — wyzeruj to, co testy mogłyby zmienić.
        AppSettings().apply { darkTheme = false; themeId = "violet"; activeCustomBg = ""; photoBackground = false; drawerOpen = true; activeArea = -1L }
        seed()
    }

    private fun seed() {
        val repo = TaskRepository()
        val today = LocalDate.now().toEpochDay()
        val praca = repo.addProject("Praca", 0xFF4D6BFF)
        val dom = repo.addProject("Dom", 0xFF34D399)
        repo.addLabel("pilne", 0xFFC24DFF); val lab = repo.labels.first().id
        repo.addSection(praca, "Do zrobienia")
        repo.add(CloudTask(id = 0, title = "Raport kwartalny dla zarządu", priority = "P1", dueDate = today - 2, projectId = praca, labelIds = listOf(lab), durationMinutes = 90, deadline = today + 1))
        repo.add(CloudTask(id = 0, title = "Zadzwonić do księgowej", priority = "P2", dueDate = today, dueTimeMinutes = 9 * 60 + 30, projectId = praca))
        val t = repo.add(CloudTask(id = 0, title = "Przetłumaczyć ofertę na angielski", priority = "P3", dueDate = today, dueTimeMinutes = 14 * 60, projectId = praca, durationMinutes = 45))
        repo.addSubtask(t.id, praca, "Zebrać materiały"); repo.addSubtask(t.id, praca, "Wysłać do korekty")
        repo.add(CloudTask(id = 0, title = "Zakupy na weekend", dueDate = today, dueTimeMinutes = 18 * 60, projectId = dom))
        repo.add(CloudTask(id = 0, title = "Trening", dueDate = today, recurrence = "DAILY"))
        repo.add(CloudTask(id = 0, title = "Czytanie 20 minut", dueDate = today, recurrence = "DAILY"))
        repo.add(CloudTask(id = 0, title = "Przegląd samochodu", dueDate = today + 2, projectId = dom))
        repo.add(CloudTask(id = 0, title = "Pomysł: newsletter", projectId = null))
        repo.add(CloudTask(id = 0, title = "Zrobione wczoraj", isCompleted = true, completedAt = System.currentTimeMillis() - 86_400_000, dueDate = today - 1))
    }

    private fun render(name: String, setup: (AppState) -> Unit) {
        val holder = AppHolder()
        ImageComposeScene(1180, 820, Density(1f)) { App(holder) }.use { scene ->
            scene.render(0L)
            holder.state!!.splashDone = true
            setup(holder.state!!)
            // Kilka klatek, żeby animacje (AnimatedVisibility) doszły do końca.
            var t = 0L
            repeat(12) { t += 100_000_000L; scene.render(t) }
            val img = scene.render(t + 100_000_000L)
            val bytes = img.encodeToData(EncodedImageFormat.PNG)!!.bytes
            File(out, "$name.png").writeBytes(bytes)
            assertTrue(bytes.size > 10_000)
        }
    }

    @Test fun today() = render("today") { it.showView(AppView.Today) }
    @Test fun routinesOpen() = render("routines") { it.routOpen = true }
    @Test fun upcomingDark() = render("upcoming_dark") { it.settings.darkTheme = true; it.settingsChanged(); it.showView(AppView.Upcoming) }
    @Test fun detail() = render("detail") { st -> st.openDetail(st.repo.tasks.first { it.title.startsWith("Przetłumaczyć") }.id) }
    @Test fun quickAdd() = render("quickadd") { it.quickAddOpen = true; it.quickAddText = "Raport jutro o 15:00 #Praca @pilne p1" }
    @Test fun settings() = render("settings") { it.showSettings = true }
    @Test fun account() = render("account") { it.showSettings = true; it.showAccount = true }
    @Test fun stats() = render("stats") { it.showStats = true }
    @Test fun project() = render("project") { st -> st.showView(AppView.ProjectView(st.repo.projects.first().id)) }
}
