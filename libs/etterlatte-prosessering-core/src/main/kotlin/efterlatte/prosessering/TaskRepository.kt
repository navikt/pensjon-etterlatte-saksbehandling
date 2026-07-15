package efterlatte.prosessering

import java.time.Instant

/**
 * Port mot persistenslaget. Core definerer kontrakten; en adapter (f.eks.
 * postgres-modulen) implementerer den. Core kjenner ingen `java.sql`.
 */
interface TaskRepository {
    /**
     * Plukker atomisk inntil [limit] tasker som er [Status.KLAR] og forfalt,
     * og setter dem til [Status.KJØRER]. Plukket committer med én gang (det er
     * det som får andre pods til å hoppe over dem). Egen transaksjon — skilt fra
     * kjøringen.
     */
    fun claimBatch(limit: Int): List<Task>

    /**
     * Åpner en fersk transaksjon, kjører [block] med den, og committer — eller
     * ruller tilbake hvis [block] kaster. Motoren pakker hver stegkjøring i dette
     * så stegets DB-skriv og [markerFullført] committer atomisk (transaksjon-per-steg).
     */
    fun <T> iEgenTransaksjon(block: (Transaksjon) -> T): T

    /** Setter tasken til [Status.FULLFØRT] på kallerens [transaksjon]. */
    fun markerFullført(
        transaksjon: Transaksjon,
        id: Long,
    )

    /**
     * Bokfører et feilet forsøk i sin *egen* transaksjon (stegets tx er allerede
     * rullet tilbake). Setter [nyStatus] ([Status.KLAR] for retry eller
     * [Status.STOPPET]), øker `antall_feil` og setter neste `trigger_tid`.
     */
    fun markFeilet(
        id: Long,
        nyStatus: Status,
        stoppaarsak: Stoppaarsak?,
        nesteTriggerTid: Instant,
    )

    /**
     * Tar tilbake tasker som står i [Status.KJØRER] med [Task.plukketTid] eldre
     * enn [plukketFoer], og setter dem tilbake til [Status.KLAR]. Dekker poden
     * som døde midt i et steg. Returnerer antall gjenopprettede tasker.
     */
    fun gjenopprettHengende(plukketFoer: Instant): Int

    fun finn(id: Long): Task?

    fun antallMedStatus(status: Status): Int

    /**
     * Skriver en ny task på kallerens [transaksjon] (outbox). Brukes av
     * [TaskProdusent.opprett]. Committer/lukker aldri transaksjonen.
     */
    fun insert(
        transaksjon: Transaksjon,
        type: String,
        payload: String?,
        triggerTid: Instant,
    ): Long

    /**
     * Skriver en ny task i sin egen transaksjon. Brukes av
     * [TaskProdusent.opprettFrittstående], der det ikke finnes noe forretnings-skriv.
     */
    fun insertFrittstaaende(
        type: String,
        payload: String?,
        triggerTid: Instant,
    ): Long
}
