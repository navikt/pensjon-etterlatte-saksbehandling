package no.nav.etterlatte.tidshendelser

import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.collections.shouldContainOnly
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import no.nav.etterlatte.libs.database.DataSourceBuilder
import no.nav.etterlatte.libs.database.POSTGRES_VERSION
import no.nav.etterlatte.libs.database.migrate
import no.nav.etterlatte.tidshendelser.klient.GrunnlagKlient
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import java.time.Month
import java.time.YearMonth

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AldersovergangerIntegrationTest {
    // private val pgLogger = LoggerFactory.getLogger("POSTGRES")

    @Container
    private val postgreSQLContainer =
        PostgreSQLContainer<Nothing>("postgres:$POSTGRES_VERSION")
            .also {
                // it.setCommand("postgres", "-c", "fsync=off", "-c", "log_statement=all")
                it.start()
                // it.followOutput(org.testcontainers.containers.output.Slf4jLogConsumer(pgLogger))
            }

    private val dataSource =
        DataSourceBuilder.createDataSource(
            postgreSQLContainer.jdbcUrl,
            postgreSQLContainer.username,
            postgreSQLContainer.password,
        ).also { it.migrate() }

    private val grunnlagKlient: GrunnlagKlient = mockk<GrunnlagKlient>()
    private val hendelseDao = HendelseDao(dataSource)
    private val jobbTestdata = JobbTestdata(dataSource, hendelseDao)
    private val aldersovergangerService = AldersovergangerService(hendelseDao, grunnlagKlient)

    @AfterEach
    fun beforeAll() {
        clearAllMocks()
    }

    @AfterAll
    fun afterAll() {
        postgreSQLContainer.stop()
    }

    @Test
    fun `hvis ingen saker for aktuell maaned saa skal jobb ferdigstilles`() {
        val behandlingsmaaned = YearMonth.of(2024, Month.APRIL)
        val jobb = jobbTestdata.opprettJobb(JobbType.AO_BP20, behandlingsmaaned)

        every { grunnlagKlient.hentSaker(behandlingsmaaned.minusYears(20)) } returns emptyList()

        aldersovergangerService.execute(jobb)

        hendelseDao.hentHendelserForJobb(jobb.id) shouldHaveSize 0
        hendelseDao.hentJobb(jobb.id).status shouldBe "FERDIG"
    }

    @Test
    fun `skal hente saker som skal vurderes og lagre hendelser for hver enkelt`() {
        val behandlingsmaaned = YearMonth.of(2024, Month.MARCH)
        val jobb = jobbTestdata.opprettJobb(JobbType.AO_BP20, behandlingsmaaned)

        every { grunnlagKlient.hentSaker(behandlingsmaaned.minusYears(20)) } returns listOf(1, 2, 3)

        aldersovergangerService.execute(jobb)

        val hendelser = hendelseDao.hentHendelserForJobb(jobb.id)

        hendelser shouldHaveSize 3
        hendelser.map { it.sakId } shouldContainExactlyInAnyOrder listOf(2, 1, 3)
        hendelser.map { it.jobbId } shouldContainOnly setOf(jobb.id)
        hendelser.map { it.status } shouldContainOnly setOf("NY")

        with(hendelseDao.hentJobb(jobb.id)) {
            status shouldBe "STARTET"
            versjon shouldBe 2
        }
    }
}
