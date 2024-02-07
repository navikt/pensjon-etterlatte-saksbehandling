package no.nav.etterlatte.tidshendelser

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.etterlatte.libs.database.DataSourceBuilder
import no.nav.etterlatte.libs.database.POSTGRES_VERSION
import no.nav.etterlatte.libs.database.migrate
import org.junit.jupiter.api.Test
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import java.time.LocalDate
import java.time.Month
import java.time.YearMonth

class JobbPollerIntegrationTest {
    @Container
    private val postgreSQLContainer =
        PostgreSQLContainer<Nothing>("postgres:$POSTGRES_VERSION")
            .also { it.start() }

    private val dataSource =
        DataSourceBuilder.createDataSource(
            postgreSQLContainer.jdbcUrl,
            postgreSQLContainer.username,
            postgreSQLContainer.password,
        ).also { it.migrate() }

    private val aldersovergangerService = mockk<AldersovergangerService>()
    private val hendelseDao = HendelseDao(dataSource)
    private val jobbTestdata = JobbTestdata(dataSource, hendelseDao)
    private val jobbPoller = JobbPoller(hendelseDao, aldersovergangerService)

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
