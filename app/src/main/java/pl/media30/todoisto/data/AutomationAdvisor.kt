package pl.media30.todoisto.data

/**
 * Doradca automatyzacji — dla konkretnego zadania podpowiada rzeczowo, jak zrobić
 * je szybciej/efektywniej i którym narzędziem, w formie krótkiego samouczka.
 * Czysta logika (bez sieci/LLM), oparta o zestaw narzędzi użytkownika. Dodatkowo
 * buduje gotowy prompt, który można wysłać do Claude/GPT po odpowiedź „na żywo".
 */
data class AutomationTip(
    val canAutomate: Boolean,
    val headline: String,
    val tools: List<String>,
    val steps: List<String>,
    val aiPrompt: String
)

object AutomationAdvisor {

    private const val STACK =
        "Claude, ChatGPT, Codex, Gemini, NotebookLM, Canva, Webflow, Vercel, DeepL, " +
        "Google Workspace (Gmail, Docs), Notion, VS Code, GitHub, Acrobat PDF, Zoom"

    private class Rule(val keys: List<String>, val build: (String) -> AutomationTip)

    private fun tip(head: String, tools: List<String>, steps: List<String>, title: String) =
        AutomationTip(true, head, tools, steps, prompt(title))

    private fun prompt(title: String) =
        "Zadanie: \"$title\".\n" +
        "Mam dostęp do: $STACK.\n" +
        "Wyjaśnij krótko i konkretnie, krok po kroku, jak wykonać to zadanie najszybciej i " +
        "najefektywniej — które narzędzie wybrać, jak je zautomatyzować i gdzie AI może zrobić " +
        "to za mnie. Na końcu podaj gotowy prompt do użycia."

    private val rules = listOf(
        Rule(listOf("tłumacz", "przetłumacz", "translation", "translate", "po angielsku", "na angielski", "na polski")) {
            tip("Przetłumacz błyskawicznie", listOf("DeepL", "Claude"), listOf(
                "Wklej tekst do DeepL (albo DeepL Write, by dopracować styl).",
                "Branżowe terminy i kontekst dopytaj w Claude.",
                "Sprawdź spójność nazw własnych i skrótów."
            ), it)
        },
        Rule(listOf("spotkanie", "zoom", "call", "rozmow", "meeting", "narada")) {
            tip("Spotkanie bez ręcznego notowania", listOf("Zoom", "Google Calendar", "NotebookLM"), listOf(
                "Zaplanuj w Google Calendar z linkiem Zoom (Workspace).",
                "Włącz nagrywanie/transkrypcję w Zoomie.",
                "Wrzuć transkrypt do NotebookLM → streszczenie i lista działań (action items).",
                "Zadania z action items dodaj z powrotem do Todoisto."
            ), it)
        },
        Rule(listOf("kod", "bug", "błąd", "refactor", "test", "skrypt", "api", "integracj", "deploy", "wdroż", "aplikacj")) {
            tip("Zautomatyzuj część programistyczną", listOf("VS Code", "Codex", "GitHub", "Vercel"), listOf(
                "Opisz zadanie w VS Code (Codex/Copilot) — wygeneruj szkic i testy.",
                "Commit i PR na GitHub; poproś AI o review zmian.",
                "Wdrożenie i podgląd PR przez Vercel."
            ), it)
        },
        Rule(listOf("strona", "landing", "www", "witryn", "web", "serwis")) {
            tip("Zbuduj i wdroż stronę", listOf("Claude", "Webflow", "Vercel"), listOf(
                "Teksty i strukturę sekcji wygeneruj w Claude.",
                "Złóż stronę w Webflow (szablon + CMS), użyj Brand Kit organizacji.",
                "Podłącz domenę / wdroż i sprawdź podgląd (Vercel)."
            ), it)
        },
        Rule(listOf("grafik", "plakat", "post", "baner", "social", "prezentacj", "slajd", "wizual", "kreacj", "okładk")) {
            tip("Zaprojektuj w Canvie", listOf("Canva", "Claude"), listOf(
                "W Canvie wybierz szablon i włącz Brand Kit organizacji (NGO).",
                "Treści/hasła wygeneruj w Claude i wklej do szablonu.",
                "Magic Resize — z jednego projektu zrób wszystkie formaty.",
                "Eksport PNG/PDF."
            ), it)
        },
        Rule(listOf("faktur", "pdf", "dokument", "podpis", "formularz", "umow", "wniosek")) {
            tip("Dokumenty i PDF", listOf("Google Workspace", "Acrobat PDF"), listOf(
                "Szablon w Google Docs → eksport do PDF.",
                "W Acrobacie: pola formularza, scalanie plików, podpis elektroniczny.",
                "Powtarzalne dokumenty trzymaj jako szablon Docs."
            ), it)
        },
        Rule(listOf("mail", "e-mail", "email", "odpis", "newsletter", "wiadomo", "follow", "zaproszeni")) {
            tip("Szybka wiadomość / newsletter", listOf("Claude", "Gmail (Workspace)", "DeepL"), listOf(
                "Wklej wątek do Claude i poproś: odpisz krótko, uprzejmie, po polsku, w 3 zdaniach.",
                "Obcy język — przepuść przez DeepL.",
                "Wklej do Gmaila i zapisz jako Szablon (Ustawienia → Szablony)."
            ), it)
        },
        Rule(listOf("kampani", "marketing", "promocj", "ogłosze", "reklam")) {
            tip("Zaplanuj kampanię end-to-end", listOf("Claude", "Canva", "Webflow", "Gmail"), listOf(
                "Strategia i kalendarz treści w Claude.",
                "Kreacje w Canvie (Brand Kit).",
                "Landing w Webflow, wysyłka przez Gmail/Workspace.",
                "Wyniki podsumuj w NotebookLM."
            ), it)
        },
        Rule(listOf("nauk", "fiszk", "słówk", "kurs", "ucz", "materiał", "notebooklm")) {
            tip("Ucz się szybciej", listOf("NotebookLM", "Notion", "Claude"), listOf(
                "Wrzuć materiały do NotebookLM → streszczenie, pytania, quiz.",
                "Plan i fiszki w Notion; ustaw powtórki.",
                "Trudne tematy rozłóż z Claude na proste kroki."
            ), it)
        },
        Rule(listOf("raport", "podsumowanie", "analiz", "notatk", "streść", "streszcz", "research", "artykuł", "opis", "tekst", "oferta", "scenariusz", "plan", "brief")) {
            tip("Napisz to z AI", listOf("Claude", "ChatGPT", "Google Docs"), listOf(
                "Wrzuć materiały źródłowe do Claude i opisz cel jednym zdaniem.",
                "Poproś o szkic w konkretnej strukturze (nagłówki, długość, ton).",
                "Iteruj sekcjami: skróć wstęp, dodaj wnioski, zmień ton na bardziej formalny.",
                "Wklej do Google Docs i dopracuj formatowanie."
            ), it)
        }
    )

    fun advise(title: String, notes: String = ""): AutomationTip {
        val hay = (title + " " + notes).lowercase()
        rules.firstOrNull { r -> r.keys.any { hay.contains(it) } }?.let { return it.build(title) }
        return AutomationTip(
            canAutomate = false,
            headline = "Zapytaj AI, jak to przyspieszyć",
            tools = listOf("Claude", "ChatGPT"),
            steps = listOf("To zadanie nie ma gotowego przepisu, ale AI pomoże rozłożyć je na kroki i wskazać skróty."),
            aiPrompt = prompt(title)
        )
    }
}
