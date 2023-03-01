package no.nav.etterlatte.vedtaksvurdering

import io.mockk.mockk
import no.nav.etterlatte.vedtaksvurdering.klienter.BehandlingKlient
import no.nav.etterlatte.vedtaksvurdering.klienter.BeregningKlient
import no.nav.etterlatte.vedtaksvurdering.klienter.VilkaarsvurderingKlient

internal class UtledeUtbetalingsperioderTest {
    private val repositoryMock: VedtaksvurderingRepository = mockk()
    private val beregning = mockk<BeregningKlient>(relaxed = true)
    private val vilkaarsvurdering = mockk<VilkaarsvurderingKlient>(relaxed = true)
    private val behandling = mockk<BehandlingKlient>(relaxed = true)
    private val service = VedtaksvurderingService(
        repositoryMock,
        beregning,
        vilkaarsvurdering,
        behandling,
        mockk(),
        mockk()
    )
/*
    @Test
    fun `vedtak med sluttdato uten beregninger skal ha en opphørsperiode`() {
        val virk = Periode(YearMonth.of(2022, 2), YearMonth.of(2022, 5))

        val utbetalingsperioder = service.utbetalingsperioderFraVedtak(VedtakType.INNVILGELSE, virk, emptyList())
        Assertions.assertEquals(1, utbetalingsperioder.size)
        Assertions.assertEquals(
            Utbetalingsperiode(0, virk.copy(tom = null), null, UtbetalingsperiodeType.OPPHOER),
            utbetalingsperioder.first()
        )
    }

    @Test
    fun `vedtak uten sluttdato uten beregninger skal ha en opphørsperiode`() {
        val virk = Periode(YearMonth.of(2022, 2), null)

        val utbetalingsperioder = service.utbetalingsperioderFraVedtak(VedtakType.INNVILGELSE, virk, emptyList())
        Assertions.assertEquals(1, utbetalingsperioder.size)
        Assertions.assertEquals(
            Utbetalingsperiode(0, virk, null, UtbetalingsperiodeType.OPPHOER),
            utbetalingsperioder.first()
        )
    }

    @Test
    fun `siste utbetalingsperiode skal aldri ha tom-dato`() {
        val virk = Periode(YearMonth.of(2022, 2), YearMonth.of(2022, 4))

        val utbetalingsperioder = service.utbetalingsperioderFraVedtak(
            VedtakType.INNVILGELSE,
            virk,
            listOf(
                Beregningsperiode(virk, BigDecimal.valueOf(100))
            )
        )
        Assertions.assertEquals(2, utbetalingsperioder.size)
        Assertions.assertEquals(
            Utbetalingsperiode(0, virk, BigDecimal.valueOf(100), UtbetalingsperiodeType.UTBETALING),
            utbetalingsperioder.first()
        )
        Assertions.assertEquals(
            Utbetalingsperiode(
                0,
                virk.copy(fom = virk.tom!!.plusMonths(1), tom = null),
                null,
                UtbetalingsperiodeType.OPPHOER
            ),
            utbetalingsperioder[1]
        )
    }

    @Test
    fun `dersom vedtak starter før beregningene, skal perioden starte med et opphør`() {
        val virk = Periode(YearMonth.of(2022, 2), null)

        val utbetalingsperioder = service.utbetalingsperioderFraVedtak(
            VedtakType.INNVILGELSE,
            virk,
            listOf(
                Beregningsperiode(virk.copy(fom = virk.fom.plusMonths(2)), BigDecimal.valueOf(100))
            )
        )
        Assertions.assertEquals(2, utbetalingsperioder.size)
        Assertions.assertEquals(
            Utbetalingsperiode(0, virk.copy(tom = virk.fom.plusMonths(1)), null, UtbetalingsperiodeType.OPPHOER),
            utbetalingsperioder.first()
        )
        Assertions.assertEquals(
            Utbetalingsperiode(
                0,
                virk.copy(fom = virk.fom.plusMonths(2), tom = null),
                BigDecimal.valueOf(100),
                UtbetalingsperiodeType.UTBETALING
            ),
            utbetalingsperioder[1]
        )
    }

 */
}