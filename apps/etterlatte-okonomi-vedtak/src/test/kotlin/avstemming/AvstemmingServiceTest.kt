package no.nav.etterlatte.avstemming

import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import no.nav.etterlatte.domain.UtbetalingsoppdragStatus
import no.nav.etterlatte.oppdrag.UtbetalingsoppdragDao
import no.nav.etterlatte.utbetalingsoppdrag
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.util.*

internal class AvstemmingServiceTest {

    private val avstemmingDao: AvstemmingDao = mockk()
    private val utbetalingsoppdragDao: UtbetalingsoppdragDao = mockk()
    private val avstemmingSender: AvstemmingSender = mockk()

    private val avstemmingService: AvstemmingService = AvstemmingService(
        avstemmingSender = avstemmingSender,
        avstemmingDao = avstemmingDao,
        utbetalingsoppdragDao = utbetalingsoppdragDao
    )

    @Test
    fun `skal opprette avstemming og sende til oppdrag`() {
        val fraOgMed = LocalDateTime.now().minusDays(1)
        val til = LocalDateTime.now()
        val utbetalingsoppdrag = listOf(utbetalingsoppdrag(status = UtbetalingsoppdragStatus.FEILET))

        val avstemming = Avstemming(
            opprettet = LocalDateTime.now(),
            fraOgMed = fraOgMed,
            til = til,
            antallAvstemteOppdrag = 10
        )

        every { avstemmingDao.hentSisteAvstemming() } returns avstemming
        every { utbetalingsoppdragDao.hentAlleUtbetalingsoppdragMellom(any(), any()) } returns utbetalingsoppdrag
        every { avstemmingSender.sendAvstemming(any()) } just runs
        every { avstemmingDao.opprettAvstemming(any()) } returns 1

        avstemmingService.startAvstemming(fraOgMed, til)

        verify(exactly = 3) { avstemmingSender.sendAvstemming(any()) }
        verify { avstemmingDao.opprettAvstemming(match {
            it.antallAvstemteOppdrag == 1 && it.fraOgMed == fraOgMed
        }) }
    }


}