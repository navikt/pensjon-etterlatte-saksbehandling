package no.nav.etterlatte.behandling.generellbehandling

import io.kotest.inspectors.forExactly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.ConnectionAutoclosingTest
import no.nav.etterlatte.DatabaseExtension
import no.nav.etterlatte.behandling.BehandlingDao
import no.nav.etterlatte.behandling.BehandlingHendelserKafkaProducer
import no.nav.etterlatte.behandling.BehandlingService
import no.nav.etterlatte.behandling.domain.Foerstegangsbehandling
import no.nav.etterlatte.behandling.hendelse.HendelseDao
import no.nav.etterlatte.behandling.klienter.GrunnlagKlient
import no.nav.etterlatte.behandling.kommerbarnettilgode.KommerBarnetTilGodeDao
import no.nav.etterlatte.behandling.revurdering.RevurderingDao
import no.nav.etterlatte.common.Enheter
import no.nav.etterlatte.grunnlagsOpplysningMedPersonopplysning
import no.nav.etterlatte.ktor.simpleSaksbehandler
import no.nav.etterlatte.libs.common.behandling.BehandlingType
import no.nav.etterlatte.libs.common.behandling.BoddEllerArbeidetUtlandet
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.generellbehandling.DokumentMedSendtDato
import no.nav.etterlatte.libs.common.generellbehandling.GenerellBehandling
import no.nav.etterlatte.libs.common.generellbehandling.GenerellBehandlingHendelseType
import no.nav.etterlatte.libs.common.generellbehandling.Innhold
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstype
import no.nav.etterlatte.libs.common.oppgave.OppgaveIntern
import no.nav.etterlatte.libs.common.oppgave.OppgaveKilde
import no.nav.etterlatte.libs.common.oppgave.OppgaveType
import no.nav.etterlatte.libs.common.oppgave.Status
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.ktor.token.Saksbehandler
import no.nav.etterlatte.mockSaksbehandler
import no.nav.etterlatte.nyKontekstMedBrukerOgDatabase
import no.nav.etterlatte.oppgave.OppgaveDao
import no.nav.etterlatte.oppgave.OppgaveDaoImpl
import no.nav.etterlatte.oppgave.OppgaveDaoMedEndringssporingImpl
import no.nav.etterlatte.oppgave.OppgaveService
import no.nav.etterlatte.opprettBehandling
import no.nav.etterlatte.personOpplysning
import no.nav.etterlatte.sak.SakDao
import no.nav.etterlatte.saksbehandler.SaksbehandlerInfoDao
import no.nav.etterlatte.tilgangsstyring.SaksbehandlerMedRoller
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.util.UUID.randomUUID
import javax.sql.DataSource

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(DatabaseExtension::class)
internal class GenerellBehandlingServiceTest(
    val dataSource: DataSource,
) {
    private lateinit var dao: GenerellBehandlingDao
    private lateinit var oppgaveDao: OppgaveDao
    private lateinit var hendelseDao: HendelseDao
    private lateinit var oppgaveService: OppgaveService
    private lateinit var sakRepo: SakDao
    private lateinit var service: GenerellBehandlingService
    private lateinit var behandlingRepo: BehandlingDao
    private val hendelser: BehandlingHendelserKafkaProducer = mockk()
    private val grunnlagKlient = mockk<GrunnlagKlient>()
    private val behandlingService = mockk<BehandlingService>()
    private val saksbehandlerInfoDao = mockk<SaksbehandlerInfoDao>()
    private val saksbehandlerNavn = "Ola Nordmann"
    private val saksbehandler = mockSaksbehandler("Z123456")

    @BeforeAll
    fun beforeAll() {
        dao = GenerellBehandlingDao(ConnectionAutoclosingTest(dataSource))
        oppgaveDao = OppgaveDaoImpl(ConnectionAutoclosingTest(dataSource))
        sakRepo = SakDao(ConnectionAutoclosingTest(dataSource))
        hendelseDao = spyk(HendelseDao(ConnectionAutoclosingTest(dataSource)))
        behandlingRepo =
            BehandlingDao(
                KommerBarnetTilGodeDao(ConnectionAutoclosingTest(dataSource)),
                RevurderingDao(ConnectionAutoclosingTest(dataSource)),
                ConnectionAutoclosingTest(dataSource),
            )
        oppgaveService =
            OppgaveService(
                OppgaveDaoMedEndringssporingImpl(oppgaveDao, ConnectionAutoclosingTest(dataSource)),
                sakRepo,
                hendelseDao,
                hendelser,
            )

        every { saksbehandlerInfoDao.hentSaksbehandlerNavn(any()) } returns saksbehandlerNavn
        service =
            GenerellBehandlingService(
                dao,
                oppgaveService,
                behandlingService,
                grunnlagKlient,
                hendelseDao,
                saksbehandlerInfoDao,
            )

        nyKontekstMedBrukerOgDatabase(saksbehandler, dataSource)
    }

    @AfterEach
    fun afterEach() {
        dataSource.connection.use {
            it.prepareStatement("TRUNCATE generellbehandling CASCADE; TRUNCATE oppgave CASCADE").execute()
        }
    }

    @Test
    fun `Kan kun oppdatere hvis statusen er opprettet`() {
        val sak = sakRepo.opprettSak("fnr", SakType.BARNEPENSJON, Enheter.AALESUND.enhetNr)
        val manueltOpprettetBehandling =
            GenerellBehandling.opprettFraType(
                GenerellBehandling.GenerellBehandlingType.KRAVPAKKE_UTLAND,
                sak.id,
            )
        val opprettBehandling = service.opprettBehandling(manueltOpprettetBehandling, SAKSBEHANDLER)
        dao.oppdaterGenerellBehandling(opprettBehandling.copy(status = GenerellBehandling.Status.FATTET))
        assertThrows<KanIkkeEndreGenerellBehandling> {
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
                skalSendeKravpakke = true,
            ),
        )
        val foerstegangsbehandlingHentet =
            behandlingRepo.hentBehandling(foerstegangsbehandling.id) as Foerstegangsbehandling
        val brukerTokenInfo = simpleSaksbehandler()
        every { behandlingService.hentBehandlingerForSak(sak.id) } returns listOf(foerstegangsbehandlingHentet)
        val doedsdato = LocalDate.parse("2016-12-30")
        val personopplysning = personOpplysning(doedsdato = doedsdato)
        val grunnlagsopplysningMedPersonopplysning = grunnlagsOpplysningMedPersonopplysning(personopplysning)
        coEvery {
            grunnlagKlient.finnPersonOpplysning(
                foerstegangsbehandling.id,
                Opplysningstype.AVDOED_PDL_V1,
                brukerTokenInfo,
            )
        } returns grunnlagsopplysningMedPersonopplysning

        val manueltOpprettetBehandling =
            GenerellBehandling
                .opprettFraType(
                    GenerellBehandling.GenerellBehandlingType.KRAVPAKKE_UTLAND,
                    sak.id,
                ).copy(tilknyttetBehandling = foerstegangsbehandling.id, status = GenerellBehandling.Status.ATTESTERT)
        val opprettBehandlingGenerell = service.opprettBehandling(manueltOpprettetBehandling, SAKSBEHANDLER)

        val kravpakkeMedArbeidetUtlandet = runBlocking { service.hentKravpakkeForSak(sak.id, brukerTokenInfo) }
        assertEquals(opprettBehandlingGenerell.id, kravpakkeMedArbeidetUtlandet.kravpakke.id)
        assertEquals(foerstegangsbehandling.id, kravpakkeMedArbeidetUtlandet.kravpakke.tilknyttetBehandling)
    }

    @Test
    fun `Kan hente behandlinger på sak`() {
        val sak = sakRepo.opprettSak("fnr", SakType.BARNEPENSJON, Enheter.AALESUND.enhetNr)
        val manueltOpprettetBehandling =
            GenerellBehandling.opprettFraType(
                GenerellBehandling.GenerellBehandlingType.KRAVPAKKE_UTLAND,
                sak.id,
            )
        val opprettBehandling = service.opprettBehandling(manueltOpprettetBehandling, SAKSBEHANDLER)
        assertEquals(GenerellBehandling.GenerellBehandlingType.KRAVPAKKE_UTLAND, opprettBehandling.type)
        assertEquals(sak.id, opprettBehandling.sakId)
        assertEquals(GenerellBehandling.Status.OPPRETTET, opprettBehandling.status)
        assertNotNull(opprettBehandling.opprettet)
        assertNull(opprettBehandling.innhold)

        val behandlinger = service.hentBehandlingerForSak(sak.id)
        assertEquals(1, behandlinger.size)
        behandlinger[0] shouldBe opprettBehandling
    }

    @Test
    fun kanOppretteBehandlingOgFaarDaOgsaaEnOppgaveManuellOpprettelseUtenTildelingAvSaksbehandler() {
        val sak = sakRepo.opprettSak("fnr", SakType.BARNEPENSJON, Enheter.AALESUND.enhetNr)
        val manueltOpprettetBehandling =
            GenerellBehandling.opprettFraType(
                GenerellBehandling.GenerellBehandlingType.KRAVPAKKE_UTLAND,
                sak.id,
            )
        val opprettBehandling = service.opprettBehandling(manueltOpprettetBehandling, SAKSBEHANDLER)
        assertEquals(GenerellBehandling.GenerellBehandlingType.KRAVPAKKE_UTLAND, opprettBehandling.type)
        assertEquals(sak.id, opprettBehandling.sakId)
        assertEquals(GenerellBehandling.Status.OPPRETTET, opprettBehandling.status)
        assertNotNull(opprettBehandling.opprettet)
        assertNull(opprettBehandling.innhold)

        val oppgaverforGenerellBehandling =
            oppgaveService.hentOppgaverForReferanse(manueltOpprettetBehandling.id.toString())
        assertEquals(1, oppgaverforGenerellBehandling.size)
        val manuellBehandlingOppgave = oppgaverforGenerellBehandling[0]
        assertEquals(manueltOpprettetBehandling.id.toString(), manuellBehandlingOppgave.referanse)
        assertEquals(OppgaveKilde.GENERELL_BEHANDLING, manuellBehandlingOppgave.kilde)
        assertEquals(OppgaveType.KRAVPAKKE_UTLAND, manuellBehandlingOppgave.type)
        assertEquals(sak.id, manuellBehandlingOppgave.sakId)
        assertNull(manuellBehandlingOppgave.saksbehandler)
    }

    @Test
    fun kanOppretteBehandlingOgFaarDaOgsaaEnOppgaveManuellOpprettelseMedTildelingAvSaksbehandler() {
        val sak = sakRepo.opprettSak("fnr", SakType.BARNEPENSJON, Enheter.AALESUND.enhetNr)
        val behandlingId = randomUUID()
        val manueltOpprettetBehandling =
            GenerellBehandling.opprettUtland(
                sak.id,
                behandlingId,
            )
        val opprettBehandling = service.opprettBehandling(manueltOpprettetBehandling, SAKSBEHANDLER)
        assertEquals(GenerellBehandling.GenerellBehandlingType.KRAVPAKKE_UTLAND, opprettBehandling.type)
        assertEquals(sak.id, opprettBehandling.sakId)
        assertEquals(GenerellBehandling.Status.OPPRETTET, opprettBehandling.status)
        assertNotNull(opprettBehandling.opprettet)
        assertNull(opprettBehandling.innhold)

        val oppgaveForFoerstegangsBehandling =
            oppgaveService.opprettFoerstegangsbehandlingsOppgaveForInnsendtSoeknad(behandlingId.toString(), sak.id)
        val saksbehandler = "saksbehandler"
        oppgaveService.tildelSaksbehandler(oppgaveForFoerstegangsBehandling.id, saksbehandler)

        val tokenInfo = simpleSaksbehandler(saksbehandler)

        oppgaveService.ferdigStillOppgaveUnderBehandling(
            behandlingId.toString(),
            OppgaveType.FOERSTEGANGSBEHANDLING,
            tokenInfo,
        )

        val oppgaverforGenerellBehandling =
            oppgaveService.hentOppgaverForReferanse(manueltOpprettetBehandling.id.toString())
        assertEquals(1, oppgaverforGenerellBehandling.size)
        val manuellBehandlingOppgave = oppgaverforGenerellBehandling[0]
        assertEquals(manueltOpprettetBehandling.id.toString(), manuellBehandlingOppgave.referanse)
        assertEquals(OppgaveKilde.GENERELL_BEHANDLING, manuellBehandlingOppgave.kilde)
        assertEquals(OppgaveType.KRAVPAKKE_UTLAND, manuellBehandlingOppgave.type)
        assertEquals(sak.id, manuellBehandlingOppgave.sakId)
        assertNull(manuellBehandlingOppgave.saksbehandler)

        verify {
            hendelseDao.generellBehandlingHendelse(
                any(),
                any(),
                GenerellBehandlingHendelseType.OPPRETTET,
                any(),
                any(),
            )
        }
    }

    @Test
    fun `Kan avbryte redigerbar behandling`() {
        val sak = sakRepo.opprettSak("fnr", SakType.BARNEPENSJON, Enheter.AALESUND.enhetNr)
        val behandlingId = randomUUID()
        val manueltOpprettetBehandling =
            GenerellBehandling.opprettUtland(
                sak.id,
                behandlingId,
            )
        val opprettBehandling = service.opprettBehandling(manueltOpprettetBehandling, SAKSBEHANDLER)
        assertEquals(GenerellBehandling.Status.OPPRETTET, opprettBehandling.status)
        val hentOppgaverForReferanse = oppgaveService.hentOppgaverForReferanse(opprettBehandling.id.toString())
        assertEquals(1, hentOppgaverForReferanse.size)
        val utlandsOppgave = hentOppgaverForReferanse[0]
        oppgaveService.tildelSaksbehandler(utlandsOppgave.id, SAKSBEHANDLER.ident)

        service.avbrytBehandling(opprettBehandling.id, SAKSBEHANDLER)
        val avbruttBehandling = service.hentBehandlingMedId(opprettBehandling.id)
        assertEquals(GenerellBehandling.Status.AVBRUTT, avbruttBehandling!!.status)
        val avbruttOppgave = oppgaveService.hentOppgave(utlandsOppgave.id)
        assertEquals(Status.AVBRUTT, avbruttOppgave.status)
    }

    @Test
    fun `Kan ikke avbryte fattet behandling`() {
        val sak = sakRepo.opprettSak("fnr", SakType.BARNEPENSJON, Enheter.AALESUND.enhetNr)
        val behandlingId = randomUUID()
        val manueltOpprettetBehandling =
            GenerellBehandling.opprettUtland(
                sak.id,
                behandlingId,
            )
        val opprettBehandling = service.opprettBehandling(manueltOpprettetBehandling, SAKSBEHANDLER)
        assertEquals(GenerellBehandling.Status.OPPRETTET, opprettBehandling.status)

        val hentOppgaverForReferanse = oppgaveService.hentOppgaverForReferanse(opprettBehandling.id.toString())
        assertEquals(1, hentOppgaverForReferanse.size)
        val utlandsOppgave = hentOppgaverForReferanse[0]

        oppgaveService.tildelSaksbehandler(utlandsOppgave.id, SAKSBEHANDLER.ident)
        val kravpakkeUtlandInnhold =
            Innhold.KravpakkeUtland(
                listOf("AFG"),
                listOf(DokumentMedSendtDato("P2000", true, LocalDate.now())),
                "2grwg2",
                "124124124",
            )
        val behandlingUtfylt = opprettBehandling.copy(innhold = kravpakkeUtlandInnhold)
        val oppdaterBehandling = service.lagreNyeOpplysninger(behandlingUtfylt)
        service.sendTilAttestering(oppdaterBehandling, SAKSBEHANDLER)
        val fattetBehandling = service.hentBehandlingMedId(oppdaterBehandling.id)

        fattetBehandling?.status shouldBe GenerellBehandling.Status.FATTET
        assertThrows<KanIkkeEndreGenerellBehandling> {
            service.avbrytBehandling(opprettBehandling.id, SAKSBEHANDLER)
        }
    }

    @Test
    fun `Kan sende til attestering(fatte)`() {
        val sak = sakRepo.opprettSak("fnr", SakType.BARNEPENSJON, Enheter.AALESUND.enhetNr)
        val behandlingId = randomUUID()
        val manueltOpprettetBehandling =
            GenerellBehandling.opprettUtland(
                sak.id,
                behandlingId,
            )
        val opprettBehandling = service.opprettBehandling(manueltOpprettetBehandling, SAKSBEHANDLER)
        assertEquals(GenerellBehandling.Status.OPPRETTET, opprettBehandling.status)

        val hentOppgaverForReferanse = oppgaveService.hentOppgaverForReferanse(opprettBehandling.id.toString())
        assertEquals(1, hentOppgaverForReferanse.size)
        val utlandsOppgave = hentOppgaverForReferanse[0]

        oppgaveService.tildelSaksbehandler(utlandsOppgave.id, SAKSBEHANDLER.ident)
        val kravpakkeUtlandInnhold =
            Innhold.KravpakkeUtland(
                listOf("AFG"),
                listOf(DokumentMedSendtDato("P2000", true, LocalDate.now())),
                "2grwg2",
                "124124124",
            )
        val behandlingUtfylt = opprettBehandling.copy(innhold = kravpakkeUtlandInnhold)
        val oppdaterBehandling = service.lagreNyeOpplysninger(behandlingUtfylt)
        service.sendTilAttestering(oppdaterBehandling, SAKSBEHANDLER)
        val fattetBehandling = service.hentBehandlingMedId(oppdaterBehandling.id)

        fattetBehandling?.status shouldBe GenerellBehandling.Status.FATTET

        val oppgaveTilAttestering =
            oppgaveService.hentOppgaverForReferanse(opprettBehandling.id.toString()).single()

        oppgaveTilAttestering.status shouldBe Status.ATTESTERING
        oppgaveTilAttestering.merknad shouldContain saksbehandlerNavn

        verify {
            hendelseDao.generellBehandlingHendelse(
                any(),
                any(),
                GenerellBehandlingHendelseType.FATTET,
                any(),
                any(),
            )
        }
    }

    @Test
    fun `Skal feile når dato sendt mangler på dokument`() {
        val sak = sakRepo.opprettSak("fnr", SakType.BARNEPENSJON, Enheter.AALESUND.enhetNr)
        val behandlingId = randomUUID()
        val manueltOpprettetBehandling =
            GenerellBehandling.opprettUtland(
                sak.id,
                behandlingId,
            )
        val opprettBehandling = service.opprettBehandling(manueltOpprettetBehandling, SAKSBEHANDLER)
        val hentOppgaverForReferanse = oppgaveService.hentOppgaverForReferanse(opprettBehandling.id.toString())
        assertEquals(1, hentOppgaverForReferanse.size)
        val utlandsOppgave = hentOppgaverForReferanse[0]

        oppgaveService.tildelSaksbehandler(utlandsOppgave.id, SAKSBEHANDLER.ident)
        val kravpakkeUtlandInnhold =
            Innhold.KravpakkeUtland(
                listOf("AFG"),
                listOf(DokumentMedSendtDato("P2000", true, null)),
                "2grwg2",
                "124124124",
            )
        val behandlingUtfylt = opprettBehandling.copy(innhold = kravpakkeUtlandInnhold)
        val oppdaterBehandling = service.lagreNyeOpplysninger(behandlingUtfylt)
        assertThrows<DokumentManglerDatoException> {
            service.sendTilAttestering(oppdaterBehandling, SAKSBEHANDLER)
        }
    }

    @Test
    fun `Kan ikke attestere med feil status på behandling`() {
        val sak = sakRepo.opprettSak("fnr", SakType.BARNEPENSJON, Enheter.AALESUND.enhetNr)
        val behandlingId = randomUUID()
        val manueltOpprettetBehandling =
            GenerellBehandling.opprettUtland(
                sak.id,
                behandlingId,
            )
        val opprettBehandling = service.opprettBehandling(manueltOpprettetBehandling, SAKSBEHANDLER)
        val hentOppgaverForReferanse = oppgaveService.hentOppgaverForReferanse(opprettBehandling.id.toString())
        assertEquals(1, hentOppgaverForReferanse.size)
        val utlandsOppgave = hentOppgaverForReferanse[0]

        oppgaveService.tildelSaksbehandler(utlandsOppgave.id, SAKSBEHANDLER.ident)
        val kravpakkeUtlandInnhold =
            Innhold.KravpakkeUtland(
                listOf("AFG"),
                listOf(DokumentMedSendtDato("P2000", true, LocalDate.now())),
                "2grwg2",
                "124124124",
            )
        val behandlingUtfylt =
            opprettBehandling.copy(innhold = kravpakkeUtlandInnhold, status = GenerellBehandling.Status.FATTET)
        val oppdaterBehandling = service.lagreNyeOpplysninger(behandlingUtfylt)
        assertThrows<IllegalStateException> {
            service.sendTilAttestering(oppdaterBehandling, SAKSBEHANDLER)
        }
    }

    @Test
    fun `Skal feile når rina nummer er tomt`() {
        val sak = sakRepo.opprettSak("fnr", SakType.BARNEPENSJON, Enheter.AALESUND.enhetNr)
        val behandlingId = randomUUID()
        val manueltOpprettetBehandling =
            GenerellBehandling.opprettUtland(
                sak.id,
                behandlingId,
            )
        val opprettBehandling = service.opprettBehandling(manueltOpprettetBehandling, SAKSBEHANDLER)
        val hentOppgaverForReferanse = oppgaveService.hentOppgaverForReferanse(opprettBehandling.id.toString())
        assertEquals(1, hentOppgaverForReferanse.size)
        val utlandsOppgave = hentOppgaverForReferanse[0]

        oppgaveService.tildelSaksbehandler(utlandsOppgave.id, SAKSBEHANDLER.ident)
        val kravpakkeUtlandInnhold =
            Innhold.KravpakkeUtland(
                listOf("AFG"),
                listOf(DokumentMedSendtDato("P2000", true, LocalDate.now())),
                "2grwg2",
                "",
            )
        val behandlingUtfylt = opprettBehandling.copy(innhold = kravpakkeUtlandInnhold)
        val oppdaterBehandling = service.lagreNyeOpplysninger(behandlingUtfylt)
        assertThrows<ManglerRinanummerException> {
            service.sendTilAttestering(oppdaterBehandling, SAKSBEHANDLER)
        }
    }

    @Test
    fun `Skal feile hvis landkodelisten er tom`() {
        val sak = sakRepo.opprettSak("fnr", SakType.BARNEPENSJON, Enheter.AALESUND.enhetNr)
        val behandlingId = randomUUID()
        val manueltOpprettetBehandling =
            GenerellBehandling.opprettUtland(
                sak.id,
                behandlingId,
            )
        val opprettBehandling = service.opprettBehandling(manueltOpprettetBehandling, SAKSBEHANDLER)
        val hentOppgaverForReferanse = oppgaveService.hentOppgaverForReferanse(opprettBehandling.id.toString())
        assertEquals(1, hentOppgaverForReferanse.size)
        val utlandsOppgave = hentOppgaverForReferanse[0]

        oppgaveService.tildelSaksbehandler(utlandsOppgave.id, SAKSBEHANDLER.ident)
        val kravpakkeUtlandInnhold =
            Innhold.KravpakkeUtland(
                emptyList(),
                listOf(DokumentMedSendtDato("P2000", true, LocalDate.now())),
                "2grwg2",
                "rinanummer",
            )
        val behandlingUtfylt = opprettBehandling.copy(innhold = kravpakkeUtlandInnhold)
        val oppdaterBehandling = service.lagreNyeOpplysninger(behandlingUtfylt)
        assertThrows<ManglerLandkodeException> {
            service.sendTilAttestering(oppdaterBehandling, SAKSBEHANDLER)
        }
    }

    @Test
    fun `Skal feile når isokode 3 er feil lengde`() {
        val sak = sakRepo.opprettSak("fnr", SakType.BARNEPENSJON, Enheter.AALESUND.enhetNr)
        val behandlingId = randomUUID()
        val manueltOpprettetBehandling =
            GenerellBehandling.opprettUtland(
                sak.id,
                behandlingId,
            )
        val opprettBehandling = service.opprettBehandling(manueltOpprettetBehandling, SAKSBEHANDLER)
        val hentOppgaverForReferanse = oppgaveService.hentOppgaverForReferanse(opprettBehandling.id.toString())
        assertEquals(1, hentOppgaverForReferanse.size)
        val utlandsOppgave = hentOppgaverForReferanse[0]

        oppgaveService.tildelSaksbehandler(utlandsOppgave.id, SAKSBEHANDLER.ident)
        val kravpakkeUtlandInnhold =
            Innhold.KravpakkeUtland(
                listOf("AFG", "ABDEF"),
                listOf(DokumentMedSendtDato("P2000", true, LocalDate.now())),
                "2grwg2",
                "rinanummer",
            )
        val behandlingUtfylt = opprettBehandling.copy(innhold = kravpakkeUtlandInnhold)
        val oppdaterBehandling = service.lagreNyeOpplysninger(behandlingUtfylt)
        assertThrows<UgyldigLandkodeIsokode3> {
            service.sendTilAttestering(oppdaterBehandling, SAKSBEHANDLER)
        }
    }

    @Test
    fun `Kan attestere behandling`() {
        val sak = sakRepo.opprettSak("fnr", SakType.BARNEPENSJON, Enheter.AALESUND.enhetNr)
        val behandlingId = randomUUID()
        val manueltOpprettetBehandling =
            GenerellBehandling.opprettUtland(
                sak.id,
                behandlingId,
            )
        val opprettBehandling = service.opprettBehandling(manueltOpprettetBehandling, SAKSBEHANDLER)
        assertEquals(GenerellBehandling.Status.OPPRETTET, opprettBehandling.status)

        val hentOppgaverForReferanse = oppgaveService.hentOppgaverForReferanse(opprettBehandling.id.toString())
        assertEquals(1, hentOppgaverForReferanse.size)
        val utlandsOppgave = hentOppgaverForReferanse[0]

        oppgaveService.tildelSaksbehandler(utlandsOppgave.id, SAKSBEHANDLER.ident)
        val kravpakkeUtlandInnhold =
            Innhold.KravpakkeUtland(
                listOf("AFG"),
                listOf(DokumentMedSendtDato("P2000", true, LocalDate.now())),
                "2grwg2",
                "124124124",
            )
        val behandlingUtfylt = opprettBehandling.copy(innhold = kravpakkeUtlandInnhold)
        val oppdaterBehandling = service.lagreNyeOpplysninger(behandlingUtfylt)
        service.sendTilAttestering(oppdaterBehandling, SAKSBEHANDLER)

        val fattetBehandling = service.hentBehandlingMedId(oppdaterBehandling.id)
        assertEquals(GenerellBehandling.Status.FATTET, fattetBehandling?.status)

        val behandlingsOppgaverFattetOgAttestering =
            oppgaveService.hentOppgaverForReferanse(opprettBehandling.id.toString())
        assertEquals(1, behandlingsOppgaverFattetOgAttestering.size)

        behandlingsOppgaverFattetOgAttestering.forExactly(1) { oppgave ->
            oppgave.status shouldBe Status.ATTESTERING
            oppgave.type.shouldBe(OppgaveType.KRAVPAKKE_UTLAND)
        }

        val attestant = ATTESTANT
        val attesteringsOppgave =
            oppgaveService
                .hentOppgaverForReferanse(opprettBehandling.id.toString())
                .single(OppgaveIntern::erAttestering)

        val trettidagerfrem = Tidspunkt.now().plus(30L, ChronoUnit.DAYS).toNorskLocalDate()
        assertEquals(trettidagerfrem, attesteringsOppgave.frist!!.toNorskLocalDate())
        oppgaveService.tildelSaksbehandler(attesteringsOppgave.id, attestant.ident)

        service.attester(oppdaterBehandling.id, attestant)
        val attestertBehandling = service.hentBehandlingMedId(oppdaterBehandling.id)
        assertEquals(GenerellBehandling.Status.ATTESTERT, attestertBehandling?.status)

        verify {
            hendelseDao.generellBehandlingHendelse(
                any(),
                any(),
                GenerellBehandlingHendelseType.ATTESTERT,
                any(),
                any(),
            )
        }
    }

    @Test
    fun `Kan ikke attestere behandling hvis det er samme saksbehandler som behandlet`() {
        val sak = sakRepo.opprettSak("fnr", SakType.BARNEPENSJON, Enheter.AALESUND.enhetNr)
        val behandlingId = randomUUID()
        val manueltOpprettetBehandling =
            GenerellBehandling.opprettUtland(
                sak.id,
                behandlingId,
            )
        val opprettBehandling = service.opprettBehandling(manueltOpprettetBehandling, SAKSBEHANDLER)
        assertEquals(GenerellBehandling.Status.OPPRETTET, opprettBehandling.status)

        val hentOppgaverForReferanse = oppgaveService.hentOppgaverForReferanse(opprettBehandling.id.toString())
        assertEquals(1, hentOppgaverForReferanse.size)
        val utlandsOppgave = hentOppgaverForReferanse[0]

        oppgaveService.tildelSaksbehandler(utlandsOppgave.id, SAKSBEHANDLER.ident)
        val kravpakkeUtlandInnhold =
            Innhold.KravpakkeUtland(
                listOf("AFG"),
                listOf(DokumentMedSendtDato("P2000", true, LocalDate.now())),
                "2grwg2",
                "124124124",
            )
        val behandlingUtfylt = opprettBehandling.copy(innhold = kravpakkeUtlandInnhold)
        val oppdaterBehandling = service.lagreNyeOpplysninger(behandlingUtfylt)
        service.sendTilAttestering(oppdaterBehandling, SAKSBEHANDLER)
        val fattetBehandling = service.hentBehandlingMedId(oppdaterBehandling.id)
        assertEquals(GenerellBehandling.Status.FATTET, fattetBehandling?.status)
        val behandlingsOppgaverFattetOgAttestering =
            oppgaveService.hentOppgaverForReferanse(opprettBehandling.id.toString())
        assertEquals(1, behandlingsOppgaverFattetOgAttestering.size)

        behandlingsOppgaverFattetOgAttestering.forExactly(1) { oppgave ->
            oppgave.status shouldBe Status.ATTESTERING
            oppgave.type.shouldBe(OppgaveType.KRAVPAKKE_UTLAND)
        }

        val attestant = SAKSBEHANDLER // Intentional
        val attesteringsOppgave = oppgaveService.hentOppgaverForReferanse(opprettBehandling.id.toString()).single()

        val saksbehandlerMedRoller =
            mockk<SaksbehandlerMedRoller> {
                every { harRolleAttestant() } returns true
            }
        every { saksbehandler.saksbehandlerMedRoller } returns saksbehandlerMedRoller

        val trettidagerfrem = Tidspunkt.now().plus(30L, ChronoUnit.DAYS).toNorskLocalDate()

        assertEquals(trettidagerfrem, attesteringsOppgave.frist!!.toNorskLocalDate())
        oppgaveService.tildelSaksbehandler(attesteringsOppgave.id, attestant.ident)
        val ugyldigAttesteringsForespoersel =
            assertThrows<UgyldigAttesteringsForespoersel> {
                service.attester(oppdaterBehandling.id, attestant)
            }
        assertEquals("ATTESTERING_SAMME_SAKSBEHANDLER", ugyldigAttesteringsForespoersel.code)
    }

    @Test
    fun `Kan underkjenne behandling som er fattet`() {
        every { saksbehandler.saksbehandlerMedRoller.harRolleAttestant() } returns true

        val sak = sakRepo.opprettSak("fnr", SakType.BARNEPENSJON, Enheter.AALESUND.enhetNr)
        val behandling =
            service.opprettBehandling(GenerellBehandling.opprettUtland(sak.id, randomUUID()), SAKSBEHANDLER)

        val behandlingsoppgave = oppgaveService.hentOppgaverForReferanse(behandling.id.toString()).single()
        oppgaveService.tildelSaksbehandler(behandlingsoppgave.id, SAKSBEHANDLER.ident)

        val behandlingMedKravpakke = service.lagreNyeOpplysninger(behandling.copy(innhold = kravpakkeUtland()))
        service.sendTilAttestering(behandlingMedKravpakke, SAKSBEHANDLER)

        val nyAttesteringsoppgave =
            oppgaveService
                .hentOppgaverForReferanse(behandling.id.toString())
                .single(OppgaveIntern::erAttestering)

        oppgaveService.tildelSaksbehandler(nyAttesteringsoppgave.id, ATTESTANT.ident)

        val begrunnelse = "Ikke godkjent"
        val kommentar = Kommentar(begrunnelse)
        service.underkjenn(behandlingMedKravpakke.id, ATTESTANT, kommentar)
        val underkjentBehandling = service.hentBehandlingMedId(behandlingMedKravpakke.id)

        underkjentBehandling?.status shouldBe GenerellBehandling.Status.RETURNERT
        underkjentBehandling?.returnertKommenar shouldBe begrunnelse
        verify {
            hendelseDao.generellBehandlingHendelse(
                any(),
                any(),
                GenerellBehandlingHendelseType.UNDERKJENT,
                any(),
                any(),
                any(),
                kommentar.begrunnelse,
            )
        }
    }

    private fun kravpakkeUtland() =
        Innhold.KravpakkeUtland(
            listOf("AFG"),
            listOf(DokumentMedSendtDato("P2000", true, LocalDate.now())),
            "2grwg2",
            "124124124",
        )

    private companion object {
        val SAKSBEHANDLER = Saksbehandler("token", "saksbehandler", null)
        val ATTESTANT = Saksbehandler("token", "attestant", null)
    }
}
