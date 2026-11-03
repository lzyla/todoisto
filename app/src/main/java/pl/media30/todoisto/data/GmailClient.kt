package pl.media30.todoisto.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Properties
import javax.mail.Flags
import javax.mail.Folder
import javax.mail.Session
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeMessage
import javax.mail.search.FlagTerm

/**
 * Gmail przez IMAP z HASŁEM DO APLIKACJI (myaccount.google.com/apppasswords) —
 * bez Google Cloud Console i OAuth. Czytamy maile oznaczone GWIAZDKĄ i
 * zamieniamy je w zadania. Dane logowania zostają tylko na urządzeniu.
 */
object GmailClient {

    data class Mail(val subject: String, val from: String, val messageId: String) {
        /** Link otwierający ten wątek w Gmailu (wyszukiwanie po Message-ID). */
        val gmailLink: String
            get() = "https://mail.google.com/mail/#search/rfc822msgid%3A" +
                java.net.URLEncoder.encode(messageId.trim('<', '>'), "UTF-8")
    }

    /** Pobiera do [max] NAJNOWSZYCH maili z gwiazdką ze skrzynki odbiorczej. */
    suspend fun fetchStarred(user: String, appPassword: String, max: Int = 20): List<Mail> =
        withContext(Dispatchers.IO) {
            val props = Properties().apply {
                put("mail.store.protocol", "imaps")
                put("mail.imaps.host", "imap.gmail.com")
                put("mail.imaps.port", "993")
                put("mail.imaps.ssl.enable", "true")
                put("mail.imaps.connectiontimeout", "10000")
                put("mail.imaps.timeout", "20000")
            }
            val store = Session.getInstance(props).getStore("imaps")
            try {
                store.connect("imap.gmail.com", user.trim(), appPassword.replace(" ", ""))
                val inbox = store.getFolder("INBOX")
                inbox.open(Folder.READ_ONLY)
                val starred = inbox.search(FlagTerm(Flags(Flags.Flag.FLAGGED), true))
                starred.takeLast(max).mapNotNull { m ->
                    val mid = (m as? MimeMessage)?.messageID ?: return@mapNotNull null
                    val from = (m.from?.firstOrNull() as? InternetAddress)
                        ?.let { it.personal ?: it.address } ?: "nieznany nadawca"
                    Mail(subject = m.subject ?: "(bez tematu)", from = from, messageId = mid)
                }.also { inbox.close(false) }
            } finally {
                runCatching { store.close() }
            }
        }

    /** Surowy błąd → czytelna podpowiedź po polsku. */
    fun friendlyError(e: Throwable): String {
        val m = e.message.orEmpty().lowercase()
        return when {
            "auth" in m || "credentials" in m || "password" in m ->
                "Logowanie odrzucone. Użyj HASŁA DO APLIKACJI (nie zwykłego hasła): włącz weryfikację dwuetapową, potem myaccount.google.com/apppasswords."
            "unknown host" in m || "network" in m || "timed out" in m || "timeout" in m ->
                "Brak połączenia z Gmailem — sprawdź internet i spróbuj ponownie."
            else -> e.message ?: "Nie udało się połączyć z Gmailem."
        }
    }
}
