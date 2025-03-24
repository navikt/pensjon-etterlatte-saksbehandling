package no.nav.etterlatte.avkorting

import no.nav.etterlatte.beregning.BeregningService
import no.nav.etterlatte.beregning.regler.omstillingstoenad.OMS_GYLDIG_FRA
import no.nav.etterlatte.libs.common.beregning.AvkortingDto
import no.nav.etterlatte.libs.common.beregning.EtteroppgjoerBeregnFaktiskInntektRequest
import no.nav.etterlatte.libs.common.beregning.EtteroppgjoerBeregnetAvkorting
import no.nav.etterlatte.libs.common.beregning.EtteroppgjoerBeregnetAvkortingRequest
import no.nav.etterlatte.libs.common.feilhaandtering.GenerellIkkeFunnetException
import no.nav.etterlatte.libs.common.feilhaandtering.InternfeilException
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.ktor.token.BrukerTokenInfo
import no.nav.etterlatte.libs.regler.FaktumNode
import no.nav.etterlatte.libs.regler.KonstantRegel
import no.nav.etterlatte.libs.regler.Regel
import no.nav.etterlatte.libs.regler.RegelMeta
import no.nav.etterlatte.libs.regler.RegelReferanse
import no.nav.etterlatte.libs.regler.benytter
import no.nav.etterlatte.libs.regler.finnFaktumIGrunnlag
import no.nav.etterlatte.libs.regler.med
import no.nav.etterlatte.libs.regler.og
import no.nav.etterlatte.sanksjon.SanksjonService
import java.time.temporal.ChronoUnit
import java.util.UUID

data class EtteroppgjoerDifferanseGrunnlag(
    val utbetaltStoenad: FaktumNode<Aarsoppgjoer>,
    val nyBruttoStoenad: FaktumNode<Aarsoppgjoer>,
)

val nyBruttoStoenad: Regel<EtteroppgjoerDifferanseGrunnlag, List<AvkortetYtelse>> =
    finnFaktumIGrunnlag(
        gjelderFra = OMS_GYLDIG_FRA,
        beskrivelse = "",
        finnFaktum = EtteroppgjoerDifferanseGrunnlag::nyBruttoStoenad,
        finnFelt = { it.avkortetYtelse },
    )

val utbetaltStoenad: Regel<EtteroppgjoerDifferanseGrunnlag, List<AvkortetYtelse>> =
    finnFaktumIGrunnlag(
        gjelderFra = OMS_GYLDIG_FRA,
        beskrivelse = "",
        finnFaktum = EtteroppgjoerDifferanseGrunnlag::utbetaltStoenad,
        finnFelt = { it.avkortetYtelse },
    )

val sumUtbetaltStoenad =
    RegelMeta(
        gjelderFra = OMS_GYLDIG_FRA,
        beskrivelse = "",
        regelReferanse = RegelReferanse(id = "", versjon = ""),
    ) benytter utbetaltStoenad med { avkortetYtelse ->
        // TODO egen regel?
        avkortetYtelse.sumOf {
            (it.periode.fom.until(it.periode.tom, ChronoUnit.MONTHS) + 1) * it.ytelseEtterAvkorting
        }
    }

val sumNyBruttoStoenad =
    RegelMeta(
        gjelderFra = OMS_GYLDIG_FRA,
        beskrivelse = "",
        regelReferanse = RegelReferanse(id = "", versjon = ""),
    ) benytter nyBruttoStoenad med { avkortetYtelse ->
        // TODO egen regel?
        avkortetYtelse.sumOf {
            (it.periode.fom.until(it.periode.tom, ChronoUnit.MONTHS) + 1) * it.ytelseEtterAvkorting
        }
    }

val differanse =
    RegelMeta(
        gjelderFra = OMS_GYLDIG_FRA,
        beskrivelse = "",
        regelReferanse = RegelReferanse(id = "", versjon = ""),
    ) benytter sumUtbetaltStoenad og sumNyBruttoStoenad med { sumUtbetalt, sumNyBrutto ->
        sumUtbetalt - sumNyBrutto
    }

val grenser =
    KonstantRegel<EtteroppgjoerDifferanseGrunnlag, EtteroppgjoerGrenser>(
        gjelderFra = OMS_GYLDIG_FRA,
        beskrivelse = "",
        regelReferanse = RegelReferanse(id = "", versjon = ""),
        verdi = EtteroppgjoerGrenser(1000, 1000),
    )

data class EtteroppgjoerGrenser(
    val tilbakekreving: Int,
    val etterbetaling: Int,
)

val etteroppgjoerRegel =
    RegelMeta(
        gjelderFra = OMS_GYLDIG_FRA,
        beskrivelse = "",
        regelReferanse = RegelReferanse(id = "", versjon = ""),
    ) benytter differanse og grenser med { differanse, grenser ->

        when {
            differanse > grenser.tilbakekreving -> EtteroppgjoerResultatType.TILBAKREVING
            differanse * -1 > grenser.etterbetaling -> EtteroppgjoerResultatType.ETTERBETALING
            else -> EtteroppgjoerResultatType.IKKE_ETTEROPPGJOER
        }
    }

class EtteroppgjoerService(
    private val avkortingRepository: AvkortingRepository,
    private val beregningService: BeregningService,
    private val sanksjonService: SanksjonService,
) {
    fun beregnEtteroppgjoerResultat(request: EtteroppgjoerBeregnetAvkortingRequest): EtteroppgjoerResultat {
        val forbehandlingAvkorting = finnAarsoppgjoerForEtteroppgjoer(request.aar, request.forbehandling)
        val behandlingAvkorting = finnAarsoppgjoerForEtteroppgjoer(request.aar, request.sisteIverksatteBehandling)

        // TODO REGEL START ------------
        val nyBruttoStoenad =
            forbehandlingAvkorting.avkortetYtelse.sumOf {
                it.periode.fom.until(it.periode.tom, ChronoUnit.MONTHS) * it.ytelseEtterAvkorting
            }

        val utbetaltStoenad =
            behandlingAvkorting.avkortetYtelse.sumOf {
                it.periode.fom.until(it.periode.tom, ChronoUnit.MONTHS) * it.ytelseEtterAvkorting
            }

        // TODO fra et brukerperspektiv eller fra nav perspektiv
        val diff = utbetaltStoenad - nyBruttoStoenad

        val grenseTilbakreving = 2000
        val grenseEtterbetaling = 2000

        // TODO resultat av at regel kjører
        val resultatBeregning =
            when {
                diff > grenseTilbakreving -> EtteroppgjoerResultatType.TILBAKREVING
                diff * -1 > grenseEtterbetaling -> EtteroppgjoerResultatType.ETTERBETALING
                else -> EtteroppgjoerResultatType.IKKE_ETTEROPPGJOER
            }
        // TODO REGEL SLUTT ------------

        // TODO lagre resultatet fra regel

        return EtteroppgjoerResultat(
            123,
            123,
            123,
            123,
            resultatBeregning,
            listOf(request.forbehandling, request.sisteIverksatteBehandling),
        )
    }

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

    private fun finnAarsoppgjoerForEtteroppgjoer(
        aar: Int,
        id: UUID,
    ): Aarsoppgjoer {
        val avkorting = avkortingRepository.hentAvkorting(id) ?: throw GenerellIkkeFunnetException()
        return avkorting.aarsoppgjoer.single { it.aar == aar }
    }
}

data class EtteroppgjoerResultat(
    val utbetaltStoenad: Int, // siste iverksatte behandling
    val nyBruttoStoenad: Int, // forbehandling
    val differanse: Int,
    val grense: Int, // rettsgebyr
    val resultat: EtteroppgjoerResultatType,
    val ref: List<UUID>,
)

enum class EtteroppgjoerResultatType {
    TILBAKREVING,
    ETTERBETALING,
    IKKE_ETTEROPPGJOER,
}
