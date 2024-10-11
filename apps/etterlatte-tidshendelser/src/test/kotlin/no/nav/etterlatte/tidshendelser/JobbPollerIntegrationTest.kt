package no.nav.etterlatte.tidshendelser

import io.kotest.matchers.shouldBe
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.etterlatte.behandling.sakId1
import no.nav.etterlatte.behandling.sakId2
import no.nav.etterlatte.behandling.sakId3
import no.nav.etterlatte.libs.common.feilhaandtering.InternfeilException
import no.nav.etterlatte.libs.common.sak.SakId
import no.nav.etterlatte.tidshendelser.regulering.ReguleringService
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.RegisterExtension
import java.time.LocalDate
import java.time.Month
import java.time.YearMonth
import javax.sql.DataSource

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class JobbPollerIntegrationTest(
    dataSource: DataSource,
) {
    companion object {
        @RegisterExtension
        private val dbExtension = DatabaseExtension()
    }

    private val aldersovergangerService = mockk<AldersovergangerService>()
    private val omstillingsstoenadService = mockk<OmstillingsstoenadService>()
    private val reguleringService = mockk<ReguleringService>()
    private val hendelseDao = HendelseDao(dataSource)
    private val jobbTestdata = JobbTestdata(dataSource, hendelseDao)
    private val jobbPoller =
        JobbPoller(hendelseDao, aldersovergangerService, omstillingsstoenadService, reguleringService)

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
    fun `skal kun hente en jobb for dagens dato, og trigge den`() {
        val jobb1 = jobbTestdata.opprettJobb(JobbType.AO_BP20, YearMonth.of(2024, Month.MARCH), LocalDate.now())
        val jobb2 = jobbTestdata.opprettJobb(JobbType.AO_BP20, YearMonth.of(2024, Month.MARCH), LocalDate.now())

        every { aldersovergangerService.execute(jobb1) } returns emptyList()

        jobbPoller.poll()

        verify { aldersovergangerService.execute(jobb1) }
        verify(exactly = 0) { aldersovergangerService.execute(jobb2) }

        hendelseDao.hentJobb(jobb1.id).status shouldBe JobbStatus.FERDIG
        hendelseDao.hentJobb(jobb2.id).status shouldBe JobbStatus.NY
    }

    @Test
    fun `skal ikke ferdigstille jobb som har opprettet hendelser`() {
        val jobb = jobbTestdata.opprettJobb(JobbType.AO_BP20, YearMonth.of(2024, Month.APRIL), LocalDate.now())

        every { aldersovergangerService.execute(jobb) } returns listOf(sakId1)

        jobbPoller.poll()

        verify { aldersovergangerService.execute(jobb) }

        hendelseDao.hentJobb(jobb.id).status shouldBe JobbStatus.STARTET
    }

    @Test
    fun `skal ikke trigge jobb som ikke er for dagens dato`() {
        jobbTestdata.opprettJobb(JobbType.AO_BP20, YearMonth.of(2024, Month.MARCH), LocalDate.now().minusDays(1))

        jobbPoller.poll()

        verify(exactly = 0) { aldersovergangerService.execute(any()) }
    }

    @Test
    fun `feil kastet under henting av saker for jobb skal resette jobbens status`() {
        val jobb = jobbTestdata.opprettJobb(JobbType.AO_BP20, YearMonth.of(2024, Month.APRIL), LocalDate.now())

        every { aldersovergangerService.execute(jobb) } throws InternfeilException("Whoops")
        jobbPoller.poll()

        verify(exactly = 1) { aldersovergangerService.execute(any()) }
        hendelseDao.hentJobb(jobb.id).status shouldBe JobbStatus.NY
    }

    @Test
    fun `feil kastet under henting av saker for jobb skal ikke resette status hvis hendelser er laget allerede`() {
        val jobb = jobbTestdata.opprettJobb(JobbType.AO_BP20, YearMonth.of(2024, Month.APRIL), LocalDate.now())

        // Denne er litt kunstig, siden dette vil i prinsippet skje i løpet av jobbPoller.poll().
        // Men vi vil teste at tilbakestilling ikke skjer hvis vi ikke kan gjøre det trygt
        hendelseDao.opprettHendelserForSaker(
            jobbId = jobb.id,
            saksIDer = listOf(sakId1, sakId2, sakId3, SakId(4)),
            steg = Steg.IDENTIFISERT_SAK,
        )
        every { aldersovergangerService.execute(jobb) } throws InternfeilException("Å nei")

        jobbPoller.poll()
        verify(exactly = 1) { aldersovergangerService.execute(any()) }

        // Ikke tilbakestilt
        hendelseDao.hentJobb(jobb.id).status shouldBe JobbStatus.STARTET
    }
}
