package no.nav.etterlatte.tidshendelser

import io.kotest.matchers.shouldBe
import io.mockk.clearAllMocks
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.slf4j.LoggerFactory
import java.time.Month
import java.time.YearMonth
import javax.sql.DataSource

@ExtendWith(TidshendelserDatabaseExtension::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class HendelsePollerIntegrationTest(dataSource: DataSource) {
    private val logger = LoggerFactory.getLogger(this::class.java)

    private val hendelseDao = HendelseDao(dataSource)
    private val jobbTestdata = JobbTestdata(dataSource, hendelseDao)
    private val hendelsePoller =
        HendelsePoller(
            hendelseDao,
            HendelsePublisher { key, _ -> logger.info("Publiserer melding [key=$key]") },
        )

    @AfterEach
    fun afterEach() {
        clearAllMocks()
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
                it.sakId <= 12 -> it.status shouldBe HendelseStatus.SENDT
                else -> it.status shouldBe HendelseStatus.NY
            }
        }
    }
}
