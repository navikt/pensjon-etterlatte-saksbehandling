package no.nav.etterlatte.avkorting.etteroppgjoer

import com.fasterxml.jackson.databind.JsonNode
import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.avkorting.Aarsoppgjoer
import no.nav.etterlatte.avkorting.AarsoppgjoerLoepende
import no.nav.etterlatte.avkorting.Avkorting
import no.nav.etterlatte.avkorting.AvkortingRepository
import no.nav.etterlatte.avkorting.AvkortingService
import no.nav.etterlatte.avkorting.Etteroppgjoer
import no.nav.etterlatte.avkorting.regler.EtteroppgjoerDifferanseGrunnlag
import no.nav.etterlatte.avkorting.regler.EtteroppgjoerGrense
import no.nav.etterlatte.avkorting.regler.beregneEtteroppgjoerRegel
import no.nav.etterlatte.avkorting.toDto
import no.nav.etterlatte.libs.common.beregning.AvkortingDto
import no.nav.etterlatte.libs.common.beregning.BeregnetEtteroppgjoerResultatDto
import no.nav.etterlatte.libs.common.beregning.EtteroppgjoerBeregnFaktiskInntektRequest
import no.nav.etterlatte.libs.common.beregning.EtteroppgjoerBeregnetAvkorting
import no.nav.etterlatte.libs.common.beregning.EtteroppgjoerResultatType
import no.nav.etterlatte.libs.common.feilhaandtering.GenerellIkkeFunnetException
import no.nav.etterlatte.libs.common.feilhaandtering.InternfeilException
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.sak.SakId
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
    private val sanksjonService: SanksjonService,
    private val etteroppgjoerRepository: EtteroppgjoerRepository,
    private val avkortingService: AvkortingService,
) {
    fun beregnOgLagreEtteroppgjoerResultat(
        forbehandlingId: UUID,
        sisteIverksatteBehandlingId: UUID,
        aar: Int,
    ): BeregnetEtteroppgjoerResultat {
        val etteroppgjoerResultat = beregnEtteroppgjoerResultat(aar, forbehandlingId, sisteIverksatteBehandlingId)

        etteroppgjoerRepository.lagreEtteroppgjoerResultat(etteroppgjoerResultat)
        return etteroppgjoerResultat
    }

    fun hentBeregnetEtteroppgjoerResultat(
        forbehandlingId: UUID,
        sisteIverksatteBehandlingId: UUID,
        aar: Int,
    ): BeregnetEtteroppgjoerResultat? = etteroppgjoerRepository.hentEtteroppgjoerResultat(aar, forbehandlingId, sisteIverksatteBehandlingId)

    fun hentBeregnetAvkorting(
        forbehandlingId: UUID,
        sisteIverksatteBehandlingId: UUID,
        sakId: SakId,
        aar: Int,
        brukerTokenInfo: BrukerTokenInfo,
    ): EtteroppgjoerBeregnetAvkorting {
        val avkortingMedForventaInntekt =
            runBlocking {
                avkortingService.hentAvkortingMedReparertAarsoppgjoer(
                    sakId = sakId,
                    behandlingId = sisteIverksatteBehandlingId,
                    brukerTokenInfo = brukerTokenInfo,
                )
            }.toDto()

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
        val sanksjoner = sanksjonService.hentSanksjon(request.sisteIverksatteBehandling) ?: emptyList()

        val tidligereAarsoppgjoer =
            avkortingRepository.hentAvkorting(request.sisteIverksatteBehandling)?.let {
                it.aarsoppgjoer.single { aarsoppgjoer -> aarsoppgjoer.aar == request.aar }
            } ?: throw InternfeilException("Mangler avkorting")

        val avkorting =
            with(request) {
                Avkorting(
                    // Trenger bare gjeldende år i en forbehandling
                    aarsoppgjoer = listOf(tidligereAarsoppgjoer),
                ).beregnEtteroppgjoer(
                    brukerTokenInfo = brukerTokenInfo,
                    aar = aar,
                    loennsinntekt = loennsinntekt,
                    afp = afp,
                    naeringsinntekt = naeringsinntekt,
                    utland = utlandsinntekt,
                    sanksjoner = sanksjoner,
                    spesifikasjon = spesifikasjon,
                )
            }

        avkortingRepository.lagreAvkorting(request.forbehandlingId, request.sakId, avkorting) // TODO lagre med flagg forbehandling?
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

            is Etteroppgjoer ->
                AvkortingDto(
                    avkortingGrunnlag = listOf(aarsoppgjoer.inntekt.toDto()),
                    avkortetYtelse = aarsoppgjoer.avkortetYtelse.map { it.toDto() },
                )

            else -> null
        }
    }

    private fun finnAarsoppgjoerForEtteroppgjoer(
        aar: Int,
        behandlingId: UUID,
    ): Aarsoppgjoer {
        val avkorting = avkortingRepository.hentAvkorting(behandlingId) ?: throw GenerellIkkeFunnetException()

        val aarsoppgjoer = avkorting.aarsoppgjoer.filter { it.aar == aar }
        return when (aarsoppgjoer.size) {
            1 -> aarsoppgjoer.single()
            0 -> throw InternfeilException("Fant ikke aarsoppgjoer for $aar")
            else -> throw InternfeilException("Fant ${aarsoppgjoer.size} for $aar som ikke håndteres riktig")
        }
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
) {
    fun toDto(): BeregnetEtteroppgjoerResultatDto =
        BeregnetEtteroppgjoerResultatDto(
            id = this.id,
            aar = this.aar,
            forbehandlingId = this.forbehandlingId,
            sisteIverksatteBehandlingId = this.sisteIverksatteBehandlingId,
            utbetaltStoenad = this.utbetaltStoenad,
            nyBruttoStoenad = this.nyBruttoStoenad,
            differanse = this.differanse,
            grense = this.grense.toDto(),
            resultatType = this.resultatType,
            tidspunkt = this.tidspunkt,
            kilde = this.kilde,
            avkortingForbehandlingId = this.referanseAvkorting.avkortingForbehandling,
            avkortingSisteIverksatteId = this.referanseAvkorting.avkortingSisteIverksatte,
        )
}

data class ReferanseEtteroppgjoer(
    val avkortingForbehandling: UUID,
    val avkortingSisteIverksatte: UUID,
)
