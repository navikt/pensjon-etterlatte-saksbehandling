package no.nav.etterlatte.vedtaksvurdering

import io.mockk.mockk
import no.nav.etterlatte.libs.common.behandling.BehandlingType
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.beregning.AvkortetYtelseDto
import no.nav.etterlatte.libs.common.beregning.AvkortingDto
import no.nav.etterlatte.libs.common.beregning.BeregningDTO
import no.nav.etterlatte.libs.common.beregning.Beregningsperiode
import no.nav.etterlatte.libs.common.beregning.Beregningstype
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.vedtak.Periode
import no.nav.etterlatte.libs.common.vedtak.Utbetalingsperiode
import no.nav.etterlatte.libs.common.vedtak.UtbetalingsperiodeType
import no.nav.etterlatte.libs.common.vedtak.VedtakStatus
import no.nav.etterlatte.libs.common.vedtak.VedtakType
import no.nav.etterlatte.libs.testdata.grunnlag.SOEKER_FOEDSELSNUMMER
import no.nav.etterlatte.no.nav.etterlatte.vedtaksvurdering.VedtakOgBeregningSammenligner
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.math.BigDecimal
import java.time.Month
import java.time.YearMonth
import java.util.UUID

class VedtakOgBeregningSammenlignerTest {
    @Test
    fun `sammenligner behandling gaar fint naar beregning og utbetaling stemmer overens`() {
        VedtakOgBeregningSammenligner.sammenlign(BeregningOgAvkorting(lagBeregning(), null), lagVedtak())
    }

    @Test
    fun `sammenligner behandling feiler naar sum for beregning og utbetaling ikke stemmer`() {
        assertThrows<IllegalStateException> {
            VedtakOgBeregningSammenligner.sammenlign(BeregningOgAvkorting(lagBeregning(100), null), lagVedtak())
        }
    }

    @Test
    fun `sammenligner behandling feiler naar periode for beregning og utbetaling ikke stemmer`() {
        assertThrows<IllegalStateException> {
            VedtakOgBeregningSammenligner.sammenlign(
                BeregningOgAvkorting(
                    lagBeregning(
                        foersteMaaned =
                            YearMonth.of(
                                2023,
                                Month.JUNE,
                            ),
                    ),
                    null,
                ),
                lagVedtak(),
            )
        }
    }

    @Test
    fun `sammenligner behandling feiler naar beloep fra avkorting og utbetaling ikke stemmer`() {
        assertThrows<IllegalStateException> {
            VedtakOgBeregningSammenligner.sammenlign(
                BeregningOgAvkorting(lagBeregning(), lagAvkorting(1200)),
                lagVedtak(),
            )
        }
    }

    @Test
    fun `sammenligner behandling gaar fint naar beloep fra avkorting og utbetaling stemmer`() {
        VedtakOgBeregningSammenligner.sammenlign(
            BeregningOgAvkorting(lagBeregning(), lagAvkorting(1000)),
            lagVedtak(),
        )
    }

    private fun lagAvkorting(ytelseEtterAvkorting: Int = 1000) =
        AvkortingDto(
            avkortingGrunnlag = listOf(),
            avkortetYtelse =
                listOf(
                    AvkortetYtelseDto(
                        fom = YearMonth.of(2024, Month.JANUARY),
                        tom = YearMonth.of(2024, Month.APRIL),
                        ytelseFoerAvkorting = 1700,
                        ytelseEtterAvkorting = ytelseEtterAvkorting,
                        avkortingsbeloep = 500,
                        restanse = 0,
                        sanksjon = null,
                    ),
                    AvkortetYtelseDto(
                        fom = YearMonth.of(2024, Month.MAY),
                        tom = YearMonth.of(2024, Month.JUNE),
                        ytelseFoerAvkorting = 2500,
                        ytelseEtterAvkorting = ytelseEtterAvkorting * 2,
                        avkortingsbeloep = 500,
                        restanse = 0,
                        sanksjon = null,
                    ),
                ),
        )

    private fun lagBeregning(
        sum: Int = 1000,
        foersteMaaned: YearMonth = YearMonth.of(2024, Month.JANUARY),
    ) = BeregningDTO(
        beregningId = UUID.randomUUID(),
        behandlingId = UUID.randomUUID(),
        type = Beregningstype.BP,
        beregningsperioder =
            listOf(
                Beregningsperiode(
                    datoFOM = foersteMaaned,
                    datoTOM = YearMonth.of(2024, Month.APRIL),
                    utbetaltBeloep = sum,
                    grunnbelopMnd = 100,
                    grunnbelop = 1200,
                    trygdetid = 40,
                ),
                Beregningsperiode(
                    datoFOM = YearMonth.of(2024, Month.MAY),
                    datoTOM = YearMonth.of(2024, Month.JUNE),
                    utbetaltBeloep = sum * 2,
                    grunnbelopMnd = 100,
                    grunnbelop = 1200,
                    trygdetid = 40,
                ),
            ),
        beregnetDato = Tidspunkt.now(),
        grunnlagMetadata = mockk(),
        overstyrBeregning = null,
    )

    private fun lagVedtak() =
        Vedtak(
            id = 1L,
            soeker = SOEKER_FOEDSELSNUMMER,
            sakId = 1L,
            sakType = SakType.BARNEPENSJON,
            behandlingId = UUID.randomUUID(),
            status = VedtakStatus.FATTET_VEDTAK,
            type = VedtakType.ENDRING,
            innhold =
                VedtakInnhold.Behandling(
                    behandlingType = BehandlingType.REVURDERING,
                    revurderingAarsak = null,
                    virkningstidspunkt = YearMonth.of(2024, Month.JANUARY),
                    beregning = null,
                    avkorting = null,
                    vilkaarsvurdering = null,
                    utbetalingsperioder =
                        listOf(
                            Utbetalingsperiode(
                                periode =
                                    Periode(
                                        fom = YearMonth.of(2024, Month.JANUARY),
                                        tom = YearMonth.of(2024, Month.APRIL),
                                    ),
                                beloep = BigDecimal(1000),
                                type = UtbetalingsperiodeType.UTBETALING,
                            ),
                            Utbetalingsperiode(
                                periode =
                                    Periode(
                                        fom = YearMonth.of(2024, Month.MAY),
                                        tom = YearMonth.of(2024, Month.JUNE),
                                    ),
                                beloep = BigDecimal(2000),
                                type = UtbetalingsperiodeType.UTBETALING,
                            ),
                        ),
                ),
        )
}
