package no.nav.etterlatte.tidshendelser

import io.kotest.matchers.shouldBe
import io.mockk.clearAllMocks
import no.nav.etterlatte.libs.database.DataSourceBuilder
import no.nav.etterlatte.libs.database.POSTGRES_VERSION
import no.nav.etterlatte.libs.database.migrate
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import java.time.Month
import java.time.YearMonth

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class HendelsePollerIntegrationTest {
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

    private val hendelseDao = HendelseDao(dataSource)
    private val jobbTestdata = JobbTestdata(dataSource, hendelseDao)
    private val hendelsePoller = HendelsePoller(hendelseDao)

    @AfterEach
    fun beforeAll() {
        clearAllMocks()
    }

    @AfterAll
    fun afterAll() {
        postgreSQLContainer.stop()
    }

    @Test
    fun `poll skal sjekke for hendelser med status NY og starte behandling`() {
        val behandlingsmaaned = YearMonth.of(2024, Month.FEBRUARY)
        val jobb = jobbTestdata.opprettJobb(JobbType.AO_BP18, behandlingsmaaned)
        hendelseDao.opprettHendelserForSaker(jobb.id, listOf(5, 7, 12, 20, 33, 50), Steg.IDENTIFISERT_SAK)

        hendelsePoller.poll(3)

        val hendelserEtterPoll = hendelseDao.hentHendelserForJobb(jobb.id)

        hendelserEtterPoll.forEach {
            when {
                it.sakId <= 12 -> it.status shouldBe "VURDER_LOEPENDE_YTELSE"
                else -> it.status shouldBe HENDELSE_STATUS_OPPRETTET
            }
        }
    }
}
