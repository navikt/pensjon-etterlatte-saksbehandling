package no.nav.etterlatte.avkorting

import no.nav.etterlatte.avkorting.AvkortingRegelkjoring.beregnInntektInnvilgetPeriodeFaktiskInntekt
import no.nav.etterlatte.beregning.BeregningService
import no.nav.etterlatte.libs.common.beregning.AvkortingDto
import no.nav.etterlatte.libs.common.beregning.EtteroppgjoerBeregnFaktiskInntektRequest
import no.nav.etterlatte.libs.common.beregning.EtteroppgjoerBeregnetAvkorting
import no.nav.etterlatte.libs.common.beregning.EtteroppgjoerBeregnetAvkortingRequest
import no.nav.etterlatte.libs.common.feilhaandtering.InternfeilException
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
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

                is Etteroppgjoer -> TODO() // Kan skje hvis et skatteoppgjør endrer seg...
            }
        }
    }

    fun beregnAvkortingForbehandling(
        request: EtteroppgjoerBeregnFaktiskInntektRequest,
        brukerTokenInfo: BrukerTokenInfo,
    ) {
        val (sakId, forbehandlingId, sisteIverksatteBehandling, aar, loennsinntekt, afp, naeringsinntekt, utland) = request

        val sanksjoner = sanksjonService.hentSanksjon(sisteIverksatteBehandling) ?: emptyList()

        val tidligereAarsoppgjoer =
            avkortingRepository.hentAvkorting(sisteIverksatteBehandling)?.let {
                it.aarsoppgjoer.single { aarsoppgjoer -> aarsoppgjoer.aar == aar }
            } ?: throw InternfeilException("Mangler avkorting")

        val kilde = Grunnlagsopplysning.Saksbehandler(brukerTokenInfo.ident(), Tidspunkt.now())
        val inntekt =
            FaktiskInntekt(
                id = UUID.randomUUID(),
                periode = tidligereAarsoppgjoer.periode(),
                innvilgaMaaneder = tidligereAarsoppgjoer.innvilgaMaaneder(),
                loennsinntekt = loennsinntekt,
                naeringsinntekt = naeringsinntekt,
                utlandsinntekt = utland,
                afp = afp,
                kilde = kilde,
                inntektInnvilgetPeriode =
                    beregnInntektInnvilgetPeriodeFaktiskInntekt(
                        loennsinntekt = loennsinntekt,
                        afp = afp,
                        naeringsinntekt = naeringsinntekt,
                        utland = utland,
                        kilde = kilde,
                    ),
            )

        val avkorting =
            Avkorting(
                aarsoppgjoer =
                    listOf(
                        Etteroppgjoer(
                            id = UUID.randomUUID(),
                            aar = tidligereAarsoppgjoer.aar,
                            fom = tidligereAarsoppgjoer.fom,
                            ytelseFoerAvkorting = tidligereAarsoppgjoer.ytelseFoerAvkorting,
                            inntekt = inntekt,
                        ),
                    ),
            ).beregnAvkorting(
                tidligereAarsoppgjoer.fom,
                null,
                sanksjoner,
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
