package no.nav.etterlatte.tidshendelser

import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.collections.shouldContainOnly
import io.kotest.matchers.collections.shouldHaveSize
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.sak.SakId
import no.nav.etterlatte.tidshendelser.klient.BehandlingKlient
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
class AldersovergangerIntegrationTest(
    dataSource: DataSource,
) {
    private val grunnlagKlient: GrunnlagKlient = mockk<GrunnlagKlient>()
    private val behandlingKlient: BehandlingKlient = mockk<BehandlingKlient>()
    private val hendelseDao = HendelseDao(dataSource)
    private val jobbTestdata = JobbTestdata(dataSource, hendelseDao)
    private val aldersovergangerService = AldersovergangerService(hendelseDao, grunnlagKlient, behandlingKlient)

    @AfterEach
    fun afterEach() {
        clearAllMocks()
    }

    @Test
    fun `hvis ingen saker for aktuell maaned saa skal ingen hendelser opprettes`() {
        val behandlingsmaaned = YearMonth.of(2024, Month.APRIL)
        val jobb = jobbTestdata.opprettJobb(JobbType.AO_BP20, behandlingsmaaned)

        every { grunnlagKlient.hentSaker(behandlingsmaaned.minusYears(20)) } returns emptyList()
        every { behandlingKlient.hentSaker(emptyList()) } returns emptyMap()

        aldersovergangerService.execute(jobb)

        hendelseDao.hentHendelserForJobb(jobb.id) shouldHaveSize 0
    }

    @Test
    fun `skal ignorere saker som ikke er barnepensjon`() {
        val behandlingsmaaned = YearMonth.of(2025, Month.MARCH)
        val jobb = jobbTestdata.opprettJobb(JobbType.AO_BP21, behandlingsmaaned)

        val sakIder: List<SakId> = listOf(1, 2, 3)
        val saker =
            sakIder
                .map {
                    val sakType = if (it != 3L) SakType.BARNEPENSJON else SakType.OMSTILLINGSSTOENAD
                    sak(it, sakType)
                }.associateBy { it.id }

        every { grunnlagKlient.hentSaker(behandlingsmaaned.minusYears(21)) } returns sakIder
        every { behandlingKlient.hentSaker(sakIder) } returns saker

        aldersovergangerService.execute(jobb)
        val hendelserForJobb = hendelseDao.hentHendelserForJobb(jobb.id)
        hendelserForJobb shouldHaveSize 2
        hendelserForJobb.map { it.sakId } shouldContainExactlyInAnyOrder listOf(1, 2)
    }

    @Test
    fun `skal hente saker som skal vurderes og lagre hendelser for hver enkelt`() {
        val behandlingsmaaned = YearMonth.of(2025, Month.MARCH)
        val jobb = jobbTestdata.opprettJobb(JobbType.AO_BP21, behandlingsmaaned)
        val sakIder: List<SakId> = listOf(1, 2, 3)
        val saker =
            sakIder
                .map {
                    sak(it, SakType.BARNEPENSJON)
                }.associateBy { it.id }

        every { grunnlagKlient.hentSaker(behandlingsmaaned.minusYears(21)) } returns sakIder
        every { behandlingKlient.hentSaker(sakIder) } returns saker

        aldersovergangerService.execute(jobb)

        val hendelser = hendelseDao.hentHendelserForJobb(jobb.id)

        hendelser shouldHaveSize 3
        hendelser.map { it.sakId } shouldContainExactlyInAnyOrder listOf(2, 1, 3)
        hendelser.map { it.jobbId } shouldContainOnly setOf(jobb.id)
        hendelser.map { it.status } shouldContainOnly setOf(HendelseStatus.NY)
    }
}
