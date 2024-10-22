package no.nav.etterlatte.tidshendelser

import io.kotest.matchers.collections.shouldHaveSize
import io.mockk.clearAllMocks
import no.nav.etterlatte.libs.tidshendelser.JobbType
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.RegisterExtension
import java.time.YearMonth
import javax.sql.DataSource

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class OpprettJobberIntegrationTest(
    dataSource: DataSource,
) {
    companion object {
        @RegisterExtension
        private val dbExtension = DatabaseExtension()
    }

    private val hendelseDao = HendelseDao(dataSource)
    private val jobbTestdata = JobbTestdata(dataSource, hendelseDao)
    private val opprettJobb = OpprettJobb(hendelseDao)

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
        opprettJobb.poll()
        hendelseDao.finnJobberMedKjoeringForMaaned(YearMonth.now().plusMonths(1)) shouldHaveSize 3
    }

    @Test
    fun `skal ikke lage jobber om de er laget fra før`() {
        jobbTestdata.opprettJobb(
            JobbType.OMS_DOED_4MND,
            YearMonth.now().plusMonths(1),
            YearMonth.now().plusMonths(1).atDay(5),
        )
        opprettJobb.poll()

        hendelseDao.finnJobberMedKjoeringForMaaned(YearMonth.now().plusMonths(1)) shouldHaveSize 3
    }

    @Test
    fun `skal lage jobber om selv om det fins andre ikke faste jobber`() {
        jobbTestdata.opprettJobb(
            JobbType.AO_BP20,
            YearMonth.now().plusMonths(1),
            YearMonth.now().plusMonths(1).atDay(5),
        )
        opprettJobb.poll()

        hendelseDao.finnJobberMedKjoeringForMaaned(YearMonth.now().plusMonths(1)) shouldHaveSize 4
    }
}
