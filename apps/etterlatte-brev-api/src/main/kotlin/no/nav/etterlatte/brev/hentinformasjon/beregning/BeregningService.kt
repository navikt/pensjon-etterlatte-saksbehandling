package no.nav.etterlatte.brev.hentinformasjon.beregning

import no.nav.etterlatte.brev.behandling.Beregningsperiode
import no.nav.etterlatte.brev.behandling.Utbetalingsinfo
import no.nav.etterlatte.brev.behandling.hentUtbetaltBeloep
import no.nav.etterlatte.brev.hentinformasjon.BeregningKlient
import no.nav.etterlatte.brev.hentinformasjon.hentBenyttetTrygdetidOgProratabroek
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.token.BrukerTokenInfo
import no.nav.pensjon.brevbaker.api.model.Kroner
import java.time.YearMonth
import java.util.UUID

class BeregningService(private val beregningKlient: BeregningKlient) {
    suspend fun hentGrunnbeloep(bruker: BrukerTokenInfo) = beregningKlient.hentGrunnbeloep(bruker)

    suspend fun hentBeregning(
        behandlingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
    ) = beregningKlient.hentBeregning(behandlingId, brukerTokenInfo)

    suspend fun hentBeregningsGrunnlag(
        behandlingId: UUID,
        sakType: SakType,
        brukerTokenInfo: BrukerTokenInfo,
    ) = beregningKlient.hentBeregningsGrunnlag(behandlingId, sakType, brukerTokenInfo)

    suspend fun hentYtelseMedGrunnlag(
        behandlingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
    ) = beregningKlient.hentYtelseMedGrunnlag(behandlingId, brukerTokenInfo)

    suspend fun finnUtbetalingsinfoNullable(
        behandlingId: UUID,
        virkningstidspunkt: YearMonth,
        brukerTokenInfo: BrukerTokenInfo,
        sakType: SakType,
    ): Utbetalingsinfo? {
        val beregning = hentBeregning(behandlingId, brukerTokenInfo) ?: return null
        val beregningsGrunnlag = hentBeregningsGrunnlag(behandlingId, sakType, brukerTokenInfo)

        val beregningsperioder =
            beregning.beregningsperioder.map {
                val (benyttetTrygdetid, prorataBroek) = hentBenyttetTrygdetidOgProratabroek(it)

                Beregningsperiode(
                    datoFOM = it.datoFOM.atDay(1),
                    datoTOM = it.datoTOM?.atEndOfMonth(),
                    grunnbeloep = Kroner(it.grunnbelop),
                    antallBarn = (it.soeskenFlokk?.size ?: 0) + 1,
                    // Legger til 1 pga at beregning fjerner soeker
                    utbetaltBeloep = Kroner(it.utbetaltBeloep),
                    trygdetid = benyttetTrygdetid,
                    prorataBroek = prorataBroek,
                    institusjon = it.institusjonsopphold != null,
                    beregningsMetodeAnvendt = requireNotNull(it.beregningsMetode),
                    beregningsMetodeFraGrunnlag =
                        beregningsGrunnlag?.beregningsMetode?.beregningsMetode
                            ?: requireNotNull(it.beregningsMetode),
                    // ved manuelt overstyrt beregning har vi ikke grunnlag
                )
            }

        val soeskenjustering = beregning.beregningsperioder.any { !it.soeskenFlokk.isNullOrEmpty() }
        val antallBarn = if (soeskenjustering) beregningsperioder.last().antallBarn else 1

        return Utbetalingsinfo(
            antallBarn,
            Kroner(beregningsperioder.hentUtbetaltBeloep()),
            virkningstidspunkt.atDay(1),
            soeskenjustering,
            beregningsperioder,
        )
    }
}
