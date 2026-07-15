package efterlatte.prosessering

import java.time.Instant

/**
 * Det et [TaskStep] når mens det kjører: den deserialiserte [payload], task-metadata,
 * og den [transaksjon] stegets DB-skriv og `FULLFØRT`-oppdateringen deler.
 *
 * [opprettNesteTask] legger neste task i kø på *samme* transaksjon, slik at
 * kjeden er atomisk: enten committer både «dette steget fullført» og «neste task
 * opprettet», eller ingen av delene. Ingen luke der steg A ble ferdig, men steg B
 * aldri ble opprettet.
 */
class TaskKontekst<P : Any>(
    val task: Task,
    val payload: P,
    val transaksjon: Transaksjon,
    private val produsent: TaskProdusent,
) {
    fun <Q : Any> opprettNesteTask(
        type: TaskType<Q>,
        payload: Q,
        triggerTid: Instant? = null,
    ): TaskId =
        produsent.opprett(
            transaksjon = transaksjon,
            type = type,
            payload = payload,
            triggerTid = triggerTid,
        )
}
