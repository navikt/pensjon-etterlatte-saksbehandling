package no.nav.etterlatte.utbetaling.iverksetting.utbetaling

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.etterlatte.libs.common.tidspunkt.utcKlokke
import no.nav.etterlatte.utbetaling.common.forsteDagIMaaneden
import no.nav.etterlatte.utbetaling.common.toXMLDate
import no.nav.etterlatte.utbetaling.iverksetting.oppdrag.OppdragMapper
import no.nav.etterlatte.utbetaling.iverksetting.oppdrag.OppdragSender
import no.nav.etterlatte.utbetaling.iverksetting.utbetaling.IverksettResultat.UtbetalingForVedtakEksisterer
import no.nav.etterlatte.utbetaling.iverksetting.utbetaling.IverksettResultat.UtbetalingslinjerForVedtakEksisterer
import no.nav.etterlatte.utbetaling.utbetaling
import no.nav.etterlatte.utbetaling.utbetalingslinje
import no.nav.etterlatte.utbetaling.utbetalingsvedtak
import no.trygdeetaten.skjema.oppdrag.TkodeStatusLinje
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth
import java.util.*

internal class UtbetalingServiceTest {

    private val oppdragSender: OppdragSender = mockk()
    private val utbetalingDao: UtbetalingDao = mockk()
    private val utbetalingService: UtbetalingService = UtbetalingService(
        oppdragMapper = OppdragMapper,
        oppdragSender = oppdragSender,
        utbetalingDao = utbetalingDao,
        rapidsConnection = mockk(),
        clock = utcKlokke()
    )

    /**
     * Vedtak ID 1: |---------->
     * Vedtak ID 1: |----------> (eksisterende vedtak) // IKKE OK
     */
    @Test
    fun `skal stoppe opprettelse av utbetaling hvis vedtak finnes fra for`() {
        every { utbetalingDao.hentUtbetaling(any()) } returns utbetaling(
            utbetalingshendelser = listOf(
                Utbetalingshendelse(
                    utbetalingId = UUID.randomUUID(),
                    status = UtbetalingStatus.GODKJENT
                )
            )
        )
        every { utbetalingDao.hentDupliserteUtbetalingslinjer(any(), any()) } returns emptyList()

        val vedtak = vedtakLoepende()
        val resultat = utbetalingService.iverksettUtbetaling(vedtak)

        assertTrue(resultat is UtbetalingForVedtakEksisterer)
    }

    /**
     *                  ID 1
     * Vedtak ID 1: |---------->
     *                  ID 1 (eksisterende utbetalingslinje) // IKKE OK
     * Vedtak ID 2: |---------->
     */
    @Test
    fun `skal stoppe opprettelse av utbetaling hvis en eller flere utbetalingslinje-IDer finnes fra for`() {
        every { utbetalingDao.hentUtbetaling(any()) } returns utbetaling()
        every { utbetalingDao.hentDupliserteUtbetalingslinjer(any(), any()) } returns listOf(utbetalingslinje())

        val vedtak = vedtakLoepende()
        val resultat = utbetalingService.iverksettUtbetaling(vedtak)

        assertTrue(resultat is UtbetalingslinjerForVedtakEksisterer)
    }

    /**
     * Vedtak 1: |--------------->  // OK
     */
    @Test
    fun `skal opprette loepende utbetaling uten tidligere utbetalinger`() {
        every { utbetalingDao.hentUtbetaling(any()) } returns null
        every { utbetalingDao.hentDupliserteUtbetalingslinjer(any(), any()) } returns emptyList()
        every { utbetalingDao.hentUtbetalinger(any()) } returns emptyList()
        every { utbetalingDao.opprettUtbetaling(any()) } returns utbetaling()
        every { utbetalingDao.nyUtbetalingshendelse(any(), any()) } returns utbetaling()
        every { oppdragSender.sendOppdrag(any()) } returns ""

        val vedtak = vedtakLoepende()
        utbetalingService.iverksettUtbetaling(vedtak)

        verify {
            oppdragSender.sendOppdrag(
                match {
                    with(it.oppdrag110.oppdragsLinje150.first()) {
                        kodeStatusLinje == null &&
                            refDelytelseId == null &&
                            refFagsystemId == null &&
                            datoVedtakFom == vedtak.pensjonTilUtbetaling.first().periode.fom.toXmlDate() &&
                            sats == vedtak.pensjonTilUtbetaling.first().beloep
                    }
                }
            )
        }
    }

    /**
     * Vedtak ID 1: |---------->
     * Vedtak ID 2:     |------> // Ok
     */
    @Test
    fun `skal opprette loepende utbetaling med en tidligere loepende utbetaling`() {
        val utbetalingId = UUID.randomUUID()
        val eksisterendeUtbetaling = utbetaling(
            id = utbetalingId,
            utbetalingslinjeId = 1L,
            utbetalingshendelser = listOf(
                Utbetalingshendelse(
                    utbetalingId = utbetalingId,
                    status = UtbetalingStatus.GODKJENT
                )
            )
        )
        val utbetaling = utbetaling()
        every { utbetalingDao.hentUtbetaling(any()) } returns null
        every { utbetalingDao.hentDupliserteUtbetalingslinjer(any(), any()) } returns emptyList()
        every { utbetalingDao.hentUtbetalinger(any()) } returns listOf(eksisterendeUtbetaling)
        every { utbetalingDao.opprettUtbetaling(any()) } returns utbetaling
        every { utbetalingDao.nyUtbetalingshendelse(any(), any()) } returns utbetaling
        every { oppdragSender.sendOppdrag(any()) } returns ""

        val vedtak = vedtakLoepende()
        utbetalingService.iverksettUtbetaling(vedtak)

        verify {
            oppdragSender.sendOppdrag(
                match {
                    with(it.oppdrag110.oppdragsLinje150.first()) {
                        kodeStatusLinje == null &&
                            refDelytelseId == eksisterendeUtbetaling.utbetalingslinjer.first().id.value.toString() &&
                            refFagsystemId == eksisterendeUtbetaling.sakId.value.toString() &&
                            datoVedtakFom == vedtak.pensjonTilUtbetaling.first().periode.fom.toXmlDate() &&
                            sats == vedtak.pensjonTilUtbetaling.first().beloep
                    }
                }
            )
        }
    }

    /**
     * Vedtak ID 1: |---------->
     * Vedtak ID 2:     | (Opphoer) // OK
     */
    @Test
    fun `skal opprette utbetaling med opphoer`() {
        val utbetalingId = UUID.randomUUID()
        val eksisterendeUtbetaling = utbetaling(
            id = utbetalingId,
            utbetalingslinjeId = 1L,
            utbetalingshendelser = listOf(
                Utbetalingshendelse(
                    utbetalingId = utbetalingId,
                    status = UtbetalingStatus.GODKJENT
                )
            )
        )
        every { utbetalingDao.hentUtbetaling(any()) } returns null
        every { utbetalingDao.hentDupliserteUtbetalingslinjer(any(), any()) } returns emptyList()
        every { utbetalingDao.hentUtbetalinger(any()) } returns listOf(eksisterendeUtbetaling)
        every { utbetalingDao.opprettUtbetaling(any()) } returns utbetaling()
        every { utbetalingDao.nyUtbetalingshendelse(any(), any()) } returns utbetaling()
        every { oppdragSender.sendOppdrag(any()) } returns ""

        val vedtak = vedtakMedOpphoer()
        utbetalingService.iverksettUtbetaling(vedtak)

        verify {
            oppdragSender.sendOppdrag(
                match {
                    with(it.oppdrag110.oppdragsLinje150.first()) {
                        kodeStatusLinje == TkodeStatusLinje.OPPH &&
                            refDelytelseId == eksisterendeUtbetaling.utbetalingslinjer.first().id.value.toString() &&
                            refFagsystemId == eksisterendeUtbetaling.sakId.value.toString() &&
                            datoVedtakFom == vedtak.pensjonTilUtbetaling.first().periode.fom.toXmlDate() &&
                            sats == null
                    }
                }
            )
        }
    }

    /**
     * Vedtak ID 2:     | (Opphoer) // IKKE OK
     */
    @Test
    fun `skal feile dersom vedtak inneholder opphoer men det finnes ingen eksisterende utbetalinger`() {
        every { utbetalingDao.hentUtbetaling(any()) } returns null
        every { utbetalingDao.hentDupliserteUtbetalingslinjer(any(), any()) } returns emptyList()
        every { utbetalingDao.hentUtbetalinger(any()) } returns emptyList()

        assertThrows<IngenEksisterendeUtbetalingException> {
            utbetalingService.iverksettUtbetaling(vedtakMedOpphoer())
        }
    }

    /**
     *                  ID 1
     * Vedtak ID 1: |----------------------------------------------->
     *                 ID 2 (ref ID1)  ID 3 (ref ID2)  ID 4 (ref ID3)
     * Vedtak ID 2:   |--------------|---------------|-------------->
     */
    @Test
    fun `skal revurdere flere perioder tilbake i tid og sette referanser til tidligere utbetalingslinjer`() {
        val eksisterendeUtbetaling = utbetaling(utbetalingslinjeId = 1L, periodeFra = LocalDate.parse("2021-01-01"))
        every { utbetalingDao.hentUtbetaling(any()) } returns null
        every { utbetalingDao.hentDupliserteUtbetalingslinjer(any(), any()) } returns emptyList()
        every { utbetalingDao.hentUtbetalinger(any()) } returns listOf(eksisterendeUtbetaling)
        every { utbetalingDao.opprettUtbetaling(any()) } returns utbetaling()
        every { utbetalingDao.nyUtbetalingshendelse(any(), any()) } returns utbetaling()
        every { oppdragSender.sendOppdrag(any()) } returns ""

        val vedtak = vedtakRevurderingFlerePerioder()
        utbetalingService.iverksettUtbetaling(vedtak)

        verify {
            oppdragSender.sendOppdrag(
                match {
                    with(it.oppdrag110) {
                        kodeEndring = "ENDR"
                    }
                    with(it.oppdrag110.oppdragsLinje150.first()) {
                        refDelytelseId == eksisterendeUtbetaling.utbetalingslinjer.first().id.value.toString() &&
                            refFagsystemId == eksisterendeUtbetaling.sakId.value.toString() &&
                            datoVedtakFom == vedtak.pensjonTilUtbetaling.first().periode.fom.toXmlDate() &&
                            sats == vedtak.pensjonTilUtbetaling.first().beloep
                    }
                    with(it.oppdrag110.oppdragsLinje150[1]) {
                        refDelytelseId == vedtak.pensjonTilUtbetaling.first().id.toString() &&
                            refFagsystemId == eksisterendeUtbetaling.sakId.value.toString() &&
                            datoVedtakFom == vedtak.pensjonTilUtbetaling[1].periode.fom.toXmlDate() &&
                            sats == vedtak.pensjonTilUtbetaling[1].beloep
                    }
                    with(it.oppdrag110.oppdragsLinje150[2]) {
                        refDelytelseId == vedtak.pensjonTilUtbetaling[1].id.toString() &&
                            refFagsystemId == eksisterendeUtbetaling.sakId.value.toString() &&
                            datoVedtakFom == vedtak.pensjonTilUtbetaling[2].periode.fom.toXmlDate() &&
                            sats == vedtak.pensjonTilUtbetaling[2].beloep
                    }
                }
            )
        }
    }

    /**
     * Vedtak ID 1: |----------------------->
     *                 (revurdering)
     * Vedtak ID 2:   |------------| (opphør) // OK
     */
    @Test
    fun `skal revurdere og opphoere i samme vedtak`() {
        val eksisterendeUtbetaling = utbetaling(utbetalingslinjeId = 1L, periodeFra = LocalDate.parse("2022-01-01"))
        every { utbetalingDao.hentUtbetaling(any()) } returns null
        every { utbetalingDao.hentDupliserteUtbetalingslinjer(any(), any()) } returns emptyList()
        every { utbetalingDao.hentUtbetalinger(any()) } returns listOf(eksisterendeUtbetaling)
        every { utbetalingDao.opprettUtbetaling(any()) } returns utbetaling()
        every { utbetalingDao.nyUtbetalingshendelse(any(), any()) } returns utbetaling()
        every { oppdragSender.sendOppdrag(any()) } returns ""

        val vedtak = vedtakRevurderingOgOpphoer()
        utbetalingService.iverksettUtbetaling(vedtak)

        verify {
            oppdragSender.sendOppdrag(
                match {
                    with(it.oppdrag110) {
                        kodeEndring = "ENDR"
                    }
                    with(it.oppdrag110.oppdragsLinje150.first()) {
                        refDelytelseId == eksisterendeUtbetaling.utbetalingslinjer.first().id.value.toString() &&
                            refFagsystemId == eksisterendeUtbetaling.sakId.value.toString() &&
                            datoVedtakFom == vedtak.pensjonTilUtbetaling.first().periode.fom.toXmlDate() &&
                            sats == vedtak.pensjonTilUtbetaling.first().beloep
                    }
                    with(it.oppdrag110.oppdragsLinje150[1]) {
                        kodeStatusLinje == TkodeStatusLinje.OPPH &&
                            refDelytelseId == vedtak.pensjonTilUtbetaling.first().id.toString() &&
                            refFagsystemId == eksisterendeUtbetaling.sakId.value.toString() &&
                            datoVedtakFom == vedtak.pensjonTilUtbetaling[1].periode.fom.toXmlDate() &&
                            sats == vedtak.pensjonTilUtbetaling[1].beloep
                    }
                }
            )
        }
    }

    private fun YearMonth.toXmlDate() = forsteDagIMaaneden(this).toXMLDate()

    private fun vedtakMedOpphoer() = utbetalingsvedtak(
        utbetalingsperioder = listOf(
            Utbetalingsperiode(
                id = 2L,
                periode = Periode(fom = YearMonth.of(2022, 10), tom = null),
                beloep = null,
                type = UtbetalingsperiodeType.OPPHOER
            )
        )
    )

    private fun vedtakLoepende(startPeriode: YearMonth = YearMonth.of(2022, 10)) = utbetalingsvedtak(
        vedtakId = 2,
        utbetalingsperioder = listOf(
            Utbetalingsperiode(
                id = 2L,
                periode = Periode(fom = startPeriode, tom = null),
                beloep = BigDecimal.valueOf(3000),
                type = UtbetalingsperiodeType.UTBETALING
            )
        )
    )

    private fun vedtakRevurderingFlerePerioder() = utbetalingsvedtak(
        vedtakId = 2,
        utbetalingsperioder = listOf(
            Utbetalingsperiode(
                id = 2L,
                periode = Periode(fom = YearMonth.of(2021, 4), tom = YearMonth.of(2021, 8)),
                beloep = BigDecimal.valueOf(4000),
                type = UtbetalingsperiodeType.UTBETALING
            ),
            Utbetalingsperiode(
                id = 3L,
                periode = Periode(fom = YearMonth.of(2021, 9), tom = YearMonth.of(2021, 12)),
                beloep = BigDecimal.valueOf(5000),
                type = UtbetalingsperiodeType.UTBETALING
            ),
            Utbetalingsperiode(
                id = 4L,
                periode = Periode(fom = YearMonth.of(2022, 1), tom = null),
                beloep = BigDecimal.valueOf(3000),
                type = UtbetalingsperiodeType.UTBETALING
            )
        )
    )

    private fun vedtakRevurderingOgOpphoer() = utbetalingsvedtak(
        vedtakId = 2,
        utbetalingsperioder = listOf(
            Utbetalingsperiode(
                id = 2L,
                periode = Periode(fom = YearMonth.of(2022, 2), tom = YearMonth.of(2022, 8)),
                beloep = BigDecimal.valueOf(4000),
                type = UtbetalingsperiodeType.UTBETALING
            ),
            Utbetalingsperiode(
                id = 3L,
                periode = Periode(fom = YearMonth.of(2022, 9), tom = null),
                beloep = null,
                type = UtbetalingsperiodeType.OPPHOER
            )
        )
    )
}