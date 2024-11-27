package no.nav.etterlatte.avkorting

import no.nav.etterlatte.libs.common.beregning.AvkortingGrunnlagLagreDto
import no.nav.etterlatte.libs.common.beregning.MottattInntektsjusteringAvkortigRequest
import no.nav.etterlatte.libs.common.feilhaandtering.InternfeilException
import no.nav.etterlatte.libs.ktor.token.BrukerTokenInfo
import java.util.UUID

class MottattInntektsjusteringService(
    private val avkortingService: AvkortingService,
) {
    suspend fun opprettAvkortingMedBrukeroppgittInntekt(
        request: MottattInntektsjusteringAvkortigRequest,
        brukerTokenInfo: BrukerTokenInfo,
    ): Avkorting {
        val (behandlingId, virkningstidspunkt, inntekt, inntektUtland) = request

        avkortingService.tilstandssjekk(behandlingId, brukerTokenInfo)

        val eksisterende =
            avkortingService.hentOpprettEllerReberegnAvkorting(behandlingId, brukerTokenInfo)
                ?: throw InternfeilException("Fant ikke og klarte opprette avkorting under inntektsjustering")
        val eksisterendeInntekt = eksisterende.avkortingGrunnlag.find { it.aar == virkningstidspunkt.year }?.fraVirk

        val nyttGrunnlag =
            AvkortingGrunnlagLagreDto(
                id = eksisterendeInntekt?.id ?: UUID.randomUUID(),
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
