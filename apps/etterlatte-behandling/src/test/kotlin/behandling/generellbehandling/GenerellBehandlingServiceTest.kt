package no.nav.etterlatte.behandling.generellbehandling

import io.kotest.inspectors.forExactly
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import no.nav.etterlatte.Context
import no.nav.etterlatte.DatabaseKontekst
import no.nav.etterlatte.Kontekst
import no.nav.etterlatte.SaksbehandlerMedEnheterOgRoller
import no.nav.etterlatte.common.Enheter
import no.nav.etterlatte.funksjonsbrytere.DummyFeatureToggleService
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.generellbehandling.DokumentMedSendtDato
import no.nav.etterlatte.libs.common.generellbehandling.Dokumenter
import no.nav.etterlatte.libs.common.generellbehandling.GenerellBehandling
import no.nav.etterlatte.libs.common.generellbehandling.Innhold
import no.nav.etterlatte.libs.common.oppgave.OppgaveKilde
import no.nav.etterlatte.libs.common.oppgave.OppgaveType
import no.nav.etterlatte.libs.common.oppgave.Status
import no.nav.etterlatte.libs.database.DataSourceBuilder
import no.nav.etterlatte.libs.database.POSTGRES_VERSION
import no.nav.etterlatte.libs.database.migrate
import no.nav.etterlatte.oppgave.OppgaveDao
import no.nav.etterlatte.oppgave.OppgaveDaoImpl
import no.nav.etterlatte.oppgave.OppgaveDaoMedEndringssporingImpl
import no.nav.etterlatte.oppgave.OppgaveService
import no.nav.etterlatte.sak.SakDao
import no.nav.etterlatte.tilgangsstyring.SaksbehandlerMedRoller
import no.nav.etterlatte.token.BrukerTokenInfo
import no.nav.etterlatte.token.Saksbehandler
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertThrows
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import java.sql.Connection
import java.time.LocalDate
import java.util.UUID
import javax.sql.DataSource

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class GenerellBehandlingServiceTest {
    @Container
    private val postgreSQLContainer = PostgreSQLContainer<Nothing>("postgres:$POSTGRES_VERSION")

    private lateinit var dataSource: DataSource
    private lateinit var dao: GenerellBehandlingDao
    private lateinit var oppgaveDao: OppgaveDao
    private lateinit var oppgaveService: OppgaveService
    private lateinit var sakRepo: SakDao
    private lateinit var service: GenerellBehandlingService

    @BeforeAll
    fun beforeAll() {
        postgreSQLContainer.start()
        postgreSQLContainer.withUrlParam("user", postgreSQLContainer.username)
        postgreSQLContainer.withUrlParam("password", postgreSQLContainer.password)

        dataSource =
            DataSourceBuilder.createDataSource(
                jdbcUrl = postgreSQLContainer.jdbcUrl,
                username = postgreSQLContainer.username,
                password = postgreSQLContainer.password,
            ).apply { migrate() }

        val connection = dataSource.connection
        dao = GenerellBehandlingDao { connection }
        oppgaveDao = OppgaveDaoImpl { connection }
        sakRepo = SakDao { connection }

        oppgaveService = OppgaveService(OppgaveDaoMedEndringssporingImpl(oppgaveDao) { connection }, sakRepo, DummyFeatureToggleService())
        service = GenerellBehandlingService(dao, oppgaveService)
    }

    @AfterEach
    fun afterEach() {
        dataSource.connection.use {
            it.prepareStatement("TRUNCATE generellbehandling CASCADE; TRUNCATE oppgave CASCADE").execute()
        }
    }

    @AfterAll
    fun afterAll() {
        postgreSQLContainer.stop()
    }

    private val user = mockk<SaksbehandlerMedEnheterOgRoller>()

    @BeforeEach
    fun beforeEach() {
        Kontekst.set(
            Context(
                user,
                object : DatabaseKontekst {
                    override fun activeTx(): Connection {
                        throw IllegalArgumentException()
                    }

                    override fun <T> inTransaction(
                        gjenbruk: Boolean,
                        block: () -> T,
                    ): T {
                        return block()
                    }
                },
            ),
        )
    }

    @Test
    fun `Kan kun oppdatere hvis statusen er opprettet`() {
        val sak = sakRepo.opprettSak("fnr", SakType.BARNEPENSJON, Enheter.AALESUND.enhetNr)
        val manueltOpprettetBehandling = GenerellBehandling.opprettFraType(GenerellBehandling.GenerellBehandlingType.UTLAND, sak.id)
        val opprettBehandling = service.opprettBehandling(manueltOpprettetBehandling)
        dao.oppdaterGenerellBehandling(opprettBehandling.copy(status = GenerellBehandling.Status.FATTET))
        assertThrows<KanIkkeEndreFattetEllerAttestertBehandling> {
            service.lagreNyeOpplysninger(opprettBehandling)
        }
    }

    @Test
    fun kanOppretteBehandlingOgFaarDaOgsaaEnOppgaveManuellOpprettelseUtenTildelingAvSaksbehandler() {
        val sak = sakRepo.opprettSak("fnr", SakType.BARNEPENSJON, Enheter.AALESUND.enhetNr)
        val manueltOpprettetBehandling = GenerellBehandling.opprettFraType(GenerellBehandling.GenerellBehandlingType.UTLAND, sak.id)
        val opprettBehandling = service.opprettBehandling(manueltOpprettetBehandling)
        Assertions.assertEquals(GenerellBehandling.GenerellBehandlingType.UTLAND, opprettBehandling.type)
        Assertions.assertEquals(sak.id, opprettBehandling.sakId)
        Assertions.assertEquals(GenerellBehandling.Status.OPPRETTET, opprettBehandling.status)
        Assertions.assertNotNull(opprettBehandling.opprettet)
        Assertions.assertNull(opprettBehandling.innhold)

        val oppgaverforGenerellBehandling = oppgaveService.hentOppgaverForReferanse(manueltOpprettetBehandling.id.toString())
        Assertions.assertEquals(1, oppgaverforGenerellBehandling.size)
        val manuellBehandlingOppgave = oppgaverforGenerellBehandling[0]
        Assertions.assertEquals(manueltOpprettetBehandling.id.toString(), manuellBehandlingOppgave.referanse)
        Assertions.assertEquals(OppgaveKilde.GENERELL_BEHANDLING, manuellBehandlingOppgave.kilde)
        Assertions.assertEquals(OppgaveType.UTLAND, manuellBehandlingOppgave.type)
        Assertions.assertEquals(sak.id, manuellBehandlingOppgave.sakId)
        Assertions.assertNull(manuellBehandlingOppgave.saksbehandler)
    }

    @Test
    fun kanOppretteBehandlingOgFaarDaOgsaaEnOppgaveManuellOpprettelseMedTildelingAvSaksbehandler() {
        val sak = sakRepo.opprettSak("fnr", SakType.BARNEPENSJON, Enheter.AALESUND.enhetNr)
        val behandlingId = UUID.randomUUID()
        val manueltOpprettetBehandling =
            GenerellBehandling.opprettUtland(
                sak.id,
                behandlingId,
            )
        val opprettBehandling = service.opprettBehandling(manueltOpprettetBehandling)
        Assertions.assertEquals(GenerellBehandling.GenerellBehandlingType.UTLAND, opprettBehandling.type)
        Assertions.assertEquals(sak.id, opprettBehandling.sakId)
        Assertions.assertEquals(GenerellBehandling.Status.OPPRETTET, opprettBehandling.status)
        Assertions.assertNotNull(opprettBehandling.opprettet)
        Assertions.assertNull(opprettBehandling.innhold)

        val oppgaveForFoerstegangsBehandling =
            oppgaveService.opprettFoerstegangsbehandlingsOppgaveForInnsendtSoeknad(behandlingId.toString(), sak.id)
        val saksbehandler = "saksbehandler"
        oppgaveService.tildelSaksbehandler(oppgaveForFoerstegangsBehandling.id, saksbehandler)

        val tokenInfo = BrukerTokenInfo.of(UUID.randomUUID().toString(), saksbehandler, null, null, null)
        oppgaveService.ferdigStillOppgaveUnderBehandling(behandlingId.toString(), tokenInfo)

        val oppgaverforGenerellBehandling = oppgaveService.hentOppgaverForReferanse(manueltOpprettetBehandling.id.toString())
        Assertions.assertEquals(1, oppgaverforGenerellBehandling.size)
        val manuellBehandlingOppgave = oppgaverforGenerellBehandling[0]
        Assertions.assertEquals(manueltOpprettetBehandling.id.toString(), manuellBehandlingOppgave.referanse)
        Assertions.assertEquals(OppgaveKilde.GENERELL_BEHANDLING, manuellBehandlingOppgave.kilde)
        Assertions.assertEquals(OppgaveType.UTLAND, manuellBehandlingOppgave.type)
        Assertions.assertEquals(sak.id, manuellBehandlingOppgave.sakId)
        Assertions.assertNull(manuellBehandlingOppgave.saksbehandler)
    }

    @Test
    fun `Kan sende til attestering(fatte)`() {
        val saksbehandler = Saksbehandler("token", "saksbehandler", null)

        val sak = sakRepo.opprettSak("fnr", SakType.BARNEPENSJON, Enheter.AALESUND.enhetNr)
        val behandlingId = UUID.randomUUID()
        val manueltOpprettetBehandling =
            GenerellBehandling.opprettUtland(
                sak.id,
                behandlingId,
            )
        val opprettBehandling = service.opprettBehandling(manueltOpprettetBehandling)
        Assertions.assertEquals(GenerellBehandling.Status.OPPRETTET, opprettBehandling.status)

        val hentOppgaverForReferanse = oppgaveService.hentOppgaverForReferanse(opprettBehandling.id.toString())
        Assertions.assertEquals(1, hentOppgaverForReferanse.size)
        val utlandsOppgave = hentOppgaverForReferanse[0]

        oppgaveService.tildelSaksbehandler(utlandsOppgave.id, saksbehandler.ident)
        val utlandInnhold =
            Innhold.Utland(
                listOf("AFG"),
                Dokumenter(
                    DokumentMedSendtDato(true, LocalDate.now()),
                    DokumentMedSendtDato(true, LocalDate.now()),
                    DokumentMedSendtDato(true, LocalDate.now()),
                    DokumentMedSendtDato(true, LocalDate.now()),
                    DokumentMedSendtDato(true, LocalDate.now()),
                ),
                "2grwg2",
                "124124124",
            )
        val behandlingUtfylt = opprettBehandling.copy(innhold = utlandInnhold)
        val oppdaterBehandling = service.lagreNyeOpplysninger(behandlingUtfylt)
        service.sendTilAttestering(oppdaterBehandling, saksbehandler)
        val fattetBehandling = service.hentBehandlingMedId(oppdaterBehandling.id)
        Assertions.assertEquals(GenerellBehandling.Status.FATTET, fattetBehandling?.status)
        val behandlingsOppgaverFattetOgAttestering = oppgaveService.hentOppgaverForReferanse(opprettBehandling.id.toString())
        Assertions.assertEquals(2, behandlingsOppgaverFattetOgAttestering.size)

        behandlingsOppgaverFattetOgAttestering.forExactly(1) { oppgave ->
            oppgave.type.shouldBe(OppgaveType.ATTESTERING)
        }
        behandlingsOppgaverFattetOgAttestering.forExactly(1) { oppgave ->
            oppgave.status.shouldBe(Status.FERDIGSTILT)
        }
    }

    @Test
    fun `Skal feile når dato sendt mangler på dokument`() {
        val saksbehandler = Saksbehandler("token", "saksbehandler", null)

        val sak = sakRepo.opprettSak("fnr", SakType.BARNEPENSJON, Enheter.AALESUND.enhetNr)
        val behandlingId = UUID.randomUUID()
        val manueltOpprettetBehandling =
            GenerellBehandling.opprettUtland(
                sak.id,
                behandlingId,
            )
        val opprettBehandling = service.opprettBehandling(manueltOpprettetBehandling)
        val hentOppgaverForReferanse = oppgaveService.hentOppgaverForReferanse(opprettBehandling.id.toString())
        Assertions.assertEquals(1, hentOppgaverForReferanse.size)
        val utlandsOppgave = hentOppgaverForReferanse[0]

        oppgaveService.tildelSaksbehandler(utlandsOppgave.id, saksbehandler.ident)
        val utlandInnhold =
            Innhold.Utland(
                listOf("AFG"),
                Dokumenter(
                    DokumentMedSendtDato(true, LocalDate.now()),
                    DokumentMedSendtDato(true, LocalDate.now()),
                    DokumentMedSendtDato(true, LocalDate.now()),
                    DokumentMedSendtDato(true, LocalDate.now()),
                    DokumentMedSendtDato(true, null),
                ),
                "2grwg2",
                "124124124",
            )
        val behandlingUtfylt = opprettBehandling.copy(innhold = utlandInnhold)
        val oppdaterBehandling = service.lagreNyeOpplysninger(behandlingUtfylt)
        assertThrows<DokumentManglerDatoException> {
            service.sendTilAttestering(oppdaterBehandling, saksbehandler)
        }
    }

    @Test
    fun `Kan ikke attestere med feil status på behandling`() {
        val saksbehandler = Saksbehandler("token", "saksbehandler", null)

        val sak = sakRepo.opprettSak("fnr", SakType.BARNEPENSJON, Enheter.AALESUND.enhetNr)
        val behandlingId = UUID.randomUUID()
        val manueltOpprettetBehandling =
            GenerellBehandling.opprettUtland(
                sak.id,
                behandlingId,
            )
        val opprettBehandling = service.opprettBehandling(manueltOpprettetBehandling)
        val hentOppgaverForReferanse = oppgaveService.hentOppgaverForReferanse(opprettBehandling.id.toString())
        Assertions.assertEquals(1, hentOppgaverForReferanse.size)
        val utlandsOppgave = hentOppgaverForReferanse[0]

        oppgaveService.tildelSaksbehandler(utlandsOppgave.id, saksbehandler.ident)
        val utlandInnhold =
            Innhold.Utland(
                listOf("AFG"),
                Dokumenter(
                    DokumentMedSendtDato(true, LocalDate.now()),
                    DokumentMedSendtDato(true, LocalDate.now()),
                    DokumentMedSendtDato(true, LocalDate.now()),
                    DokumentMedSendtDato(true, LocalDate.now()),
                    DokumentMedSendtDato(true, null),
                ),
                "2grwg2",
                "124124124",
            )
        val behandlingUtfylt = opprettBehandling.copy(innhold = utlandInnhold, status = GenerellBehandling.Status.FATTET)
        val oppdaterBehandling = service.lagreNyeOpplysninger(behandlingUtfylt)
        assertThrows<IllegalStateException> {
            service.sendTilAttestering(oppdaterBehandling, saksbehandler)
        }
    }

    @Test
    fun `Skal feile når rina nummer er tomt`() {
        val saksbehandler = Saksbehandler("token", "saksbehandler", null)

        val sak = sakRepo.opprettSak("fnr", SakType.BARNEPENSJON, Enheter.AALESUND.enhetNr)
        val behandlingId = UUID.randomUUID()
        val manueltOpprettetBehandling =
            GenerellBehandling.opprettUtland(
                sak.id,
                behandlingId,
            )
        val opprettBehandling = service.opprettBehandling(manueltOpprettetBehandling)
        val hentOppgaverForReferanse = oppgaveService.hentOppgaverForReferanse(opprettBehandling.id.toString())
        Assertions.assertEquals(1, hentOppgaverForReferanse.size)
        val utlandsOppgave = hentOppgaverForReferanse[0]

        oppgaveService.tildelSaksbehandler(utlandsOppgave.id, saksbehandler.ident)
        val utlandInnhold =
            Innhold.Utland(
                listOf("AFG"),
                Dokumenter(
                    DokumentMedSendtDato(true, LocalDate.now()),
                    DokumentMedSendtDato(true, LocalDate.now()),
                    DokumentMedSendtDato(true, LocalDate.now()),
                    DokumentMedSendtDato(true, LocalDate.now()),
                    DokumentMedSendtDato(true, LocalDate.now()),
                ),
                "2grwg2",
                "",
            )
        val behandlingUtfylt = opprettBehandling.copy(innhold = utlandInnhold)
        val oppdaterBehandling = service.lagreNyeOpplysninger(behandlingUtfylt)
        assertThrows<ManglerRinanummerException> {
            service.sendTilAttestering(oppdaterBehandling, saksbehandler)
        }
    }

    @Test
    fun `Skal feile landkodelisten er tom`() {
        val saksbehandler = Saksbehandler("token", "saksbehandler", null)

        val sak = sakRepo.opprettSak("fnr", SakType.BARNEPENSJON, Enheter.AALESUND.enhetNr)
        val behandlingId = UUID.randomUUID()
        val manueltOpprettetBehandling =
            GenerellBehandling.opprettUtland(
                sak.id,
                behandlingId,
            )
        val opprettBehandling = service.opprettBehandling(manueltOpprettetBehandling)
        val hentOppgaverForReferanse = oppgaveService.hentOppgaverForReferanse(opprettBehandling.id.toString())
        Assertions.assertEquals(1, hentOppgaverForReferanse.size)
        val utlandsOppgave = hentOppgaverForReferanse[0]

        oppgaveService.tildelSaksbehandler(utlandsOppgave.id, saksbehandler.ident)
        val utlandInnhold =
            Innhold.Utland(
                emptyList(),
                Dokumenter(
                    DokumentMedSendtDato(true, LocalDate.now()),
                    DokumentMedSendtDato(true, LocalDate.now()),
                    DokumentMedSendtDato(true, LocalDate.now()),
                    DokumentMedSendtDato(true, LocalDate.now()),
                    DokumentMedSendtDato(true, LocalDate.now()),
                ),
                "2grwg2",
                "rinanummer",
            )
        val behandlingUtfylt = opprettBehandling.copy(innhold = utlandInnhold)
        val oppdaterBehandling = service.lagreNyeOpplysninger(behandlingUtfylt)
        assertThrows<ManglerLandkodeException> {
            service.sendTilAttestering(oppdaterBehandling, saksbehandler)
        }
    }

    @Test
    fun `Skal feile når isokode 3 er feil lengde`() {
        val saksbehandler = Saksbehandler("token", "saksbehandler", null)

        val sak = sakRepo.opprettSak("fnr", SakType.BARNEPENSJON, Enheter.AALESUND.enhetNr)
        val behandlingId = UUID.randomUUID()
        val manueltOpprettetBehandling =
            GenerellBehandling.opprettUtland(
                sak.id,
                behandlingId,
            )
        val opprettBehandling = service.opprettBehandling(manueltOpprettetBehandling)
        val hentOppgaverForReferanse = oppgaveService.hentOppgaverForReferanse(opprettBehandling.id.toString())
        Assertions.assertEquals(1, hentOppgaverForReferanse.size)
        val utlandsOppgave = hentOppgaverForReferanse[0]

        oppgaveService.tildelSaksbehandler(utlandsOppgave.id, saksbehandler.ident)
        val utlandInnhold =
            Innhold.Utland(
                listOf("AFG", "ABDEF"),
                Dokumenter(
                    DokumentMedSendtDato(true, LocalDate.now()),
                    DokumentMedSendtDato(true, LocalDate.now()),
                    DokumentMedSendtDato(true, LocalDate.now()),
                    DokumentMedSendtDato(true, LocalDate.now()),
                    DokumentMedSendtDato(true, LocalDate.now()),
                ),
                "2grwg2",
                "rinanummer",
            )
        val behandlingUtfylt = opprettBehandling.copy(innhold = utlandInnhold)
        val oppdaterBehandling = service.lagreNyeOpplysninger(behandlingUtfylt)
        assertThrows<LandFeilIsokodeException> {
            service.sendTilAttestering(oppdaterBehandling, saksbehandler)
        }
    }

    @Test
    fun `Kan attestere behandling`() {
        val saksbehandler = Saksbehandler("token", "saksbehandler", null)

        val sak = sakRepo.opprettSak("fnr", SakType.BARNEPENSJON, Enheter.AALESUND.enhetNr)
        val behandlingId = UUID.randomUUID()
        val manueltOpprettetBehandling =
            GenerellBehandling.opprettUtland(
                sak.id,
                behandlingId,
            )
        val opprettBehandling = service.opprettBehandling(manueltOpprettetBehandling)
        Assertions.assertEquals(GenerellBehandling.Status.OPPRETTET, opprettBehandling.status)

        val hentOppgaverForReferanse = oppgaveService.hentOppgaverForReferanse(opprettBehandling.id.toString())
        Assertions.assertEquals(1, hentOppgaverForReferanse.size)
        val utlandsOppgave = hentOppgaverForReferanse[0]

        oppgaveService.tildelSaksbehandler(utlandsOppgave.id, saksbehandler.ident)
        val utlandInnhold =
            Innhold.Utland(
                listOf("AFG"),
                Dokumenter(
                    DokumentMedSendtDato(true, LocalDate.now()),
                    DokumentMedSendtDato(true, LocalDate.now()),
                    DokumentMedSendtDato(true, LocalDate.now()),
                    DokumentMedSendtDato(true, LocalDate.now()),
                    DokumentMedSendtDato(true, LocalDate.now()),
                ),
                "2grwg2",
                "124124124",
            )
        val behandlingUtfylt = opprettBehandling.copy(innhold = utlandInnhold)
        val oppdaterBehandling = service.lagreNyeOpplysninger(behandlingUtfylt)
        service.sendTilAttestering(oppdaterBehandling, saksbehandler)
        val fattetBehandling = service.hentBehandlingMedId(oppdaterBehandling.id)
        Assertions.assertEquals(GenerellBehandling.Status.FATTET, fattetBehandling?.status)
        val behandlingsOppgaverFattetOgAttestering = oppgaveService.hentOppgaverForReferanse(opprettBehandling.id.toString())
        Assertions.assertEquals(2, behandlingsOppgaverFattetOgAttestering.size)

        behandlingsOppgaverFattetOgAttestering.forExactly(1) { oppgave ->
            oppgave.type.shouldBe(OppgaveType.ATTESTERING)
        }
        behandlingsOppgaverFattetOgAttestering.forExactly(1) { oppgave ->
            oppgave.status.shouldBe(Status.FERDIGSTILT)
        }

        val attestant = Saksbehandler("token", "attestant", null)
        val oppgaveForAttestering = oppgaveService.hentOppgaverForReferanse(opprettBehandling.id.toString())
        Assertions.assertEquals(2, oppgaveForAttestering.size)
        val nyattesteringsoppgave = oppgaveForAttestering.filter { o -> o.status === Status.NY && o.erAttestering() }

        val saksbehandlerMedRoller =
            mockk<SaksbehandlerMedRoller> {
                every { harRolleAttestant() } returns true
            }
        every { user.saksbehandlerMedRoller } returns saksbehandlerMedRoller

        val attesteringsOppgave = nyattesteringsoppgave[0]
        oppgaveService.tildelSaksbehandler(attesteringsOppgave.id, attestant.ident)

        service.attester(oppdaterBehandling.id, attestant)
        val attestertBehandling = service.hentBehandlingMedId(oppdaterBehandling.id)
        Assertions.assertEquals(GenerellBehandling.Status.ATTESTERT, attestertBehandling?.status)
    }
}
