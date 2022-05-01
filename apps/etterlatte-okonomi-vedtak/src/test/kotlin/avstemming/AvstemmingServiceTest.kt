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
        val avstemmingsnokkelFra = LocalDateTime.now().minusDays(1)
        val avstemmingsnokkelTil = LocalDateTime.now()
        val avstemming = Avstemming(
            id = UUID.randomUUID(),
            opprettet = LocalDateTime.now(),
            avstemmingsnokkelTilOgMed = avstemmingsnokkelFra,
            antallAvstemteOppdrag = 10
        )

        val utbetalingsoppdrag = listOf(utbetalingsoppdrag(status = UtbetalingsoppdragStatus.FEILET))

        every { avstemmingDao.hentSisteAvstemming() } returns avstemming
        every { utbetalingsoppdragDao.hentAlleUtbetalingsoppdragMellom(avstemmingsnokkelFra, avstemmingsnokkelTil) } returns utbetalingsoppdrag
        every { avstemmingSender.sendAvstemming(any()) } returns 3
        every { avstemmingDao.opprettAvstemming(any()) } returns 1

        avstemmingService.startAvstemming(avstemmingNokkelTil = avstemmingsnokkelTil)

        verify { avstemmingSender.sendAvstemming(any()) }
        verify { avstemmingDao.opprettAvstemming(match {
            it.antallAvstemteOppdrag == 1 && it.avstemmingsnokkelTilOgMed == avstemmingsnokkelTil
        }) }
    }


}