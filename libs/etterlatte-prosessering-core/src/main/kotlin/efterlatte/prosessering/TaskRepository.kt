package efterlatte.prosessering

import java.time.Instant

/**
 * Port mot persistenslaget. Core definerer kontrakten; en adapter (f.eks.
 * postgres-modulen) implementerer den. Core kjenner ingen `java.sql`.
 */
interface TaskRepository {
    /**
     * Plukker atomisk inntil [limit] tasker som er [Status.KLAR] og forfalt,
     * og setter dem til [Status.KJØRER]. Skal være trygg mot dobbel-plukk på
     * tvers av parallelle kall / pods.
     */
    fun claimBatch(limit: Int): List<Task>

    fun markFullført(id: Long)

    fun markFeilet(
        id: Long,
        nyStatus: Status,
        stoppaarsak: Stoppaarsak?,
        nesteTriggerTid: Instant,
    )

    fun finn(id: Long): Task?

    fun antallMedStatus(status: Status): Int

    fun insert(
        type: String,
        payload: String? = null,
        triggerTid: Instant = Instant.now(),
        status: Status = Status.KLAR,
    ): Long
}
