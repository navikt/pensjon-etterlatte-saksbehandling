package behandling.etteroppgjoer

import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import no.nav.etterlatte.ConnectionAutoclosingTest
import no.nav.etterlatte.DatabaseExtension
import no.nav.etterlatte.User
import no.nav.etterlatte.behandling.etteroppgjoer.Etteroppgjoer
import no.nav.etterlatte.behandling.etteroppgjoer.EtteroppgjoerDao
import no.nav.etterlatte.behandling.etteroppgjoer.EtteroppgjoerStatus
import no.nav.etterlatte.behandling.etteroppgjoer.EtteroppgjoerSvarfrist
import no.nav.etterlatte.behandling.etteroppgjoer.forbehandling.EtteroppgjoerForbehandling
import no.nav.etterlatte.behandling.etteroppgjoer.forbehandling.EtteroppgjoerForbehandlingDao
import no.nav.etterlatte.common.Enheter
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.behandling.etteroppgjoer.EtteroppgjoerForbehandlingStatus
import no.nav.etterlatte.libs.common.periode.Periode
import no.nav.etterlatte.libs.common.sak.Sak
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.nyKontekstMedBrukerOgDatabase
import no.nav.etterlatte.sak.SakSkrivDao
import no.nav.etterlatte.sak.SakendringerDao
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.extension.ExtendWith
import java.time.LocalDate
import java.time.YearMonth
import java.util.UUID
import javax.sql.DataSource

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(DatabaseExtension::class)
class EtteroppgjoerDaoTest(
    val dataSource: DataSource,
) {
    private lateinit var sakSkrivDao: SakSkrivDao
    private lateinit var etteroppgjoerDao: EtteroppgjoerDao
    private lateinit var etteroppgjoerForbehandlingDao: EtteroppgjoerForbehandlingDao

    private lateinit var sak: Sak
    private lateinit var sak2: Sak

    @BeforeAll
    fun setup() {
        sakSkrivDao = SakSkrivDao(SakendringerDao(ConnectionAutoclosingTest(dataSource)))
        etteroppgjoerDao = EtteroppgjoerDao(ConnectionAutoclosingTest(dataSource))
        etteroppgjoerForbehandlingDao = EtteroppgjoerForbehandlingDao(ConnectionAutoclosingTest(dataSource))

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
                fnr = "bruker1",
                type = SakType.OMSTILLINGSSTOENAD,
                enhet = Enheter.PORSGRUNN.enhetNr,
            )

        sak2 =
            sakSkrivDao.opprettSak(
                fnr = "bruker2",
                type = SakType.OMSTILLINGSSTOENAD,
                enhet = Enheter.AALESUND.enhetNr,
            )
    }

    @Test
    fun `skal hente etteroppgjoer som venter paa skatteoppgjoer`() {
        etteroppgjoerDao.lagreEtteroppgjoer(Etteroppgjoer(sak.id, 2024, EtteroppgjoerStatus.VENTER_PAA_SKATTEOPPGJOER))
        etteroppgjoerDao.lagreEtteroppgjoer(Etteroppgjoer(sak.id, 2025, EtteroppgjoerStatus.VENTER_PAA_SKATTEOPPGJOER))
        etteroppgjoerDao.lagreEtteroppgjoer(Etteroppgjoer(sak2.id, 2024, EtteroppgjoerStatus.MOTTATT_SKATTEOPPGJOER))

        etteroppgjoerDao.hentEtteroppgjoerSomVenterPaaSkatteoppgjoer(10) shouldBe
            listOf(
                Etteroppgjoer(sak.id, 2024, EtteroppgjoerStatus.VENTER_PAA_SKATTEOPPGJOER),
                Etteroppgjoer(sak.id, 2025, EtteroppgjoerStatus.VENTER_PAA_SKATTEOPPGJOER),
            )
    }

    @Test
    fun `oppdater ferdigstilt forbehandlingId`() {
        val forbehandlingId = UUID.randomUUID()
        val inntektsaar = 2024
        etteroppgjoerDao.lagreEtteroppgjoer(
            Etteroppgjoer(
                sakId = sak.id,
                inntektsaar = inntektsaar,
                status = EtteroppgjoerStatus.VENTER_PAA_SVAR,
                sisteFerdigstilteForbehandling = forbehandlingId,
            ),
        )

        val etteroppgjoer = etteroppgjoerDao.hentEtteroppgjoerForInntektsaar(sak.id, inntektsaar)
        etteroppgjoer!!.sisteFerdigstilteForbehandling shouldBe forbehandlingId
    }

    @Test
    fun `skal hente etteroppgjoer for inntektsaar`() {
        val forbehandlingId = UUID.randomUUID()
        val inntektsaar = 2024

        // negative null result
        assertDoesNotThrow { etteroppgjoerDao.hentEtteroppgjoerForInntektsaar(sak.id, 2024) }

        etteroppgjoerDao.lagreEtteroppgjoer(
            Etteroppgjoer(
                sakId = sak.id,
                inntektsaar = inntektsaar,
                status = EtteroppgjoerStatus.VENTER_PAA_SVAR,
                sisteFerdigstilteForbehandling = forbehandlingId,
            ),
        )
        assertDoesNotThrow { etteroppgjoerDao.hentEtteroppgjoerForInntektsaar(sak.id, 2024) }
    }

    @Test
    fun `skal hente etteroppgjoer med svarfrist utloept`() {
        val forbehandlinger =
            listOf(
                opprettForbehandling(EtteroppgjoerForbehandlingStatus.FERDIGSTILT, 2024, LocalDate.now().minusMonths(2)),
                opprettForbehandling(EtteroppgjoerForbehandlingStatus.FERDIGSTILT, 2025, LocalDate.now().minusMonths(2)),
                opprettForbehandling(EtteroppgjoerForbehandlingStatus.FERDIGSTILT, 2026, LocalDate.now()),
            )

        forbehandlinger.forEach { etteroppgjoerForbehandlingDao.lagreForbehandling(it) }
        forbehandlinger.forEach {
            etteroppgjoerDao.lagreEtteroppgjoer(
                Etteroppgjoer(
                    sakId = sak.id,
                    inntektsaar = it.aar,
                    status = EtteroppgjoerStatus.VENTER_PAA_SVAR,
                    sisteFerdigstilteForbehandling = it.id,
                ),
            )
        }

        val etteroppgjoerMedSvarfristUtloept =
            forbehandlinger
                .filter { it.aar != 2026 }
                .map {
                    Etteroppgjoer(
                        sakId = sak.id,
                        inntektsaar = it.aar,
                        status = EtteroppgjoerStatus.VENTER_PAA_SVAR,
                        sisteFerdigstilteForbehandling = it.id,
                    )
                }

        etteroppgjoerDao
            .hentEtteroppgjoerMedSvarfristUtloept(EtteroppgjoerSvarfrist.EN_MND)
            .shouldContainExactlyInAnyOrder(etteroppgjoerMedSvarfristUtloept)
    }

    @Test
    fun `lagre og oppdatere etteroppgjoer`() {
        val uuid = UUID.randomUUID()

        etteroppgjoerDao.lagreEtteroppgjoer(
            Etteroppgjoer(
                sakId = sak.id,
                inntektsaar = 2024,
                status = EtteroppgjoerStatus.VENTER_PAA_SKATTEOPPGJOER,
                sisteFerdigstilteForbehandling = uuid,
            ),
        )
        val lagret = etteroppgjoerDao.hentEtteroppgjoerForInntektsaar(sak.id, 2024)
        with(lagret!!) {
            sakId shouldBe sakId
            inntektsaar shouldBe inntektsaar
            status shouldBe EtteroppgjoerStatus.VENTER_PAA_SKATTEOPPGJOER
            harOpphoer shouldBe false
        }

        etteroppgjoerDao.lagreEtteroppgjoer(
            Etteroppgjoer(
                sak.id,
                2024,
                EtteroppgjoerStatus.UNDER_FORBEHANDLING,
                true,
                uuid,
            ),
        )

        val oppdatert = etteroppgjoerDao.hentEtteroppgjoerForInntektsaar(sak.id, 2024)
        with(oppdatert!!) {
            sakId shouldBe sakId
            inntektsaar shouldBe inntektsaar
            status shouldBe EtteroppgjoerStatus.UNDER_FORBEHANDLING
            harOpphoer shouldBe true
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

    private fun opprettForbehandling(
        status: EtteroppgjoerForbehandlingStatus,
        inntektsaar: Int,
        varselbrevSendt: LocalDate,
    ): EtteroppgjoerForbehandling =
        EtteroppgjoerForbehandling(
            id = UUID.randomUUID(),
            status = status,
            aar = inntektsaar,
            opprettet = Tidspunkt.now(),
            sak = sak,
            brevId = null,
            innvilgetPeriode = Periode(YearMonth.of(inntektsaar, 1), YearMonth.of(inntektsaar, 12)),
            kopiertFra = null,
            sisteIverksatteBehandlingId = UUID.randomUUID(),
            harMottattNyInformasjon = null,
            endringErTilUgunstForBruker = null,
            beskrivelseAvUgunst = null,
            varselbrevSendt = varselbrevSendt,
            opphoerSkyldesDoedsfall = null,
            opphoerSkyldesDoedsfallIEtteroppgjoersaar = null,
        )
}
