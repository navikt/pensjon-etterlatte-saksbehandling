package no.nav.etterlatte.avkorting

import no.nav.etterlatte.libs.common.beregning.AvkortingGrunnlagLagreDto
import no.nav.etterlatte.libs.common.beregning.MottattInntektsjusteringAvkortigRequest
import no.nav.etterlatte.libs.ktor.token.BrukerTokenInfo

class MottattInntektsjusteringService(
    private val avkortingService: AvkortingService,
) {
    suspend fun opprettAvkortingMedBrukeroppgittInntekt(
        request: MottattInntektsjusteringAvkortigRequest,
        brukerTokenInfo: BrukerTokenInfo,
    ): Avkorting {
        val (behandlingId, virkningstidspunkt, inntekt, inntektUtland) = request

        avkortingService.tilstandssjekk(behandlingId, brukerTokenInfo)

        // Avkorting opprettes her med tidligere årsoppgjør
        avkortingService.hentOpprettEllerReberegnAvkorting(behandlingId, brukerTokenInfo)

        val nyttGrunnlag =
            AvkortingGrunnlagLagreDto(
                inntektTom = inntekt,
                fratrekkInnAar = 0, // TODO må tilpasses når vi skal støtte inntektsjustering inneværende år
                inntektUtlandTom = inntektUtland,
                fratrekkInnAarUtland = 0, // TODO må tilpasses når vi skal støtte inntektsjustering inneværende år
                spesifikasjon = "Mottatt inntekt fra bruker gjennom selvbetjening", // TODO avklar med fag
                fom = virkningstidspunkt,
            )
        avkortingService.beregnAvkortingMedNyttGrunnlag(behandlingId, brukerTokenInfo, nyttGrunnlag)

        return avkortingService.hentAvkorting(behandlingId) ?: throw AvkortingFinnesIkkeException(behandlingId)
    }
}
