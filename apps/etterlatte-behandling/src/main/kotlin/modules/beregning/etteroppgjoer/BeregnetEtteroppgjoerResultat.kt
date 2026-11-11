package no.nav.etterlatte.modules.beregning.etteroppgjoer

import no.nav.etterlatte.libs.common.beregning.BeregnetEtteroppgjoerResultatDto
import no.nav.etterlatte.libs.common.beregning.EtteroppgjoerResultatType
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import tools.jackson.databind.JsonNode
import java.util.UUID

data class BeregnetEtteroppgjoerResultat(
    val id: UUID,
    val aar: Int,
    val forbehandlingId: UUID,
    val sisteIverksatteBehandlingId: UUID,
    val utbetaltStoenad: Long,
    val nyBruttoStoenad: Long,
    val differanse: Long,
    val grense: EtteroppgjoerGrense,
    val resultatType: EtteroppgjoerResultatType,
    val harIngenInntekt: Boolean,
    val tidspunkt: Tidspunkt,
    val regelResultat: JsonNode,
    val kilde: Grunnlagsopplysning.Kilde,
    val referanseAvkorting: ReferanseEtteroppgjoer,
) {
    fun toDto(): BeregnetEtteroppgjoerResultatDto =
        BeregnetEtteroppgjoerResultatDto(
            id = this.id,
            aar = this.aar,
            forbehandlingId = this.forbehandlingId,
            sisteIverksatteBehandlingId = this.sisteIverksatteBehandlingId,
            utbetaltStoenad = this.utbetaltStoenad,
            nyBruttoStoenad = this.nyBruttoStoenad,
            differanse = this.differanse,
            grense = this.grense.toDto(),
            resultatType = this.resultatType,
            harIngenInntekt = this.harIngenInntekt,
            tidspunkt = this.tidspunkt,
            kilde = this.kilde,
            avkortingForbehandlingId = this.referanseAvkorting.avkortingForbehandling,
            avkortingSisteIverksatteId = this.referanseAvkorting.avkortingSisteIverksatte,
            vedtakReferanse = this.referanseAvkorting.vedtakReferanse,
        )
}

data class ReferanseEtteroppgjoer(
    val avkortingForbehandling: UUID,
    val avkortingSisteIverksatte: UUID?,
    val vedtakReferanse: List<Long>?,
)
