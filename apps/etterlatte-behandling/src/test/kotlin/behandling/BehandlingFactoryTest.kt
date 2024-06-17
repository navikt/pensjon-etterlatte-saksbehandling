package no.nav.etterlatte.behandling

import io.mockk.Runs
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.SaksbehandlerMedEnheterOgRoller
import no.nav.etterlatte.behandling.aktivitetsplikt.AktivitetspliktDao
import no.nav.etterlatte.behandling.domain.Foerstegangsbehandling
import no.nav.etterlatte.behandling.domain.OpprettBehandling
import no.nav.etterlatte.behandling.domain.Revurdering
import no.nav.etterlatte.behandling.hendelse.HendelseDao
import no.nav.etterlatte.behandling.klage.KlageService
import no.nav.etterlatte.behandling.kommerbarnettilgode.KommerBarnetTilGodeService
import no.nav.etterlatte.behandling.revurdering.AutomatiskRevurderingService
import no.nav.etterlatte.behandling.revurdering.RevurderingDao
import no.nav.etterlatte.behandling.revurdering.RevurderingService
import no.nav.etterlatte.common.Enheter
import no.nav.etterlatte.common.klienter.PdlTjenesterKlient
import no.nav.etterlatte.grunnlagsendring.GrunnlagsendringshendelseDao
import no.nav.etterlatte.libs.common.Vedtaksloesning
import no.nav.etterlatte.libs.common.behandling.BehandlingHendelseType
import no.nav.etterlatte.libs.common.behandling.BehandlingStatus
import no.nav.etterlatte.libs.common.behandling.JaNei
import no.nav.etterlatte.libs.common.behandling.KommerBarnetTilgode
import no.nav.etterlatte.libs.common.behandling.NyBehandlingRequest
import no.nav.etterlatte.libs.common.behandling.Persongalleri
import no.nav.etterlatte.libs.common.behandling.Revurderingaarsak
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.behandling.Virkningstidspunkt
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.oppgave.OppgaveKilde
import no.nav.etterlatte.libs.common.oppgave.OppgaveType
import no.nav.etterlatte.libs.common.oppgave.opprettNyOppgaveMedReferanseOgSak
import no.nav.etterlatte.libs.common.person.AdressebeskyttelseGradering
import no.nav.etterlatte.libs.common.sak.Sak
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.tidspunkt.toLocalDatetimeUTC
import no.nav.etterlatte.libs.ktor.token.Saksbehandler
import no.nav.etterlatte.libs.testdata.grunnlag.AVDOED_FOEDSELSNUMMER
import no.nav.etterlatte.libs.testdata.grunnlag.GJENLEVENDE_FOEDSELSNUMMER
import no.nav.etterlatte.libs.testdata.grunnlag.INNSENDER_FOEDSELSNUMMER
import no.nav.etterlatte.nyKontekstMedBruker
import no.nav.etterlatte.oppgave.OppgaveService
import no.nav.etterlatte.revurdering
import no.nav.etterlatte.sak.SakService
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.time.YearMonth
import java.util.UUID

class BehandlingFactoryTest {
    private val user = mockk<SaksbehandlerMedEnheterOgRoller>()
    private val behandlingDaoMock = mockk<BehandlingDao>(relaxUnitFun = true)
    private val hendelseDaoMock = mockk<HendelseDao>(relaxUnitFun = true)
    private val behandlingHendelserKafkaProducerMock = mockk<BehandlingHendelserKafkaProducer>(relaxUnitFun = true)
    private val grunnlagsendringshendelseDao = mockk<GrunnlagsendringshendelseDao>()
    private val grunnlagService = mockk<GrunnlagServiceImpl>(relaxUnitFun = true)
    private val oppgaveService = mockk<OppgaveService>()
    private val behandlingService = mockk<BehandlingService>()
    private val sakServiceMock = mockk<SakService>()
    private val klageService = mockk<KlageService>()
    private val aktivitetspliktDao = mockk<AktivitetspliktDao>()
    private val gyldighetsproevingService = mockk<GyldighetsproevingService>(relaxUnitFun = true)
    private val pdlTjenesterKlientMock = mockk<PdlTjenesterKlient>()
    private val mockOppgave =
        opprettNyOppgaveMedReferanseOgSak(
            "behandling",
            Sak("ident", SakType.BARNEPENSJON, 1L, Enheter.AALESUND.enhetNr),
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
        AutomatiskRevurderingService(
            RevurderingService(
                oppgaveService,
                grunnlagService,
                behandlingHendelserKafkaProducerMock,
                behandlingDaoMock,
                hendelseDaoMock,
                grunnlagsendringshendelseDao,
                kommerBarnetTilGodeService,
                revurderingDao,
                klageService,
                behandlingService,
                aktivitetspliktDao,
            ),
        )
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
            mockk(),
        )

    @BeforeEach
    fun before() {
        every { user.name() } returns "User"
        every { user.enheter() } returns listOf(Enheter.defaultEnhet.enhetNr)

        nyKontekstMedBruker(user)
    }

    @AfterEach
    fun after() {
        confirmVerified(
            sakServiceMock,
            behandlingDaoMock,
            hendelseDaoMock,
            behandlingHendelserKafkaProducerMock,
            grunnlagService,
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
                        id = 1,
                        enhet = Enheter.defaultEnhet.enhetNr,
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
                kilde = Vedtaksloesning.GJENNY,
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
        every { behandlingDaoMock.lagreGyldighetsproving(any()) } returns Unit
        every { behandlingDaoMock.alleBehandlingerISak(any()) } returns emptyList()
        every { behandlingDaoMock.lagreStatus(any(), any(), any()) } returns Unit
        every {
            behandlingHendelserKafkaProducerMock.sendMeldingForHendelseMedDetaljertBehandling(
                any(),
                any(),
            )
        } returns Unit
        every { grunnlagService.leggInnNyttGrunnlag(any(), any()) } returns Unit
        every {
            oppgaveService.opprettFoerstegangsbehandlingsOppgaveForInnsendtSoeknad(any(), any())
        } returns mockOppgave

        val resultat =
            behandlingFactory
                .opprettBehandling(
                    1,
                    persongalleri,
                    datoNaa.toString(),
                    Vedtaksloesning.GJENNY,
                    behandlingFactory.hentDataForOpprettBehandling(1),
                )!!
                .also { it.sendMeldingForHendelse() }
                .behandling

        Assertions.assertEquals(opprettetBehandling, resultat)
        Assertions.assertEquals(opprettetBehandling.sak, resultat.sak)
        Assertions.assertEquals(opprettetBehandling.id, resultat.id)
        Assertions.assertEquals(opprettetBehandling.behandlingOpprettet, resultat.behandlingOpprettet)
        Assertions.assertEquals(1, behandlingOpprettes.captured.sakId)
        Assertions.assertEquals(behandlingHentes.captured, behandlingOpprettes.captured.id)

        verify(exactly = 1) {
            sakServiceMock.finnSak(any())
            behandlingDaoMock.hentBehandling(any())
            behandlingDaoMock.opprettBehandling(any())
            hendelseDaoMock.behandlingOpprettet(any())
            behandlingDaoMock.alleBehandlingerISak(any())
            behandlingHendelserKafkaProducerMock.sendMeldingForHendelseMedDetaljertBehandling(
                any(),
                BehandlingHendelseType.OPPRETTET,
            )
            grunnlagService.leggInnNyttGrunnlag(any(), any())
            oppgaveService.opprettFoerstegangsbehandlingsOppgaveForInnsendtSoeknad(any(), any())
            oppgaveService.opprettFoerstegangsbehandlingsOppgaveForInnsendtSoeknad(any(), any())
        }
    }

    @Test
    fun `skal opprette kun foerstegangsbehandling hvis det ikke finnes noen tidligere behandlinger`() {
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
                        id = 1,
                        enhet = Enheter.defaultEnhet.enhetNr,
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
                kilde = Vedtaksloesning.GJENNY,
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

        every { sakServiceMock.finnSak(any()) } returns opprettetBehandling.sak
        every { behandlingDaoMock.opprettBehandling(capture(behandlingOpprettes)) } returns Unit
        every { behandlingDaoMock.hentBehandling(capture(behandlingHentes)) } returns opprettetBehandling
        every { behandlingDaoMock.alleBehandlingerISak(any()) } returns emptyList()
        every { hendelseDaoMock.behandlingOpprettet(any()) } returns Unit
        every {
            behandlingHendelserKafkaProducerMock.sendMeldingForHendelseMedDetaljertBehandling(
                any(),
                any(),
            )
        } returns Unit
        every { grunnlagService.leggInnNyttGrunnlag(any(), any()) } returns Unit
        every {
            oppgaveService.opprettFoerstegangsbehandlingsOppgaveForInnsendtSoeknad(any(), any())
        } returns mockOppgave

        val foerstegangsbehandling =
            behandlingFactory
                .opprettBehandling(
                    1,
                    persongalleri,
                    datoNaa.toString(),
                    Vedtaksloesning.GJENNY,
                    behandlingFactory.hentDataForOpprettBehandling(1),
                )!!
                .also { it.sendMeldingForHendelse() }
                .behandling

        Assertions.assertTrue(foerstegangsbehandling is Foerstegangsbehandling)

        verify(exactly = 1) {
            sakServiceMock.finnSak(any())
            behandlingDaoMock.hentBehandling(any())
            behandlingDaoMock.opprettBehandling(any())
            hendelseDaoMock.behandlingOpprettet(any())
            behandlingDaoMock.alleBehandlingerISak(any())
            behandlingHendelserKafkaProducerMock.sendMeldingForHendelseMedDetaljertBehandling(any(), any())
            grunnlagService.leggInnNyttGrunnlag(any(), any())
            oppgaveService.opprettFoerstegangsbehandlingsOppgaveForInnsendtSoeknad(any(), any())
            oppgaveService.opprettFoerstegangsbehandlingsOppgaveForInnsendtSoeknad(any(), any())
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
                        id = 1,
                        enhet = Enheter.defaultEnhet.enhetNr,
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
                kilde = Vedtaksloesning.GJENNY,
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

        every { sakServiceMock.finnSak(any()) } returns underArbeidBehandling.sak
        every { behandlingDaoMock.opprettBehandling(capture(behandlingOpprettes)) } returns Unit
        every { behandlingDaoMock.hentBehandling(capture(behandlingHentes)) } returns underArbeidBehandling
        every {
            behandlingDaoMock.alleBehandlingerISak(any())
        } returns emptyList()
        every { behandlingDaoMock.lagreStatus(any(), any(), any()) } returns Unit
        every { hendelseDaoMock.behandlingOpprettet(any()) } returns Unit
        every {
            behandlingHendelserKafkaProducerMock.sendMeldingForHendelseMedDetaljertBehandling(
                any(),
                any(),
            )
        } returns Unit
        every { grunnlagService.leggInnNyttGrunnlag(any(), any()) } returns Unit
        every {
            oppgaveService.opprettFoerstegangsbehandlingsOppgaveForInnsendtSoeknad(any(), any())
        } returns mockOppgave
        every {
            oppgaveService.avbrytAapneOppgaverMedReferanse(any())
        } just runs

        val foerstegangsbehandling =
            behandlingFactory
                .opprettBehandling(
                    1,
                    persongalleri,
                    datoNaa.toString(),
                    Vedtaksloesning.GJENNY,
                    behandlingFactory.hentDataForOpprettBehandling(1),
                )!!
                .also { it.sendMeldingForHendelse() }
                .behandling

        Assertions.assertTrue(foerstegangsbehandling is Foerstegangsbehandling)

        every {
            behandlingDaoMock.alleBehandlingerISak(any())
        } returns listOf(underArbeidBehandling)

        val nyfoerstegangsbehandling =
            behandlingFactory
                .opprettBehandling(
                    1,
                    persongalleri,
                    datoNaa.toString(),
                    Vedtaksloesning.GJENNY,
                    behandlingFactory.hentDataForOpprettBehandling(1),
                )?.also { it.sendMeldingForHendelse() }
                ?.behandling
        Assertions.assertTrue(nyfoerstegangsbehandling is Foerstegangsbehandling)

        verify(exactly = 2) {
            sakServiceMock.finnSak(any())
            behandlingDaoMock.hentBehandling(any())
            behandlingDaoMock.opprettBehandling(any())
            hendelseDaoMock.behandlingOpprettet(any())
            behandlingDaoMock.alleBehandlingerISak(any())
            behandlingHendelserKafkaProducerMock.sendMeldingForHendelseMedDetaljertBehandling(any(), any())
            grunnlagService.leggInnNyttGrunnlag(any(), any())
            oppgaveService.opprettFoerstegangsbehandlingsOppgaveForInnsendtSoeknad(any(), any())
            oppgaveService.opprettFoerstegangsbehandlingsOppgaveForInnsendtSoeknad(any(), any())
        }
        verify {
            behandlingDaoMock.lagreStatus(any(), BehandlingStatus.AVBRUTT, any())
            oppgaveService.avbrytAapneOppgaverMedReferanse(nyfoerstegangsbehandling!!.id.toString())
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
                        id = 1,
                        enhet = Enheter.defaultEnhet.enhetNr,
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
                kilde = Vedtaksloesning.GJENNY,
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
        every { behandlingDaoMock.opprettBehandling(capture(behandlingOpprettes)) } returns Unit
        every { behandlingDaoMock.hentBehandling(capture(behandlingHentes)) } returns nyBehandling
        every {
            behandlingDaoMock.alleBehandlingerISak(any())
        } returns emptyList()
        every { behandlingDaoMock.lagreStatus(any(), any(), any()) } returns Unit
        every { hendelseDaoMock.behandlingOpprettet(any()) } returns Unit
        every {
            behandlingHendelserKafkaProducerMock.sendMeldingForHendelseMedDetaljertBehandling(
                any(),
                any(),
            )
        } returns Unit
        every { grunnlagService.leggInnNyttGrunnlag(any(), any()) } returns Unit
        every {
            oppgaveService.opprettFoerstegangsbehandlingsOppgaveForInnsendtSoeknad(any(), any())
        } returns mockOppgave
        every {
            oppgaveService.opprettOppgave(any(), any(), any(), any(), any())
        } returns mockOppgave
        every {
            oppgaveService.tildelSaksbehandler(any(), any())
        } just runs

        val foerstegangsbehandling =
            behandlingFactory
                .opprettBehandling(
                    1,
                    persongalleri,
                    datoNaa.toString(),
                    Vedtaksloesning.GJENNY,
                    behandlingFactory.hentDataForOpprettBehandling(1),
                )!!
                .also { it.sendMeldingForHendelse() }
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
                        id = 1,
                        enhet = Enheter.defaultEnhet.enhetNr,
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
                kilde = Vedtaksloesning.GJENNY,
                sendeBrev = true,
            )

        every {
            behandlingDaoMock.alleBehandlingerISak(any())
        } returns listOf(iverksattBehandling)
        every { aktivitetspliktDao.kopierAktiviteter(any(), any()) } returns 1

        every { behandlingDaoMock.hentBehandling(any()) } returns
            revurdering(
                sakId = 1,
                revurderingAarsak = Revurderingaarsak.NY_SOEKNAD,
                enhet = Enheter.defaultEnhet.enhetNr,
            )

        val revurderingsBehandling =
            behandlingFactory
                .opprettBehandling(
                    1,
                    persongalleri,
                    datoNaa.toString(),
                    Vedtaksloesning.GJENNY,
                    behandlingFactory.hentDataForOpprettBehandling(1),
                )?.also { it.sendMeldingForHendelse() }
                ?.behandling
        Assertions.assertTrue(revurderingsBehandling is Revurdering)
        verify {
            grunnlagService.leggInnNyttGrunnlag(any(), any())
            oppgaveService.opprettFoerstegangsbehandlingsOppgaveForInnsendtSoeknad(any(), any())
            oppgaveService.opprettOppgave(any(), any(), any(), any(), any())
        }
        verify(exactly = 2) {
            sakServiceMock.finnSak(any())
            behandlingDaoMock.hentBehandling(any())
            behandlingDaoMock.opprettBehandling(any())
            hendelseDaoMock.behandlingOpprettet(any())
            behandlingDaoMock.alleBehandlingerISak(any())
            behandlingHendelserKafkaProducerMock.sendMeldingForHendelseMedDetaljertBehandling(any(), any())
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

        val sak = Sak(persongalleri.soeker, SakType.BARNEPENSJON, 1, Enheter.defaultEnhet.enhetNr)

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
                kilde = Vedtaksloesning.GJENNY,
                sendeBrev = true,
            )

        every { sakServiceMock.finnEllerOpprettSakMedGrunnlag(any(), any()) } returns sak
        every { sakServiceMock.finnSak(any<Long>()) } returns sak
        every { behandlingDaoMock.opprettBehandling(capture(behandlingOpprettes)) } just Runs
        every { behandlingDaoMock.hentBehandling(capture(behandlingHentes)) } returns opprettetBehandling
        every { behandlingDaoMock.alleBehandlingerISak(any()) } returns emptyList()
        every {
            oppgaveService.opprettFoerstegangsbehandlingsOppgaveForInnsendtSoeknad(any(), any())
        } returns mockOppgave

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
                    Saksbehandler("token", "Z123456", null),
                )
            }

        Assertions.assertEquals(opprettetBehandling, resultat)
        Assertions.assertEquals(opprettetBehandling.sak, resultat.sak)
        Assertions.assertEquals(opprettetBehandling.id, resultat.id)
        Assertions.assertEquals(opprettetBehandling.behandlingOpprettet, resultat.behandlingOpprettet)
        Assertions.assertEquals(1, behandlingOpprettes.captured.sakId)
        Assertions.assertEquals(behandlingHentes.captured, behandlingOpprettes.captured.id)

        verify(exactly = 1) {
            sakServiceMock.finnEllerOpprettSakMedGrunnlag(persongalleri.soeker, SakType.BARNEPENSJON)
            sakServiceMock.finnSak(sak.id)
            behandlingDaoMock.opprettBehandling(any())
            hendelseDaoMock.behandlingOpprettet(any())
            behandlingDaoMock.hentBehandling(any())
            behandlingDaoMock.alleBehandlingerISak(any())
            behandlingHendelserKafkaProducerMock.sendMeldingForHendelseMedDetaljertBehandling(
                any(),
                BehandlingHendelseType.OPPRETTET,
            )
            grunnlagService.leggInnNyttGrunnlag(any(), any())
            grunnlagService.leggTilNyeOpplysninger(any(), any())
            grunnlagService.leggTilNyeOpplysninger(any(), any())
            oppgaveService.opprettFoerstegangsbehandlingsOppgaveForInnsendtSoeknad(any(), any())
            oppgaveService.opprettFoerstegangsbehandlingsOppgaveForInnsendtSoeknad(any(), any())
        }
    }
}
