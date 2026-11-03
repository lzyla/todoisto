package pl.media30.todoisto.shared

import java.security.SecureRandom

/**
 * Globalnie unikalne identyfikatory rekordów (zadania, projekty, etykiety…).
 *
 * Dlaczego nie autoinkrement / `currentTimeMillis()++`: scalanie migawek łączy
 * rekordy po `id`. Telefon numerował 1, 2, 3…, Mac od bieżącego milisekundowego
 * znacznika — dwa różne rekordy z tym samym `id` po synchronizacji zlewały się
 * w jeden i jeden z nich znikał bez śladu.
 *
 * Zakres [2^41, 2^53):
 *  - powyżej wszystkich dotychczasowych id (autoinkrement i milisekundy ≈ 1,8·10^12 < 2^41),
 *    więc nowe id nie trafią w istniejące;
 *  - poniżej 2^53, więc id przechodzi bez utraty precyzji przez JSON w JavaScripcie
 *    (Edge Function, panel Supabase).
 * Przy 100 tys. rekordów prawdopodobieństwo jakiejkolwiek kolizji ≈ 10^-6.
 */
object Ids {
    private const val MIN = 1L shl 41
    private const val MAX = 1L shl 53
    private val rnd = SecureRandom()

    fun next(): Long = MIN + (rnd.nextLong() ushr 1) % (MAX - MIN)

    /** Czy id pochodzi z nowego generatora (a nie z autoinkrementu / zegara). */
    fun isGlobal(id: Long): Boolean = id in MIN until MAX
}
