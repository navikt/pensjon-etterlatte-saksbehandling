package no.nav.etterlatte.avkorting

import no.nav.etterlatte.libs.common.beregning.AarligInntektsjusteringAvkortingSjekkRequest
import no.nav.etterlatte.libs.common.beregning.AarligInntektsjusteringAvkortingSjekkResponse
import no.nav.etterlatte.libs.common.beregning.AvkortingGrunnlagLagreDto
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

    fun hentSjekkAarligInntektsjustering(
        request: AarligInntektsjusteringAvkortingSjekkRequest,
    ): AarligInntektsjusteringAvkortingSjekkResponse {
        val sanksjoner = sanksjonService.hentSanksjon(request.sisteBehandling)
        return AarligInntektsjusteringAvkortingSjekkResponse(
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
            forrigeAvkorting.aarsoppgjoer
                .last()
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
                spesifikasjon = siseInntekt.spesifikasjon,
                fom = YearMonth.of(aar, 1),
            )

        // Avkorting opprettes her med tidligere årsoppgjør
        avkortingService.hentOpprettEllerReberegnAvkorting(behandlingId, brukerTokenInfo)

        avkortingService.beregnAvkortingMedNyttGrunnlag(behandlingId, brukerTokenInfo, nyttGrunnlag)
        return avkortingRepository.hentAvkorting(behandlingId)
            ?: throw AvkortingFinnesIkkeException(behandlingId)
    }
}
