package no.nav.etterlatte.behandling.jobs.brevjobber

import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import no.nav.etterlatte.ConnectionAutoclosingTest
import no.nav.etterlatte.Context
import no.nav.etterlatte.DatabaseContextTest
import no.nav.etterlatte.DatabaseExtension
import no.nav.etterlatte.Self
import no.nav.etterlatte.libs.common.Enhetsnummer
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.sak.SakId
import no.nav.etterlatte.libs.testdata.grunnlag.SOEKER_FOEDSELSNUMMER
import no.nav.etterlatte.sak.SakSkrivDao
import no.nav.etterlatte.sak.SakendringerDao
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import javax.sql.DataSource

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(DatabaseExtension::class)
internal class BrevutsendelseJobTest(
    val dataSource: DataSource,
) {
    private val kontekst = Context(Self(this::class.java.simpleName), DatabaseContextTest(mockk()), mockk(), null)

    private val arbeidstabellDao: ArbeidstabellDao = ArbeidstabellDao(ConnectionAutoclosingTest(dataSource))
    private val sakDao: SakSkrivDao = SakSkrivDao(SakendringerDao(ConnectionAutoclosingTest(dataSource)) { mockk() })
    private val brevutsendelseService: BrevutsendelseService = mockk()

    private val brevutsendelseJob: BrevutsendelseJob =
        BrevutsendelseJob(
            arbeidstabellDao = arbeidstabellDao,
            brevutsendelseService = brevutsendelseService,
        )

    @BeforeEach
    fun beforeEach() {
    }

    @AfterEach
    fun afterEach() {
        dataSource.connection.use {
            it.prepareStatement("TRUNCATE arbeidstabell CASCADE;").execute()
        }
    }

    @Test
    fun `skal prosessere brevutsendelse og oppdatere status til ferdig`() {
        val sak = sakDao.opprettSak(SOEKER_FOEDSELSNUMMER.value, SakType.BARNEPENSJON, Enhetsnummer("1234"))
        val brevutsendelse = arbeidstabellDao.opprettJobb(nyArbeidsjobb(sak.id))

        every { brevutsendelseService.prosesserBrevutsendelse(any(), any()) } just runs

        brevutsendelseJob.setupKontekstAndRun(kontekst)

        val oppdatertBrevutsendelse = arbeidstabellDao.hentJobb(brevutsendelse.id)
        oppdatertBrevutsendelse?.status shouldBe ArbeidStatus.FERDIG

        verify {
            brevutsendelseService.prosesserBrevutsendelse(match { it.status == ArbeidStatus.PAAGAAENDE }, any())
        }
    }

    @Test
    fun `skal prosessere brevutsendelse og oppdatere status til feilet ved feil`() {
        val sak = sakDao.opprettSak(SOEKER_FOEDSELSNUMMER.value, SakType.BARNEPENSJON, Enhetsnummer("1234"))
        val brevutsendelse = arbeidstabellDao.opprettJobb(nyArbeidsjobb(sak.id))

        every { brevutsendelseService.prosesserBrevutsendelse(any(), any()) } throws Exception("Brevutsendelse feilet")

        brevutsendelseJob.setupKontekstAndRun(kontekst)

        val oppdatertBrevutsendelse = arbeidstabellDao.hentJobb(brevutsendelse.id)
        oppdatertBrevutsendelse?.status shouldBe ArbeidStatus.FEILET
    }

    // TODO flere tester for ulike scenarioer

    private fun nyArbeidsjobb(sakId: SakId): Arbeidsjobb =
        lagNyArbeidsJobb(
            sakId = sakId,
            type = JobbType.TREKKPLIKT_2025,
            merknad = null,
        )
}
