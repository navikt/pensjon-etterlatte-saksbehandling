package no.nav.etterlatte.sak

import no.nav.etterlatte.DatabaseExtension
import no.nav.etterlatte.common.Enheter
import no.nav.etterlatte.grunnlagsendring.GrunnlagsendringshendelseService
import no.nav.etterlatte.libs.common.behandling.Flyktning
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import java.time.LocalDate

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(DatabaseExtension::class)
internal class SakDaoTest {
    private val dataSource = DatabaseExtension.dataSource
    private lateinit var sakRepo: SakDao
    private lateinit var tilgangService: TilgangService

    @BeforeAll
    fun beforeAll() {
        val connection = dataSource.connection
        sakRepo = SakDao { connection }
        tilgangService = TilgangServiceImpl(SakTilgangDao(dataSource))
    }

    @Test
    fun `kan opprett sak`() {
        val opprettSak = sakRepo.opprettSak("fnr", SakType.BARNEPENSJON, Enheter.PORSGRUNN.enhetNr)

        Assertions.assertEquals(Enheter.PORSGRUNN.enhetNr, opprettSak.enhet)
    }

    @Test
    fun `kan lagre og hente flyktning`() {
        val flyktning = Flyktning(true, LocalDate.of(2024, 1, 1), "Migrert", Grunnlagsopplysning.Pesys.create())
        val opprettSak = sakRepo.opprettSak("fnr", SakType.BARNEPENSJON, Enheter.PORSGRUNN.enhetNr)

        sakRepo.oppdaterFlyktning(opprettSak.id, flyktning)
        val oppdatertFlyktning = sakRepo.finnFlyktningForSak(opprettSak.id)

        Assertions.assertEquals(flyktning, oppdatertFlyktning)
    }

    @Test
    fun `Returnerer null dersom flyktning ikke finnes`() {
        val opprettSak = sakRepo.opprettSak("fnr", SakType.BARNEPENSJON, Enheter.PORSGRUNN.enhetNr)

        Assertions.assertEquals(sakRepo.finnFlyktningForSak(opprettSak.id), null)
    }

    @Test
    fun `Skal kunne oppdatere enhet`() {
        val fnr = "fnr"
        val sak = sakRepo.opprettSak(fnr, SakType.BARNEPENSJON, Enheter.PORSGRUNN.enhetNr)
        val funnetSaker = sakRepo.finnSaker(fnr)
        Assertions.assertEquals(1, funnetSaker.size)
        Assertions.assertEquals(sak.id, funnetSaker[0].id)
        sakRepo.opprettSak(fnr, SakType.OMSTILLINGSSTOENAD, Enheter.PORSGRUNN.enhetNr).also {
            Assertions.assertNotNull(it)
        }
        val funnetSakermed2saker = sakRepo.finnSaker(fnr)
        Assertions.assertEquals(2, funnetSakermed2saker.size)

        val sakerMedNyEnhet =
            funnetSakermed2saker.map {
                GrunnlagsendringshendelseService.SakMedEnhet(it.id, Enheter.EGNE_ANSATTE.enhetNr)
            }

        sakRepo.oppdaterEnheterPaaSaker(sakerMedNyEnhet)

        val sakerMedEgenAnsattEnhet = sakRepo.finnSaker(fnr)
        sakerMedEgenAnsattEnhet.forEach {
            Assertions.assertEquals(Enheter.EGNE_ANSATTE.enhetNr, it.enhet)
        }
    }
}
