package no.nav.etterlatte.libs.common.behandling

import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import java.time.LocalDate
import java.util.UUID

enum class Aldersgruppe {
    OVER_18,
    UNDER_18,
}

data class EtterbetalingDto(
    val behandlingId: UUID,
    val datoFom: LocalDate,
    val datoTom: LocalDate,
    val kilde: Grunnlagsopplysning.Kilde,
)

data class BrevutfallDto(
    val behandlingId: UUID,
    val aldersgruppe: Aldersgruppe?,
    val kilde: Grunnlagsopplysning.Kilde,
)
