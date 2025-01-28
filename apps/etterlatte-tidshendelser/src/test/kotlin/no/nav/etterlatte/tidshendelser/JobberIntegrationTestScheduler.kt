package no.nav.etterlatte.tidshendelser

import io.kotest.matchers.collections.shouldHaveSize
import io.mockk.clearAllMocks
import no.nav.etterlatte.libs.tidshendelser.JobbType
import no.nav.etterlatte.tidshendelser.JobbScheduler.PeriodiskeJobber
import no.nav.etterlatte.tidshendelser.hendelser.HendelseDao
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.RegisterExtension
import java.time.YearMonth
import javax.sql.DataSource

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class JobberIntegrationTestScheduler(
    dataSource: DataSource,
) {
    companion object {
        @RegisterExtension
        private val dbExtension = DatabaseExtension()
    }

    private val nesteMaaned = YearMonth.now().plusMonths(1)
    private val hendelseDao = HendelseDao(dataSource)
    private val jobbTestdata = JobbTestdata(dataSource, hendelseDao)
    private val jobbScheduler = JobbScheduler(hendelseDao)

    @BeforeEach
    fun beforeEach() {
        jobbTestdata.slettAlleJobber()
    }

    @AfterEach
    fun afterEach() {
        clearAllMocks()
        dbExtension.resetDb()
    }

    @Test
    fun `skal lage jobber om de ikke er laget fra før`() {
        jobbScheduler.poll()
        hendelseDao.finnJobberMedKjoeringForMaaned(nesteMaaned) shouldHaveSize PeriodiskeJobber.entries.size
    }

    @Test
    fun `skal ikke lage jobber om de er laget fra før`() {
        jobbTestdata.opprettJobb(
            JobbType.OMS_DOED_4MND,
            nesteMaaned,
            nesteMaaned.atDay(5),
        )
        jobbScheduler.poll()

        hendelseDao.finnJobberMedKjoeringForMaaned(nesteMaaned) shouldHaveSize PeriodiskeJobber.entries.size
    }

    @Test
    fun `skal lage jobber om selv om det fins andre ikke faste jobber`() {
        jobbTestdata.opprettJobb(
            JobbType.AO_BP20,
            nesteMaaned,
            nesteMaaned.atDay(5),
        )
        jobbScheduler.poll()

        hendelseDao.finnJobberMedKjoeringForMaaned(nesteMaaned) shouldHaveSize PeriodiskeJobber.entries.size + 1
    }
}
