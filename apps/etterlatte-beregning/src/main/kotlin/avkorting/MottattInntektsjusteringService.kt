package no.nav.etterlatte.avkorting

import no.nav.etterlatte.libs.common.beregning.AvkortingGrunnlagLagreDto
import no.nav.etterlatte.libs.common.beregning.AvkortingOverstyrtInnvilgaMaanederDto
import no.nav.etterlatte.libs.common.beregning.MottattInntektsjusteringAvkortigRequest
import no.nav.etterlatte.libs.common.feilhaandtering.InternfeilException
import no.nav.etterlatte.libs.ktor.token.BrukerTokenInfo
import java.time.YearMonth
import java.util.UUID

class MottattInntektsjusteringService(
    private val avkortingService: AvkortingService,
) {
    suspend fun opprettAvkortingMedBrukeroppgittInntekt(
        request: MottattInntektsjusteringAvkortigRequest,
        brukerTokenInfo: BrukerTokenInfo,
    ): Avkorting {
        val (behandlingId, virkningstidspunkt, mottattInntektsjustering) = request

        avkortingService.tilstandssjekk(behandlingId, brukerTokenInfo)

        val eksisterende =
            avkortingService.hentOpprettEllerReberegnAvkorting(behandlingId, brukerTokenInfo)
                ?: throw InternfeilException("Fant ikke og klarte opprette avkorting under inntektsjustering")
        // val eksisterendeInntekt = eksisterende.avkortingGrunnlag.find { it.aar == virkningstidspunkt.year }?.fraVirk
        val eksisterendeInntekt = eksisterende.redigerbarForventetInntekt

        val nyttGrunnlag =
            AvkortingGrunnlagLagreDto(
                id = eksisterendeInntekt?.id ?: UUID.randomUUID(),
                inntektTom =
                    with(mottattInntektsjustering) {
                        arbeidsinntekt + naeringsinntekt + (afpInntekt ?: 0)
                    },
                fratrekkInnAar = 0, // TODO må tilpasses når vi skal støtte inntektsjustering inneværende år
                inntektUtlandTom = mottattInntektsjustering.inntektFraUtland,
                fratrekkInnAarUtland = 0, // TODO må tilpasses når vi skal støtte inntektsjustering inneværende år
                spesifikasjon = "Mottatt inntekt fra bruker gjennom selvbetjening", // TODO avklar med fag - muligens noe om afp osv bør inn
                fom = virkningstidspunkt,
                overstyrtInnvilgaMaaneder =
                    mottattInntektsjustering.datoForAaGaaAvMedAlderspensjon?.let {
                        overstyrMedTidligAlderspensjon(
                            it,
                        )
                    },
            )
        avkortingService.beregnAvkortingMedNyttGrunnlag(behandlingId, brukerTokenInfo, nyttGrunnlag)

        return avkortingService.hentAvkorting(behandlingId) ?: throw AvkortingFinnesIkkeException(behandlingId)
    }

    private fun overstyrMedTidligAlderspensjon(datoAlderspensjon: YearMonth) =
        AvkortingOverstyrtInnvilgaMaanederDto(
            antall = datoAlderspensjon.monthValue - 1,
            aarsak = OverstyrtInnvilgaMaanederAarsak.TAR_UT_PENSJON_TIDLIG.name,
            begrunnelse = "Bruker har oppgitt tidlig alderspensjon i inntektsjusteringskjema",
        )
}
