package no.nav.etterlatte.tidshendelser

import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import java.time.LocalDate
import java.time.Month
import java.time.YearMonth
import javax.sql.DataSource

@ExtendWith(TidshendelserDatabaseExtension::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class JobbPollerIntegrationTest(dataSource: DataSource, private val resetDb: ResetDb) {
    private val aldersovergangerService = mockk<AldersovergangerService>()
    private val hendelseDao = HendelseDao(dataSource)
    private val jobbTestdata = JobbTestdata(dataSource, hendelseDao)
    private val jobbPoller = JobbPoller(hendelseDao, aldersovergangerService)

    @AfterEach
    fun afterEach() {
        clearAllMocks()
        resetDb.invoke()
    }

    @Test
    fun `skal kun hente en jobb for dagens dato, og trigge den`() {
        val jobb1 = jobbTestdata.opprettJobb(JobbType.AO_BP20, YearMonth.of(2024, Month.MARCH), LocalDate.now())
        val jobb2 = jobbTestdata.opprettJobb(JobbType.AO_BP20, YearMonth.of(2024, Month.MARCH), LocalDate.now())

        every { aldersovergangerService.execute(jobb1) } returns Unit

        jobbPoller.poll()

        verify { aldersovergangerService.execute(jobb1) }
        verify(exactly = 0) { aldersovergangerService.execute(jobb2) }
    }

    @Test
    fun `skal ikke trigge jobb som ikke er for dagens dato`() {
        jobbTestdata.opprettJobb(JobbType.AO_BP20, YearMonth.of(2024, Month.MARCH), LocalDate.now().minusDays(1))

        jobbPoller.poll()

        verify(exactly = 0) { aldersovergangerService.execute(any()) }
    }
}
