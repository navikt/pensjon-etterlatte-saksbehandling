package no.nav.etterlatte.utbetaling.avstemming

import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.utbetaling.DatabaseExtension
import no.nav.etterlatte.utbetaling.avstemming.avstemmingsdata.KonsistensavstemmingDataMapper
import no.nav.etterlatte.utbetaling.iverksetting.utbetaling.Saktype
import no.nav.etterlatte.utbetaling.oppdragForKonsistensavstemming
import no.nav.etterlatte.utbetaling.oppdragslinjeForKonsistensavstemming
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.RegisterExtension
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import javax.sql.DataSource

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class AvstemmingDaoIntegrationTest(dataSource: DataSource) {
    companion object {
        @RegisterExtension
        val dbExtension = DatabaseExtension()
    }

    private val avstemmingDao = AvstemmingDao(dataSource)

    @Test
    fun `skal opprette konsistensavstemming for barnepensjon`() {
        val konsistensavstemmingMedData = opprettKonsistensavstemmingMedData()

        val antallRaderOppdatert = avstemmingDao.opprettKonsistensavstemming(konsistensavstemmingMedData)
        assertEquals(1, antallRaderOppdatert)
    }

    @Test
    fun `skal opprette konsistensavstemming for OMS`() {
        val konsistensavstemmingMedData = opprettKonsistensavstemmingMedDataOMS()

        val antallRaderOppdatert = avstemmingDao.opprettKonsistensavstemming(konsistensavstemmingMedData)
        assertEquals(1, antallRaderOppdatert)
    }

    @Test
    fun `skal hente siste konsistensavstemming for barnepensjon`() {
        val konsistensavstemming1 = opprettKonsistensavstemmingMedData(Tidspunkt.now())
        val konsistensavstemming2 = opprettKonsistensavstemmingMedData(Tidspunkt.now().minus(3, ChronoUnit.DAYS))
        val konsistensavstemming3 = opprettKonsistensavstemmingMedData(Tidspunkt.now().minus(6, ChronoUnit.DAYS))

        avstemmingDao.opprettKonsistensavstemming(konsistensavstemming1)
        avstemmingDao.opprettKonsistensavstemming(konsistensavstemming2)
        avstemmingDao.opprettKonsistensavstemming(konsistensavstemming3)

        val datoSisteKonsistensavstemming = avstemmingDao.hentDatoOpprettetForSisteKonsistensavstemming(Saktype.BARNEPENSJON)
        assertEquals(konsistensavstemming1.opprettet, datoSisteKonsistensavstemming)
    }

    @Test
    fun `skal hente siste konsistensavstemming for OMS`() {
        val konsistensavstemming1 = opprettKonsistensavstemmingMedDataOMS(Tidspunkt.now())
        val konsistensavstemming2 = opprettKonsistensavstemmingMedDataOMS(Tidspunkt.now().minus(3, ChronoUnit.DAYS))
        val konsistensavstemming3 = opprettKonsistensavstemmingMedDataOMS(Tidspunkt.now().minus(6, ChronoUnit.DAYS))

        avstemmingDao.opprettKonsistensavstemming(konsistensavstemming1)
        avstemmingDao.opprettKonsistensavstemming(konsistensavstemming2)
        avstemmingDao.opprettKonsistensavstemming(konsistensavstemming3)

        val datoOpprettetSisteKonsistensavstemming = avstemmingDao.hentDatoOpprettetForSisteKonsistensavstemming(Saktype.OMSTILLINGSSTOENAD)
        assertEquals(konsistensavstemming1.opprettet, datoOpprettetSisteKonsistensavstemming)
    }

    @Test
    fun `skal opprette grensesnittavstemming for barnepensjon`() {
        val grensesnittavstemming =
            Grensesnittavstemming(
                periode =
                    Avstemmingsperiode(
                        fraOgMed = Tidspunkt.now().minus(1, ChronoUnit.DAYS),
                        til = Tidspunkt.now(),
                    ),
                antallOppdrag = 1,
                opprettet = Tidspunkt.now(),
                avstemmingsdata = "",
                saktype = Saktype.BARNEPENSJON,
            )

        val antallRaderOppdatert = avstemmingDao.opprettGrensesnittavstemming(grensesnittavstemming)

        assertEquals(1, antallRaderOppdatert)
    }

    @Test
    fun `skal opprette grensesnittavstemming for OMS`() {
        val grensesnittavstemming =
            Grensesnittavstemming(
                periode =
                    Avstemmingsperiode(
                        fraOgMed = Tidspunkt.now().minus(1, ChronoUnit.DAYS),
                        til = Tidspunkt.now(),
                    ),
                antallOppdrag = 1,
                opprettet = Tidspunkt.now(),
                avstemmingsdata = "",
                saktype = Saktype.OMSTILLINGSSTOENAD,
            )

        val antallRaderOppdatert = avstemmingDao.opprettGrensesnittavstemming(grensesnittavstemming)

        assertEquals(1, antallRaderOppdatert)
    }

    @Test
    fun `skal hente nyeste grensesnittavstemming`() {
        val now = Tidspunkt.now()

        val grensesnittavstemming1 =
            Grensesnittavstemming(
                opprettet = now,
                periode =
                    Avstemmingsperiode(
                        fraOgMed = now.minus(1, ChronoUnit.DAYS),
                        til = now,
                    ),
                antallOppdrag = 1,
                avstemmingsdata = "",
                saktype = Saktype.BARNEPENSJON,
            )

        val grensesnittavstemming2 =
            Grensesnittavstemming(
                opprettet = now.minus(1, ChronoUnit.DAYS),
                periode =
                    Avstemmingsperiode(
                        fraOgMed = now.minus(2, ChronoUnit.DAYS),
                        til = now.minus(1, ChronoUnit.DAYS),
                    ),
                antallOppdrag = 2,
                avstemmingsdata = "",
                saktype = Saktype.OMSTILLINGSSTOENAD,
            )

        val grensesnittavstemming3 =
            Grensesnittavstemming(
                opprettet = now.minus(2, ChronoUnit.DAYS),
                periode =
                    Avstemmingsperiode(
                        fraOgMed = now.minus(3, ChronoUnit.DAYS),
                        til = now.minus(2, ChronoUnit.DAYS),
                    ),
                antallOppdrag = 3,
                avstemmingsdata = "",
                saktype = Saktype.BARNEPENSJON,
            )

        avstemmingDao.opprettGrensesnittavstemming(grensesnittavstemming1)
        avstemmingDao.opprettGrensesnittavstemming(grensesnittavstemming3)
        avstemmingDao.opprettGrensesnittavstemming(grensesnittavstemming2)

        val nyesteAvstemming = avstemmingDao.hentSisteGrensesnittavstemming(Saktype.BARNEPENSJON)
        assertEquals(grensesnittavstemming1, nyesteAvstemming)
    }

    @Test
    fun `skal gi null dersom det ikke finnes noen grensesnittavstemming`() {
        val nyesteAvstemming = avstemmingDao.hentSisteGrensesnittavstemming(saktype = Saktype.BARNEPENSJON)

        assertNull(nyesteAvstemming)
    }

    @Test
    fun `skal gi null dersom det ikke finnes noen konsistensavstemming`() {
        val datoNyesteAvstemming = avstemmingDao.hentDatoOpprettetForSisteKonsistensavstemming(saktype = Saktype.BARNEPENSJON)

        assertNull(datoNyesteAvstemming)
    }

    @AfterEach
    fun afterEach() {
        dbExtension.resetDb()
    }

    fun opprettKonsistensavstemmingMedData(opprettetTilOgMed: Tidspunkt = Tidspunkt.now()): Konsistensavstemming {
        val oppdragslinjer = listOf(oppdragslinjeForKonsistensavstemming(fraOgMed = LocalDate.of(2022, 10, 7)))
        val oppdrag = oppdragForKonsistensavstemming(oppdragslinjeForKonsistensavstemming = oppdragslinjer)
        val konsistensavstemmingUtenData =
            Konsistensavstemming(
                id = UUIDBase64(),
                sakType = Saktype.BARNEPENSJON,
                opprettet = Tidspunkt.now(),
                avstemmingsdata = null,
                loependeFraOgMed = Tidspunkt.now(),
                opprettetTilOgMed = opprettetTilOgMed,
                loependeUtbetalinger = listOf(oppdrag),
            )
        val avstemmingsdata = KonsistensavstemmingDataMapper(konsistensavstemmingUtenData).opprettAvstemmingsmelding(Saktype.BARNEPENSJON)
        return konsistensavstemmingUtenData.copy(avstemmingsdata = avstemmingsdata.joinToString("\n"))
    }

    fun opprettKonsistensavstemmingMedDataOMS(opprettetTilOgMed: Tidspunkt = Tidspunkt.now()): Konsistensavstemming {
        val oppdragslinjer = listOf(oppdragslinjeForKonsistensavstemming(fraOgMed = LocalDate.of(2022, 10, 7)))
        val oppdrag = oppdragForKonsistensavstemming(oppdragslinjeForKonsistensavstemming = oppdragslinjer)
        val konsistensavstemmingUtenData =
            Konsistensavstemming(
                id = UUIDBase64(),
                sakType = Saktype.OMSTILLINGSSTOENAD,
                opprettet = Tidspunkt.now(),
                avstemmingsdata = null,
                loependeFraOgMed = Tidspunkt.now(),
                opprettetTilOgMed = opprettetTilOgMed,
                loependeUtbetalinger = listOf(oppdrag),
            )
        val avstemmingsdata = KonsistensavstemmingDataMapper(konsistensavstemmingUtenData).opprettAvstemmingsmelding(Saktype.BARNEPENSJON)
        return konsistensavstemmingUtenData.copy(avstemmingsdata = avstemmingsdata.joinToString("\n"))
    }
}
