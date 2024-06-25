package no.nav.etterlatte.libs.common.behandling

import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import java.time.LocalDate
import java.util.UUID

data class EtterbetalingDto(
    val behandlingId: UUID?,
    val datoFom: LocalDate?,
    val datoTom: LocalDate?,
    val inneholderKrav: Boolean?,
    val kilde: Grunnlagsopplysning.Kilde?,
)
