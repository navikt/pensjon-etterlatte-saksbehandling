package efterlatte.prosessering

import java.time.Instant

/**
 * Oppretter tasker. Bevisst **blokkerende, ikke `suspend`**: en Spring-transaksjon
 * er trådbundet, og en suspending opprettelse kunne fortsette på en annen tråd og
 * stille koble seg fra transaksjonen — nettopp den dual-write-feilen biblioteket
 * finnes for å eliminere. Inserten er et sub-millisekund-skriv på en connection
 * kalleren allerede holder.
 *
 * - [opprett] skriver på kallerens [Transaksjon] (outbox-garantien).
 * - [opprettFrittstående] åpner sin egen lille transaksjon; det tydelige navnet
 *   signaliserer at outbox bevisst *ikke* er i spill (ingen forretnings-skriv å
 *   henge tasken på).
 */
interface TaskProdusent {
    fun <P : Any> opprett(
        transaksjon: Transaksjon,
        type: TaskType<P>,
        payload: P,
        triggerTid: Instant? = null,
    ): TaskId

    fun <P : Any> opprettFrittstående(
        type: TaskType<P>,
        payload: P,
        triggerTid: Instant? = null,
    ): TaskId
}

/**
 * Standard-produsenten: serialiserer payload via [TaskType] og delegerer skrivet
 * til [TaskRepository]. Ingen skjult transaksjon åpnes noen gang på vegne av
 * [opprett] — bare [opprettFrittstående] gjør det, eksplisitt.
 */
class StandardTaskProdusent(
    private val repo: TaskRepository,
) : TaskProdusent {
    override fun <P : Any> opprett(
        transaksjon: Transaksjon,
        type: TaskType<P>,
        payload: P,
        triggerTid: Instant?,
    ): TaskId =
        TaskId(
            repo.insert(
                transaksjon = transaksjon,
                type = type.navn,
                payload = type.serialiser(payload),
                triggerTid = triggerTid ?: Instant.now(),
            ),
        )

    override fun <P : Any> opprettFrittstående(
        type: TaskType<P>,
        payload: P,
        triggerTid: Instant?,
    ): TaskId =
        TaskId(
            repo.insertFrittstaaende(
                type = type.navn,
                payload = type.serialiser(payload),
                triggerTid = triggerTid ?: Instant.now(),
            ),
        )
}
