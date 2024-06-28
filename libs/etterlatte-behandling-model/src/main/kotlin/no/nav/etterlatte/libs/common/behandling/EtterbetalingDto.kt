package no.nav.etterlatte.libs.common.behandling

import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import java.time.LocalDate
import java.util.UUID

data class EtterbetalingDto(
    val behandlingId: UUID?,
    val datoFom: LocalDate?,
    val datoTom: LocalDate?,
    val inneholderKrav: Boolean?,
    val frivilligSkattetrekk: Boolean?,
    val etterbetalingPeriodeValg: EtterbetalingPeriodeValg?,
    val skatteTrekkFomTomDatoSatt: Boolean?,
    val kilde: Grunnlagsopplysning.Kilde?,
)

enum class EtterbetalingPeriodeValg {
    UNDER_3_MND,
    FRA_3_MND,
}
