package no.nav.etterlatte.avkorting

import no.nav.etterlatte.beregning.BeregningService
import no.nav.etterlatte.libs.common.beregning.AvkortingDto
import no.nav.etterlatte.libs.common.beregning.AvkortingGrunnlagLagreDto
import no.nav.etterlatte.libs.common.beregning.EtteroppgjoerBeregnFaktiskInntektRequest
import no.nav.etterlatte.libs.common.beregning.EtteroppgjoerBeregnetAvkorting
import no.nav.etterlatte.libs.common.beregning.EtteroppgjoerBeregnetAvkortingRequest
import no.nav.etterlatte.libs.common.feilhaandtering.InternfeilException
import no.nav.etterlatte.libs.ktor.token.BrukerTokenInfo
import no.nav.etterlatte.sanksjon.SanksjonService

class EtteroppgjoerService(
    private val avkortingRepository: AvkortingRepository,
    private val beregningService: BeregningService,
    private val sanksjonService: SanksjonService,
) {
    fun hentBeregnetAvkorting(request: EtteroppgjoerBeregnetAvkortingRequest): EtteroppgjoerBeregnetAvkorting {
        val (sisteIverksatteBehandling, aar) = request

        val avkorting =
            avkortingRepository.hentAvkorting(sisteIverksatteBehandling)
                ?: throw InternfeilException("Mangler avkorting for siste iverksatte behandling id=$sisteIverksatteBehandling")
        val aarsoppgjoer = avkorting.aarsoppgjoer.single { it.aar == aar }
        val avkortingMedForventaInntekt =
            AvkortingDto(
                avkortingGrunnlag = aarsoppgjoer.inntektsavkorting.map { it.grunnlag.toDto() },
                avkortetYtelse = aarsoppgjoer.avkortetYtelseAar.map { it.toDto() },
            )

        // TODO hent avkorting med faktisk inntekt fra forbehandling (nullable)

        return EtteroppgjoerBeregnetAvkorting(
            avkortingMedForventaInntekt = avkortingMedForventaInntekt,
            avkortingMedFaktiskInntekt = null, // TODO
        )
    }

    fun beregnAvkortingForbehandling(
        request: EtteroppgjoerBeregnFaktiskInntektRequest,
        brukerTokenInfo: BrukerTokenInfo,
    ) {
        val (sakId, forbehandlingId, sisteIverksatteBehandling, fraOgMed, _, loennsinntekt, afp, naeringsinntekt, utland) = request

        // TODO må byttes ut med egen regelkjøring...
        val inntekt =
            AvkortingGrunnlagLagreDto(
                inntektTom = loennsinntekt + naeringsinntekt + afp,
                fratrekkInnAar = 0,
                inntektUtlandTom = utland,
                fratrekkInnAarUtland = 0,
                spesifikasjon = "wip",
                fom = fraOgMed,
            )

        val sanksjoner = sanksjonService.hentSanksjon(sisteIverksatteBehandling) ?: emptyList()

        // TODO egen regelkjøring...
        val avkorting =
            Avkorting().beregnAvkortingMedNyttGrunnlag(
                inntekt,
                brukerTokenInfo,
                null,
                sanksjoner,
                null, // TODO endre håndtering av opphør? Alltid sette tom i desember for et år?
                null,
            )

        avkortingRepository.lagreAvkorting(forbehandlingId, sakId, avkorting) // TODO lagre med flagg forbehandling?
    }

    fun beregnAvkortingRevurdering() {
        // TODO henter faktisk inntekt brukt i forbehandling
        sjekkIngenDiffForbehandlingOgRevurdering()
    }

    fun sjekkIngenDiffForbehandlingOgRevurdering() {
        // TODO
    }
}
