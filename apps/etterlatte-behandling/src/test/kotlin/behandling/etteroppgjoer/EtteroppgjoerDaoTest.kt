package behandling.etteroppgjoer

import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import no.nav.etterlatte.ConnectionAutoclosingTest
import no.nav.etterlatte.DatabaseExtension
import no.nav.etterlatte.User
import no.nav.etterlatte.behandling.etteroppgjoer.ETTEROPPGJOER_AAR
import no.nav.etterlatte.behandling.etteroppgjoer.Etteroppgjoer
import no.nav.etterlatte.behandling.etteroppgjoer.EtteroppgjoerDao
import no.nav.etterlatte.behandling.etteroppgjoer.EtteroppgjoerStatus
import no.nav.etterlatte.behandling.etteroppgjoer.EtteroppgjoerSvarfrist
import no.nav.etterlatte.behandling.etteroppgjoer.forbehandling.EtteroppgjoerForbehandling
import no.nav.etterlatte.behandling.etteroppgjoer.forbehandling.EtteroppgjoerForbehandlingDao
import no.nav.etterlatte.behandling.jobs.etteroppgjoer.EtteroppgjoerFilter
import no.nav.etterlatte.behandling.jobs.etteroppgjoer.FilterVerdi
import no.nav.etterlatte.common.Enheter
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.behandling.etteroppgjoer.EtteroppgjoerForbehandlingStatus
import no.nav.etterlatte.libs.common.oppgave.OppgaveKilde
import no.nav.etterlatte.libs.common.oppgave.OppgaveType
import no.nav.etterlatte.libs.common.oppgave.opprettNyOppgaveMedReferanseOgSak
import no.nav.etterlatte.libs.common.periode.Periode
import no.nav.etterlatte.libs.common.sak.Sak
import no.nav.etterlatte.libs.common.sak.SakId
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.tidspunkt.setTidspunkt
import no.nav.etterlatte.libs.database.setSakId
import no.nav.etterlatte.nyKontekstMedBrukerOgDatabase
import no.nav.etterlatte.sak.SakSkrivDao
import no.nav.etterlatte.sak.SakendringerDao
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
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
    fun `skal hente saker med etteroppgjoer for spesifikke enheter`() {
        etteroppgjoerDao.lagreEtteroppgjoer(Etteroppgjoer(sak.id, 2024, EtteroppgjoerStatus.MOTTATT_SKATTEOPPGJOER))
        etteroppgjoerDao.lagreEtteroppgjoer(Etteroppgjoer(sak2.id, 2024, EtteroppgjoerStatus.MOTTATT_SKATTEOPPGJOER))

        val etteroppgjoerSak =
            etteroppgjoerDao
                .hentEtteroppgjoerSakerIBulk(
                    inntektsaar = 2024,
                    antall = 2,
                    etteroppgjoerFilter = EtteroppgjoerFilter.ENKEL,
                    status = EtteroppgjoerStatus.MOTTATT_SKATTEOPPGJOER,
                    spesifikkeSaker = emptyList(),
                    ekskluderteSaker = emptyList(),
                    spesifikkeEnheter = listOf(Enheter.PORSGRUNN.enhetNr.toString()),
                ).firstOrNull()

        SakId(etteroppgjoerSak!!.sakId) shouldBe sak.id
    }

    @Test
    fun `skal hente saker både med og uten aktivitetskrav hvis filter er satt til don't care`() {
        etteroppgjoerDao.lagreEtteroppgjoer(
            Etteroppgjoer(
                sakId = sak.id,
                inntektsaar = 2024,
                status = EtteroppgjoerStatus.MOTTATT_SKATTEOPPGJOER,
                harAktivitetskrav = true,
            ),
        )
        etteroppgjoerDao.lagreEtteroppgjoer(
            Etteroppgjoer(
                sakId = sak2.id,
                inntektsaar = 2024,
                status = EtteroppgjoerStatus.MOTTATT_SKATTEOPPGJOER,
                harAktivitetskrav = false,
            ),
        )
        assertEquals(FilterVerdi.DONT_CARE, EtteroppgjoerFilter.MED_AKTIVITET_OG_SKJERMET.harAktivitetskrav)

        val saker =
            etteroppgjoerDao.hentEtteroppgjoerSakerIBulk(
                inntektsaar = 2024,
                antall = 10,
                status = EtteroppgjoerStatus.MOTTATT_SKATTEOPPGJOER,
                etteroppgjoerFilter = EtteroppgjoerFilter.MED_AKTIVITET_OG_SKJERMET,
                spesifikkeSaker = listOf(),
                ekskluderteSaker = listOf(),
                spesifikkeEnheter = listOf(),
            )
        saker.size shouldBe 2
        saker shouldContainExactlyInAnyOrder listOf(sak.id, sak2.id)
    }

    @Test
    fun `skal ignorere saker med oppgaver for etteroppgjør som har tom referanse og ikke er ferdigstilt`() {
        etteroppgjoerDao.lagreEtteroppgjoer(Etteroppgjoer(sak.id, 2024, EtteroppgjoerStatus.MOTTATT_SKATTEOPPGJOER))
        etteroppgjoerDao.lagreEtteroppgjoer(Etteroppgjoer(sak2.id, 2024, EtteroppgjoerStatus.MOTTATT_SKATTEOPPGJOER))

        val sakerUtenOppgaver =
            etteroppgjoerDao.hentEtteroppgjoerSakerIBulk(
                inntektsaar = 2024,
                antall = 50,
                status = EtteroppgjoerStatus.MOTTATT_SKATTEOPPGJOER,
                etteroppgjoerFilter = EtteroppgjoerFilter.ENKEL,
                spesifikkeSaker = listOf(),
                ekskluderteSaker = listOf(),
                spesifikkeEnheter = listOf(),
            )
        sakerUtenOppgaver.size shouldBe 2
        sakerUtenOppgaver shouldContainExactlyInAnyOrder listOf(sak.id, sak2.id)

        // oppretter en oppgave for sak1
        val oppgaveForForbehandlingEtteroppgjoer =
            opprettNyOppgaveMedReferanseOgSak(
                referanse = "",
                sak = sak,
                kilde = OppgaveKilde.HENDELSE,
                type = OppgaveType.ETTEROPPGJOER,
                merknad = "Etteroppgjøret for $ETTEROPPGJOER_AAR er klart til behandling",
            )
        with(dataSource.connection) {
            val statement =
                prepareStatement(
                    """
                    INSERT INTO oppgave(id, status, enhet, sak_id, type, saksbehandler, referanse, gruppe_id, merknad, opprettet, saktype, fnr, frist, kilde)
                    VALUES(?::UUID, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """.trimIndent(),
                )
            statement.setObject(1, oppgaveForForbehandlingEtteroppgjoer.id)
            statement.setString(2, oppgaveForForbehandlingEtteroppgjoer.status.name)
            statement.setString(3, oppgaveForForbehandlingEtteroppgjoer.enhet.enhetNr)
            statement.setSakId(4, oppgaveForForbehandlingEtteroppgjoer.sakId)
            statement.setString(5, oppgaveForForbehandlingEtteroppgjoer.type.name)
            statement.setString(6, oppgaveForForbehandlingEtteroppgjoer.saksbehandler?.ident)
            statement.setString(7, oppgaveForForbehandlingEtteroppgjoer.referanse)
            statement.setString(8, oppgaveForForbehandlingEtteroppgjoer.gruppeId)
            statement.setString(9, oppgaveForForbehandlingEtteroppgjoer.merknad)
            statement.setTidspunkt(10, oppgaveForForbehandlingEtteroppgjoer.opprettet)
            statement.setString(11, oppgaveForForbehandlingEtteroppgjoer.sakType.name)
            statement.setString(12, oppgaveForForbehandlingEtteroppgjoer.fnr)
            statement.setTidspunkt(13, oppgaveForForbehandlingEtteroppgjoer.frist)
            statement.setString(14, oppgaveForForbehandlingEtteroppgjoer.kilde?.name)
            statement.executeUpdate()
        }
        val sakerUtenOppgaver2 =
            etteroppgjoerDao.hentEtteroppgjoerSakerIBulk(
                inntektsaar = 2024,
                antall = 50,
                status = EtteroppgjoerStatus.MOTTATT_SKATTEOPPGJOER,
                etteroppgjoerFilter = EtteroppgjoerFilter.ENKEL,
                spesifikkeSaker = listOf(),
                ekskluderteSaker = listOf(),
                spesifikkeEnheter = listOf(),
            )
        sakerUtenOppgaver2.size shouldBe 1
        sakerUtenOppgaver2 shouldContainExactlyInAnyOrder listOf(sak2.id)
    }

    @Test
    fun `oppdater ferdigstilt forbehandlingId`() {
        val inntektsaar = 2024

        val forbehandling =
            EtteroppgjoerForbehandling(
                id = UUID.randomUUID(),
                status = EtteroppgjoerForbehandlingStatus.FERDIGSTILT,
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
                varselbrevSendt = LocalDate.now().minusMonths(1),
            )
        etteroppgjoerForbehandlingDao.lagreForbehandling(forbehandling)

        etteroppgjoerDao.lagreEtteroppgjoer(
            Etteroppgjoer(
                sakId = sak.id,
                inntektsaar = inntektsaar,
                status = EtteroppgjoerStatus.VENTER_PAA_SVAR,
                sisteFerdigstilteForbehandling = forbehandling.id,
            ),
        )

        // negative test
        etteroppgjoerForbehandlingDao.lagreForbehandling(forbehandling.copy(varselbrevSendt = LocalDate.now()))

        val svarfristUtloept =
            etteroppgjoerDao.hentEtteroppgjoerMedSvarfristUtloept(
                inntektsaar,
                EtteroppgjoerSvarfrist.ETT_MINUTT,
            )

        svarfristUtloept!!.size shouldBe 1
        svarfristUtloept.first().sakId shouldBe sak.id
        svarfristUtloept.first().inntektsaar shouldBe inntektsaar
        svarfristUtloept.first().status shouldBe EtteroppgjoerStatus.VENTER_PAA_SVAR
        svarfristUtloept.first().sisteFerdigstilteForbehandling shouldBe forbehandling.id
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
            harSanksjon shouldBe false
            harOpphoer shouldBe false
            harBosattUtland shouldBe false
            harInstitusjonsopphold shouldBe false
        }

        etteroppgjoerDao.lagreEtteroppgjoer(
            Etteroppgjoer(
                sak.id,
                2024,
                EtteroppgjoerStatus.UNDER_FORBEHANDLING,
                true,
                true,
                true,
                true,
                true,
                true,
                true,
                true,
                uuid,
            ),
        )

        val oppdatert = etteroppgjoerDao.hentEtteroppgjoerForInntektsaar(sak.id, 2024)
        with(oppdatert!!) {
            sakId shouldBe sakId
            inntektsaar shouldBe inntektsaar
            status shouldBe EtteroppgjoerStatus.UNDER_FORBEHANDLING
            harSanksjon shouldBe true
            harOpphoer shouldBe true
            harBosattUtland shouldBe true
            harUtlandstilsnitt shouldBe true
            harInstitusjonsopphold shouldBe true
            harAdressebeskyttelseEllerSkjermet shouldBe true
            harAktivitetskrav shouldBe true
            harOverstyrtBeregning shouldBe true
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
}
