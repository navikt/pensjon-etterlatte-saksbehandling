package no.nav.etterlatte.tidshendelser

import io.mockk.clearAllMocks
import io.mockk.spyk
import io.mockk.verify
import no.nav.etterlatte.libs.tidshendelser.JobbType
import no.nav.etterlatte.tidshendelser.hendelser.HendelseDao
import no.nav.etterlatte.tidshendelser.hendelser.HendelserJobb
import no.nav.etterlatte.tidshendelser.hendelser.UferdigeJobberPoller
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import java.time.LocalDate
import java.time.YearMonth
import javax.sql.DataSource

@ExtendWith(DatabaseExtension::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class UferdigeJobberPollerIntegrationTest(
    dataSource: DataSource,
) {
    private val hendelseDao = HendelseDao(dataSource)
    private val jobbTestdata = JobbTestdata(dataSource, hendelseDao)
    private val poller = spyk(UferdigeJobberPoller(hendelseDao))

    @AfterEach
    fun afterEach() {
        clearAllMocks()
    }

    @Test
    @Suppress("UnusedVariable", "unused")
    fun `poll skal logge hvis uferdige jobber`() {
        val forrigeMaaned = YearMonth.now().minusMonths(1)
        val maanedFoerForrige = YearMonth.now().minusMonths(2)
        val jobb1 = opprettJobb(JobbType.AO_BP20, forrigeMaaned)
        val jobb2 = opprettJobb(JobbType.AO_BP20, forrigeMaaned).settFerdig()

        val jobb3 = opprettJobb(JobbType.AO_BP21, forrigeMaaned)
        val jobb4 = opprettJobb(JobbType.AO_BP21, maanedFoerForrige).settFerdig()

        val jobb5 = opprettJobb(JobbType.AO_OMS67, forrigeMaaned).settFerdig()
        val jobb6 = opprettJobb(JobbType.AO_OMS67, maanedFoerForrige)

        poller.poll()

        verify { poller.loggUferdigeJobber(listOf(jobb3, jobb6)) }
    }

    @Test
    @Suppress("UnusedVariable", "unused")
    fun `poll skal ikke logge hvis ingen uferdige jobber`() {
        val forrigeMaaned = YearMonth.now().minusMonths(1)
        val maanedFoerForrige = YearMonth.now().minusMonths(2)
        val jobb1 = opprettJobb(JobbType.AO_BP20, forrigeMaaned)
        val jobb2 = opprettJobb(JobbType.AO_BP20, forrigeMaaned).settFerdig()

        val jobb3 = opprettJobb(JobbType.AO_BP21, maanedFoerForrige).settFerdig()
        val jobb4 = opprettJobb(JobbType.AO_BP21, forrigeMaaned).settFerdig()

        val jobb5 = opprettJobb(JobbType.AO_OMS67, maanedFoerForrige)
        val jobb6 = opprettJobb(JobbType.AO_OMS67, maanedFoerForrige, kjoeredato = LocalDate.now())

        poller.poll()

        verify(exactly = 0) { poller.loggUferdigeJobber(any()) }
    }

    private fun opprettJobb(
        jobbType: JobbType,
        behandlingsmaaned: YearMonth,
        kjoeredato: LocalDate = behandlingsmaaned.atDay(1),
    ): HendelserJobb =
        jobbTestdata.opprettJobb(
            type = jobbType,
            behandlingsmaaned = behandlingsmaaned,
            kjoeredato = kjoeredato,
        )

    private fun HendelserJobb.settFerdig(): HendelserJobb {
        hendelseDao.oppdaterJobbstatusFerdig(this)
        return this
    }
}
