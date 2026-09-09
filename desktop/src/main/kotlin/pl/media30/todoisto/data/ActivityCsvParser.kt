package pl.media30.todoisto.data

/**
 * Parser CSV listy aktywności (czysty, testowalny jak QuickAddParser).
 * Elastyczny: separator `,` lub `;`, opcjonalny nagłówek, kolumny w kolejności
 * nazwa[, minuty[, wysiłek[, miejsce[, energia]]]]. Puste/niepełne wiersze pomija.
 */
object ActivityCsvParser {

    /** Normalizuje typowy link Google Sheets do postaci eksportu CSV. */
    fun normalizeSheetUrl(raw: String): String {
        val url = raw.trim()
        if (!url.contains("/edit")) return url
        val base = Regex("""(https://docs\.google\.com/spreadsheets/d/[^/]+)""")
            .find(url)?.groupValues?.get(1) ?: return url
        val gid = Regex("""[#&?]gid=(\d+)""").find(url)?.groupValues?.get(1)
        return base + "/export?format=csv" + (gid?.let { "&gid=$it" } ?: "")
    }

    fun parse(csv: String): List<Activity> {
        val lines = csv.split('\n', '\r').map { it.trim() }.filter { it.isNotEmpty() }
        if (lines.isEmpty()) return emptyList()
        val start = if (looksLikeHeader(lines.first())) 1 else 0
        val out = mutableListOf<Activity>()
        for (i in start until lines.size) {
            val cols = splitRow(lines[i])
            val name = cols.getOrNull(0)?.trim().orEmpty()
            if (name.isEmpty()) continue
            out += Activity(
                name = name,
                durationMinutes = cols.getOrNull(1)?.let { parseMinutes(it) } ?: 30,
                effortType = parseEffort(cols.getOrNull(2)),
                place = parsePlace(cols.getOrNull(3)),
                energyCost = parseEnergy(cols.getOrNull(4))
            )
        }
        return out
    }

    private fun looksLikeHeader(row: String): Boolean {
        val l = row.lowercase()
        return l.contains("nazwa") || l.contains("name") || l.contains("aktywno")
    }

    private fun splitRow(row: String): List<String> {
        val sep = if (row.count { it == ';' } > row.count { it == ',' }) ';' else ','
        // minimalna obsługa cudzysłowów
        val result = mutableListOf<String>()
        val sb = StringBuilder()
        var inQuotes = false
        for (c in row) {
            when {
                c == '"' -> inQuotes = !inQuotes
                c == sep && !inQuotes -> { result.add(sb.toString()); sb.clear() }
                else -> sb.append(c)
            }
        }
        result.add(sb.toString())
        return result
    }

    private fun parseMinutes(s: String): Int {
        val n = Regex("""\d+""").find(s)?.value?.toIntOrNull() ?: return 30
        return n.coerceIn(5, 600)
    }

    private fun parseEffort(s: String?): EffortType {
        val l = s?.lowercase()?.trim().orEmpty()
        return when {
            l.startsWith("fiz") || l.contains("phys") || l.contains("💪") -> EffortType.PHYSICAL
            l.startsWith("umys") || l.contains("ment") || l.contains("🧠") -> EffortType.MENTAL
            l.startsWith("regen") || l.contains("relax") || l.contains("odpocz") || l.contains("🌿") -> EffortType.RELAX
            else -> EffortType.RELAX
        }
    }

    private fun parsePlace(s: String?): Place {
        val l = s?.lowercase()?.trim().orEmpty()
        return if (l.contains("zew") || l.contains("out") || l.contains("dwor")) Place.OUTSIDE else Place.HOME
    }

    private fun parseEnergy(s: String?): EnergyCost {
        val l = s?.lowercase()?.trim().orEmpty()
        return when {
            l.startsWith("wys") || l.contains("high") -> EnergyCost.HIGH
            l.startsWith("nis") || l.contains("low") -> EnergyCost.LOW
            else -> EnergyCost.MED
        }
    }
}
