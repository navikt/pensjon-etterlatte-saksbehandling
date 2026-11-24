package no.nav.etterlatte.modules.beregning

import no.nav.etterlatte.behandling.klienter.BeregningKlient
import no.nav.etterlatte.beregning.grunnlag.BeregningsGrunnlag
import no.nav.etterlatte.libs.common.beregning.AvkortingDto
import no.nav.etterlatte.libs.common.beregning.BeregnetEtteroppgjoerResultatDto
import no.nav.etterlatte.libs.common.beregning.BeregningDTO
import no.nav.etterlatte.libs.common.beregning.BeregningOgAvkortingDto
import no.nav.etterlatte.libs.common.beregning.EtteroppgjoerBeregnFaktiskInntektRequest
import no.nav.etterlatte.libs.common.beregning.EtteroppgjoerBeregnetAvkorting
import no.nav.etterlatte.libs.common.beregning.EtteroppgjoerBeregnetAvkortingRequest
import no.nav.etterlatte.libs.common.beregning.EtteroppgjoerHentBeregnetResultatRequest
import no.nav.etterlatte.libs.common.beregning.InntektsjusteringAvkortingInfoRequest
import no.nav.etterlatte.libs.common.beregning.InntektsjusteringAvkortingInfoResponse
import no.nav.etterlatte.libs.common.beregning.MaanederMedGammelSanksjonIAvkorting
import no.nav.etterlatte.libs.common.beregning.OverstyrBeregningDTO
import no.nav.etterlatte.libs.common.beregning.Sanksjon
import no.nav.etterlatte.libs.common.feilhaandtering.GenerellIkkeFunnetException
import no.nav.etterlatte.libs.common.sak.SakId
import no.nav.etterlatte.libs.ktor.route.BehandlingTilgangsSjekk
import no.nav.etterlatte.libs.ktor.route.IkkeTilgangTilBehandlingException
import no.nav.etterlatte.libs.ktor.token.BrukerTokenInfo
import no.nav.etterlatte.libs.ktor.token.Saksbehandler
import no.nav.etterlatte.modules.beregning.avkorting.AvkortingService
import no.nav.etterlatte.modules.beregning.avkorting.inntektsjustering.AarligInntektsjusteringService
import no.nav.etterlatte.modules.beregning.etteroppgjoer.EtteroppgjoerService
import no.nav.etterlatte.modules.beregning.grunnbeloep.GrunnbeloepService
import no.nav.etterlatte.modules.beregning.grunnlag.BeregningsGrunnlagService
import no.nav.etterlatte.modules.beregning.sanksjon.SanksjonService
import no.nav.etterlatte.modules.beregning.ytelseMedGrunnlag.BeregningOgAvkortingBrevService
import java.time.YearMonth
import java.util.UUID

class BeregningKlientIntern(
    beregningService: () -> BeregningService,
    avkortingService: () -> AvkortingService,
    beregningsGrunnlagService: () -> BeregningsGrunnlagService,
    sanksjonService: () -> SanksjonService,
    grunnbeloepService: () -> GrunnbeloepService,
    aarligInntektsjusteringService: () -> AarligInntektsjusteringService,
    etteroppgjoerService: () -> EtteroppgjoerService,
    beregningOgAvkortingBrevService: () -> BeregningOgAvkortingBrevService,
    private val behandlingTilgangsSjekk: BehandlingTilgangsSjekk,
) : BeregningKlient {
    private val beregningService by lazy(beregningService)
    private val avkortingService by lazy(avkortingService)
    private val beregningsGrunnlagService by lazy(beregningsGrunnlagService)
    private val sanksjonService by lazy(sanksjonService)
    private val grunnbeloepService by lazy(grunnbeloepService)
    private val aarligInntektsjusteringService by lazy(aarligInntektsjusteringService)
    private val etteroppgjoerService by lazy(etteroppgjoerService)
    private val beregningOgAvkortingBrevService by lazy(beregningOgAvkortingBrevService)

    override suspend fun hentBeregningOgAvkorting(
        behandlingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
    ): BeregningOgAvkortingDto {
        sjekkTilgang(behandlingId, brukerTokenInfo)
        return beregningOgAvkortingBrevService.hentBeregningOgAvkorting(behandlingId, brukerTokenInfo)
            ?: throw GenerellIkkeFunnetException()
    }

    override suspend fun slettAvkorting(
        behandlingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
    ) {
        sjekkTilgang(behandlingId, brukerTokenInfo, skrivetilgang = true)
        avkortingService.slettAvkorting(behandlingId)
    }

    override suspend fun harOverstyrt(
        behandlingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
    ): Boolean = hentOverstyrtBeregning(behandlingId, brukerTokenInfo) != null

    override suspend fun hentGrunnbeloep(brukerTokenInfo: BrukerTokenInfo) = grunnbeloepService.hentGrunnbeloep(YearMonth.now())

    override suspend fun inntektsjusteringAvkortingInfoSjekk(
        sakId: SakId,
        aar: Int,
        sisteBehandling: UUID,
        brukerTokenInfo: BrukerTokenInfo,
    ): InntektsjusteringAvkortingInfoResponse =
        aarligInntektsjusteringService.hentSjekkAarligInntektsjustering(
            InntektsjusteringAvkortingInfoRequest(sakId, aar, sisteBehandling),
        )

    override suspend fun hentAvkortingForForbehandlingEtteroppgjoer(
        request: EtteroppgjoerBeregnetAvkortingRequest,
        brukerTokenInfo: BrukerTokenInfo,
    ): EtteroppgjoerBeregnetAvkorting {
        sjekkTilgang(request.forbehandling, brukerTokenInfo)
        return etteroppgjoerService.hentBeregnetAvkorting(
            forbehandlingId = request.forbehandling,
            sisteIverksatteBehandlingId = request.sisteIverksatteBehandling,
            sakId = request.sakId,
            aar = request.aar,
            brukerTokenInfo = brukerTokenInfo,
        )
    }

    override suspend fun hentBeregnetEtteroppgjoerResultat(
        request: EtteroppgjoerHentBeregnetResultatRequest,
        brukerTokenInfo: BrukerTokenInfo,
    ): BeregnetEtteroppgjoerResultatDto? {
        sjekkTilgang(request.forbehandlingId, brukerTokenInfo)
        return etteroppgjoerService
            .hentBeregnetEtteroppgjoerResultat(request.forbehandlingId, request.sisteIverksatteBehandlingId, request.aar)
            ?.toDto()
    }

    override suspend fun beregnAvkortingFaktiskInntekt(
        request: EtteroppgjoerBeregnFaktiskInntektRequest,
        brukerTokenInfo: BrukerTokenInfo,
    ): BeregnetEtteroppgjoerResultatDto {
        sjekkTilgang(request.forbehandlingId, brukerTokenInfo, skrivetilgang = true)
        return etteroppgjoerService
            .beregnOgLagreEtteroppgjoer(request, brukerTokenInfo)
            .toDto()
    }

    override suspend fun opprettBeregningsgrunnlagFraForrigeBehandling(
        behandlingId: UUID,
        forrigeBehandlingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
    ) {
        sjekkTilgang(behandlingId, brukerTokenInfo, skrivetilgang = true)
        beregningsGrunnlagService.dupliserBeregningsGrunnlag(behandlingId, forrigeBehandlingId, brukerTokenInfo)
    }

    override suspend fun beregnBehandling(
        behandlingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
    ) {
        sjekkTilgang(behandlingId, brukerTokenInfo, skrivetilgang = true)
        beregningService.opprettBeregning(behandlingId, brukerTokenInfo)
    }

    override suspend fun hentSanksjoner(
        behandlingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
    ): List<Sanksjon>? {
        sjekkTilgang(behandlingId, brukerTokenInfo)
        return sanksjonService.hentSanksjon(behandlingId).takeIf { it.isNotEmpty() }
    }

    override suspend fun hentBeregningsgrunnlag(
        behandlingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
    ): BeregningsGrunnlag {
        sjekkTilgang(behandlingId, brukerTokenInfo)
        return beregningsGrunnlagService.hentEllerKopierBeregningsGrunnlag(behandlingId, brukerTokenInfo)
            ?: throw BeregningsgrunnlagMangler(behandlingId)
    }

    override suspend fun hentOverstyrtBeregning(
        behandlingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
    ): OverstyrBeregningDTO? {
        sjekkTilgang(behandlingId, brukerTokenInfo)
        return beregningService.hentOverstyrBeregningPaaBehandlingId(behandlingId, brukerTokenInfo)?.toDTO()
    }

    override suspend fun hentBeregning(
        behandlingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
    ): BeregningDTO? {
        sjekkTilgang(behandlingId, brukerTokenInfo)
        return beregningService.hentBeregning(behandlingId, brukerTokenInfo)?.toDTO()
            ?: throw GenerellIkkeFunnetException()
    }

    override suspend fun hentAvkorting(
        behandlingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
    ): AvkortingDto? {
        sjekkTilgang(behandlingId, brukerTokenInfo)
        return avkortingService.hentFullfoertAvkorting(behandlingId, brukerTokenInfo)
    }

    override suspend fun harAvkortingMedSanksjonGamleRegler(
        behandlingId: UUID,
        inntektsaar: Int,
        brukerTokenInfo: BrukerTokenInfo,
    ): MaanederMedGammelSanksjonIAvkorting {
        sjekkTilgang(behandlingId, brukerTokenInfo)
        return avkortingService.harAvkortingMedSanksjonGamleRegler(behandlingId, inntektsaar, brukerTokenInfo)
    }

    private suspend fun sjekkTilgang(
        behandlingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
        skrivetilgang: Boolean = false,
    ) {
        if (brukerTokenInfo is Saksbehandler &&
            !behandlingTilgangsSjekk.harTilgangTilBehandling(behandlingId, skrivetilgang, brukerTokenInfo)
        ) {
            throw IkkeTilgangTilBehandlingException()
        }
    }
}
