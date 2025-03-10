package no.nav.etterlatte.avkorting

import no.nav.etterlatte.beregning.BeregningService
import no.nav.etterlatte.libs.common.beregning.AvkortingGrunnlagLagreDto
import no.nav.etterlatte.libs.common.beregning.EtteroppgjoerBeregnFaktiskInntektRequest
import no.nav.etterlatte.libs.ktor.token.BrukerTokenInfo
import no.nav.etterlatte.sanksjon.SanksjonService

class EtteroppgjoerService(
    private val avkortingRepository: AvkortingRepository,
    private val beregningService: BeregningService,
    private val sanksjonService: SanksjonService,
) {
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

        val beregning = beregningService.hentBeregningNonnull(sisteIverksatteBehandling)
        val sanksjoner = sanksjonService.hentSanksjon(sisteIverksatteBehandling) ?: emptyList()

        // TODO egen regelkjøring...
        val avkorting =
            Avkorting().beregnAvkortingMedNyttGrunnlag(
                inntekt,
                brukerTokenInfo,
                beregning,
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
