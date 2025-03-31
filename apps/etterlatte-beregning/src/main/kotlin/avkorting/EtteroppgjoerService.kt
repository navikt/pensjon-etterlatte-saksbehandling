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
    private val etteroppgjoerRepository: EtteroppgjoerRepository,
) {
    fun beregnOgLagreEtteroppgjoerResultat(request: EtteroppgjoerBeregnetAvkortingRequest): BeregnetEtteroppgjoerResultat {
        val (forbehandlingId, sisteIverksatteBehandling, aar) = request
        val etteroppgjoerResultat = beregnEtteroppgjoerResultat(aar, forbehandlingId, sisteIverksatteBehandling)

        etteroppgjoerRepository.lagreEtteroppgjoerResultat(etteroppgjoerResultat)
        return etteroppgjoerResultat
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
        val (sakId, forbehandlingId, sisteIverksatteBehandling, aar, loennsinntekt, afp, naeringsinntekt, utland) = request

        val sanksjoner = sanksjonService.hentSanksjon(sisteIverksatteBehandling) ?: emptyList()

        val tidligereAarsoppgjoer =
            avkortingRepository.hentAvkorting(sisteIverksatteBehandling)?.let {
                it.aarsoppgjoer.single { aarsoppgjoer -> aarsoppgjoer.aar == aar }
            } ?: throw InternfeilException("Mangler avkorting")

        val avkorting =
            Avkorting(
                // Trenger bare gjeldende år i en forbehandling
                aarsoppgjoer = listOf(tidligereAarsoppgjoer),
            ).beregnEtteroppgjoer(
                brukerTokenInfo = brukerTokenInfo,
                aar = aar,
                loennsinntekt = loennsinntekt,
                afp = afp,
                naeringsinntekt = naeringsinntekt,
                utland = utland,
                sanksjoner = sanksjoner,
            )

        avkortingRepository.lagreAvkorting(forbehandlingId, sakId, avkorting) // TODO lagre med flagg forbehandling?
    }

    private fun beregnEtteroppgjoerResultat(
        aar: Int,
        forbehandlingId: UUID,
        sisteIverksatteBehandlingId: UUID,
    ): BeregnetEtteroppgjoerResultat {
        // For å sikre at rettsgebyret forblir konsekvent i senere kjøringer,
        // setter vi regelperioden basert på etteroppgjørsåret og ikke tidspunktet for kjøringen.
        val kjoeringAar = aar + 1 // Etteroppgjøret kjøres året etter det året vi utfører etteroppgjøret for.
        val regelPeriode = RegelPeriode(LocalDate.of(kjoeringAar, 1, 1), LocalDate.of(kjoeringAar, 12, 31))

        val sisteIverksatteAvkorting = finnAarsoppgjoerForEtteroppgjoer(aar, sisteIverksatteBehandlingId)
        val nyForbehandlingAvkorting = finnAarsoppgjoerForEtteroppgjoer(aar, forbehandlingId)

        val differanseGrunnlag =
            EtteroppgjoerDifferanseGrunnlag(
                FaktumNode(sisteIverksatteAvkorting, sisteIverksatteBehandlingId, ""),
                FaktumNode(nyForbehandlingAvkorting, forbehandlingId, ""),
            )

        return when (
            val beregningResultat =
                beregneEtteroppgjoerRegel.eksekver(KonstantGrunnlag(differanseGrunnlag), regelPeriode)
        ) {
            is RegelkjoeringResultat.Suksess -> {
                val data =
                    beregningResultat.periodiserteResultater
                        .single()
                        .resultat.verdi

                BeregnetEtteroppgjoerResultat(
                    id = UUID.randomUUID(),
                    forbehandlingId = forbehandlingId,
                    sisteIverksatteBehandlingId = sisteIverksatteBehandlingId,
                    utbetaltStoenad = data.differanse.utbetaltStoenad,
                    nyBruttoStoenad = data.differanse.nyBruttoStoenad,
                    differanse = data.differanse.differanse,
                    grense = data.grense,
                    resultatType = data.resultatType,
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
                            avkortingForbehandling = nyForbehandlingAvkorting.id,
                            avkortingSisteIverksatte = sisteIverksatteAvkorting.id,
                        ),
                    aar = aar,
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
        val aarsoppgjoer = avkorting?.aarsoppgjoer?.single { it.aar == aar }

        return when (aarsoppgjoer) {
            is AarsoppgjoerLoepende ->
                AvkortingDto(
                    avkortingGrunnlag = aarsoppgjoer.inntektsavkorting.map { it.grunnlag.toDto() },
                    avkortetYtelse = aarsoppgjoer.avkortetYtelse.map { it.toDto() },
                )
            is Etteroppgjoer -> TODO() // Kan skje hvis et skatteoppgjør endrer seg...
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
    val id: UUID,
    val aar: Int,
    val forbehandlingId: UUID,
    val sisteIverksatteBehandlingId: UUID,
    val utbetaltStoenad: Long,
    val nyBruttoStoenad: Long,
    val differanse: Long,
    val grense: EtteroppgjoerGrense,
    val resultatType: EtteroppgjoerResultatType,
    val tidspunkt: Tidspunkt,
    val regelResultat: JsonNode,
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
