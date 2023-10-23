package no.nav.etterlatte.behandling.generellbehandling

import io.kotest.inspectors.forExactly
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.Context
import no.nav.etterlatte.DatabaseKontekst
import no.nav.etterlatte.Kontekst
import no.nav.etterlatte.SaksbehandlerMedEnheterOgRoller
import no.nav.etterlatte.behandling.BehandlingDao
import no.nav.etterlatte.behandling.BehandlingService
import no.nav.etterlatte.behandling.domain.Foerstegangsbehandling
import no.nav.etterlatte.behandling.klienter.GrunnlagKlient
import no.nav.etterlatte.behandling.kommerbarnettilgode.KommerBarnetTilGodeDao
import no.nav.etterlatte.behandling.revurdering.RevurderingDao
import no.nav.etterlatte.common.Enheter
import no.nav.etterlatte.funksjonsbrytere.DummyFeatureToggleService
import no.nav.etterlatte.grunnlagsOpplysningMedPersonopplysning
import no.nav.etterlatte.libs.common.behandling.BehandlingType
import no.nav.etterlatte.libs.common.behandling.BoddEllerArbeidetUtlandet
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.generellbehandling.DokumentMedSendtDato
import no.nav.etterlatte.libs.common.generellbehandling.Dokumenter
import no.nav.etterlatte.libs.common.generellbehandling.GenerellBehandling
import no.nav.etterlatte.libs.common.generellbehandling.Innhold
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstype
import no.nav.etterlatte.libs.common.oppgave.OppgaveKilde
import no.nav.etterlatte.libs.common.oppgave.OppgaveType
import no.nav.etterlatte.libs.common.oppgave.Status
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.database.DataSourceBuilder
import no.nav.etterlatte.libs.database.POSTGRES_VERSION
import no.nav.etterlatte.libs.database.migrate
import no.nav.etterlatte.oppgave.OppgaveDao
import no.nav.etterlatte.oppgave.OppgaveDaoImpl
import no.nav.etterlatte.oppgave.OppgaveDaoMedEndringssporingImpl
import no.nav.etterlatte.oppgave.OppgaveService
import no.nav.etterlatte.opprettBehandling
import no.nav.etterlatte.personOpplysning
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
import java.time.temporal.ChronoUnit
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
    private lateinit var behandlingRepo: BehandlingDao
    val grunnlagKlient = mockk<GrunnlagKlient>()
    val behandlingService = mockk<BehandlingService>()

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
        behandlingRepo =
            BehandlingDao(KommerBarnetTilGodeDao { connection }, RevurderingDao { connection }) { connection }
        oppgaveService = OppgaveService(OppgaveDaoMedEndringssporingImpl(oppgaveDao) { connection }, sakRepo, DummyFeatureToggleService())
        service = GenerellBehandlingService(dao, oppgaveService, behandlingService, grunnlagKlient)
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

                    override fun <T> inTransaction(block: () -> T): T {
                        return block()
                    }
                },
            ),
        )
    }

    @Test
    fun `Kan kun oppdatere hvis statusen er opprettet`() {
        val sak = sakRepo.opprettSak("fnr", SakType.BARNEPENSJON, Enheter.AALESUND.enhetNr)
        val manueltOpprettetBehandling =
            GenerellBehandling.opprettFraType(
                GenerellBehandling.GenerellBehandlingType.KRAVPAKKE_UTLAND,
                sak.id,
            )
        val opprettBehandling = service.opprettBehandling(manueltOpprettetBehandling)
        dao.oppdaterGenerellBehandling(opprettBehandling.copy(status = GenerellBehandling.Status.FATTET))
        assertThrows<KanIkkeEndreFattetEllerAttestertBehandling> {
            service.lagreNyeOpplysninger(opprettBehandling)
        }
    }

    @Test
    fun `Finner kravpakke på sak`() {
        val sak = sakRepo.opprettSak("fnr", SakType.BARNEPENSJON, Enheter.AALESUND.enhetNr)
        val foerstegangsbehandling =
            opprettBehandling(
                type = BehandlingType.FØRSTEGANGSBEHANDLING,
                sakId = sak.id,
            )
        behandlingRepo.opprettBehandling(foerstegangsbehandling)
        behandlingRepo.lagreBoddEllerArbeidetUtlandet(
            foerstegangsbehandling.id,
            BoddEllerArbeidetUtlandet(
                true,
                Grunnlagsopplysning.Saksbehandler.create("ident"),
                "begrunnelse",
                boddArbeidetIkkeEosEllerAvtaleland = true,
                boddArbeidetEosNordiskKonvensjon = true,
                boddArbeidetAvtaleland = true,
                vurdereAvoededsTrygdeavtale = true,
                norgeErBehandlendeland = true,
                skalSendeKravpakke = true,
            ),
        )
        val foerstegangsbehandlingHentet = behandlingRepo.hentBehandling(foerstegangsbehandling.id) as Foerstegangsbehandling
        val brukerTokenInfo = BrukerTokenInfo.of("token", "s1", null, null, null)
        every { behandlingService.hentBehandlingerISak(sak.id) } returns listOf(foerstegangsbehandlingHentet)
        val doedsdato = LocalDate.parse("2016-12-30")
        val personopplysning = personOpplysning(doedsdato = doedsdato)
        val grunnlagsopplysningMedPersonopplysning = grunnlagsOpplysningMedPersonopplysning(personopplysning)
        coEvery {
            grunnlagKlient.finnPersonOpplysning(sak.id, foerstegangsbehandling.id, Opplysningstype.AVDOED_PDL_V1, brukerTokenInfo)
        } returns grunnlagsopplysningMedPersonopplysning

        val manueltOpprettetBehandling =
            GenerellBehandling.opprettFraType(
                GenerellBehandling.GenerellBehandlingType.KRAVPAKKE_UTLAND,
                sak.id,
            ).copy(tilknyttetBehandling = foerstegangsbehandling.id, status = GenerellBehandling.Status.ATTESTERT)
        val opprettBehandlingGenerell = service.opprettBehandling(manueltOpprettetBehandling)

        val kravpakkeMedArbeidetUtlandet = runBlocking { service.hentKravpakkeForSak(sak.id, brukerTokenInfo) }
        Assertions.assertEquals(opprettBehandlingGenerell.id, kravpakkeMedArbeidetUtlandet.kravpakke.id)
        Assertions.assertEquals(foerstegangsbehandling.id, kravpakkeMedArbeidetUtlandet.kravpakke.tilknyttetBehandling)
    }

    @Test
    fun kanOppretteBehandlingOgFaarDaOgsaaEnOppgaveManuellOpprettelseUtenTildelingAvSaksbehandler() {
        val sak = sakRepo.opprettSak("fnr", SakType.BARNEPENSJON, Enheter.AALESUND.enhetNr)
        val manueltOpprettetBehandling =
            GenerellBehandling.opprettFraType(
                GenerellBehandling.GenerellBehandlingType.KRAVPAKKE_UTLAND,
                sak.id,
            )
        val opprettBehandling = service.opprettBehandling(manueltOpprettetBehandling)
        Assertions.assertEquals(GenerellBehandling.GenerellBehandlingType.KRAVPAKKE_UTLAND, opprettBehandling.type)
        Assertions.assertEquals(sak.id, opprettBehandling.sakId)
        Assertions.assertEquals(GenerellBehandling.Status.OPPRETTET, opprettBehandling.status)
        Assertions.assertNotNull(opprettBehandling.opprettet)
        Assertions.assertNull(opprettBehandling.innhold)

        val oppgaverforGenerellBehandling = oppgaveService.hentOppgaverForReferanse(manueltOpprettetBehandling.id.toString())
        Assertions.assertEquals(1, oppgaverforGenerellBehandling.size)
        val manuellBehandlingOppgave = oppgaverforGenerellBehandling[0]
        Assertions.assertEquals(manueltOpprettetBehandling.id.toString(), manuellBehandlingOppgave.referanse)
        Assertions.assertEquals(OppgaveKilde.GENERELL_BEHANDLING, manuellBehandlingOppgave.kilde)
        Assertions.assertEquals(OppgaveType.KRAVPAKKE_UTLAND, manuellBehandlingOppgave.type)
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
        Assertions.assertEquals(GenerellBehandling.GenerellBehandlingType.KRAVPAKKE_UTLAND, opprettBehandling.type)
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
        Assertions.assertEquals(OppgaveType.KRAVPAKKE_UTLAND, manuellBehandlingOppgave.type)
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
        val kravpakkeUtlandInnhold =
            Innhold.KravpakkeUtland(
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
        val behandlingUtfylt = opprettBehandling.copy(innhold = kravpakkeUtlandInnhold)
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
        val kravpakkeUtlandInnhold =
            Innhold.KravpakkeUtland(
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
        val behandlingUtfylt = opprettBehandling.copy(innhold = kravpakkeUtlandInnhold)
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
        val kravpakkeUtlandInnhold =
            Innhold.KravpakkeUtland(
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
        val behandlingUtfylt = opprettBehandling.copy(innhold = kravpakkeUtlandInnhold, status = GenerellBehandling.Status.FATTET)
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
        val kravpakkeUtlandInnhold =
            Innhold.KravpakkeUtland(
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
        val behandlingUtfylt = opprettBehandling.copy(innhold = kravpakkeUtlandInnhold)
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
        val kravpakkeUtlandInnhold =
            Innhold.KravpakkeUtland(
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
        val behandlingUtfylt = opprettBehandling.copy(innhold = kravpakkeUtlandInnhold)
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
        val kravpakkeUtlandInnhold =
            Innhold.KravpakkeUtland(
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
        val behandlingUtfylt = opprettBehandling.copy(innhold = kravpakkeUtlandInnhold)
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
        val kravpakkeUtlandInnhold =
            Innhold.KravpakkeUtland(
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
        val behandlingUtfylt = opprettBehandling.copy(innhold = kravpakkeUtlandInnhold)
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
            oppgave.type.shouldBe(OppgaveType.KRAVPAKKE_UTLAND)
        }

        val attestant = Saksbehandler("token", "attestant", null)
        val oppgaveForAttestering = oppgaveService.hentOppgaverForReferanse(opprettBehandling.id.toString())
        Assertions.assertEquals(2, oppgaveForAttestering.size)
        val nyattesteringsoppgave = oppgaveForAttestering.filter { o -> o.status === Status.NY && o.erAttestering() }
        Assertions.assertEquals(1, nyattesteringsoppgave.size)
        val saksbehandlerMedRoller =
            mockk<SaksbehandlerMedRoller> {
                every { harRolleAttestant() } returns true
            }
        every { user.saksbehandlerMedRoller } returns saksbehandlerMedRoller

        val attesteringsOppgave = nyattesteringsoppgave[0]
        val trettidagerfrem = Tidspunkt.now().plus(30L, ChronoUnit.DAYS).toNorskLocalDate()
        Assertions.assertEquals(trettidagerfrem, attesteringsOppgave.frist!!.toNorskLocalDate())
        oppgaveService.tildelSaksbehandler(attesteringsOppgave.id, attestant.ident)

        service.attester(oppdaterBehandling.id, attestant)
        val attestertBehandling = service.hentBehandlingMedId(oppdaterBehandling.id)
        Assertions.assertEquals(GenerellBehandling.Status.ATTESTERT, attestertBehandling?.status)
    }
}
