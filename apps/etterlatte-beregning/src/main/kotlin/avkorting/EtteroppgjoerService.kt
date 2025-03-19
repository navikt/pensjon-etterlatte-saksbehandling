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
import java.util.UUID

class EtteroppgjoerService(
    private val avkortingRepository: AvkortingRepository,
    private val beregningService: BeregningService,
    private val sanksjonService: SanksjonService,
) {
    fun hentBeregnetAvkorting(request: EtteroppgjoerBeregnetAvkortingRequest): EtteroppgjoerBeregnetAvkorting {
        val (forbehandlingId, sisteIverksatteBehandling, aar) = request

        val avkortingMedForventaInntekt =
            hentAvkorting(sisteIverksatteBehandling, aar)
                ?: throw InternfeilException("Mangler avkorting for siste iverksatte behandling id=$sisteIverksatteBehandling")

        val avkortingFaktiskInntekt = hentAvkorting(forbehandlingId, aar)

        return EtteroppgjoerBeregnetAvkorting(
            avkortingMedForventaInntekt = avkortingMedForventaInntekt,
            avkortingMedFaktiskInntekt = avkortingFaktiskInntekt,
        )
    }

    private fun hentAvkorting(
        behandlingId: UUID,
        aar: Int,
    ): AvkortingDto? {
        val avkorting = avkortingRepository.hentAvkorting(behandlingId)
        val aarsoppgjoer = avkorting?.aarsoppgjoer?.single { it.aar == aar }
        return aarsoppgjoer?.let { aarsoppgjoer ->
            when (aarsoppgjoer) {
                is AarsoppgjoerLoepende ->
                    AvkortingDto(
                        avkortingGrunnlag = aarsoppgjoer.inntektsavkorting.map { it.grunnlag.toDto() },
                        avkortetYtelse = aarsoppgjoer.avkortetYtelse.map { it.toDto() },
                    )

                is Etteroppgjoer -> TODO()
            }
        }
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

        val tidligereAarsoppgjoer =
            avkortingRepository.hentAvkorting(sisteIverksatteBehandling)?.let {
                it.aarsoppgjoer.single { aarsoppgjoer -> aarsoppgjoer.aar == fraOgMed.year }
            } ?: throw InternfeilException("Mangler avkorting")

        val avkorting =
            Avkorting(
                aarsoppgjoer =
                    listOf(
                        AarsoppgjoerLoepende(
                            id = UUID.randomUUID(),
                            aar = fraOgMed.year,
                            fom = fraOgMed,
                            ytelseFoerAvkorting = tidligereAarsoppgjoer.ytelseFoerAvkorting,
                        ),
                    ),
            ).beregnAvkortingMedNyttGrunnlag(
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
