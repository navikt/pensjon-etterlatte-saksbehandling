package no.nav.etterlatte.avkorting.inntektsjustering

import no.nav.etterlatte.avkorting.AarsoppgjoerLoepende
import no.nav.etterlatte.avkorting.Avkorting
import no.nav.etterlatte.avkorting.AvkortingFinnesIkkeException
import no.nav.etterlatte.avkorting.AvkortingRepository
import no.nav.etterlatte.avkorting.AvkortingService
import no.nav.etterlatte.libs.common.beregning.AvkortingGrunnlagLagreDto
import no.nav.etterlatte.libs.common.beregning.InntektsjusteringAvkortingInfoRequest
import no.nav.etterlatte.libs.common.beregning.InntektsjusteringAvkortingInfoResponse
import no.nav.etterlatte.libs.common.feilhaandtering.InternfeilException
import no.nav.etterlatte.libs.ktor.token.BrukerTokenInfo
import no.nav.etterlatte.sanksjon.SanksjonService
import org.slf4j.LoggerFactory
import java.time.YearMonth
import java.util.UUID

class AarligInntektsjusteringService(
    private val avkortingService: AvkortingService,
    private val avkortingRepository: AvkortingRepository,
    private val sanksjonService: SanksjonService,
) {
    private val logger = LoggerFactory.getLogger(this::class.java)

    fun hentSjekkAarligInntektsjustering(request: InntektsjusteringAvkortingInfoRequest): InntektsjusteringAvkortingInfoResponse {
        val sanksjoner = sanksjonService.hentSanksjon(request.sisteBehandling)
        return InntektsjusteringAvkortingInfoResponse(
            sakId = request.sakId,
            aar = request.aar,
            harInntektForAar = avkortingRepository.harSakInntektForAar(request),
            harSanksjon = sanksjoner?.any { it.tom == null } ?: false,
        )
    }

    suspend fun kopierAarligInntektsjustering(
        aar: Int,
        behandlingId: UUID,
        forrigeBehandlingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
    ): Avkorting {
        avkortingService.tilstandssjekk(behandlingId, brukerTokenInfo)
        logger.info("Oppretter avkorting for nytt inntektsår med siste inntekt fra behandling=$forrigeBehandlingId")
        val forrigeAvkorting = avkortingService.hentForrigeAvkorting(forrigeBehandlingId)

        val siseInntekt =
            (forrigeAvkorting.aarsoppgjoer.last() as AarsoppgjoerLoepende)
                .inntektsavkorting
                .last()
                .grunnlag

        with(siseInntekt.periode) {
            if (fom.year != aar - 1 || (tom != null && tom?.year != aar - 1)) {
                throw InternfeilException("Årlig inntektsjustering feilet - inntekt som overføres er i feil år")
            }
        }

        val nyttGrunnlag =
            AvkortingGrunnlagLagreDto(
                inntektTom = siseInntekt.inntektTom,
                fratrekkInnAar = 0,
                inntektUtlandTom = siseInntekt.inntektUtlandTom,
                fratrekkInnAarUtland = 0,
                spesifikasjon = "Automatisk jobb viderefører inntekt fra $aar med id=${siseInntekt.id}",
                fom = YearMonth.of(aar, 1),
            )

        // Avkorting opprettes her med tidligere årsoppgjør
        avkortingService.hentOpprettEllerReberegnAvkorting(behandlingId, brukerTokenInfo)

        avkortingService.beregnAvkortingMedNyeGrunnlag(behandlingId, listOf(nyttGrunnlag), brukerTokenInfo)
        return avkortingRepository.hentAvkorting(behandlingId)
            ?: throw AvkortingFinnesIkkeException(behandlingId)
    }
}
