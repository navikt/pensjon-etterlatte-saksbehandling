package no.nav.etterlatte.avstemming

import io.mockk.every
import io.mockk.mockk
import no.nav.etterlatte.domain.Utbetalingsoppdrag
import no.nav.etterlatte.domain.UtbetalingsoppdragStatus
import no.nav.etterlatte.oppdrag.UtbetalingsoppdragDao
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

internal class AvstemmingMapperTest {


    @Test
    fun `grunnlagsdata opprettes korrekt`() {

        val utbetalingsoppdragsDao = mockk<UtbetalingsoppdragDao>(relaxed = true) {
            every { hentAlleUtbetalingsoppdragMellom(any(), any()) } returns mockk {
                listOf(mockk<Utbetalingsoppdrag>(relaxed = true) {
                    every { status } returnsMany listOf(
                        UtbetalingsoppdragStatus.GODKJENT,
                        UtbetalingsoppdragStatus.GODKJENT,
                        UtbetalingsoppdragStatus.GODKJENT,
                        UtbetalingsoppdragStatus.GODKJENT,
                        UtbetalingsoppdragStatus.FEILET,
                        UtbetalingsoppdragStatus.AVVIST,
                        UtbetalingsoppdragStatus.AVVIST,
                        UtbetalingsoppdragStatus.GODKJENT_MED_FEIL,
                        UtbetalingsoppdragStatus.GODKJENT_MED_FEIL,
                        UtbetalingsoppdragStatus.GODKJENT_MED_FEIL,
                        UtbetalingsoppdragStatus.SENDT,
                        UtbetalingsoppdragStatus.SENDT,
                        UtbetalingsoppdragStatus.SENDT
                    )
                })
            }
        }

        val utbetalingsOppdrag =
            utbetalingsoppdragsDao.hentAlleUtbetalingsoppdragMellom(LocalDateTime.MIN, LocalDateTime.MAX)
    }
}