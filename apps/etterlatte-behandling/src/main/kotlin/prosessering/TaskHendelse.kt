package no.nav.etterlatte.prosessering

import java.time.Instant

/**
 * Lokal duplikat av `efterlatte.prosessering.TaskLoggType`/`TaskLogg` fra
 * navikt/efterlatte-prosessering#25 (ikke publisert ennå). Fjernes til fordel for
 * bibliotekets typer når TaskLoggRepository publiseres.
 */
enum class TaskHendelseType {
    STATUS_ENDRET,
    KOMMENTAR,
    AVVIK,
}

data class TaskHendelse(
    val id: Long,
    val taskId: Long,
    val type: TaskHendelseType,
    val melding: String,
    val endretAv: String,
    val node: String,
    val tidspunkt: Instant,
)
