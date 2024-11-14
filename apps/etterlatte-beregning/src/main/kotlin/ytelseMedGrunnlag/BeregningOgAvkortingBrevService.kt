package no.nav.etterlatte.ytelseMedGrunnlag

import no.nav.etterlatte.avkorting.AvkortingRepository
import no.nav.etterlatte.beregning.BeregningRepository
import no.nav.etterlatte.beregning.grunnlag.BeregningsGrunnlagService
import no.nav.etterlatte.klienter.BehandlingKlient
import no.nav.etterlatte.libs.common.behandling.virkningstidspunkt
import no.nav.etterlatte.libs.common.beregning.BeregningOgAvkortingDto
import no.nav.etterlatte.libs.common.beregning.BeregningOgAvkortingPeriodeDto
import no.nav.etterlatte.libs.common.feilhaandtering.IkkeFunnetException
import no.nav.etterlatte.libs.common.periode.Periode
import no.nav.etterlatte.libs.ktor.token.BrukerTokenInfo
import java.util.UUID

/**
 * Hensikten med denne tjenesten er å la brev hente data om beregning og avkorting i et samlet sted
 */
class BeregningOgAvkortingBrevService(
    private val beregningRepository: BeregningRepository,
    private val avkortingRepository: AvkortingRepository,
    private val beregningsGrunnlagService: BeregningsGrunnlagService,
    private val behandlingKlient: BehandlingKlient,
) {
    suspend fun hentBeregningOgAvkorting(
        behandlingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
    ): BeregningOgAvkortingDto? {
        val avkortingUtenLoependeYtelse = avkortingRepository.hentAvkorting(behandlingId) ?: return null
        val virkningstidspunkt = behandlingKlient.hentBehandling(behandlingId, brukerTokenInfo).virkningstidspunkt()
        val avkorting = avkortingUtenLoependeYtelse.toDto(virkningstidspunkt.dato)
        val beregning = beregningRepository.hent(behandlingId) ?: throw BeregningFinnesIkkeException(behandlingId)
        val behandlingsGrunnlag = beregningsGrunnlagService.hentBeregningsGrunnlag(behandlingId, brukerTokenInfo)

        val avkortinger =
            avkorting.avkortetYtelse.map { avkortetYtelse ->
                val beregningIPeriode =
                    beregning.beregningsperioder
                        .filter { it.datoFOM <= avkortetYtelse.fom }
                        .maxBy { it.datoFOM }
                val grunnlag =
                    avkorting.avkortingGrunnlag
                        .filter { it.fom <= avkortetYtelse.fom }
                        .maxBy { it.fom }
                val oppgittInntekt = grunnlag.inntektTom + grunnlag.inntektUtlandTom
                val fratrekkInnAar = grunnlag.fratrekkInnAar + grunnlag.fratrekkInnAarUtland

                BeregningOgAvkortingPeriodeDto(
                    periode = Periode(avkortetYtelse.fom, avkortetYtelse.tom),
                    ytelseEtterAvkorting = avkortetYtelse.ytelseEtterAvkorting,
                    restanse = avkortetYtelse.restanse,
                    avkortingsbeloep = avkortetYtelse.avkortingsbeloep,
                    ytelseFoerAvkorting = beregningIPeriode.utbetaltBeloep,
                    trygdetid = beregningIPeriode.trygdetid,
                    oppgittInntekt = oppgittInntekt,
                    fratrekkInnAar = fratrekkInnAar,
                    innvilgaMaaneder = grunnlag.innvilgaMaaneder,
                    grunnbelop = beregningIPeriode.grunnbelop,
                    grunnbelopMnd = beregningIPeriode.grunnbelopMnd,
                    beregningsMetode = beregningIPeriode.beregningsMetode,
                    beregningsMetodeFraGrunnlag = behandlingsGrunnlag?.beregningsMetode?.beregningsMetode,
                    sanksjon = avkortetYtelse.sanksjon,
                    institusjonsopphold = beregningIPeriode.institusjonsopphold,
                    erOverstyrtInnvilgaMaaneder = grunnlag.overstyrtInnvilgaMaaneder != null,
                )
            }

        return BeregningOgAvkortingDto(
            perioder = avkortinger,
        )
    }
}

class BeregningFinnesIkkeException(
    behandlingId: UUID,
) : IkkeFunnetException(
        code = "BEREGNING_IKKE_FUNNET",
        detail = "Mangler beregning for behandling $behandlingId",
    )
