package no.nav.etterlatte.utbetaling.iverksetting.utbetaling

import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import no.nav.etterlatte.utbetaling.common.forsteDagIMaaneden
import no.nav.etterlatte.utbetaling.common.toXMLDate
import no.nav.etterlatte.utbetaling.iverksetting.oppdrag.OppdragMapper
import no.nav.etterlatte.utbetaling.iverksetting.oppdrag.OppdragSender
import no.nav.etterlatte.utbetaling.utbetaling
import no.nav.etterlatte.utbetaling.utbetalingsvedtak
import no.trygdeetaten.skjema.oppdrag.TkodeStatusLinje
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.math.BigDecimal
import java.time.Clock
import java.time.YearMonth

internal class UtbetalingServiceTest {

    private val oppdragSender: OppdragSender = mockk()
    private val utbetalingDao: UtbetalingDao = mockk()
    private val utbetalingService: UtbetalingService = UtbetalingService(
        oppdragMapper = OppdragMapper,
        oppdragSender = oppdragSender,
        utbetalingDao = utbetalingDao,
        rapidsConnection = mockk(),
        clock = Clock.systemUTC()
    )

    @Test
    fun `skal opprette loepende utbetaling uten tidligere utbetalinger`() {
        every { utbetalingDao.hentUtbetalinger(any()) } returns emptyList()
        every { utbetalingDao.opprettUtbetaling(any()) } returns utbetaling()
        every { oppdragSender.sendOppdrag(any()) } just runs

        val vedtak = vedtakLoepende()
        utbetalingService.iverksettUtbetaling(vedtak)

        verify {
            oppdragSender.sendOppdrag(match {
                with(it.oppdrag110.oppdragsLinje150.first()) {
                    kodeStatusLinje == null &&
                            refDelytelseId == null &&
                            refFagsystemId == null &&
                            datoVedtakFom == vedtak.pensjonTilUtbetaling.first().periode.fom.toXmlDate() &&
                            sats == vedtak.pensjonTilUtbetaling.first().beloep
                }
            })
        }
    }

    @Test
    fun `skal opprette loepende utbetaling med en tidligere loepende utbetaling`() {
        val eksisterendeUtbetaling = utbetaling(utbetalingslinjeId = 1)
        every { utbetalingDao.hentUtbetalinger(any()) } returns listOf(eksisterendeUtbetaling)
        every { utbetalingDao.opprettUtbetaling(any()) } returns utbetaling()
        every { oppdragSender.sendOppdrag(any()) } just runs

        val vedtak = vedtakLoepende()
        utbetalingService.iverksettUtbetaling(vedtak)

        verify {
            oppdragSender.sendOppdrag(match {
                with(it.oppdrag110.oppdragsLinje150.first()) {
                    kodeStatusLinje == null &&
                            refDelytelseId == eksisterendeUtbetaling.utbetalingslinjer.first().id.value.toString() &&
                            refFagsystemId == eksisterendeUtbetaling.sakId.value.toString() &&
                            datoVedtakFom == vedtak.pensjonTilUtbetaling.first().periode.fom.toXmlDate() &&
                            sats == vedtak.pensjonTilUtbetaling.first().beloep
                }
            })
        }
    }

    @Test
    fun `skal opprette utbetaling med opphoer`() {
        val eksisterendeUtbetaling = utbetaling(utbetalingslinjeId = 1L)
        every { utbetalingDao.hentUtbetalinger(any()) } returns listOf(eksisterendeUtbetaling)
        every { utbetalingDao.opprettUtbetaling(any()) } returns utbetaling()
        every { oppdragSender.sendOppdrag(any()) } just runs

        val vedtak = vedtakMedOpphoer()
        utbetalingService.iverksettUtbetaling(vedtak)

        verify {
            oppdragSender.sendOppdrag(match {
                with(it.oppdrag110.oppdragsLinje150.first()) {
                    kodeStatusLinje == TkodeStatusLinje.OPPH &&
                            refDelytelseId == eksisterendeUtbetaling.utbetalingslinjer.first().id.value.toString() &&
                            refFagsystemId == eksisterendeUtbetaling.sakId.value.toString() &&
                            datoVedtakFom == vedtak.pensjonTilUtbetaling.first().periode.fom.toXmlDate() &&
                            sats == null
                }
            })
        }
    }

    @Test
    fun `skal feile dersom vedtak inneholder opphoer men det finnes ingen eksisterende utbetalinger`() {
        every { utbetalingDao.hentUtbetalinger(any()) } returns emptyList()

        assertThrows<IngenEksisterendeUtbetalingException> {
            utbetalingService.iverksettUtbetaling(vedtakMedOpphoer())
        }
    }

    @Test
    fun `skal returnere EksisterendeData med datatype EKSISTERENDE_VEDTAKID om vedtak eksisterer`() {
        every { utbetalingDao.hentUtbetaling(any()) } returns utbetaling()
        val eksisterendeData = utbetalingService.eksisterendeData(utbetalingsvedtak())
        assertEquals(Datatype.EKSISTERENDE_VEDTAKID, eksisterendeData.eksisterendeDatatype)
    }

    @Test
    fun `skal returnere EksisterendeData med datatype EKSISTERENDE_UTBETALINGSLINJEID om utbetalingslinjer eksisterer`() {
        every { utbetalingDao.hentUtbetaling(any()) } returns null
        every { utbetalingDao.hentUtbetalingslinjer(any()) } returns listOf(mockk<Utbetalingslinje>() {
            every { id } returns UtbetalingslinjeId(1)
        })
        val eksisterendeData = utbetalingService.eksisterendeData(utbetalingsvedtak())
        assertEquals(Datatype.EKSISTERENDE_UTBETALINGSLINJEID, eksisterendeData.eksisterendeDatatype)
    }

    @Test
    fun `skal returnere EksisterendeData med datatype INGEN_EKSISTERENDE_DATA om ingen data eksisterer`() {
        every { utbetalingDao.hentUtbetaling(any()) } returns null
        every { utbetalingDao.hentUtbetalingslinjer(any()) } returns emptyList()
        val eksisterendeData = utbetalingService.eksisterendeData(utbetalingsvedtak())
        assertEquals(Datatype.INGEN_EKSISTERENDE_DATA, eksisterendeData.eksisterendeDatatype)
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

    private fun vedtakLoepende() = utbetalingsvedtak(
        utbetalingsperioder = listOf(
            Utbetalingsperiode(
                id = 2L,
                periode = Periode(fom = YearMonth.of(2022, 10), tom = null),
                beloep = BigDecimal.valueOf(3000),
                type = UtbetalingsperiodeType.UTBETALING
            )
        )
    )

}