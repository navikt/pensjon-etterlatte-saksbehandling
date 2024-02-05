package no.nav.etterlatte.tidshendelser

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.util.UUID

const val HENDELSE_STATUS_OPPRETTET = "OPPRETTET"

data class HendelserJobb(
    val id: Int,
    val opprettet: LocalDateTime,
    val endret: LocalDateTime,
    val versjon: Int,
    val type: JobType,
    val kjoeredato: LocalDate,
    val behandlingsmaaned: YearMonth,
    val dryrun: Boolean,
    val status: String,
)

data class Hendelse(
    val id: UUID,
    val jobbId: Int,
    val sakId: Int,
    val opprettet: LocalDateTime,
    val endret: LocalDateTime,
    val versjon: Int,
    val status: String,
    val utfall: String?,
    val info: Any?,
)
