package no.nav.etterlatte.tidshendelser

import no.nav.etterlatte.libs.common.behandling.SakType
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

enum class JobbType(
    val beskrivelse: String,
    val kategori: JobbKategori,
    val sakType: SakType?,
) {
    AO_BP20("Aldersovergang barnepensjon ved 20 år", JobbKategori.ALDERSOVERGANG, SakType.BARNEPENSJON),
    AO_BP21("Aldersovergang barnepensjon ved 21 år", JobbKategori.ALDERSOVERGANG, SakType.BARNEPENSJON),
    AO_OMS67("Aldersovergang omstillingsstønad ved 67 år", JobbKategori.ALDERSOVERGANG, SakType.OMSTILLINGSSTOENAD),
    OMS_DOED_3AAR(
        "Omstillingsstønad opphør 3 år etter dødsdato",
        JobbKategori.OMS_DOEDSDATO,
        SakType.OMSTILLINGSSTOENAD,
    ),
    OMS_DOED_5AAR(
        "Omstillingsstønad opphør 5 år etter dødsdato",
        JobbKategori.OMS_DOEDSDATO,
        SakType.OMSTILLINGSSTOENAD,
    ),
    OMS_DOED_4MND(
        "Omstillingsstønad varselbrev om aktivitetsplikt 4 mnd etter dødsdato",
        JobbKategori.OMS_DOEDSDATO,
        SakType.OMSTILLINGSSTOENAD,
    ),
    OMS_DOED_6MND(
        "Omstillingsstønad vurdering av aktivitetsplikt 6 mnd etter dødsdato",
        JobbKategori.OMS_DOEDSDATO,
        SakType.OMSTILLINGSSTOENAD,
    ),
    REGULERING(
        "Regulering",
        JobbKategori.REGULERING,
        sakType = null,
    ),
}

enum class JobbKategori {
    ALDERSOVERGANG,
    OMS_DOEDSDATO,
    REGULERING,
}

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
