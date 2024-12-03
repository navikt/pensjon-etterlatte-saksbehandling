package no.nav.etterlatte.behandling.jobs.brevjobber

import io.kotest.matchers.shouldBe
import io.mockk.mockk
import no.nav.etterlatte.ConnectionAutoclosingTest
import no.nav.etterlatte.DatabaseExtension
import no.nav.etterlatte.common.Enheter
import no.nav.etterlatte.libs.common.behandling.SakType
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
internal class ArbeidstabellDaoTest(
    val dataSource: DataSource,
) {
    private lateinit var arbeidstabellDao: ArbeidstabellDao
    private lateinit var sakRepo: SakSkrivDao

    @BeforeAll
    fun beforeAll() {
        arbeidstabellDao = ArbeidstabellDao(ConnectionAutoclosingTest(dataSource = dataSource))
        sakRepo = SakSkrivDao(SakendringerDao(ConnectionAutoclosingTest(dataSource)) { mockk() })
    }

    @AfterEach
    fun afterEach() {
        dataSource.connection.use {
            it.prepareStatement("TRUNCATE arbeidstabell CASCADE;").execute()
        }
    }

    @Test
    fun `Kan opprette jobber og hente jobber`() {
        val sak = sakRepo.opprettSak("123", SakType.BARNEPENSJON, Enheter.defaultEnhet.enhetNr)
        val sakId = sak.id

        val testjobb = lagNyArbeidsJobb(sakId, merknad = "testmerknad", type = JobbType.TREKKPLIKT_2025)
        arbeidstabellDao.opprettJobb(testjobb)

        val nyeJobber = arbeidstabellDao.hentKlareJobber()

        nyeJobber.size shouldBe 1
        nyeJobber.first() shouldBe testjobb

        val enSakMedFlerejobber = lagNyArbeidsJobb(sakId, merknad = "jobbtoforsak", type = JobbType.TREKKPLIKT_2025)
        arbeidstabellDao.opprettJobb(enSakMedFlerejobber)

        val sakMedToJobber = arbeidstabellDao.hentKlareJobber()
        sakMedToJobber.size shouldBe 2
    }
}
