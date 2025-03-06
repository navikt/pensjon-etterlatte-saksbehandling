package behandling.etteroppgjoer

import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import no.nav.etterlatte.ConnectionAutoclosingTest
import no.nav.etterlatte.DatabaseExtension
import no.nav.etterlatte.User
import no.nav.etterlatte.behandling.etteroppgjoer.EtteroppgjoerDao
import no.nav.etterlatte.behandling.etteroppgjoer.EtteroppgjoerStatus
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
    }

    @Test
    fun `lagre og oppdatere etteroppgjoer`() {
        etteroppgjoerDao.lagerEtteroppgjoer(sak.id, 2024, EtteroppgjoerStatus.VENTER_PAA_SKATTEOPPGJOER)
        val lagret = etteroppgjoerDao.hentEtteroppgjoer(sak.id, 2024)
        with(lagret!!) {
            sakId shouldBe sakId
            inntektsaar shouldBe inntektsaar
            status shouldBe EtteroppgjoerStatus.VENTER_PAA_SKATTEOPPGJOER
        }

        etteroppgjoerDao.lagerEtteroppgjoer(sak.id, 2024, EtteroppgjoerStatus.UNDER_FORBEHANDLING)
        val oppdatert = etteroppgjoerDao.hentEtteroppgjoer(sak.id, 2024)
        with(oppdatert!!) {
            sakId shouldBe sakId
            inntektsaar shouldBe inntektsaar
            status shouldBe EtteroppgjoerStatus.UNDER_FORBEHANDLING
        }
    }
}
