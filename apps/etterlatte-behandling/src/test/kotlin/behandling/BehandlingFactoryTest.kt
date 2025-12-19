package no.nav.etterlatte.behandling

import io.kotest.matchers.shouldBe
import io.mockk.Runs
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.just
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.runs
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.SaksbehandlerMedEnheterOgRoller
import no.nav.etterlatte.behandling.aktivitetsplikt.AktivitetspliktDao
import no.nav.etterlatte.behandling.aktivitetsplikt.AktivitetspliktKopierService
import no.nav.etterlatte.behandling.domain.ArbeidsFordelingEnhet
import no.nav.etterlatte.behandling.domain.Foerstegangsbehandling
import no.nav.etterlatte.behandling.domain.OpprettBehandling
import no.nav.etterlatte.behandling.domain.Revurdering
import no.nav.etterlatte.behandling.hendelse.HendelseDao
import no.nav.etterlatte.behandling.kommerbarnettilgode.KommerBarnetTilGodeService
import no.nav.etterlatte.behandling.revurdering.RevurderingDao
import no.nav.etterlatte.behandling.revurdering.RevurderingService
import no.nav.etterlatte.common.Enheter
import no.nav.etterlatte.common.klienter.PdlTjenesterKlient
import no.nav.etterlatte.funksjonsbrytere.DummyFeatureToggleService
import no.nav.etterlatte.grunnlag.GrunnlagService
import no.nav.etterlatte.ktor.token.simpleSaksbehandler
import no.nav.etterlatte.libs.common.Enhetsnummer
import no.nav.etterlatte.libs.common.Vedtaksloesning
import no.nav.etterlatte.libs.common.behandling.BehandlingHendelseType
import no.nav.etterlatte.libs.common.behandling.BehandlingOpprinnelse
import no.nav.etterlatte.libs.common.behandling.BehandlingStatus
import no.nav.etterlatte.libs.common.behandling.BehandlingType
import no.nav.etterlatte.libs.common.behandling.BoddEllerArbeidetUtlandet
import no.nav.etterlatte.libs.common.behandling.JaNei
import no.nav.etterlatte.libs.common.behandling.KommerBarnetTilgode
import no.nav.etterlatte.libs.common.behandling.NyBehandlingRequest
import no.nav.etterlatte.libs.common.behandling.Persongalleri
import no.nav.etterlatte.libs.common.behandling.Prosesstype
import no.nav.etterlatte.libs.common.behandling.Revurderingaarsak
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.behandling.Virkningstidspunkt
import no.nav.etterlatte.libs.common.feilhaandtering.UgyldigForespoerselException
import no.nav.etterlatte.libs.common.grunnlag.Grunnlag
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.oppgave.OppgaveIntern
import no.nav.etterlatte.libs.common.oppgave.OppgaveKilde
import no.nav.etterlatte.libs.common.oppgave.OppgaveSaksbehandler
import no.nav.etterlatte.libs.common.oppgave.OppgaveType
import no.nav.etterlatte.libs.common.oppgave.Status
import no.nav.etterlatte.libs.common.oppgave.opprettNyOppgaveMedReferanseOgSak
import no.nav.etterlatte.libs.common.person.AdressebeskyttelseGradering
import no.nav.etterlatte.libs.common.sak.Sak
import no.nav.etterlatte.libs.common.sak.SakId
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.tidspunkt.toLocalDatetimeNorskTid
import no.nav.etterlatte.libs.common.tidspunkt.toLocalDatetimeUTC
import no.nav.etterlatte.libs.common.tidspunkt.toNorskTidspunkt
import no.nav.etterlatte.libs.common.vilkaarsvurdering.Vilkaarsvurdering
import no.nav.etterlatte.libs.common.vilkaarsvurdering.VilkaarsvurderingMedBehandlingGrunnlagsversjon
import no.nav.etterlatte.libs.testdata.behandling.VirkningstidspunktTestData
import no.nav.etterlatte.libs.testdata.grunnlag.AVDOED_FOEDSELSNUMMER
import no.nav.etterlatte.libs.testdata.grunnlag.GJENLEVENDE_FOEDSELSNUMMER
import no.nav.etterlatte.libs.testdata.grunnlag.INNSENDER_FOEDSELSNUMMER
import no.nav.etterlatte.nyKontekstMedBruker
import no.nav.etterlatte.oppgave.OppgaveService
import no.nav.etterlatte.revurdering
import no.nav.etterlatte.sak.SakLesDao
import no.nav.etterlatte.sak.SakService
import no.nav.etterlatte.sak.SakTilgang
import no.nav.etterlatte.tilgangsstyring.OppdaterTilgangService
import no.nav.etterlatte.vilkaarsvurdering.service.VilkaarsvurderingService
import no.nav.etterlatte.vilkaarsvurdering.vilkaar.BarnepensjonVilkaar1967
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDateTime
import java.time.YearMonth
import java.util.UUID

/*
Grunnlag vet ikke om behandlingen har blitt opprettet enda da opprett grunnlag kallet går så dette må gjøres med systembruker
grunnlagService.leggInnNyttGrunnlag(any(), any(), any(Systembruker::class))
 */
internal class BehandlingFactoryTest {
    private val user = mockk<SaksbehandlerMedEnheterOgRoller>(relaxed = true)
    private val behandlingDaoMock = mockk<BehandlingDao>(relaxUnitFun = true)
    private val hendelseDaoMock = mockk<HendelseDao>(relaxUnitFun = true)
    private val behandlingHendelserKafkaProducerMock = mockk<BehandlingHendelserKafkaProducer>(relaxUnitFun = true)
    private val kommerBarnetTilGodeServiceMock = mockk<KommerBarnetTilGodeService>()
    private val vilkaarsvurderingService = mockk<VilkaarsvurderingService>()
    private val grunnlagService = mockk<GrunnlagService>(relaxUnitFun = true)
    private val oppgaveService =
        mockk<OppgaveService> {
            every { oppdaterEnhetForRelaterteOppgaver(any()) } just Runs
        }
    private val brukerService =
        mockk<BrukerService> {
            every { finnEnhetForPersonOgTema(any(), any(), any()) } returns
                ArbeidsFordelingEnhet(Enheter.defaultEnhet.navn, Enheter.defaultEnhet.enhetNr)
        }
    private val sakTilgang =
        mockk<SakTilgang> {
            every { oppdaterAdressebeskyttelse(any(), any()) } just Runs
            every { oppdaterSkjerming(any(), any()) } just Runs
        }
    private val aktivitetspliktDao = mockk<AktivitetspliktDao>()
    private val aktivitetspliktKopierService = mockk<AktivitetspliktKopierService>()
    private val gyldighetsproevingService = mockk<GyldighetsproevingService>(relaxUnitFun = true)
    private val pdlTjenesterKlientMock = mockk<PdlTjenesterKlient>()
    private val mockOppgave =
        opprettNyOppgaveMedReferanseOgSak(
            "behandling",
            Sak("ident", SakType.BARNEPENSJON, sakId1, Enheter.AALESUND.enhetNr, null, false),
            OppgaveKilde.BEHANDLING,
            OppgaveType.FOERSTEGANGSBEHANDLING,
            null,
        )
    private val kommerBarnetTilGodeService =
        mockk<KommerBarnetTilGodeService>().also {
            every { it.hentKommerBarnetTilGode(any()) } returns null
        }
    private val revurderingDao = mockk<RevurderingDao>()
    private val revurderingService =
        RevurderingService(
            oppgaveService,
            grunnlagService,
            behandlingHendelserKafkaProducerMock,
            behandlingDaoMock,
            hendelseDaoMock,
            kommerBarnetTilGodeService,
            revurderingDao,
            aktivitetspliktDao,
            aktivitetspliktKopierService,
        )

    private val sakLesDaoMock = mockk<SakLesDao>()
    private val sakServiceMock = mockk<SakService>()
    private val featureToggleService = DummyFeatureToggleService()
    private val behandlingFactory =
        BehandlingFactory(
            oppgaveService,
            grunnlagService,
            revurderingService,
            gyldighetsproevingService,
            sakServiceMock,
            behandlingDaoMock,
            hendelseDaoMock,
            behandlingHendelserKafkaProducerMock,
            vilkaarsvurderingService = vilkaarsvurderingService,
            kommerBarnetTilGodeService = kommerBarnetTilGodeServiceMock,
            behandlingInfoService = mockk(),
            tilgangsService =
                OppdaterTilgangService(
                    mockk(relaxed = true),
                    mockk(relaxed = true),
                    brukerService = brukerService,
                    oppgaveService,
                    mockk(relaxed = true),
                    sakTilgang,
                    sakLesDaoMock,
                    featureToggleService,
                ),
        )

    @BeforeEach
    fun before() {
        every { user.name() } returns "User"
        every { user.enheter() } returns listOf(Enheter.defaultEnhet.enhetNr)
        justRun { oppgaveService.avbrytAapneOppgaverMedReferanse(any()) }
        every { grunnlagService.hentOpplysningsgrunnlagForSak(any()) } returns Grunnlag.empty()

        nyKontekstMedBruker(user)
    }

    @AfterEach
    fun after() {
        confirmVerified(
            sakTilgang,
            behandlingDaoMock,
            hendelseDaoMock,
            behandlingHendelserKafkaProducerMock,
            grunnlagService,
            vilkaarsvurderingService,
            kommerBarnetTilGodeServiceMock,
        )
        clearAllMocks()
    }

    @Test
    fun startBehandling() {
        val behandlingOpprettes = slot<OpprettBehandling>()
        val behandlingHentes = slot<UUID>()
        val datoNaa = Tidspunkt.now().toLocalDatetimeUTC()

        val opprettetBehandling =
            Foerstegangsbehandling(
                id = UUID.randomUUID(),
                sak =
                    Sak(
                        ident = "Soeker",
                        sakType = SakType.BARNEPENSJON,
                        id = sakId1,
                        enhet = Enheter.defaultEnhet.enhetNr,
                        adressebeskyttelse = null,
                        erSkjermet = false,
                    ),
                behandlingOpprettet = datoNaa,
                sistEndret = datoNaa,
                status = BehandlingStatus.OPPRETTET,
                // Hvis søknad mottatt dato er null (ukjent) så skal oppgave opprettes med null for frist
                soeknadMottattDato = null,
                gyldighetsproeving = null,
                virkningstidspunkt =
                    Virkningstidspunkt(
                        YearMonth.of(2022, 1),
                        Grunnlagsopplysning.Saksbehandler.create("ident"),
                        "begrunnelse",
                    ),
                utlandstilknytning = null,
                boddEllerArbeidetUtlandet = null,
                kommerBarnetTilgode = null,
                vedtaksloesning = Vedtaksloesning.GJENNY,
                sendeBrev = true,
            )

        val persongalleri =
            Persongalleri(
                "Soeker",
                "Innsender",
                emptyList(),
                listOf("Avdoed"),
                listOf("Gjenlevende"),
            )

        every { user.enheter() } returns listOf(Enheter.defaultEnhet.enhetNr)
        every { sakServiceMock.finnSak(any()) } returns opprettetBehandling.sak
        every { behandlingDaoMock.opprettBehandling(capture(behandlingOpprettes)) } returns Unit
        every { behandlingDaoMock.hentBehandling(capture(behandlingHentes)) } returns opprettetBehandling
        every { hendelseDaoMock.behandlingOpprettet(any()) } returns Unit
        every { behandlingDaoMock.lagreGyldighetsproeving(any(), any()) } returns Unit
        every { behandlingDaoMock.hentBehandlingerForSak(any()) } returns emptyList()
        every { behandlingDaoMock.lagreStatus(any()) } returns Unit
        every {
            behandlingHendelserKafkaProducerMock.sendMeldingForHendelseStatistikk(
                any(),
                any(),
            )
        } returns Unit
        coEvery { grunnlagService.opprettGrunnlag(any(), any()) } returns Unit
        every { oppgaveService.opprettOppgave(any(), any(), any(), any(), any(), gruppeId = any(), frist = any()) } returns mockOppgave
        every { sakLesDaoMock.hentSak(opprettetBehandling.sak.id) } returns opprettetBehandling.sak

        val resultat =
            behandlingFactory
                .opprettBehandling(
                    sakId1,
                    persongalleri,
                    datoNaa.toString(),
                    Vedtaksloesning.GJENNY,
                    behandlingFactory.hentDataForOpprettBehandling(sakId1),
                    BehandlingOpprinnelse.UKJENT,
                ).also { it.sendMeldingForHendelse() }
                .behandling

        Assertions.assertEquals(opprettetBehandling, resultat)
        Assertions.assertEquals(opprettetBehandling.sak, resultat.sak)
        Assertions.assertEquals(opprettetBehandling.id, resultat.id)
        Assertions.assertEquals(opprettetBehandling.behandlingOpprettet, resultat.behandlingOpprettet)
        Assertions.assertEquals(sakId1, behandlingOpprettes.captured.sakId)
        Assertions.assertEquals(behandlingHentes.captured, behandlingOpprettes.captured.id)

        verify(exactly = 1) {
            sakServiceMock.finnSak(any())
        }
        verify(exactly = 1) {
            behandlingDaoMock.hentBehandling(any())
            behandlingDaoMock.opprettBehandling(any())
            hendelseDaoMock.behandlingOpprettet(any())
            behandlingDaoMock.hentBehandlingerForSak(any())
            behandlingHendelserKafkaProducerMock.sendMeldingForHendelseStatistikk(
                any(),
                BehandlingHendelseType.OPPRETTET,
            )
            oppgaveService.opprettOppgave(
                referanse = resultat.id.toString(),
                sakId = sakId1,
                kilde = OppgaveKilde.BEHANDLING,
                type = OppgaveType.FOERSTEGANGSBEHANDLING,
                merknad = any(),
                gruppeId = "Avdoed",
                // Sjekker at oppgave opprettes med null for frist
                frist = null,
            )
            grunnlagService.hentOpplysningsgrunnlagForSak(any())
        }
        coVerify(exactly = 1) {
            grunnlagService.opprettGrunnlag(any(), any())
        }
        verify(exactly = 1) {
            sakTilgang.oppdaterAdressebeskyttelse(any(), any())
        }
    }

    @Test
    fun `skal opprette kun foerstegangsbehandling hvis det ikke finnes noen tidligere behandlinger`() {
        val behandlingOpprettes = slot<OpprettBehandling>()
        val behandlingHentes = slot<UUID>()
        val datoNaa = Tidspunkt.now().toLocalDatetimeUTC()

        val tidspunktMottattBehandling = Tidspunkt.now().toLocalDatetimeUTC().minusDays(15L)
        val opprettetBehandling =
            Foerstegangsbehandling(
                id = UUID.randomUUID(),
                sak =
                    Sak(
                        ident = "Soeker",
                        sakType = SakType.BARNEPENSJON,
                        id = sakId1,
                        enhet = Enheter.defaultEnhet.enhetNr,
                        adressebeskyttelse = null,
                        erSkjermet = false,
                    ),
                behandlingOpprettet = datoNaa,
                sistEndret = datoNaa,
                status = BehandlingStatus.OPPRETTET,
                soeknadMottattDato = tidspunktMottattBehandling,
                gyldighetsproeving = null,
                virkningstidspunkt =
                    Virkningstidspunkt(
                        YearMonth.of(2022, 1),
                        Grunnlagsopplysning.Saksbehandler.create("ident"),
                        "begrunnelse",
                    ),
                utlandstilknytning = null,
                boddEllerArbeidetUtlandet = null,
                kommerBarnetTilgode = null,
                vedtaksloesning = Vedtaksloesning.GJENNY,
                sendeBrev = true,
            )

        val persongalleri =
            Persongalleri(
                "Soeker",
                "Innsender",
                emptyList(),
                listOf("Avdoed"),
                listOf("Gjenlevende"),
            )

        every { sakLesDaoMock.hentSak(opprettetBehandling.sak.id) } returns opprettetBehandling.sak
        every { sakServiceMock.finnSak(any()) } returns opprettetBehandling.sak
        every { behandlingDaoMock.opprettBehandling(capture(behandlingOpprettes)) } returns Unit
        every { behandlingDaoMock.hentBehandling(capture(behandlingHentes)) } returns opprettetBehandling
        every { behandlingDaoMock.hentBehandlingerForSak(any()) } returns emptyList()
        every { hendelseDaoMock.behandlingOpprettet(any()) } returns Unit
        every {
            behandlingHendelserKafkaProducerMock.sendMeldingForHendelseStatistikk(
                any(),
                any(),
            )
        } returns Unit
        coEvery { grunnlagService.opprettGrunnlag(any(), any()) } returns Unit
        every { oppgaveService.opprettOppgave(any(), any(), any(), any(), any(), gruppeId = any(), frist = any()) } returns mockOppgave

        val foerstegangsbehandling =
            behandlingFactory
                .opprettBehandling(
                    sakId1,
                    persongalleri,
                    datoNaa.toString(),
                    Vedtaksloesning.GJENNY,
                    behandlingFactory.hentDataForOpprettBehandling(sakId1),
                    BehandlingOpprinnelse.UKJENT,
                ).also { it.sendMeldingForHendelse() }
                .behandling

        Assertions.assertTrue(foerstegangsbehandling is Foerstegangsbehandling)

        verify(exactly = 1) {
            sakServiceMock.finnSak(any())
        }
        verify(exactly = 1) {
            behandlingDaoMock.hentBehandling(any())
            behandlingDaoMock.opprettBehandling(any())
            hendelseDaoMock.behandlingOpprettet(any())
            behandlingDaoMock.hentBehandlingerForSak(any())
            behandlingHendelserKafkaProducerMock.sendMeldingForHendelseStatistikk(any(), any())
            oppgaveService.opprettOppgave(
                referanse = foerstegangsbehandling.id.toString(),
                sakId = sakId1,
                kilde = OppgaveKilde.BEHANDLING,
                type = OppgaveType.FOERSTEGANGSBEHANDLING,
                merknad = any(),
                gruppeId = persongalleri.avdoed.single(),
                frist = tidspunktMottattBehandling.plusMonths(1L).toNorskTidspunkt(),
            )
            grunnlagService.hentOpplysningsgrunnlagForSak(any())
        }
        coVerify {
            grunnlagService.opprettGrunnlag(any(), any())
        }
        verify(exactly = 1) {
            sakTilgang.oppdaterAdressebeskyttelse(any(), any())
        }
    }

    @Test
    fun `skal avbryte behandling hvis under behandling og opprette en ny`() {
        val behandlingOpprettes = slot<OpprettBehandling>()
        val behandlingHentes = slot<UUID>()
        val datoNaa = Tidspunkt.now().toLocalDatetimeUTC()

        val underArbeidBehandling =
            Foerstegangsbehandling(
                id = UUID.randomUUID(),
                sak =
                    Sak(
                        ident = "Soeker",
                        sakType = SakType.BARNEPENSJON,
                        id = sakId1,
                        enhet = Enheter.defaultEnhet.enhetNr,
                        adressebeskyttelse = null,
                        erSkjermet = false,
                    ),
                behandlingOpprettet = datoNaa,
                sistEndret = datoNaa,
                status = BehandlingStatus.OPPRETTET,
                soeknadMottattDato = Tidspunkt.now().toLocalDatetimeUTC(),
                gyldighetsproeving = null,
                virkningstidspunkt =
                    Virkningstidspunkt(
                        YearMonth.of(2022, 1),
                        Grunnlagsopplysning.Saksbehandler.create("ident"),
                        "begrunnelse",
                    ),
                utlandstilknytning = null,
                boddEllerArbeidetUtlandet = null,
                kommerBarnetTilgode = null,
                vedtaksloesning = Vedtaksloesning.GJENNY,
                sendeBrev = true,
            )

        val persongalleri =
            Persongalleri(
                "Soeker",
                "Innsender",
                emptyList(),
                listOf("Avdoed"),
                listOf("Gjenlevende"),
            )

        every { sakLesDaoMock.hentSak(underArbeidBehandling.sak.id) } returns underArbeidBehandling.sak
        every { sakServiceMock.finnSak(any()) } returns underArbeidBehandling.sak
        every { behandlingDaoMock.opprettBehandling(capture(behandlingOpprettes)) } returns Unit
        every { behandlingDaoMock.hentBehandling(capture(behandlingHentes)) } returns underArbeidBehandling
        every {
            behandlingDaoMock.hentBehandlingerForSak(any())
        } returns emptyList()
        every { behandlingDaoMock.lagreStatus(any()) } returns Unit
        every { hendelseDaoMock.behandlingOpprettet(any()) } returns Unit
        every {
            behandlingHendelserKafkaProducerMock.sendMeldingForHendelseStatistikk(
                any(),
                any(),
            )
        } returns Unit
        coEvery { grunnlagService.opprettGrunnlag(any(), any()) } returns Unit
        every { oppgaveService.opprettOppgave(any(), any(), any(), any(), any(), gruppeId = any(), frist = any()) } returns mockOppgave
        every {
            oppgaveService.avbrytAapneOppgaverMedReferanse(any())
        } just runs

        val foerstegangsbehandling =
            behandlingFactory
                .opprettBehandling(
                    sakId1,
                    persongalleri,
                    datoNaa.toString(),
                    Vedtaksloesning.GJENNY,
                    behandlingFactory.hentDataForOpprettBehandling(sakId1),
                    BehandlingOpprinnelse.UKJENT,
                ).also { it.sendMeldingForHendelse() }
                .behandling

        Assertions.assertTrue(foerstegangsbehandling is Foerstegangsbehandling)

        every {
            behandlingDaoMock.hentBehandlingerForSak(any())
        } returns listOf(underArbeidBehandling)

        assertThrows<UgyldigForespoerselException> {
            behandlingFactory
                .opprettBehandling(
                    sakId1,
                    persongalleri,
                    datoNaa.toString(),
                    Vedtaksloesning.GJENNY,
                    behandlingFactory.hentDataForOpprettBehandling(sakId1),
                    BehandlingOpprinnelse.UKJENT,
                )
        }
        verify(exactly = 2) {
            sakServiceMock.finnSak(any())
        }
        verify(exactly = 2) {
            behandlingDaoMock.hentBehandlingerForSak(any())
        }
        verify(exactly = 1) {
            behandlingDaoMock.opprettBehandling(any())
            behandlingDaoMock.hentBehandling(any())
            hendelseDaoMock.behandlingOpprettet(any())
            behandlingHendelserKafkaProducerMock.sendMeldingForHendelseStatistikk(any(), any())
        }
        coVerify {
            grunnlagService.opprettGrunnlag(any(), any())
        }
        verify {
            oppgaveService.opprettOppgave(
                referanse = foerstegangsbehandling.id.toString(),
                sakId = sakId1,
                kilde = OppgaveKilde.BEHANDLING,
                type = OppgaveType.FOERSTEGANGSBEHANDLING,
                merknad = null,
                gruppeId = persongalleri.avdoed.single(),
                frist = any(),
            )
            grunnlagService.hentOpplysningsgrunnlagForSak(any())
        }
        verify(exactly = 1) {
            sakTilgang.oppdaterAdressebeskyttelse(any(), any())
        }
    }

    @Test
    fun `skal ikke kunne opprette omgjøring førstegangsbehandling hvis det er innvilget førstegangsbehandling`() {
        val sakId = sakId1
        val iverksattBehandlingId = UUID.randomUUID()
        val saksbehandler = simpleSaksbehandler()
        val iverksattBehandling =
            foerstegangsbehandling(
                status = BehandlingStatus.IVERKSATT,
                sak = sak(sakId = sakId),
                id = iverksattBehandlingId,
            )

        every { behandlingDaoMock.hentBehandlingerForSak(sakId) } returns listOf(iverksattBehandling)
        every { sakServiceMock.finnSak(sakId) } returns iverksattBehandling.sak

        assertThrows<AvslagOmgjoering.FoerstegangsbehandlingFeilStatus> {
            behandlingFactory.opprettOmgjoeringAvslag(
                sakId,
                saksbehandler,
                OmgjoeringRequest(false, false),
            )
        }
        verify {
            sakServiceMock.finnSak(sakId)
            behandlingDaoMock.hentBehandlingerForSak(sakId)
        }
    }

    @Test
    fun `skal ikke omgjøre hvis vi ikke har en førstegangsbehandling i saken`() {
        val sak = sak()
        val saksbehandler = simpleSaksbehandler()

        every { sakServiceMock.finnSak(sak.id) } returns sak
        every { behandlingDaoMock.hentBehandlingerForSak(sak.id) } returns emptyList()

        assertThrows<AvslagOmgjoering.IngenFoerstegangsbehandling> {
            behandlingFactory.opprettOmgjoeringAvslag(sak.id, saksbehandler, OmgjoeringRequest(false, false))
        }

        verify {
            sakServiceMock.finnSak(sak.id)
            behandlingDaoMock.hentBehandlingerForSak(sak.id)
        }
    }

    @Test
    fun `skal ikke omgjøre hvis vi har en åpen behandling i saken`() {
        val sak = sak()
        val saksbehandler = simpleSaksbehandler()
        val avslaattFoerstegangsbehandling = foerstegangsbehandling(sak = sak, status = BehandlingStatus.AVSLAG)
        val revurdering =
            revurdering(
                sak = sak,
                revurderingAarsak = Revurderingaarsak.NY_SOEKNAD,
                status = BehandlingStatus.OPPRETTET,
            )

        every { sakServiceMock.finnSak(sak.id) } returns sak
        every { behandlingDaoMock.hentBehandlingerForSak(sak.id) } returns
            listOf(
                avslaattFoerstegangsbehandling,
                revurdering,
            )

        assertThrows<AvslagOmgjoering.HarAapenBehandling> {
            behandlingFactory.opprettOmgjoeringAvslag(
                sak.id,
                saksbehandler,
                OmgjoeringRequest(false, false),
            )
        }

        verify {
            sakServiceMock.finnSak(sak.id)
            behandlingDaoMock.hentBehandlingerForSak(sak.id)
        }
    }

    @Test
    fun `omgjøring skal lage ny førstegangsbehandling og kopiere vurdering + vilkårsvurdering hvis flagg er satt`() {
        val sak = sak()
        val saksbehandler = simpleSaksbehandler()

        val boddEllerArbeidetUtlandet =
            BoddEllerArbeidetUtlandet(
                true,
                Grunnlagsopplysning.Saksbehandler("ident", Tidspunkt.now()),
                "begrunnelse",
                boddArbeidetIkkeEosEllerAvtaleland = true,
                boddArbeidetEosNordiskKonvensjon = true,
                boddArbeidetAvtaleland = true,
                vurdereAvdoedesTrygdeavtale = true,
                skalSendeKravpakke = true,
            )
        val opphoerFraOgMed = YearMonth.now()
        val avslaattFoerstegangsbehandling =
            foerstegangsbehandling(sak = sak, status = BehandlingStatus.AVSLAG)
                .copy(boddEllerArbeidetUtlandet = boddEllerArbeidetUtlandet, opphoerFraOgMed = opphoerFraOgMed)
        val revurdering =
            revurdering(
                sak = sak,
                revurderingAarsak = Revurderingaarsak.NY_SOEKNAD,
                status = BehandlingStatus.AVBRUTT,
            )

        every { sakServiceMock.finnSak(sak.id) } returns sak
        every { behandlingDaoMock.hentBehandlingerForSak(sak.id) } returns
            listOf(
                avslaattFoerstegangsbehandling,
                revurdering,
            )
        every { behandlingDaoMock.lagreOpphoerFom(any(), any()) } returns 1
        every { behandlingDaoMock.hentBehandling(avslaattFoerstegangsbehandling.id) } returns avslaattFoerstegangsbehandling
        every { behandlingDaoMock.hentBehandling(any()) } returns foerstegangsbehandling(sak = sak)
        every { behandlingDaoMock.lagreNyttVirkningstidspunkt(any(), any()) } returns 1
        every { kommerBarnetTilGodeServiceMock.lagreKommerBarnetTilgode(any()) } just Runs
        val vv =
            Vilkaarsvurdering(
                behandlingId = UUID.randomUUID(),
                grunnlagVersjon = 1L,
                virkningstidspunkt = VirkningstidspunktTestData.virkningstidsunkt().dato,
                vilkaar =
                    BarnepensjonVilkaar1967.inngangsvilkaar(),
            )
        every { vilkaarsvurderingService.kopierVilkaarsvurdering(any(), any(), any()) } returns
            VilkaarsvurderingMedBehandlingGrunnlagsversjon(vv, 1L)

        val opprettBehandlingSlot = slot<OpprettBehandling>()
        every { behandlingDaoMock.opprettBehandling(capture(opprettBehandlingSlot)) } just runs
        every { grunnlagService.hentPersongalleri(sak.id) } returns Persongalleri(sak.ident)
        coEvery { grunnlagService.opprettGrunnlag(any(), any()) } just runs
        every {
            oppgaveService.opprettOppgave(any(), any(), any(), any(), any(), frist = any())
        } returns
            OppgaveIntern(
                id = UUID.randomUUID(),
                status = Status.PAA_VENT,
                enhet = Enheter.defaultEnhet.enhetNr,
                sakId = sak.id,
                kilde = OppgaveKilde.BEHANDLING,
                type = OppgaveType.FOERSTEGANGSBEHANDLING,
                saksbehandler = null,
                forrigeSaksbehandlerIdent = null,
                referanse = "",
                gruppeId = null,
                merknad = null,
                opprettet = Tidspunkt.now(),
                sakType = SakType.OMSTILLINGSSTOENAD,
                fnr = null,
                frist = null,
            )
        every { oppgaveService.tildelSaksbehandler(any(), saksbehandler.ident) } just runs
        every { behandlingHendelserKafkaProducerMock.sendMeldingForHendelseStatistikk(any(), any()) } just runs

        every { oppgaveService.hentOppgaverForSak(any(), any()) } returns emptyList()

        val opprettetBehandling =
            behandlingFactory.opprettOmgjoeringAvslag(
                sak.id,
                saksbehandler,
                OmgjoeringRequest(skalKopiere = true, erSluttbehandlingUtland = false),
            )
        opprettetBehandling.sak.id shouldBe sak.id
        opprettetBehandling.type shouldBe BehandlingType.FØRSTEGANGSBEHANDLING
        opprettBehandlingSlot.captured.sakId shouldBe sak.id
        opprettBehandlingSlot.captured.type shouldBe BehandlingType.FØRSTEGANGSBEHANDLING

        verify {
            sakServiceMock.finnSak(sak.id)
            behandlingDaoMock.lagreOpphoerFom(opprettetBehandling.id, any())
            behandlingDaoMock.lagreBoddEllerArbeidetUtlandet(opprettetBehandling.id, boddEllerArbeidetUtlandet)
            behandlingDaoMock.hentBehandlingerForSak(sak.id)
            behandlingDaoMock.lagreNyttVirkningstidspunkt(opprettetBehandling.id, any())
            behandlingDaoMock.hentBehandling(any())
            behandlingDaoMock.opprettBehandling(any())
            kommerBarnetTilGodeServiceMock.lagreKommerBarnetTilgode(any())
            oppgaveService.tildelSaksbehandler(any(), saksbehandler.ident)
            oppgaveService.opprettOppgave(opprettetBehandling.id.toString(), sak.id, any(), any(), any(), frist = any())
            hendelseDaoMock.behandlingOpprettet(any())
            behandlingHendelserKafkaProducerMock.sendMeldingForHendelseStatistikk(any(), any())
            vilkaarsvurderingService.kopierVilkaarsvurdering(any(), any(), any())
            grunnlagService.hentPersongalleri(sak.id)
        }
        coVerify {
            grunnlagService.opprettGrunnlag(any(), any())
        }
    }

    @Test
    fun `skal lage ny førstegangsbehandling, oppgave og sende statisitkkmelding hvis omgjøring etter klage`() {
        val omgjoeringsOppgaveId = UUID.randomUUID()

        val sak = sak()
        val saksbehandler = simpleSaksbehandler()
        val avslaattFoerstegangsbehandling = foerstegangsbehandling(sak = sak, status = BehandlingStatus.AVSLAG)
        val revurdering =
            revurdering(
                sak = sak,
                revurderingAarsak = Revurderingaarsak.NY_SOEKNAD,
                status = BehandlingStatus.AVBRUTT,
            )

        val oppgaveInternMock =
            OppgaveIntern(
                id = omgjoeringsOppgaveId,
                status = Status.PAA_VENT,
                enhet = Enheter.defaultEnhet.enhetNr,
                sakId = sak.id,
                kilde = OppgaveKilde.BEHANDLING,
                type = OppgaveType.FOERSTEGANGSBEHANDLING,
                saksbehandler = OppgaveSaksbehandler(saksbehandler.ident),
                forrigeSaksbehandlerIdent = null,
                referanse = "",
                gruppeId = null,
                merknad = null,
                opprettet = Tidspunkt.now(),
                sakType = SakType.OMSTILLINGSSTOENAD,
                fnr = null,
                frist = null,
            )

        every { sakServiceMock.finnSak(sak.id) } returns sak
        every { behandlingDaoMock.hentBehandlingerForSak(sak.id) } returns
            listOf(
                avslaattFoerstegangsbehandling,
                revurdering,
            )
        every { behandlingDaoMock.hentBehandling(avslaattFoerstegangsbehandling.id) } returns avslaattFoerstegangsbehandling
        every { behandlingDaoMock.hentBehandling(any()) } returns foerstegangsbehandling(sak = sak)

        val opprettBehandlingSlot = slot<OpprettBehandling>()
        every { behandlingDaoMock.opprettBehandling(capture(opprettBehandlingSlot)) } just runs
        every { grunnlagService.hentPersongalleri(sak.id) } returns Persongalleri(sak.ident)
        coEvery { grunnlagService.opprettGrunnlag(any(), any()) } just runs
        every { oppgaveService.opprettOppgave(any(), any(), any(), any(), any(), frist = any()) } returns oppgaveInternMock
        every { oppgaveService.hentOppgaverForSak(any(), any()) } returns listOf(oppgaveInternMock)
        every { oppgaveService.tildelSaksbehandler(any(), saksbehandler.ident) } just runs
        every { behandlingHendelserKafkaProducerMock.sendMeldingForHendelseStatistikk(any(), any()) } just runs
        every { oppgaveService.ferdigstillOppgave(any(), any()) } returns oppgaveInternMock

        val opprettetBehandling =
            behandlingFactory.opprettOmgjoeringAvslag(
                sak.id,
                saksbehandler,
                OmgjoeringRequest(false, false, omgjoeringsOppgaveId),
            )
        opprettetBehandling.sak.id shouldBe sak.id
        opprettetBehandling.type shouldBe BehandlingType.FØRSTEGANGSBEHANDLING
        opprettBehandlingSlot.captured.sakId shouldBe sak.id
        opprettBehandlingSlot.captured.type shouldBe BehandlingType.FØRSTEGANGSBEHANDLING

        verify {
            sakServiceMock.finnSak(sak.id)
            behandlingDaoMock.hentBehandlingerForSak(sak.id)
            behandlingDaoMock.hentBehandling(any())
            behandlingDaoMock.opprettBehandling(any())
            oppgaveService.tildelSaksbehandler(any(), saksbehandler.ident)
            oppgaveService.opprettOppgave(any(), any(), any(), any(), merknad = "Omgjøring på grunn av klage", frist = any())
            hendelseDaoMock.behandlingOpprettet(any())
            behandlingHendelserKafkaProducerMock.sendMeldingForHendelseStatistikk(any(), any())
            behandlingDaoMock.lagreGyldighetsproeving(opprettetBehandling.id, any())
            oppgaveService.ferdigstillOppgave(omgjoeringsOppgaveId, saksbehandler)
            grunnlagService.hentPersongalleri(sak.id)
        }
        coVerify {
            grunnlagService.opprettGrunnlag(any(), any())
        }
    }

    @Test
    fun `skal lage ny førstegangsbehandling, oppgave og sende statisitkkmelding hvis omgjøring er lov`() {
        val sak = sak()
        val saksbehandler = simpleSaksbehandler()
        val avslaattFoerstegangsbehandling = foerstegangsbehandling(sak = sak, status = BehandlingStatus.AVSLAG)
        val revurdering =
            revurdering(
                sak = sak,
                revurderingAarsak = Revurderingaarsak.NY_SOEKNAD,
                status = BehandlingStatus.AVBRUTT,
            )

        every { sakServiceMock.finnSak(sak.id) } returns sak
        every { behandlingDaoMock.hentBehandlingerForSak(sak.id) } returns
            listOf(
                avslaattFoerstegangsbehandling,
                revurdering,
            )
        every { behandlingDaoMock.hentBehandling(avslaattFoerstegangsbehandling.id) } returns avslaattFoerstegangsbehandling
        every { behandlingDaoMock.hentBehandling(any()) } returns foerstegangsbehandling(sak = sak)

        val opprettBehandlingSlot = slot<OpprettBehandling>()
        every { behandlingDaoMock.opprettBehandling(capture(opprettBehandlingSlot)) } just runs
        every { grunnlagService.hentPersongalleri(sak.id) } returns Persongalleri(sak.ident)
        coEvery { grunnlagService.opprettGrunnlag(any(), any()) } just runs
        every {
            oppgaveService.opprettOppgave(any(), any(), any(), any(), any(), frist = any())
        } returns
            OppgaveIntern(
                id = UUID.randomUUID(),
                status = Status.PAA_VENT,
                enhet = Enheter.defaultEnhet.enhetNr,
                sakId = sak.id,
                kilde = OppgaveKilde.BEHANDLING,
                type = OppgaveType.FOERSTEGANGSBEHANDLING,
                saksbehandler = null,
                forrigeSaksbehandlerIdent = null,
                referanse = "",
                gruppeId = null,
                merknad = null,
                opprettet = Tidspunkt.now(),
                sakType = SakType.OMSTILLINGSSTOENAD,
                fnr = null,
                frist = null,
            )
        every { oppgaveService.tildelSaksbehandler(any(), saksbehandler.ident) } just runs
        every { behandlingHendelserKafkaProducerMock.sendMeldingForHendelseStatistikk(any(), any()) } just runs

        every { oppgaveService.hentOppgaverForSak(any(), any()) } returns emptyList()

        val opprettetBehandling =
            behandlingFactory.opprettOmgjoeringAvslag(sak.id, saksbehandler, OmgjoeringRequest(false, false))
        opprettetBehandling.sak.id shouldBe sak.id
        opprettetBehandling.type shouldBe BehandlingType.FØRSTEGANGSBEHANDLING
        opprettBehandlingSlot.captured.sakId shouldBe sak.id
        opprettBehandlingSlot.captured.type shouldBe BehandlingType.FØRSTEGANGSBEHANDLING

        verify {
            sakServiceMock.finnSak(sak.id)
            behandlingDaoMock.hentBehandlingerForSak(sak.id)
            behandlingDaoMock.hentBehandling(any())
            behandlingDaoMock.opprettBehandling(any())
            oppgaveService.tildelSaksbehandler(any(), saksbehandler.ident)
            oppgaveService.opprettOppgave(any(), any(), any(), any(), "Omgjøring av førstegangsbehandling", frist = any())
            hendelseDaoMock.behandlingOpprettet(any())
            behandlingHendelserKafkaProducerMock.sendMeldingForHendelseStatistikk(any(), any())
            behandlingDaoMock.lagreGyldighetsproeving(opprettetBehandling.id, any())
            grunnlagService.hentPersongalleri(sak.id)
        }
        coVerify {
            grunnlagService.opprettGrunnlag(any(), any())
        }
    }

    @Test
    fun `skal lage ny førstegangsbehandling hvis behandlinge kun er avslått og eller avbrutt`() {
        val behandlingOpprettes = slot<OpprettBehandling>()
        val behandlingHentes = slot<UUID>()
        val datoNaa = Tidspunkt.now().toLocalDatetimeUTC()

        val nyBehandling =
            Foerstegangsbehandling(
                id = UUID.randomUUID(),
                sak =
                    Sak(
                        ident = "Soeker",
                        sakType = SakType.BARNEPENSJON,
                        id = sakId1,
                        enhet = Enheter.defaultEnhet.enhetNr,
                        adressebeskyttelse = null,
                        erSkjermet = false,
                    ),
                behandlingOpprettet = datoNaa,
                sistEndret = datoNaa,
                status = BehandlingStatus.OPPRETTET,
                soeknadMottattDato = Tidspunkt.now().toLocalDatetimeUTC(),
                gyldighetsproeving = null,
                virkningstidspunkt =
                    Virkningstidspunkt(
                        YearMonth.of(2022, 1),
                        Grunnlagsopplysning.Saksbehandler.create("ident"),
                        "begrunnelse",
                    ),
                utlandstilknytning = null,
                boddEllerArbeidetUtlandet = null,
                kommerBarnetTilgode = null,
                vedtaksloesning = Vedtaksloesning.GJENNY,
                sendeBrev = true,
            )

        val persongalleri =
            Persongalleri(
                "Soeker",
                "Innsender",
                emptyList(),
                listOf("Avdoed"),
                listOf("Gjenlevende"),
            )
        every { sakLesDaoMock.hentSak(nyBehandling.sak.id) } returns nyBehandling.sak
        every { sakServiceMock.finnSak(any()) } returns nyBehandling.sak
        every { behandlingDaoMock.opprettBehandling(capture(behandlingOpprettes)) } returns Unit
        every { behandlingDaoMock.hentBehandling(capture(behandlingHentes)) } returns nyBehandling
        every {
            behandlingDaoMock.hentBehandlingerForSak(any())
        } returns emptyList()
        every { behandlingDaoMock.lagreStatus(any()) } returns Unit
        every { hendelseDaoMock.behandlingOpprettet(any()) } returns Unit
        every {
            behandlingHendelserKafkaProducerMock.sendMeldingForHendelseStatistikk(
                any(),
                any(),
            )
        } returns Unit
        coEvery { grunnlagService.opprettGrunnlag(any(), any()) } returns Unit
        every {
            oppgaveService.opprettOppgave(any(), any(), any(), any(), "Omgjøring av førstegangsbehandling")
        } returns mockOppgave
        every {
            oppgaveService.opprettOppgave(any(), any(), any(), any(), any(), gruppeId = any(), frist = any())
        } returns mockOppgave
        every {
            oppgaveService.tildelSaksbehandler(any(), any())
        } just runs

        val foerstegangsbehandling =
            behandlingFactory
                .opprettBehandling(
                    sakId1,
                    persongalleri,
                    datoNaa.toString(),
                    Vedtaksloesning.GJENNY,
                    behandlingFactory.hentDataForOpprettBehandling(sakId1),
                    BehandlingOpprinnelse.UKJENT,
                ).also { it.sendMeldingForHendelse() }
                .behandling

        Assertions.assertTrue(foerstegangsbehandling is Foerstegangsbehandling)

        val iverksattBehandlingId = UUID.randomUUID()
        val iverksattBehandling =
            Foerstegangsbehandling(
                id = iverksattBehandlingId,
                sak =
                    Sak(
                        ident = "Soeker",
                        sakType = SakType.BARNEPENSJON,
                        id = sakId1,
                        enhet = Enheter.defaultEnhet.enhetNr,
                        adressebeskyttelse = null,
                        erSkjermet = false,
                    ),
                behandlingOpprettet = datoNaa,
                sistEndret = datoNaa,
                status = BehandlingStatus.AVSLAG,
                soeknadMottattDato = Tidspunkt.now().toLocalDatetimeUTC(),
                gyldighetsproeving = null,
                virkningstidspunkt =
                    Virkningstidspunkt(
                        YearMonth.of(2022, 1),
                        Grunnlagsopplysning.Saksbehandler.create("ident"),
                        "begrunnelse",
                    ),
                utlandstilknytning = null,
                boddEllerArbeidetUtlandet = null,
                kommerBarnetTilgode =
                    KommerBarnetTilgode(
                        JaNei.JA,
                        "",
                        Grunnlagsopplysning.Saksbehandler.create("saksbehandler"),
                        iverksattBehandlingId,
                    ),
                vedtaksloesning = Vedtaksloesning.GJENNY,
                sendeBrev = true,
            )

        every {
            behandlingDaoMock.hentBehandlingerForSak(any())
        } returns listOf(iverksattBehandling)
        every { aktivitetspliktDao.kopierAktiviteter(any(), any()) } returns 1

        every { behandlingDaoMock.hentBehandling(any()) } returns
            revurdering(
                sakId = sakId1,
                revurderingAarsak = Revurderingaarsak.NY_SOEKNAD,
                enhet = Enheter.defaultEnhet.enhetNr,
            )

        val revurderingsBehandling =
            behandlingFactory
                .opprettBehandling(
                    sakId1,
                    persongalleri,
                    datoNaa.toString(),
                    Vedtaksloesning.GJENNY,
                    behandlingFactory.hentDataForOpprettBehandling(sakId1),
                    BehandlingOpprinnelse.UKJENT,
                ).also { it.sendMeldingForHendelse() }
                .behandling

        Assertions.assertTrue(revurderingsBehandling is Revurdering)
        verify {
            oppgaveService.opprettOppgave(
                referanse = foerstegangsbehandling.id.toString(),
                sakId = sakId1,
                kilde = OppgaveKilde.BEHANDLING,
                type = OppgaveType.FOERSTEGANGSBEHANDLING,
                merknad = any(),
                gruppeId = persongalleri.avdoed.single(),
                frist = any(),
            )
            oppgaveService.opprettOppgave(
                referanse = revurderingsBehandling.id.toString(),
                sakId = sakId1,
                kilde = OppgaveKilde.BEHANDLING,
                type = OppgaveType.FOERSTEGANGSBEHANDLING,
                merknad = any(),
                gruppeId = persongalleri.avdoed.single(),
                frist = any(),
            )
            grunnlagService.hentOpplysningsgrunnlagForSak(any())
        }
        coVerify {
            grunnlagService.opprettGrunnlag(any(), any())
        }

        verify(exactly = 2) {
            behandlingDaoMock.hentBehandling(any())
            behandlingDaoMock.opprettBehandling(any())
            hendelseDaoMock.behandlingOpprettet(any())
            behandlingDaoMock.hentBehandlingerForSak(any())
            behandlingHendelserKafkaProducerMock.sendMeldingForHendelseStatistikk(any(), any())
        }
        verify(exactly = 2) {
            sakServiceMock.finnSak(any())
        }
        verify(exactly = 2) {
            sakTilgang.oppdaterAdressebeskyttelse(any(), any())
        }
    }

    @Test
    fun `skal lage ny behandling som revurdering hvis behandling er satt til iverksatt`() {
        val behandlingOpprettes = slot<OpprettBehandling>()
        val behandlingHentes = slot<UUID>()
        val datoNaa = Tidspunkt.now().toLocalDatetimeUTC()

        val nyBehandling =
            Foerstegangsbehandling(
                id = UUID.randomUUID(),
                sak =
                    Sak(
                        ident = "Soeker",
                        sakType = SakType.BARNEPENSJON,
                        id = sakId1,
                        enhet = Enheter.defaultEnhet.enhetNr,
                        adressebeskyttelse = null,
                        erSkjermet = false,
                    ),
                behandlingOpprettet = datoNaa,
                sistEndret = datoNaa,
                status = BehandlingStatus.OPPRETTET,
                soeknadMottattDato = Tidspunkt.now().toLocalDatetimeUTC(),
                gyldighetsproeving = null,
                virkningstidspunkt =
                    Virkningstidspunkt(
                        YearMonth.of(2022, 1),
                        Grunnlagsopplysning.Saksbehandler.create("ident"),
                        "begrunnelse",
                    ),
                utlandstilknytning = null,
                boddEllerArbeidetUtlandet = null,
                kommerBarnetTilgode = null,
                vedtaksloesning = Vedtaksloesning.GJENNY,
                sendeBrev = true,
            )

        val persongalleri =
            Persongalleri(
                "Soeker",
                "Innsender",
                emptyList(),
                listOf("Avdoed"),
                listOf("Gjenlevende"),
            )

        every { sakServiceMock.finnSak(any()) } returns nyBehandling.sak
        every { sakLesDaoMock.hentSak(nyBehandling.sak.id) } returns nyBehandling.sak
        every { behandlingDaoMock.opprettBehandling(capture(behandlingOpprettes)) } returns Unit
        every { behandlingDaoMock.hentBehandling(capture(behandlingHentes)) } returns nyBehandling
        every {
            behandlingDaoMock.hentBehandlingerForSak(any())
        } returns emptyList()
        every { behandlingDaoMock.lagreStatus(any()) } returns Unit
        every { hendelseDaoMock.behandlingOpprettet(any()) } returns Unit
        every {
            behandlingHendelserKafkaProducerMock.sendMeldingForHendelseStatistikk(
                any(),
                any(),
            )
        } returns Unit
        coEvery { grunnlagService.opprettGrunnlag(any(), any()) } returns Unit
        every {
            oppgaveService.opprettOppgave(any(), any(), any(), any(), any(), frist = any(), gruppeId = any())
        } returns mockOppgave
        every {
            oppgaveService.opprettOppgave(any(), any(), any(), any(), any(), frist = any(), gruppeId = any())
        } returns mockOppgave
        every {
            oppgaveService.oppdaterStatusOgMerknad(any(), any(), any())
        } just runs

        val foerstegangsbehandling =
            behandlingFactory
                .opprettBehandling(
                    sakId1,
                    persongalleri,
                    datoNaa.toString(),
                    Vedtaksloesning.GJENNY,
                    behandlingFactory.hentDataForOpprettBehandling(sakId1),
                    BehandlingOpprinnelse.UKJENT,
                ).also { it.sendMeldingForHendelse() }
                .behandling

        Assertions.assertTrue(foerstegangsbehandling is Foerstegangsbehandling)

        val iverksattBehandlingId = UUID.randomUUID()
        val iverksattBehandling =
            Foerstegangsbehandling(
                id = iverksattBehandlingId,
                sak =
                    Sak(
                        ident = "Soeker",
                        sakType = SakType.BARNEPENSJON,
                        id = sakId1,
                        enhet = Enheter.defaultEnhet.enhetNr,
                        adressebeskyttelse = null,
                        erSkjermet = false,
                    ),
                behandlingOpprettet = datoNaa,
                sistEndret = datoNaa,
                status = BehandlingStatus.IVERKSATT,
                soeknadMottattDato = Tidspunkt.now().toLocalDatetimeUTC(),
                gyldighetsproeving = null,
                virkningstidspunkt =
                    Virkningstidspunkt(
                        YearMonth.of(2022, 1),
                        Grunnlagsopplysning.Saksbehandler.create("ident"),
                        "begrunnelse",
                    ),
                utlandstilknytning = null,
                boddEllerArbeidetUtlandet = null,
                kommerBarnetTilgode =
                    KommerBarnetTilgode(
                        JaNei.JA,
                        "",
                        Grunnlagsopplysning.Saksbehandler.create("saksbehandler"),
                        iverksattBehandlingId,
                    ),
                vedtaksloesning = Vedtaksloesning.GJENNY,
                sendeBrev = true,
            )

        every {
            behandlingDaoMock.hentBehandlingerForSak(any())
        } returns listOf(iverksattBehandling)
        every { aktivitetspliktDao.kopierAktiviteter(any(), any()) } returns 1
        every { aktivitetspliktKopierService.kopierVurderingTilBehandling(any(), any()) } returns Unit

        every { behandlingDaoMock.hentBehandling(any()) } returns
            revurdering(
                sakId = sakId1,
                revurderingAarsak = Revurderingaarsak.NY_SOEKNAD,
                enhet = Enheter.defaultEnhet.enhetNr,
            )

        val revurderingsBehandling =
            behandlingFactory
                .opprettBehandling(
                    sakId1,
                    persongalleri,
                    datoNaa.toString(),
                    Vedtaksloesning.GJENNY,
                    behandlingFactory.hentDataForOpprettBehandling(sakId1),
                    BehandlingOpprinnelse.UKJENT,
                ).also { it.sendMeldingForHendelse() }
                .behandling
        Assertions.assertTrue(revurderingsBehandling is Revurdering)
        verify {
            oppgaveService.opprettOppgave(
                referanse = any(),
                sakId = any(),
                kilde = any(),
                type = any(),
                merknad = any(),
                frist = any(),
                gruppeId = persongalleri.avdoed.single(),
            )
            oppgaveService.opprettOppgave(
                referanse = any(),
                sakId = any(),
                kilde = any(),
                type = any(),
                merknad = any(),
                frist = any(),
                gruppeId = persongalleri.avdoed.single(),
            )
        }
        coVerify {
            grunnlagService.opprettGrunnlag(any(), any())
        }
        verify(exactly = 2) {
            behandlingDaoMock.hentBehandling(any())
            behandlingDaoMock.opprettBehandling(any())
            hendelseDaoMock.behandlingOpprettet(any())
            behandlingDaoMock.hentBehandlingerForSak(any())
            behandlingHendelserKafkaProducerMock.sendMeldingForHendelseStatistikk(any(), any())
        }
        verify(exactly = 2) {
            sakServiceMock.finnSak(any())
        }
        verify(exactly = 1) {
            sakTilgang.oppdaterAdressebeskyttelse(any(), any())
            grunnlagService.hentOpplysningsgrunnlagForSak(any())
        }
    }

    @Test
    fun `Opprett ny sak og behandling for journalfoeringsoppgave`() {
        val datoNaa = Tidspunkt.now().toLocalDatetimeUTC()
        val behandlingOpprettes = slot<OpprettBehandling>()
        val behandlingHentes = slot<UUID>()

        val persongalleri =
            Persongalleri(
                "11057523044",
                INNSENDER_FOEDSELSNUMMER.value,
                emptyList(),
                listOf(AVDOED_FOEDSELSNUMMER.value),
                listOf(GJENLEVENDE_FOEDSELSNUMMER.value),
            )

        val sak =
            Sak(
                persongalleri.soeker,
                SakType.BARNEPENSJON,
                sakId1,
                Enheter.defaultEnhet.enhetNr,
                adressebeskyttelse = null,
                erSkjermet = false,
            )

        val opprettetBehandling =
            Foerstegangsbehandling(
                id = UUID.randomUUID(),
                sak = sak,
                behandlingOpprettet = datoNaa,
                sistEndret = datoNaa,
                status = BehandlingStatus.OPPRETTET,
                soeknadMottattDato = Tidspunkt.now().toLocalDatetimeUTC(),
                gyldighetsproeving = null,
                virkningstidspunkt =
                    Virkningstidspunkt(
                        YearMonth.of(2022, 1),
                        Grunnlagsopplysning.Saksbehandler.create("ident"),
                        "begrunnelse",
                    ),
                utlandstilknytning = null,
                boddEllerArbeidetUtlandet = null,
                kommerBarnetTilgode = null,
                vedtaksloesning = Vedtaksloesning.GJENNY,
                sendeBrev = true,
            )
        every { sakLesDaoMock.hentSak(sak.id) } returns sak
        every { sakServiceMock.finnEllerOpprettSakMedGrunnlag(any(), any()) } returns sak
        every { sakServiceMock.finnSak(any<SakId>()) } returns sak
        every { behandlingDaoMock.opprettBehandling(capture(behandlingOpprettes)) } just Runs
        every { behandlingDaoMock.hentBehandling(capture(behandlingHentes)) } returns opprettetBehandling
        every { behandlingDaoMock.hentBehandlingerForSak(any()) } returns emptyList()
        every { oppgaveService.opprettOppgave(any(), any(), any(), any(), any(), gruppeId = any(), frist = any()) } returns mockOppgave

        coEvery { pdlTjenesterKlientMock.hentAdressebeskyttelseForPerson(any()) } returns AdressebeskyttelseGradering.UGRADERT

        val resultat =
            runBlocking {
                behandlingFactory.opprettSakOgBehandlingForOppgave(
                    NyBehandlingRequest(
                        sak.sakType,
                        persongalleri,
                        LocalDateTime.now().toString(),
                        "nb",
                        Vedtaksloesning.GJENNY,
                        null,
                        null,
                    ),
                    simpleSaksbehandler(),
                )
            }

        Assertions.assertEquals(opprettetBehandling, resultat)
        Assertions.assertEquals(opprettetBehandling.sak, resultat.sak)
        Assertions.assertEquals(opprettetBehandling.id, resultat.id)
        Assertions.assertEquals(opprettetBehandling.behandlingOpprettet, resultat.behandlingOpprettet)
        Assertions.assertEquals(sakId1, behandlingOpprettes.captured.sakId)
        Assertions.assertEquals(behandlingHentes.captured, behandlingOpprettes.captured.id)

        verify(exactly = 1) {
            sakServiceMock.finnSak(any())
        }
        verify(exactly = 1) {
            sakServiceMock.finnEllerOpprettSakMedGrunnlag(persongalleri.soeker, SakType.BARNEPENSJON)
            behandlingDaoMock.opprettBehandling(any())
            hendelseDaoMock.behandlingOpprettet(any())
            behandlingDaoMock.hentBehandling(any())
            behandlingDaoMock.hentBehandlingerForSak(any())
            behandlingHendelserKafkaProducerMock.sendMeldingForHendelseStatistikk(
                any(),
                BehandlingHendelseType.OPPRETTET,
            )
            oppgaveService.opprettOppgave(
                referanse = opprettetBehandling.id.toString(),
                sakId = sak.id,
                kilde = OppgaveKilde.BEHANDLING,
                type = OppgaveType.FOERSTEGANGSBEHANDLING,
                merknad = any(),
                gruppeId = persongalleri.avdoed.single(),
                frist = any(),
            )
        }
        coVerify {
            grunnlagService.opprettGrunnlag(any(), any())
        }
        verify(exactly = 1) {
            grunnlagService.hentOpplysningsgrunnlagForSak(any())
            sakTilgang.oppdaterAdressebeskyttelse(any(), any())
            grunnlagService.lagreNyeSaksopplysninger(any(), any(), any())
        }
    }

    private fun sak(
        sakId: SakId = sakId1,
        sakType: SakType = SakType.BARNEPENSJON,
        enhet: Enhetsnummer = Enheter.defaultEnhet.enhetNr,
        adressebeskyttelse: AdressebeskyttelseGradering? = null,
        erSkjermet: Boolean = false,
    ): Sak =
        Sak(
            ident = "Soeker",
            sakType = sakType,
            id = sakId,
            enhet = enhet,
            adressebeskyttelse = adressebeskyttelse,
            erSkjermet = erSkjermet,
        )

    private fun revurdering(
        id: UUID = UUID.randomUUID(),
        sak: Sak = sak(),
        status: BehandlingStatus = BehandlingStatus.OPPRETTET,
        revurderingAarsak: Revurderingaarsak = Revurderingaarsak.ANNEN,
    ): Revurdering =
        Revurdering.opprett(
            id = id,
            sak = sak,
            behandlingOpprettet = Tidspunkt.now().toLocalDatetimeNorskTid(),
            sistEndret = Tidspunkt.now().toLocalDatetimeNorskTid(),
            status = status,
            kommerBarnetTilgode = null,
            virkningstidspunkt = null,
            boddEllerArbeidetUtlandet = null,
            revurderingsaarsak = revurderingAarsak,
            revurderingInfo = null,
            prosesstype = Prosesstype.MANUELL,
            vedtaksloesning = Vedtaksloesning.GJENNY,
            begrunnelse = null,
            relatertBehandlingId = null,
            opphoerFraOgMed = null,
            utlandstilknytning = null,
            sendeBrev = true,
            tidligereFamiliepleier = null,
            opprinnelse = BehandlingOpprinnelse.UKJENT,
        )

    private fun foerstegangsbehandling(
        id: UUID = UUID.randomUUID(),
        sak: Sak = sak(),
        status: BehandlingStatus = BehandlingStatus.OPPRETTET,
        virk: YearMonth = YearMonth.of(2022, 1),
    ): Foerstegangsbehandling =
        Foerstegangsbehandling(
            id = id,
            sak = sak,
            behandlingOpprettet = Tidspunkt.now().toLocalDatetimeNorskTid(),
            sistEndret = Tidspunkt.now().toLocalDatetimeNorskTid(),
            status = status,
            soeknadMottattDato = Tidspunkt.now().toLocalDatetimeUTC(),
            gyldighetsproeving = null,
            virkningstidspunkt =
                Virkningstidspunkt(
                    virk,
                    Grunnlagsopplysning.Saksbehandler.create("ident"),
                    "begrunnelse",
                ),
            utlandstilknytning = null,
            boddEllerArbeidetUtlandet = null,
            kommerBarnetTilgode =
                KommerBarnetTilgode(
                    JaNei.JA,
                    "",
                    Grunnlagsopplysning.Saksbehandler.create("saksbehandler"),
                    id,
                ),
            vedtaksloesning = Vedtaksloesning.GJENNY,
            sendeBrev = true,
        )
}
