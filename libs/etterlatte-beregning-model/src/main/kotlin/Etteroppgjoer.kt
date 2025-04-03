package no.nav.etterlatte.libs.common.beregning

import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import java.time.LocalDate
import java.util.UUID

data class BeregnetEtteroppgjoerResultatDto(
    val id: UUID,
    val aar: Int,
    val forbehandlingId: UUID,
    val sisteIverksatteBehandlingId: UUID,
    val utbetaltStoenad: Long,
    val nyBruttoStoenad: Long,
    val differanse: Long,
    val grense: EtteroppgjoerGrenseDto,
    val resultatType: EtteroppgjoerResultatType,
    val tidspunkt: Tidspunkt,
    val kilde: Grunnlagsopplysning.Kilde,
    val avkortingForbehandlingId: UUID,
    val avkortingSisteIverksatteId: UUID,
)

data class EtteroppgjoerGrenseDto(
    val tilbakekreving: Double,
    val etterbetaling: Double,
    val rettsgebyr: Double,
    val rettsgebyrGyldigFra: LocalDate,
)

enum class EtteroppgjoerResultatType {
    TILBAKEKREVING,
    ETTERBETALING,
    IKKE_ETTEROPPGJOER,
}
