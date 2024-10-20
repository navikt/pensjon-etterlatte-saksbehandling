package no.nav.etterlatte.tidshendelser

import no.nav.etterlatte.libs.tidshendelser.JobbType
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.util.UUID

data class HendelserJobb(
    val id: Int,
    val opprettet: LocalDateTime,
    val endret: LocalDateTime,
    val versjon: Int,
    val type: JobbType,
    val kjoeredato: LocalDate,
    val behandlingsmaaned: YearMonth,
    val dryrun: Boolean,
    val status: JobbStatus,
)

data class Hendelse(
    val id: UUID,
    val jobbId: Int,
    val sakId: Int,
    val opprettet: LocalDateTime,
    val endret: LocalDateTime,
    val versjon: Int,
    val status: HendelseStatus,
    val steg: String,
    val loependeYtelse: Boolean?,
    val info: Any?,
)

enum class JobbStatus {
    NY,
    STARTET,
    FERDIG,
    FEILET,
}

enum class HendelseStatus {
    NY,
    SENDT,
    FERDIG,
    FEILET,
}

enum class Steg {
    IDENTIFISERT_SAK,
    VURDERT_LOEPENDE_YTELSE,
}
