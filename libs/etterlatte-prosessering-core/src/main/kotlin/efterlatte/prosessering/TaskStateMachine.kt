package efterlatte.prosessering

import efterlatte.prosessering.Status.AVBRUTT
import efterlatte.prosessering.Status.FULLFØRT
import efterlatte.prosessering.Status.KJØRER
import efterlatte.prosessering.Status.KLAR
import efterlatte.prosessering.Status.STOPPET

/**
 * Én kilde til sannhet for hvilke statusoverganger som er lovlige.
 *
 * I den gamle løsningen lå denne kunnskapen spredt over TaskWorker/TaskService/
 * TaskScheduler. Her er den samlet ett sted, slik at motoren og alle andre kall
 * kan spørre om en overgang er tillatt før den persisteres.
 */
object TaskStateMachine {
    private val lovligeOverganger: Map<Status, Set<Status>> =
        mapOf(
            KLAR to setOf(KJØRER, AVBRUTT),
            KJØRER to setOf(FULLFØRT, KLAR, STOPPET),
            STOPPET to setOf(KLAR, AVBRUTT),
            AVBRUTT to setOf(KLAR),
            FULLFØRT to emptySet(),
        )

    fun erLovlig(
        fra: Status,
        til: Status,
    ): Boolean = til in (lovligeOverganger[fra] ?: emptySet())

    fun krev(
        fra: Status,
        til: Status,
    ) = check(erLovlig(fra, til)) { "Ulovlig statusovergang: $fra -> $til" }
}
