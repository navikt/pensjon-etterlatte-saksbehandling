package no.nav.etterlatte.avkorting

import no.nav.etterlatte.beregning.BeregningService
import no.nav.etterlatte.libs.common.beregning.AvkortingDto
import no.nav.etterlatte.libs.common.beregning.EtteroppgjoerBeregnFaktiskInntektRequest
import no.nav.etterlatte.libs.common.beregning.EtteroppgjoerBeregnetAvkorting
import no.nav.etterlatte.libs.common.beregning.EtteroppgjoerBeregnetAvkortingRequest
import no.nav.etterlatte.libs.common.feilhaandtering.InternfeilException
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.toJsonNode
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

        val inntekt =
            FaktiskInntekt(
                id = UUID.randomUUID(),
                innvilgaMaaneder = 12,
                loennsinntekt = loennsinntekt,
                naeringsinntekt = naeringsinntekt,
                utlandsinntekt = utland,
                afp = afp,
                kilde = Grunnlagsopplysning.Saksbehandler(brukerTokenInfo.ident(), Tidspunkt.now()),
                // TODO
                inntektInnvilgetPeriode =
                    BeregnetInntektInnvilgetPeriode(
                        verdi = 0,
                        tidspunkt = Tidspunkt.now(),
                        regelResultat = "".toJsonNode(),
                        kilde =
                            Grunnlagsopplysning.RegelKilde(
                                navn = "",
                                ts = Tidspunkt.now(),
                                versjon = "",
                            ),
                    ),
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
                        Etteroppgjoer(
                            id = UUID.randomUUID(),
                            aar = fraOgMed.year,
                            fom = fraOgMed,
                            ytelseFoerAvkorting = tidligereAarsoppgjoer.ytelseFoerAvkorting,
                            inntekt = inntekt,
                        ),
                    ),
                // TODO vil ikke fungere før regler er endret
            ).beregnAvkorting(
                fraOgMed,
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
