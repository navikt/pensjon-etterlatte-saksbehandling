package no.nav.etterlatte.brev.hentinformasjon.beregning

import no.nav.etterlatte.brev.behandling.AvkortetBeregningsperiode
import no.nav.etterlatte.brev.behandling.Avkortingsinfo
import no.nav.etterlatte.brev.behandling.Beregningsperiode
import no.nav.etterlatte.brev.behandling.Utbetalingsinfo
import no.nav.etterlatte.brev.behandling.hentUtbetaltBeloep
import no.nav.etterlatte.brev.hentinformasjon.BeregningKlient
import no.nav.etterlatte.brev.hentinformasjon.hentBenyttetTrygdetidOgProratabroek
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.vedtak.VedtakType
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

    suspend fun finnUtbetalingsinfo(
        behandlingId: UUID,
        virkningstidspunkt: YearMonth,
        brukerTokenInfo: BrukerTokenInfo,
        sakType: SakType,
    ): Utbetalingsinfo =
        requireNotNull(finnUtbetalingsinfoNullable(behandlingId, virkningstidspunkt, brukerTokenInfo, sakType)) {
            "Utbetalingsinfo er nødvendig, men mangler"
        }

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

    suspend fun finnAvkortingsinfo(
        behandlingId: UUID,
        sakType: SakType,
        virkningstidspunkt: YearMonth,
        vedtakType: VedtakType,
        brukerTokenInfo: BrukerTokenInfo,
    ): Avkortingsinfo =
        requireNotNull(finnAvkortingsinfoNullable(behandlingId, sakType, virkningstidspunkt, vedtakType, brukerTokenInfo)) {
            "Avkortingsinfo er nødvendig, men mangler"
        }

    suspend fun finnAvkortingsinfoNullable(
        behandlingId: UUID,
        sakType: SakType,
        virkningstidspunkt: YearMonth,
        vedtakType: VedtakType,
        brukerTokenInfo: BrukerTokenInfo,
    ): Avkortingsinfo? {
        if (sakType == SakType.BARNEPENSJON || vedtakType == VedtakType.OPPHOER) return null

        val ytelseMedGrunnlag = beregningKlient.hentYtelseMedGrunnlag(behandlingId, brukerTokenInfo)
        val beregningsGrunnlag = hentBeregningsGrunnlag(behandlingId, sakType, brukerTokenInfo)

        val beregningsperioder =
            ytelseMedGrunnlag.perioder.map {
                AvkortetBeregningsperiode(
                    datoFOM = it.periode.fom.atDay(1),
                    datoTOM = it.periode.tom?.atEndOfMonth(),
                    inntekt = Kroner(it.aarsinntekt - it.fratrekkInnAar),
                    ytelseFoerAvkorting = Kroner(it.ytelseFoerAvkorting),
                    utbetaltBeloep = Kroner(it.ytelseEtterAvkorting),
                    trygdetid = it.trygdetid,
                    beregningsMetodeAnvendt = requireNotNull(it.beregningsMetode),
                    beregningsMetodeFraGrunnlag =
                        beregningsGrunnlag?.beregningsMetode?.beregningsMetode
                            ?: requireNotNull(it.beregningsMetode),
                    // ved manuelt overstyrt beregning har vi ikke grunnlag
                )
            }

        val aarsInntekt = ytelseMedGrunnlag.perioder.first().aarsinntekt
        val grunnbeloep = ytelseMedGrunnlag.perioder.first().grunnbelop

        return Avkortingsinfo(
            grunnbeloep = Kroner(grunnbeloep),
            inntekt = Kroner(aarsInntekt),
            virkningsdato = virkningstidspunkt.atDay(1),
            beregningsperioder = beregningsperioder,
        )
    }
}
