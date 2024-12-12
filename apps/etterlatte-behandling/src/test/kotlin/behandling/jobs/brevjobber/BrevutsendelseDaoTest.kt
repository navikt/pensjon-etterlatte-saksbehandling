package no.nav.etterlatte.behandling.jobs.brevjobber

import io.kotest.matchers.shouldBe
import io.mockk.mockk
import no.nav.etterlatte.ConnectionAutoclosingTest
import no.nav.etterlatte.DatabaseExtension
import no.nav.etterlatte.common.Enheter
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.sak.Sak
import no.nav.etterlatte.libs.common.sak.SakId
import no.nav.etterlatte.sak.SakSkrivDao
import no.nav.etterlatte.sak.SakendringerDao
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import javax.sql.DataSource

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(DatabaseExtension::class)
internal class BrevutsendelseDaoTest(
    val dataSource: DataSource,
) {
    private lateinit var brevutsendelseDao: BrevutsendelseDao
    private lateinit var sakRepo: SakSkrivDao

    @BeforeAll
    fun beforeAll() {
        brevutsendelseDao = BrevutsendelseDao(ConnectionAutoclosingTest(dataSource = dataSource))
        sakRepo = SakSkrivDao(SakendringerDao(ConnectionAutoclosingTest(dataSource)) { mockk() })
    }

    @AfterEach
    fun afterEach() {
        dataSource.connection.use {
            it.prepareStatement("TRUNCATE brevutsendelse CASCADE;").execute()
        }
    }

    @Test
    fun `kan opprette jobb`() {
    }

    @Test
    fun `Kan hente enkel jobb med id`() {
        val sakId = opprettSak("123").id
        val brevutsendelse = defaultBrevutsendelse(sakId)
        brevutsendelseDao.opprettJobb(brevutsendelse)

        val nyJobb = brevutsendelseDao.hentJobb(brevutsendelse.id)
        nyJobb shouldBe brevutsendelse
    }

    @Test fun `Kan ekskludere spesifikke jobber med SakId`() {
        val jobb1 = brevutsendelseDao.opprettJobb(defaultBrevutsendelse(opprettSak("123").id))
        val jobb2 = brevutsendelseDao.opprettJobb(defaultBrevutsendelse(opprettSak("234").id))
        val jobb3 = brevutsendelseDao.opprettJobb(defaultBrevutsendelse(opprettSak("345").id))

        brevutsendelseDao.hentNyeJobber(3).size shouldBe 3

        val sakerSomIkkeErEkskludert = brevutsendelseDao.hentNyeJobber(3, ekskluderteSaker = listOf(jobb1.sakId))
        sakerSomIkkeErEkskludert.size shouldBe 2
        sakerSomIkkeErEkskludert[0] shouldBe jobb2
        sakerSomIkkeErEkskludert[1] shouldBe jobb3
    }

    @Test fun `Kan hente spesifikke nye jobber med SakId`() {
        val jobb1 = brevutsendelseDao.opprettJobb(defaultBrevutsendelse(opprettSak("123").id))
        val jobb2 = brevutsendelseDao.opprettJobb(defaultBrevutsendelse(opprettSak("234").id))
        brevutsendelseDao.opprettJobb(defaultBrevutsendelse(opprettSak("345").id))

        brevutsendelseDao.hentNyeJobber(3).size shouldBe 3
        brevutsendelseDao.hentNyeJobber(3, listOf(jobb1.sakId)).size shouldBe 1

        val spesifikkeJobber = brevutsendelseDao.hentNyeJobber(3, listOf(jobb1.sakId, jobb2.sakId))

        spesifikkeJobber.size shouldBe 2
        spesifikkeJobber.get(0) shouldBe jobb1
        spesifikkeJobber.get(1) shouldBe jobb2
    }

    @Test
    fun `Kan hente spesifikk antall nye jobber`() {
        brevutsendelseDao.opprettJobb(defaultBrevutsendelse(opprettSak("123").id))
        brevutsendelseDao.opprettJobb(defaultBrevutsendelse(opprettSak("234").id))
        brevutsendelseDao.opprettJobb(defaultBrevutsendelse(opprettSak("345").id))

        val alleJobber = brevutsendelseDao.hentNyeJobber(3)
        alleJobber.size shouldBe 3

        val spesifikkAntallJobber = brevutsendelseDao.hentNyeJobber(2, emptyList(), emptyList())
        spesifikkAntallJobber.size shouldBe 2
    }

    @Test
    fun `Kan opprette jobb og hente nye jobber`() {
        val sakId = opprettSak("123").id
        brevutsendelseDao.opprettJobb(defaultBrevutsendelse(sakId))

        val nyeJobber = brevutsendelseDao.hentNyeJobber(1, emptyList(), emptyList())

        nyeJobber.size shouldBe 1
        nyeJobber.first().sakId shouldBe sakId
    }

    private fun defaultBrevutsendelse(sakId: SakId): Brevutsendelse =
        opprettNyBrevutsendelse(
            sakId,
            merknad = "testmerknad",
            type = JobbType.TREKKPLIKT_2025,
        )

    private fun opprettSak(fnr: String): Sak = sakRepo.opprettSak(fnr, SakType.BARNEPENSJON, Enheter.defaultEnhet.enhetNr)
}
