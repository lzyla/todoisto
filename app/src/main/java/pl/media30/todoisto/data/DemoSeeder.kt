package pl.media30.todoisto.data

import java.time.LocalDate
import java.time.ZoneId

/**
 * Zasiew danych demonstracyjnych przy pierwszym uruchomieniu — tak, aby od razu
 * było widać w akcji wszystkie funkcje: projekty (ulubione/archiwum), sekcje,
 * etykiety, zadania z priorytetami/godzinami/czasem/terminami/cyklami/podzadaniami,
 * zadania zaległe, rutyny, skrzynkę, ukończone (liczniki celów) oraz pulę aktywności.
 */
object DemoSeeder {

    /**
     * Jednorazowo (na wersję klucza) dosiewa komplet danych demo — niezależnie od
     * tego, czy w bazie coś już jest („uzupełnij, żeby przetestować wszystko").
     * Oznacza sukces dopiero po zapisaniu, więc błąd → ponowna próba przy następnym starcie.
     */
    suspend fun seedIfEmpty(db: TodoDatabase, settings: SettingsStore) {
        if (settings.isDemoSeeded()) return
        try {
            seed(db)
            settings.seedDemoStats()
            settings.markDemoSeeded()
        } catch (e: Exception) {
            android.util.Log.e("DemoSeeder", "Seeding failed", e)
        }
    }

    private fun millis(date: LocalDate, hour: Int = 12): Long =
        date.atTime(hour, 0).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

    private suspend fun seed(db: TodoDatabase) {
        val td = db.taskDao(); val pd = db.projectDao(); val sd = db.sectionDao()
        val ld = db.labelDao(); val ad = db.activityDao(); val ar = db.areaDao()
        val today = LocalDate.now().toEpochDay()
        val now = System.currentTimeMillis()
        var pos = 0
        fun p() = pos++

        // ── Obszary ─────────────────────────────────────────────────────────
        val aOsobiste = ar.insert(Area(name = "Osobiste", colorArgb = 0xFF2DD4BF, position = 0))
        val aVocative = ar.insert(Area(name = "Vocative", colorArgb = 0xFF4D6BFF, position = 1))
        val aPraca = ar.insert(Area(name = "Praca", colorArgb = 0xFF9B6BFF, position = 2))
        val aProjekty = ar.insert(Area(name = "Projekty", colorArgb = 0xFFEB8909, position = 3))

        // ── Projekty (przypisane do obszarów) ───────────────────────────────
        val praca = pd.insert(Project(name = "Praca", colorArgb = 0xFF4D6BFF, position = 0, isFavorite = true, workEffortType = EffortType.MENTAL, areaId = aPraca))
        val dom = pd.insert(Project(name = "Dom", colorArgb = 0xFF2DD4BF, position = 1, workEffortType = EffortType.PHYSICAL, areaId = aOsobiste))
        val zdrowie = pd.insert(Project(name = "Zdrowie", colorArgb = 0xFFFB7185, position = 2, isFavorite = true, workEffortType = EffortType.PHYSICAL, areaId = aOsobiste))
        val nauka = pd.insert(Project(name = "Nauka", colorArgb = 0xFF9B6BFF, position = 3, workEffortType = EffortType.MENTAL, areaId = aProjekty))
        val kampania = pd.insert(Project(name = "Kampania NGO", colorArgb = 0xFF34D399, position = 4, workEffortType = EffortType.MENTAL, areaId = aVocative))
        pd.insert(Project(name = "Remont mieszkania", colorArgb = 0xFFEB8909, position = 5, isArchived = true, areaId = aOsobiste))

        // ── Sekcje (Praca) ──────────────────────────────────────────────────
        val secSprint = sd.insert(Section(projectId = praca, name = "Sprint bieżący", position = 0))
        val secReview = sd.insert(Section(projectId = praca, name = "Do przeglądu", position = 1))

        // ── Etykiety ────────────────────────────────────────────────────────
        val lPilne = ld.insert(Label(name = "pilne", colorArgb = 0xFFC24DFF, isFavorite = true))
        val lZakupy = ld.insert(Label(name = "zakupy", colorArgb = 0xFFEB8909))
        val lTelefon = ld.insert(Label(name = "telefon", colorArgb = 0xFF4D6BFF))
        val lCzekam = ld.insert(Label(name = "czekam", colorArgb = 0xFF34D399))

        // ── Zadania: DZISIAJ ────────────────────────────────────────────────
        // Długie zadanie z bliskim deadline'em → znacznik „⏳ Napięty deadline"
        val raport = td.insert(Task(
            title = "Przygotować raport miesięczny", notes = "Zebrać dane sprzedażowe i wysłać do zarządu.",
            priority = Priority.P1, dueDate = today, dueTimeMinutes = 9 * 60, durationMinutes = 90,
            deadline = today + 1, projectId = praca, sectionId = secSprint, labelIds = listOf(lPilne),
            position = p(), createdAt = now
        ))
        td.insert(Task(title = "Zebrać dane z CRM", isCompleted = true, completedAt = millis(LocalDate.now(), 8), parentId = raport, projectId = praca, position = p(), createdAt = now))
        td.insert(Task(title = "Zbudować wykresy", parentId = raport, projectId = praca, position = p(), createdAt = now))
        td.insert(Task(title = "Napisać podsumowanie", parentId = raport, projectId = praca, position = p(), createdAt = now))

        td.insert(Task(title = "Stand-up zespołu", priority = Priority.P3, dueDate = today, dueTimeMinutes = 9 * 60 + 30,
            recurrence = Recurrence.DAILY, projectId = praca, sectionId = secSprint, position = p(), createdAt = now))
        td.insert(Task(title = "Zadzwonić do księgowej", priority = Priority.P2, dueDate = today, dueTimeMinutes = 11 * 60,
            durationMinutes = 20, projectId = praca, labelIds = listOf(lTelefon, lCzekam), position = p(), createdAt = now))
        td.insert(Task(title = "Nadać paczkę na poczcie", priority = Priority.P4, dueDate = today,
            projectId = dom, position = p(), createdAt = now))
        td.insert(Task(title = "Przegląd pull requestów", priority = Priority.P3, dueDate = today, dueTimeMinutes = 14 * 60,
            durationMinutes = 45, projectId = praca, sectionId = secReview, position = p(), createdAt = now))
        td.insert(Task(title = "Kupić prezent dla Zosi", priority = Priority.P2, dueDate = today, dueTimeMinutes = 15 * 60,
            deadline = today + 3, projectId = dom, labelIds = listOf(lZakupy),
            attachments = listOf("https://images.example.com/prezent-jednorozec.jpg"), position = p(), createdAt = now))
        td.insert(Task(title = "Trening — siłownia", priority = Priority.P4, dueDate = today, dueTimeMinutes = 18 * 60 + 30,
            durationMinutes = 75, recurrence = Recurrence.WEEKLY, projectId = zdrowie, position = p(), createdAt = now))
        td.insert(Task(title = "Kurs Kotlin — rozdział 5", priority = Priority.P3, dueDate = today, dueTimeMinutes = 20 * 60,
            durationMinutes = 40, projectId = nauka, position = p(), createdAt = now))
        // Obszar Vocative
        td.insert(Task(title = "Zredagować newsletter NGO", priority = Priority.P2, dueDate = today, dueTimeMinutes = 13 * 60,
            durationMinutes = 45, projectId = kampania, position = p(), createdAt = now))
        td.insert(Task(title = "Przygotować grafiki do posta", priority = Priority.P3, dueDate = today + 1,
            durationMinutes = 30, projectId = kampania, position = p(), createdAt = now))
        td.insert(Task(title = "Zaplanować kampanię w social media", priority = Priority.P2, dueDate = today + 2,
            projectId = kampania, position = p(), createdAt = now))

        // ── Zaległe (dla „Asystenta tygodnia" i chipa „Zaległe") ────────────
        td.insert(Task(title = "Wysłać fakturę klientowi", priority = Priority.P1, dueDate = today - 2,
            projectId = praca, labelIds = listOf(lPilne), position = p(), createdAt = now))
        td.insert(Task(title = "Odpisać na maila od Marka", priority = Priority.P3, dueDate = today - 1,
            projectId = praca, labelIds = listOf(lCzekam), position = p(), createdAt = now))

        // ── Rutyny (cykliczne, bez godziny, P3/P4) → pasek Rutyn ────────────
        td.insert(Task(title = "Nauka japońskiego — 15 min", priority = Priority.P4, dueDate = today, recurrence = Recurrence.DAILY, projectId = nauka, position = p(), createdAt = now))
        td.insert(Task(title = "Podlać kwiaty", priority = Priority.P4, dueDate = today, recurrence = Recurrence.DAILY, projectId = dom, position = p(), createdAt = now))
        td.insert(Task(title = "Wypić 2 litry wody", priority = Priority.P4, dueDate = today, recurrence = Recurrence.DAILY, position = p(), createdAt = now))

        // ── Nadchodzące (dni +1..+7) ────────────────────────────────────────
        td.insert(Task(title = "Spotkanie z klientem", priority = Priority.P1, dueDate = today + 1, dueTimeMinutes = 10 * 60,
            durationMinutes = 60, projectId = praca, labelIds = listOf(lPilne), position = p(), createdAt = now))
        td.insert(Task(title = "Wizyta u dentysty", priority = Priority.P2, dueDate = today + 2, dueTimeMinutes = 12 * 60 + 30,
            deadline = today + 2, projectId = zdrowie, position = p(), createdAt = now))
        td.insert(Task(title = "Oddać książki do biblioteki", priority = Priority.P3, dueDate = today + 3, projectId = nauka, position = p(), createdAt = now))
        td.insert(Task(title = "Zrobić zakupy spożywcze", priority = Priority.P3, dueDate = today + 3, projectId = dom, labelIds = listOf(lZakupy), position = p(), createdAt = now))
        td.insert(Task(title = "Urodziny mamy — kupić kwiaty", priority = Priority.P1, dueDate = today + 5, projectId = dom, labelIds = listOf(lZakupy), position = p(), createdAt = now))
        td.insert(Task(title = "Przegląd kwartalny", priority = Priority.P2, dueDate = today + 6, dueTimeMinutes = 15 * 60, projectId = praca, position = p(), createdAt = now))

        // ── Skrzynka (bez projektu i terminu) ───────────────────────────────
        td.insert(Task(title = "Pomysł: aplikacja do biegania", priority = Priority.P4, areaId = aProjekty, position = p(), createdAt = now))
        td.insert(Task(title = "Sprawdzić ofertę nowego internetu", priority = Priority.P3, areaId = aOsobiste, labelIds = listOf(lZakupy), position = p(), createdAt = now))
        td.insert(Task(title = "Obejrzeć kurs o inwestowaniu", priority = Priority.P4, areaId = aOsobiste, position = p(), createdAt = now))

        // ── Ukończone dziś (licznik dzienny) ────────────────────────────────
        listOf(
            "Poranna kawa i przegląd planu" to 7,
            "Sprawdzić skrzynkę mailową" to 8,
            "Krótki spacer" to 9,
            "Przygotować prezentację" to 10
        ).forEach { (t, h) ->
            td.insert(Task(title = t, isCompleted = true, completedAt = millis(LocalDate.now(), h),
                dueDate = today, projectId = praca, position = p(), createdAt = now))
        }
        // ── Ukończone wcześniej w tygodniu (licznik tygodniowy) ─────────────
        for (d in 1..5) {
            td.insert(Task(title = "Zadanie z dnia -$d", isCompleted = true,
                completedAt = millis(LocalDate.now().minusDays(d.toLong()), 14),
                dueDate = today - d, projectId = if (d % 2 == 0) dom else praca, position = p(), createdAt = now))
            td.insert(Task(title = "Drobne zadanie -$d", isCompleted = true,
                completedAt = millis(LocalDate.now().minusDays(d.toLong()), 16),
                dueDate = today - d, projectId = nauka, position = p(), createdAt = now))
        }

        // ── Więcej zaległych (test sekcji „ZALEGŁE") ────────────────────────
        td.insert(Task(title = "Rozliczyć delegację", priority = Priority.P2, dueDate = today - 3, projectId = praca, position = p(), createdAt = now))
        td.insert(Task(title = "Umówić przegląd auta", priority = Priority.P3, dueDate = today - 4, projectId = dom, labelIds = listOf(lTelefon), position = p(), createdAt = now))
        td.insert(Task(title = "Zapłacić za prąd", priority = Priority.P1, dueDate = today - 6, projectId = dom, labelIds = listOf(lPilne), position = p(), createdAt = now))

        // ── Zadanie z linkiem (kafelek linku) + obrazkiem ───────────────────
        td.insert(Task(title = "Przeczytać dokumentację Compose", priority = Priority.P3, dueDate = today + 2, projectId = nauka,
            attachments = listOf("https://developer.android.com/jetpack/compose"), position = p(), createdAt = now))

        // ── Historia ~6 miesięcy ukończonych zadań (streak, wykresy 30 dni,
        //    dni tygodnia, godziny). Kilka luk, żeby streak był realistyczny. ──
        val projCycle = listOf(praca, dom, zdrowie, nauka, kampania)
        val histTitles = listOf(
            "Przegląd maili", "Krótki trening", "Nauka — 20 min", "Sprzątanie", "Zakupy spożywcze",
            "Telefon do klienta", "Spacer", "Czytanie 30 min", "Planowanie dnia", "Code review",
            "Odpowiedzi na wiadomości", "Porządki w plikach", "Rozciąganie", "Notatki ze spotkania"
        )
        val hoursCycle = intArrayOf(8, 9, 11, 13, 15, 17, 19, 21)
        for (d in 1..180) {
            // Luka co 9. dzień, ale tylko starsze niż 18 dni (świeży streak zostaje ciągły).
            if (d > 18 && d % 9 == 0) continue
            val date = LocalDate.now().minusDays(d.toLong())
            val count = 1 + (d + date.dayOfWeek.value) % 3   // 1..3 zadania/dzień
            for (k in 0 until count) {
                val hour = hoursCycle[(d * 2 + k) % hoursCycle.size]
                val proj = projCycle[(d + k) % projCycle.size]
                val title = histTitles[(d * 3 + k) % histTitles.size]
                td.insert(Task(
                    title = title, isCompleted = true,
                    completedAt = millis(date, hour), dueDate = date.toEpochDay(),
                    priority = Priority.entries[(d + k) % 4], projectId = proj,
                    durationMinutes = intArrayOf(15, 20, 30, 45, 60)[(d + k) % 5],
                    position = p(), createdAt = millis(date, 7)
                ))
            }
        }

        // ── Pula aktywności ─────────────────────────────────────────────────
        val acts = listOf(
            Activity(name = "Spacer w parku", effortType = EffortType.PHYSICAL, durationMinutes = 30, place = Place.OUTSIDE, energyCost = EnergyCost.LOW, frequencyTarget = 4, createdAt = now),
            Activity(name = "Rozciąganie", effortType = EffortType.PHYSICAL, durationMinutes = 15, place = Place.HOME, energyCost = EnergyCost.LOW, createdAt = now),
            Activity(name = "Czytanie książki", effortType = EffortType.RELAX, durationMinutes = 45, place = Place.HOME, energyCost = EnergyCost.LOW, frequencyTarget = 3, createdAt = now),
            Activity(name = "Medytacja", effortType = EffortType.RELAX, durationMinutes = 10, place = Place.HOME, energyCost = EnergyCost.LOW, createdAt = now),
            Activity(name = "Nauka hiszpańskiego", effortType = EffortType.MENTAL, durationMinutes = 20, place = Place.HOME, energyCost = EnergyCost.MED, frequencyTarget = 3, createdAt = now),
            Activity(name = "Trening siłowy", effortType = EffortType.PHYSICAL, durationMinutes = 60, place = Place.OUTSIDE, windowStartMin = 17 * 60, windowEndMin = 21 * 60, energyCost = EnergyCost.HIGH, createdAt = now),
            Activity(name = "Podcast o technologii", effortType = EffortType.RELAX, durationMinutes = 25, place = Place.HOME, energyCost = EnergyCost.LOW, createdAt = now),
            Activity(name = "Gra na gitarze", effortType = EffortType.RELAX, durationMinutes = 30, place = Place.HOME, energyCost = EnergyCost.MED, createdAt = now)
        )
        acts.forEach { ad.insert(it) }
    }
}
