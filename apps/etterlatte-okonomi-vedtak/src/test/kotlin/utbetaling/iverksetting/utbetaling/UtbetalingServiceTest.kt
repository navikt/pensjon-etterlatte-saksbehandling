package no.nav.etterlatte.utbetaling.iverksetting.utbetaling

import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import no.nav.etterlatte.utbetaling.common.toXMLDate
import no.nav.etterlatte.utbetaling.iverksetting.oppdrag.OppdragMapper
import no.nav.etterlatte.utbetaling.iverksetting.oppdrag.OppdragSender
import no.nav.etterlatte.utbetaling.utbetaling
import no.nav.etterlatte.utbetaling.utbetalingsvedtak
import no.trygdeetaten.skjema.oppdrag.TkodeStatusLinje
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.Clock
import java.time.LocalDate
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
    fun `skal opprette utbetaling med opphoer`() {
        every { utbetalingDao.hentUtbetalinger(any()) } returns listOf(utbetaling(utbetalingslinjeId = 1L))
        every { utbetalingDao.opprettUtbetaling(any()) } returns utbetaling()
        every { oppdragSender.sendOppdrag(any()) } just runs

        val vedtak = vedtakMedOpphoer()
        utbetalingService.iverksettUtbetaling(vedtak)

        verify {
            oppdragSender.sendOppdrag(match {
                with(it.oppdrag110.oppdragsLinje150.first()) {
                    kodeStatusLinje == TkodeStatusLinje.OPPH &&
                            refDelytelseId == "1" &&
                            refFagsystemId == "1" &&
                            datoVedtakFom == LocalDate.of(2022, 10, 1).toXMLDate() &&
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

}