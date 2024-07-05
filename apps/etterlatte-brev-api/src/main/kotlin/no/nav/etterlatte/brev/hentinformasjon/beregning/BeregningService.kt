package no.nav.etterlatte.brev.hentinformasjon.beregning

import no.nav.etterlatte.brev.behandling.AvkortetBeregningsperiode
import no.nav.etterlatte.brev.behandling.Avkortingsinfo
import no.nav.etterlatte.brev.behandling.Beregningsperiode
import no.nav.etterlatte.brev.behandling.Utbetalingsinfo
import no.nav.etterlatte.brev.behandling.hentUtbetaltBeloep
import no.nav.etterlatte.libs.common.IntBroek
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.beregning.BeregningsMetode
import no.nav.etterlatte.libs.common.feilhaandtering.UgyldigForespoerselException
import no.nav.etterlatte.libs.common.vedtak.VedtakType
import no.nav.etterlatte.libs.ktor.token.BrukerTokenInfo
import no.nav.etterlatte.regler.ANTALL_DESIMALER_INNTENKT
import no.nav.etterlatte.regler.roundingModeInntekt
import no.nav.pensjon.brevbaker.api.model.Kroner
import java.math.BigDecimal
import java.time.YearMonth
import java.util.UUID

class BeregningService(
    private val beregningKlient: BeregningKlient,
) {
    suspend fun hentGrunnbeloep(bruker: BrukerTokenInfo) = beregningKlient.hentGrunnbeloep(bruker)

    internal suspend fun hentBeregning(
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
        finnUtbetalingsinfoNullable(behandlingId, virkningstidspunkt, brukerTokenInfo, sakType)
            ?: throw UgyldigForespoerselException(
                code = "UTBETALINGSINFO_MANGLER",
                detail = "Utbetalingsinfo er nødvendig, men mangler",
            )

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
                    trygdetidForIdent = it.trygdetidForIdent,
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
        checkNotNull(finnAvkortingsinfoNullable(behandlingId, sakType, virkningstidspunkt, vedtakType, brukerTokenInfo)) {
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
                    grunnbeloep = Kroner(it.grunnbelop),
                    // (vises i brev) maanedsinntekt regel burde eksponert dette, krever omskrivning av regler som vi må bli enige om
                    inntekt =
                        Kroner(
                            BigDecimal(it.aarsinntekt - it.fratrekkInnAar).setScale(ANTALL_DESIMALER_INNTENKT, roundingModeInntekt).toInt(),
                        ),
                    aarsinntekt = Kroner(it.aarsinntekt),
                    fratrekkInnAar = Kroner(it.fratrekkInnAar),
                    relevanteMaanederInnAar = it.relevanteMaanederInnAar,
                    ytelseFoerAvkorting = Kroner(it.ytelseFoerAvkorting),
                    restanse = Kroner(it.restanse),
                    utbetaltBeloep = Kroner(it.ytelseEtterAvkorting),
                    trygdetid = it.trygdetid,
                    beregningsMetodeAnvendt = requireNotNull(it.beregningsMetode),
                    beregningsMetodeFraGrunnlag =
                        beregningsGrunnlag?.beregningsMetode?.beregningsMetode
                            ?: requireNotNull(it.beregningsMetode),
                    // ved manuelt overstyrt beregning har vi ikke grunnlag
                )
            }

        return Avkortingsinfo(
            virkningsdato = virkningstidspunkt.atDay(1),
            beregningsperioder = beregningsperioder,
        )
    }
}

private fun hentBenyttetTrygdetidOgProratabroek(
    beregningsperiode: no.nav.etterlatte.libs.common.beregning.Beregningsperiode,
): Pair<Int, IntBroek?> =
    when (beregningsperiode.beregningsMetode) {
        BeregningsMetode.NASJONAL ->
            Pair(
                beregningsperiode.samletNorskTrygdetid ?: throw SamletTeoretiskTrygdetidMangler(),
                null,
            )

        BeregningsMetode.PRORATA -> {
            Pair(
                beregningsperiode.samletTeoretiskTrygdetid ?: throw SamletTeoretiskTrygdetidMangler(),
                beregningsperiode.broek ?: throw BeregningsperiodeBroekMangler(),
            )
        }

        BeregningsMetode.BEST -> throw UgyldigBeregningsMetode()
        null -> beregningsperiode.trygdetid to null
    }

class SamletTeoretiskTrygdetidMangler :
    UgyldigForespoerselException(
        code = "SAMLET_TEORETISK_TRYGDETID_MANGLER",
        detail = "Samlet teoretisk trygdetid mangler i beregningen",
    )

class BeregningsperiodeBroekMangler :
    UgyldigForespoerselException(
        code = "BEREGNINGSPERIODE_BROEK_MANGLER",
        detail = "Beregningsperioden mangler brøk",
    )

class UgyldigBeregningsMetode :
    UgyldigForespoerselException(
        code = "UGYLDIG_BEREGNINGS_METODE",
        detail =
            "Kan ikke ha brukt beregningsmetode 'BEST' i en faktisk beregning, " +
                "siden best velger mellom nasjonal eller prorata når det beregnes.",
    )
