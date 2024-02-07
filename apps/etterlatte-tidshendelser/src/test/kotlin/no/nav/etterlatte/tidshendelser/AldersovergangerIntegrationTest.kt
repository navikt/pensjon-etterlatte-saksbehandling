package no.nav.etterlatte.tidshendelser

import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.collections.shouldContainOnly
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import no.nav.etterlatte.tidshendelser.klient.GrunnlagKlient
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import java.time.Month
import java.time.YearMonth
import javax.sql.DataSource

@ExtendWith(DatabaseExtension::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AldersovergangerIntegrationTest {
    private val dataSource: DataSource = DatabaseExtension.dataSource
    private val grunnlagKlient: GrunnlagKlient = mockk<GrunnlagKlient>()
    private val hendelseDao = HendelseDao(dataSource)
    private val jobbTestdata = JobbTestdata(dataSource, hendelseDao)
    private val aldersovergangerService = AldersovergangerService(hendelseDao, grunnlagKlient)

    @AfterEach
    fun afterEach() {
        clearAllMocks()
    }

    @Test
    fun `hvis ingen saker for aktuell maaned saa skal jobb ferdigstilles`() {
        val behandlingsmaaned = YearMonth.of(2024, Month.APRIL)
        val jobb = jobbTestdata.opprettJobb(JobbType.AO_BP20, behandlingsmaaned)

        every { grunnlagKlient.hentSaker(behandlingsmaaned.minusYears(20)) } returns emptyList()

        aldersovergangerService.execute(jobb)

        hendelseDao.hentHendelserForJobb(jobb.id) shouldHaveSize 0
        hendelseDao.hentJobb(jobb.id).status shouldBe JobbStatus.FERDIG
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
        hendelser.map { it.status } shouldContainOnly setOf(HendelseStatus.NY)

        with(hendelseDao.hentJobb(jobb.id)) {
            status shouldBe JobbStatus.STARTET
            versjon shouldBe 2
        }
    }
}
