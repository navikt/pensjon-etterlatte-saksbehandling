package behandling.etteroppgjoer

import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import no.nav.etterlatte.ConnectionAutoclosingTest
import no.nav.etterlatte.DatabaseExtension
import no.nav.etterlatte.User
import no.nav.etterlatte.behandling.etteroppgjoer.Etteroppgjoer
import no.nav.etterlatte.behandling.etteroppgjoer.EtteroppgjoerDao
import no.nav.etterlatte.behandling.etteroppgjoer.EtteroppgjoerStatus
import no.nav.etterlatte.behandling.jobs.etteroppgjoer.EtteroppgjoerFilter
import no.nav.etterlatte.common.Enheter
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.sak.Sak
import no.nav.etterlatte.nyKontekstMedBrukerOgDatabase
import no.nav.etterlatte.sak.SakSkrivDao
import no.nav.etterlatte.sak.SakendringerDao
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import javax.sql.DataSource

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(DatabaseExtension::class)
class EtteroppgjoerDaoTest(
    val dataSource: DataSource,
) {
    private lateinit var sakSkrivDao: SakSkrivDao
    private lateinit var etteroppgjoerDao: EtteroppgjoerDao

    private lateinit var sak: Sak
    private lateinit var sak2: Sak

    @BeforeAll
    fun setup() {
        sakSkrivDao = SakSkrivDao(SakendringerDao(ConnectionAutoclosingTest(dataSource)))
        etteroppgjoerDao = EtteroppgjoerDao(ConnectionAutoclosingTest(dataSource))

        nyKontekstMedBrukerOgDatabase(
            mockk<User>().also { every { it.name() } returns this::class.java.simpleName },
            dataSource,
        )
    }

    @BeforeEach
    fun resetTabell() {
        dataSource.connection.use {
            it.prepareStatement("""TRUNCATE TABLE etteroppgjoer CASCADE""").executeUpdate()
            it.prepareStatement("""TRUNCATE TABLE sak CASCADE """).executeUpdate()
        }
        sak =
            sakSkrivDao.opprettSak(
                fnr = "en bruker",
                type = SakType.OMSTILLINGSSTOENAD,
                enhet = Enheter.defaultEnhet.enhetNr,
            )

        sak2 =
            sakSkrivDao.opprettSak(
                fnr = "en bruker",
                type = SakType.OMSTILLINGSSTOENAD,
                enhet = Enheter.defaultEnhet.enhetNr,
            )
    }

    @Test
    fun `lagre og oppdatere etteroppgjoer`() {
        etteroppgjoerDao.lagreEtteroppgjoer(
            Etteroppgjoer(sak.id, 2024, EtteroppgjoerStatus.VENTER_PAA_SKATTEOPPGJOER, false, false, false, false),
        )
        val lagret = etteroppgjoerDao.hentEtteroppgjoerForInntektsaar(sak.id, 2024)
        with(lagret!!) {
            sakId shouldBe sakId
            inntektsaar shouldBe inntektsaar
            status shouldBe EtteroppgjoerStatus.VENTER_PAA_SKATTEOPPGJOER
            harSanksjon shouldBe false
            harOpphoer shouldBe false
            harBosattUtland shouldBe false
            harInstitusjonsopphold shouldBe false
        }

        etteroppgjoerDao.lagreEtteroppgjoer(Etteroppgjoer(sak.id, 2024, EtteroppgjoerStatus.UNDER_FORBEHANDLING, true, true, true, true))
        val oppdatert = etteroppgjoerDao.hentEtteroppgjoerForInntektsaar(sak.id, 2024)
        with(oppdatert!!) {
            sakId shouldBe sakId
            inntektsaar shouldBe inntektsaar
            status shouldBe EtteroppgjoerStatus.UNDER_FORBEHANDLING
            harSanksjon shouldBe true
            harOpphoer shouldBe true
            harBosattUtland shouldBe true
            harInstitusjonsopphold shouldBe true
        }
    }

    @Test
    fun `hent etteroppgjoer for status`() {
        etteroppgjoerDao.lagreEtteroppgjoer(Etteroppgjoer(sak.id, 2024, EtteroppgjoerStatus.VENTER_PAA_SKATTEOPPGJOER))
        etteroppgjoerDao.lagreEtteroppgjoer(Etteroppgjoer(sak.id, 2024, EtteroppgjoerStatus.VENTER_PAA_SKATTEOPPGJOER))

        // negative
        etteroppgjoerDao.hentEtteroppgjoerForStatus(EtteroppgjoerStatus.MOTTATT_SKATTEOPPGJOER, 2024) shouldBe emptyList()

        val resultat = etteroppgjoerDao.hentEtteroppgjoerForStatus(EtteroppgjoerStatus.VENTER_PAA_SKATTEOPPGJOER, 2024)
        with(resultat) {
            size shouldBe 1
            first().inntektsaar shouldBe 2024
        }
    }

    @Test
    fun `hent etteroppgjoer for filter`() {
        val inntektsaar = 2024
        val enkelSak = Etteroppgjoer(sak.id, inntektsaar, EtteroppgjoerStatus.VENTER_PAA_SKATTEOPPGJOER, false, false, false, false)
        val annenSak = Etteroppgjoer(sak2.id, inntektsaar, EtteroppgjoerStatus.VENTER_PAA_SKATTEOPPGJOER, true, false, false, false)
        etteroppgjoerDao.lagreEtteroppgjoer(enkelSak)
        etteroppgjoerDao.lagreEtteroppgjoer(annenSak)

        val resultat = etteroppgjoerDao.hentEtteroppgjoerForFilter(EtteroppgjoerFilter.ENKEL, inntektsaar)
        with(resultat) {
            size shouldBe 1
            first().inntektsaar shouldBe inntektsaar
            first().sakId shouldBe enkelSak.sakId
        }
    }
}
