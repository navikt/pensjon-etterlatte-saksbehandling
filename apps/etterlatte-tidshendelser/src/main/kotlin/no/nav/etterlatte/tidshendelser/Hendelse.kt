package no.nav.etterlatte.tidshendelser

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.util.UUID

const val HENDELSE_STATUS_OPPRETTET = "NY"
const val HENDELSE_STATUS_SENDT = "SENDT"

data class HendelserJobb(
    val id: Int,
    val opprettet: LocalDateTime,
    val endret: LocalDateTime,
    val versjon: Int,
    val type: JobbType,
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
    val steg: String,
    val info: Any?,
)

enum class JobbType(val beskrivelse: String) {
    AO_BP18("Aldersovergang barnepensjon ved 18 år"),
    AO_BP20("Aldersovergang barnepensjon ved 20 år"),
}

enum class Steg {
    IDENTIFISERT_SAK,
    VURDERT_LOEPENDE_YTELSE,
}
