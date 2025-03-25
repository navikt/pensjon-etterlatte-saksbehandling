package no.nav.etterlatte.avkorting

import com.fasterxml.jackson.databind.JsonNode
import no.nav.etterlatte.avkorting.regler.EtteroppgjoerDifferanseGrunnlag
import no.nav.etterlatte.avkorting.regler.EtteroppgjoerGrense
import no.nav.etterlatte.avkorting.regler.beregneEtteroppgjoerRegel
import no.nav.etterlatte.beregning.BeregningService
import no.nav.etterlatte.libs.common.beregning.AvkortingDto
import no.nav.etterlatte.libs.common.beregning.EtteroppgjoerBeregnFaktiskInntektRequest
import no.nav.etterlatte.libs.common.beregning.EtteroppgjoerBeregnetAvkorting
import no.nav.etterlatte.libs.common.beregning.EtteroppgjoerBeregnetAvkortingRequest
import no.nav.etterlatte.libs.common.feilhaandtering.GenerellIkkeFunnetException
import no.nav.etterlatte.libs.common.feilhaandtering.InternfeilException
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.toJsonNode
import no.nav.etterlatte.libs.ktor.token.BrukerTokenInfo
import no.nav.etterlatte.libs.regler.FaktumNode
import no.nav.etterlatte.libs.regler.KonstantGrunnlag
import no.nav.etterlatte.libs.regler.RegelPeriode
import no.nav.etterlatte.libs.regler.RegelkjoeringResultat
import no.nav.etterlatte.libs.regler.eksekver
import no.nav.etterlatte.sanksjon.SanksjonService
import java.time.LocalDate
import java.util.UUID

class EtteroppgjoerService(
    private val avkortingRepository: AvkortingRepository,
    private val beregningService: BeregningService,
    private val sanksjonService: SanksjonService,
) {
    fun beregnOgLagreEtteroppgjoerResultat(request: EtteroppgjoerBeregnetAvkortingRequest): BeregnetEtteroppgjoerResultat {
        val resultat = beregnEtteroppgjoerResultat(request.aar, request.forbehandling, request.sisteIverksatteBehandling)

        // TODO: lagre resultat

        return resultat
    }

    fun hentBeregnetAvkorting(request: EtteroppgjoerBeregnetAvkortingRequest): EtteroppgjoerBeregnetAvkorting {
        val (forbehandlingId, sisteIverksatteBehandling, aar) = request

        val avkortingMedForventaInntekt =
            hentAvkortingForBehandling(sisteIverksatteBehandling, aar)
                ?: throw InternfeilException("Mangler avkorting for siste iverksatte behandling id=$sisteIverksatteBehandling")

        val avkortingFaktiskInntekt = hentAvkortingForBehandling(forbehandlingId, aar)

        return EtteroppgjoerBeregnetAvkorting(
            avkortingMedForventaInntekt = avkortingMedForventaInntekt,
            avkortingMedFaktiskInntekt = avkortingFaktiskInntekt,
        )
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

    private fun beregnEtteroppgjoerResultat(
        aar: Int,
        forbehandlingId: UUID,
        sisteIverksatteBehandlingId: UUID,
    ): BeregnetEtteroppgjoerResultat {
        // Etteroppgjøret kjøres året etter det året vi utfører etteroppgjøret for.
        // For å sikre at rettsgebyret forblir konsekvent i senere kjøringer,
        // setter vi regelperioden basert på etteroppgjørsåret og ikke tidspunktet for kjøringen.
        val kjoeringAar = aar + 1
        val regelPeriode = RegelPeriode(LocalDate.of(kjoeringAar, 1, 1), LocalDate.of(kjoeringAar, 12, 31))

        val sisteAvkorting = finnAarsoppgjoerForEtteroppgjoer(aar, sisteIverksatteBehandlingId)
        val nyAvkorting = finnAarsoppgjoerForEtteroppgjoer(aar, forbehandlingId)

        val grunnlag =
            EtteroppgjoerDifferanseGrunnlag(
                FaktumNode(sisteAvkorting, sisteIverksatteBehandlingId, ""),
                FaktumNode(nyAvkorting, forbehandlingId, ""),
            )

        return when (val beregningResultat = beregneEtteroppgjoerRegel.eksekver(KonstantGrunnlag(grunnlag), regelPeriode)) {
            is RegelkjoeringResultat.Suksess -> {
                val data =
                    beregningResultat.periodiserteResultater
                        .single()
                        .resultat.verdi

                BeregnetEtteroppgjoerResultat(
                    utbetaltStoenad = data.differanse.utbetaltStoenad,
                    nyBruttoStoenad = data.differanse.nyBruttoStoenad,
                    differanse = data.differanse.differanse,
                    grense = data.grense,
                    resultat = data.resultatType,
                    tidspunkt = data.tidspunkt,
                    regelResultat = beregningResultat.toJsonNode(),
                    kilde =
                        Grunnlagsopplysning.RegelKilde(
                            navn = beregneEtteroppgjoerRegel.regelReferanse.id,
                            ts = data.tidspunkt,
                            versjon = beregningResultat.reglerVersjon,
                        ),
                    referanseAvkorting =
                        ReferanseEtteroppgjoer(
                            avkortingForbehandling = nyAvkorting.id,
                            avkortingSisteIverksatte = sisteAvkorting.id,
                        ),
                )
            }

            is RegelkjoeringResultat.UgyldigPeriode ->
                throw InternfeilException("Ugyldig regler for periode: ${beregningResultat.ugyldigeReglerForPeriode}")
        }
    }

    private fun hentAvkortingForBehandling(
        behandlingId: UUID,
        aar: Int,
    ): AvkortingDto? {
        val avkorting = avkortingRepository.hentAvkorting(behandlingId)
        val aarsoppgjoer = avkorting?.aarsoppgjoer?.firstOrNull { it.aar == aar }

        return when (aarsoppgjoer) {
            is AarsoppgjoerLoepende ->
                AvkortingDto(
                    avkortingGrunnlag = aarsoppgjoer.inntektsavkorting.map { it.grunnlag.toDto() },
                    avkortetYtelse = aarsoppgjoer.avkortetYtelse.map { it.toDto() },
                )
            is Etteroppgjoer -> TODO()
            else -> null
        }
    }

    private fun finnAarsoppgjoerForEtteroppgjoer(
        aar: Int,
        behandlingId: UUID,
    ): Aarsoppgjoer {
        val avkorting = avkortingRepository.hentAvkorting(behandlingId) ?: throw GenerellIkkeFunnetException()
        return avkorting.aarsoppgjoer.single { it.aar == aar }
    }
}

data class BeregnetEtteroppgjoerResultat(
    val utbetaltStoenad: Long,
    val nyBruttoStoenad: Long,
    val differanse: Long,
    val grense: EtteroppgjoerGrense,
    val resultat: EtteroppgjoerResultatType,
    val tidspunkt: Tidspunkt,
    val regelResultat: JsonNode?,
    val kilde: Grunnlagsopplysning.Kilde,
    val referanseAvkorting: ReferanseEtteroppgjoer,
)

enum class EtteroppgjoerResultatType {
    TILBAKREVING,
    ETTERBETALING,
    IKKE_ETTEROPPGJOER,
}

data class ReferanseEtteroppgjoer(
    val avkortingForbehandling: UUID,
    val avkortingSisteIverksatte: UUID,
)
