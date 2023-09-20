package no.nav.etterlatte.utbetaling.common

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.etterlatte.utbetaling.grensesnittavstemming.GrensesnittsavstemmingService
import no.nav.etterlatte.utbetaling.iverksetting.utbetaling.Saktype
import no.nav.etterlatte.utbetaling.iverksetting.utbetaling.UtbetalingService
import no.nav.etterlatte.utbetaling.iverksetting.utbetaling.UtbetalingStatus
import no.nav.etterlatte.utbetaling.readFile
import no.nav.etterlatte.utbetaling.utbetaling
import no.nav.etterlatte.utbetaling.utbetalingshendelse
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.junit.jupiter.api.Test

internal class OppgavetriggerTest {
    private val utbetalingService =
        mockk<UtbetalingService>(relaxed = true) {
            every { settKvitteringManuelt(1) } returns mockk(relaxed = true)
            every { utbetalingDao.hentUtbetaling(1) } returns
                utbetaling(
                    utbetalingshendelser =
                        listOf(
                            utbetalingshendelse(
                                status = UtbetalingStatus.SENDT,
                            ),
                        ),
                )
            every {
                utbetalingDao.oppdaterKvittering(
                    any(),
                    any(),
                    any(),
                )
            } returns utbetaling(utbetalingshendelser = listOf(utbetalingshendelse(status = UtbetalingStatus.GODKJENT)))
        }
    private val grensesnittsavstemmingService =
        mockk<GrensesnittsavstemmingService> {
            every { startGrensesnittsavstemming(Saktype.BARNEPENSJON) } returns Unit
        }

    private val inspector =
        TestRapid().apply {
            Oppgavetrigger(
                rapidsConnection = this,
                utbetalingService = utbetalingService,
                grensesnittsavstemmingService = grensesnittsavstemmingService,
            )
        }

    @Test
    fun `melding skal starte grensesnittavstemming`() {
        inspector.apply { sendTestMessage(oppgave_grensesnittavstemming) }

        verify(timeout = 5000) {
            grensesnittsavstemmingService.startGrensesnittsavstemming(saktype = Saktype.BARNEPENSJON)
        }
    }

    @Test
    fun `melding skal sette kvittering manuelt for vedtakId 1`() {
        inspector.apply { sendTestMessage(oppgave_sett_kvittering) }

        verify {
            utbetalingService.settKvitteringManuelt(1)
        }
    }

    companion object {
        val oppgave_grensesnittavstemming = readFile("/oppgave_grensesnittavstemming.json")
        val oppgave_sett_kvittering = readFile("/oppgave_sett_kvittering_manuelt.json")
    }
}
