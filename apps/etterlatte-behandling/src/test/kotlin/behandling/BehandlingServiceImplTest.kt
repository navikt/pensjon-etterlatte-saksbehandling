package no.nav.etterlatte.behandling

import io.mockk.coEvery
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.Context
import no.nav.etterlatte.DatabaseKontekst
import no.nav.etterlatte.GrunnlagKlientTest
import no.nav.etterlatte.Kontekst
import no.nav.etterlatte.SaksbehandlerMedEnheterOgRoller
import no.nav.etterlatte.behandling.domain.Foerstegangsbehandling
import no.nav.etterlatte.behandling.domain.Revurdering
import no.nav.etterlatte.behandling.hendelse.HendelseDao
import no.nav.etterlatte.behandling.klienter.GrunnlagKlient
import no.nav.etterlatte.common.Enheter
import no.nav.etterlatte.foerstegangsbehandling
import no.nav.etterlatte.funksjonsbrytere.FeatureToggleService
import no.nav.etterlatte.grunnlagsOpplysningMedPersonopplysning
import no.nav.etterlatte.grunnlagsendring.GrunnlagsendringshendelseDao
import no.nav.etterlatte.inTransaction
import no.nav.etterlatte.libs.common.behandling.BehandlingStatus
import no.nav.etterlatte.libs.common.behandling.BoddEllerArbeidetUtlandet
import no.nav.etterlatte.libs.common.behandling.Revurderingaarsak
import no.nav.etterlatte.libs.common.behandling.Saksrolle
import no.nav.etterlatte.libs.common.behandling.Utlandstilknytning
import no.nav.etterlatte.libs.common.behandling.UtlandstilknytningType
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstype
import no.nav.etterlatte.libs.common.oppgave.OppgaveIntern
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.oppgave.OppgaveService
import no.nav.etterlatte.personOpplysning
import no.nav.etterlatte.revurdering
import no.nav.etterlatte.token.BrukerTokenInfo
import no.nav.etterlatte.token.Saksbehandler
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import java.sql.Connection
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

class BehandlingServiceImplTest {
    private val user =
        mockk<SaksbehandlerMedEnheterOgRoller> {
            every { name() } returns "ident"
            every { enheter() } returns listOf(Enheter.defaultEnhet.enhetNr)
        }

    @BeforeEach
    fun before() {
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
    fun `skal hente behandlinger i sak`() {
        val behandlingHendelser = mockk<BehandlingHendelserKafkaProducer>()
        val behandlingDaoMock =
            mockk<BehandlingDao> {
                every { alleBehandlingerISak(1) } returns
                    listOf(
                        revurdering(sakId = 1, revurderingAarsak = Revurderingaarsak.REGULERING),
                        foerstegangsbehandling(sakId = 1),
                    )
            }
        val hendelserMock = mockk<HendelseDao>()

        val sut =
            BehandlingServiceImpl(
                behandlingDao = behandlingDaoMock,
                behandlingHendelser = behandlingHendelser,
                grunnlagsendringshendelseDao = mockk(),
                hendelseDao = hendelserMock,
                grunnlagKlient = mockk(),
                behandlingRequestLogger = mockk(),
                kommerBarnetTilGodeDao = mockk(),
                oppgaveService = mockk(),
                grunnlagService = mockk(),
            )

        val behandlinger = sut.hentBehandlingerForSak(1)

        assertAll(
            "skal hente behandlinger",
            { assertEquals(2, behandlinger.size) },
            { assertEquals(1, behandlinger.filterIsInstance<Foerstegangsbehandling>().size) },
            { assertEquals(1, behandlinger.filterIsInstance<Revurdering>().size) },
        )
    }

    @Test
    fun `avbrytBehandling sjekker om behandlingsstatusen er gyldig for avbrudd`() {
        val sakId = 1L
        val avbruttBehandling = foerstegangsbehandling(sakId = sakId, status = BehandlingStatus.AVBRUTT)
        val attestertBehandling = foerstegangsbehandling(sakId = sakId, status = BehandlingStatus.ATTESTERT)
        val iverksattBehandling = foerstegangsbehandling(sakId = sakId, status = BehandlingStatus.IVERKSATT)
        val nyFoerstegangsbehandling = foerstegangsbehandling(sakId = sakId)

        val behandlingDaoMock =
            mockk<BehandlingDao> {
                every { hentBehandling(avbruttBehandling.id) } returns avbruttBehandling
                every { hentBehandling(attestertBehandling.id) } returns attestertBehandling
                every { hentBehandling(iverksattBehandling.id) } returns iverksattBehandling
                every { hentBehandling(nyFoerstegangsbehandling.id) } returns nyFoerstegangsbehandling
                every { avbrytBehandling(nyFoerstegangsbehandling.id) } just runs
            }
        val hendelserMock =
            mockk<HendelseDao> {
                every { behandlingAvbrutt(any(), any()) } returns Unit
            }
        val hendelseskanalMock =
            mockk<BehandlingHendelserKafkaProducer> {
                every { sendMeldingForHendelseMedDetaljertBehandling(any(), any()) } returns Unit
            }
        val grunnlagsendringshendelseDaoMock =
            mockk<GrunnlagsendringshendelseDao> {
                every { kobleGrunnlagsendringshendelserFraBehandlingId(any()) } just runs
                every { hentGrunnlagsendringshendelseSomErTattMedIBehandling(any()) } returns emptyList()
            }

        val oppgaveServiceMock: OppgaveService =
            mockk {
                every { avbrytOppgaveUnderBehandling(any(), any()) } returns mockk<OppgaveIntern>()
            }

        val behandlingService =
            lagRealGenerellBehandlingService(
                behandlingDao = behandlingDaoMock,
                behandlingHendelserKafkaProducer = hendelseskanalMock,
                hendelseDao = hendelserMock,
                grunnlagsendringshendelseDao = grunnlagsendringshendelseDaoMock,
                oppgaveService = oppgaveServiceMock,
                grunnlagKlient = GrunnlagKlientTest(),
            )

        val saksbehandler = Saksbehandler("", "saksbehandler", null)
        assertThrows<IllegalStateException> {
            behandlingService.avbrytBehandling(avbruttBehandling.id, saksbehandler)
        }

        assertThrows<IllegalStateException> {
            behandlingService.avbrytBehandling(iverksattBehandling.id, saksbehandler)
        }

        assertThrows<IllegalStateException> {
            behandlingService.avbrytBehandling(attestertBehandling.id, saksbehandler)
        }
        assertDoesNotThrow {
            behandlingService.avbrytBehandling(nyFoerstegangsbehandling.id, saksbehandler)
        }
    }

    @Test
    fun `avbrytBehandling registrerer en avbruddshendelse`() {
        val sakId = 1L
        val nyFoerstegangsbehandling = foerstegangsbehandling(sakId = sakId)

        val behandlingDaoMock =
            mockk<BehandlingDao> {
                every { hentBehandling(nyFoerstegangsbehandling.id) } returns nyFoerstegangsbehandling
                every { avbrytBehandling(nyFoerstegangsbehandling.id) } just runs
            }
        val hendelserMock =
            mockk<HendelseDao> {
                every { behandlingAvbrutt(any(), any()) } returns Unit
            }
        val behandlingHendelserKafkaProducer =
            mockk<BehandlingHendelserKafkaProducer> {
                every { sendMeldingForHendelseMedDetaljertBehandling(any(), any()) } returns Unit
            }
        val grunnlagsendringshendelseDaoMock =
            mockk<GrunnlagsendringshendelseDao> {
                every { kobleGrunnlagsendringshendelserFraBehandlingId(any()) } just runs
                every { hentGrunnlagsendringshendelseSomErTattMedIBehandling(any()) } returns emptyList()
            }

        val oppgaveServiceMock: OppgaveService =
            mockk {
                every { avbrytOppgaveUnderBehandling(any(), any()) } returns mockk<OppgaveIntern>()
            }

        val behandlingService =
            lagRealGenerellBehandlingService(
                behandlingDao = behandlingDaoMock,
                behandlingHendelserKafkaProducer = behandlingHendelserKafkaProducer,
                grunnlagsendringshendelseDao = grunnlagsendringshendelseDaoMock,
                hendelseDao = hendelserMock,
                oppgaveService = oppgaveServiceMock,
                grunnlagKlient = GrunnlagKlientTest(),
            )

        behandlingService.avbrytBehandling(nyFoerstegangsbehandling.id, Saksbehandler("", "saksbehandler", null))
        verify {
            hendelserMock.behandlingAvbrutt(any(), any())
        }
    }

    @Test
    fun `avbrytBehandling ruller tilbake alt ved exception i intransaction`() {
        var didRollback = false
        Kontekst.set(
            Context(
                user,
                object : DatabaseKontekst {
                    override fun activeTx(): Connection {
                        throw IllegalArgumentException()
                    }

                    override fun <T> inTransaction(block: () -> T): T {
                        try {
                            return block()
                        } catch (ex: Throwable) {
                            didRollback = true
                            throw ex
                        }
                    }
                },
            ),
        )
        val sakId = 1L
        val nyFoerstegangsbehandling = foerstegangsbehandling(sakId = sakId)

        val behandlingDaoMock =
            mockk<BehandlingDao> {
                every { hentBehandling(nyFoerstegangsbehandling.id) } returns nyFoerstegangsbehandling
                every { avbrytBehandling(nyFoerstegangsbehandling.id) } just runs
            }
        val hendelserMock =
            mockk<HendelseDao> {
                every { behandlingAvbrutt(any(), any()) } returns Unit
            }
        val behandlingHendelserKafkaProducer =
            mockk<BehandlingHendelserKafkaProducer> {
                every { sendMeldingForHendelseMedDetaljertBehandling(any(), any()) } returns Unit
            }
        val grunnlagsendringshendelseDaoMock =
            mockk<GrunnlagsendringshendelseDao> {
                every { kobleGrunnlagsendringshendelserFraBehandlingId(any()) } throws RuntimeException("Alt må rulles tilbake")
                every { hentGrunnlagsendringshendelseSomErTattMedIBehandling(any()) } returns emptyList()
            }

        val oppgaveServiceMock: OppgaveService =
            mockk {
                every { avbrytOppgaveUnderBehandling(any(), any()) } returns mockk<OppgaveIntern>()
            }

        val behandlingService =
            lagRealGenerellBehandlingService(
                behandlingDao = behandlingDaoMock,
                behandlingHendelserKafkaProducer = behandlingHendelserKafkaProducer,
                grunnlagsendringshendelseDao = grunnlagsendringshendelseDaoMock,
                hendelseDao = hendelserMock,
                oppgaveService = oppgaveServiceMock,
            )

        assertFalse(didRollback)
        assertThrows<RuntimeException> {
            inTransaction { behandlingService.avbrytBehandling(nyFoerstegangsbehandling.id, Saksbehandler("", "saksbehandler", null)) }
        }

        assertTrue(didRollback)
    }

    @Test
    fun `avbrytBehandling sender en kafka-melding`() {
        val sakId = 1L
        val nyFoerstegangsbehandling = foerstegangsbehandling(sakId = sakId)

        val behandlingDaoMock =
            mockk<BehandlingDao> {
                every { hentBehandling(nyFoerstegangsbehandling.id) } returns nyFoerstegangsbehandling
                every { avbrytBehandling(nyFoerstegangsbehandling.id) } just runs
            }
        val hendelserMock =
            mockk<HendelseDao> {
                every { behandlingAvbrutt(any(), any()) } returns Unit
            }
        val behandlingHendelserKafkaProducerMock =
            mockk<BehandlingHendelserKafkaProducer> {
                every {
                    sendMeldingForHendelseMedDetaljertBehandling(
                        any(),
                        BehandlingHendelseType.AVBRUTT,
                    )
                } returns Unit
            }
        val grunnlagsendringshendelseDaoMock =
            mockk<GrunnlagsendringshendelseDao> {
                every { kobleGrunnlagsendringshendelserFraBehandlingId(any()) } just runs
                every { hentGrunnlagsendringshendelseSomErTattMedIBehandling(any()) } returns emptyList()
            }

        val oppgaveServiceMock: OppgaveService =
            mockk {
                every { avbrytOppgaveUnderBehandling(any(), any()) } returns mockk<OppgaveIntern>()
            }

        val behandlingService =
            lagRealGenerellBehandlingService(
                behandlingDao = behandlingDaoMock,
                behandlingHendelserKafkaProducer = behandlingHendelserKafkaProducerMock,
                grunnlagsendringshendelseDao = grunnlagsendringshendelseDaoMock,
                hendelseDao = hendelserMock,
                oppgaveService = oppgaveServiceMock,
                grunnlagKlient = GrunnlagKlientTest(),
            )

        behandlingService.avbrytBehandling(nyFoerstegangsbehandling.id, Saksbehandler("", "saksbehandler", null))
        verify {
            behandlingHendelserKafkaProducerMock.sendMeldingForHendelseMedDetaljertBehandling(
                any(),
                BehandlingHendelseType.AVBRUTT,
            )
        }
    }

    @Test
    fun `avbryt behandling setter koblede grunnlagsendringshendelser tilbake til ingen kobling`() {
        val sakId = 1L
        val nyFoerstegangsbehandling = foerstegangsbehandling(sakId = sakId)

        val behandlingDaoMock =
            mockk<BehandlingDao> {
                every { hentBehandling(nyFoerstegangsbehandling.id) } returns nyFoerstegangsbehandling
                every { avbrytBehandling(nyFoerstegangsbehandling.id) } just runs
            }
        val hendelserMock =
            mockk<HendelseDao> {
                every { behandlingAvbrutt(any(), any()) } returns Unit
            }
        val behandlingHendelserKafkaProducer =
            mockk<BehandlingHendelserKafkaProducer> {
                every {
                    sendMeldingForHendelseMedDetaljertBehandling(
                        any(),
                        BehandlingHendelseType.AVBRUTT,
                    )
                } returns Unit
            }
        val grunnlagsendringshendelseDaoMock =
            mockk<GrunnlagsendringshendelseDao> {
                every { kobleGrunnlagsendringshendelserFraBehandlingId(any()) } just runs
                every { hentGrunnlagsendringshendelseSomErTattMedIBehandling(any()) } returns emptyList()
            }

        val oppgaveServiceMock: OppgaveService =
            mockk {
                every { avbrytOppgaveUnderBehandling(any(), any()) } returns mockk<OppgaveIntern>()
            }
        val behandlingService =
            lagRealGenerellBehandlingService(
                behandlingDao = behandlingDaoMock,
                behandlingHendelserKafkaProducer = behandlingHendelserKafkaProducer,
                grunnlagsendringshendelseDao = grunnlagsendringshendelseDaoMock,
                hendelseDao = hendelserMock,
                oppgaveService = oppgaveServiceMock,
                grunnlagKlient = GrunnlagKlientTest(),
            )

        behandlingService.avbrytBehandling(nyFoerstegangsbehandling.id, Saksbehandler("", "saksbehandler", null))
        verify(exactly = 1) {
            grunnlagsendringshendelseDaoMock.kobleGrunnlagsendringshendelserFraBehandlingId(nyFoerstegangsbehandling.id)
        }
    }

    @Test
    fun `skal sette rett enum for rolle eller ukjent rolle`() {
        val kjentRolle = "gjenlevende"
        val ukjentRolle = "abcde"

        val resKjentRolle = Saksrolle.enumVedNavnEllerUkjent(kjentRolle)
        val resUkjentRolle = Saksrolle.enumVedNavnEllerUkjent(ukjentRolle)

        assertEquals(Saksrolle.GJENLEVENDE, resKjentRolle)
        assertEquals(Saksrolle.UKJENT, resUkjentRolle)
    }

    @Test
    fun `Virkningstidspunkt krever fastsetting av utlandstilknytning`() {
        val bodyVirkningstidspunkt = Tidspunkt.parse("2017-02-01T00:00:00Z")
        val bodyBegrunnelse = "begrunnelse"
        val request = VirkningstidspunktRequest(bodyVirkningstidspunkt.toString(), bodyBegrunnelse)

        val soeknadMottatt = LocalDateTime.parse("2020-01-01T00:00:00.000000000")
        val doedsdato = LocalDate.parse("2016-12-30")

        val service = behandlingServiceMedMocks(doedsdato, soeknadMottatt)
        assertThrows<VirkningstidspunktMaaHaUtenlandstilknytning> {
            runBlocking {
                service.erGyldigVirkningstidspunkt(BEHANDLINGS_ID, TOKEN, request)
            }
        }
    }

    @Test
    fun `Ved utlandstilknytning bosatt utland må kravdato være med`() {
        val bodyVirkningstidspunkt = Tidspunkt.parse("2017-02-01T00:00:00Z")
        val bodyBegrunnelse = "begrunnelse"
        val request = VirkningstidspunktRequest(bodyVirkningstidspunkt.toString(), bodyBegrunnelse)

        val soeknadMottatt = LocalDateTime.parse("2020-01-01T00:00:00.000000000")
        val doedsdato = LocalDate.parse("2016-12-30")

        val service =
            behandlingServiceMedMocks(
                doedsdato = doedsdato,
                soeknadMottatt = soeknadMottatt,
                utlandstilknytning =
                    Utlandstilknytning(
                        UtlandstilknytningType.BOSATT_UTLAND,
                        Grunnlagsopplysning.Saksbehandler.create("ident"),
                        "begrunnelse",
                    ),
            )

        assertThrows<KravdatoMaaFinnesHvisBosattutland> {
            runBlocking {
                service.erGyldigVirkningstidspunkt(BEHANDLINGS_ID, TOKEN, request)
            }
        }
    }

    @Test
    fun `Ved utlandstilknytning bosatt utland med kravdato skal kravdato brukes ikke soeknadmottatt`() {
        val bodyVirkningstidspunkt = Tidspunkt.parse("2017-02-01T00:00:00Z")
        val bodyBegrunnelse = "begrunnelse"
        val bodyKravdato = Tidspunkt.parse("2019-02-01T00:00:00Z")
        val request = VirkningstidspunktRequest(bodyVirkningstidspunkt.toString(), bodyBegrunnelse, bodyKravdato.toLocalDate())
        val doedsdato = LocalDate.parse("2014-01-01")

        val soeknadMottatt = LocalDateTime.parse("2009-01-01T00:00:00.000000000")

        val service =
            behandlingServiceMedMocks(
                doedsdato = doedsdato,
                soeknadMottatt = soeknadMottatt,
                utlandstilknytning =
                    Utlandstilknytning(
                        UtlandstilknytningType.BOSATT_UTLAND,
                        Grunnlagsopplysning.Saksbehandler.create("ident"),
                        "begrunnelse",
                    ),
            )

        val svar =
            runBlocking {
                service.erGyldigVirkningstidspunkt(BEHANDLINGS_ID, TOKEN, request)
            }
        assertTrue(svar)
    }

    @Test
    fun `erGyldigVirkningstidspunkt hvis tidspunkt er maaned etter doedsfall og maks tre aar foer mottatt soeknad`() {
        val bodyVirkningstidspunkt = Tidspunkt.parse("2017-02-01T00:00:00Z")
        val bodyBegrunnelse = "begrunnelse"
        val request = VirkningstidspunktRequest(bodyVirkningstidspunkt.toString(), bodyBegrunnelse)

        val soeknadMottatt = LocalDateTime.parse("2020-01-01T00:00:00.000000000")
        val doedsdato = LocalDate.parse("2016-12-30")

        val service =
            behandlingServiceMedMocks(
                doedsdato = doedsdato,
                soeknadMottatt = soeknadMottatt,
                utlandstilknytning =
                    Utlandstilknytning(
                        UtlandstilknytningType.NASJONAL,
                        Grunnlagsopplysning.Saksbehandler.create("ident"),
                        "begrunnelse",
                    ),
            )

        val gyldigVirkningstidspunkt =
            runBlocking {
                service.erGyldigVirkningstidspunkt(BEHANDLINGS_ID, TOKEN, request)
            }

        assertTrue(gyldigVirkningstidspunkt)
    }

    @Test
    fun `erGyldigVirkningstidspunkt er false hvis tidspunkt er foer en maaned etter doedsfall`() {
        val bodyVirkningstidspunkt = Tidspunkt.parse("2020-01-01T00:00:00Z")
        val bodyBegrunnelse = "begrunnelse"
        val request = VirkningstidspunktRequest(bodyVirkningstidspunkt.toString(), bodyBegrunnelse)

        val soeknadMottatt = LocalDateTime.parse("2020-02-01T00:00:00.000000000")
        val doedsdato = LocalDate.parse("2020-01-01")

        val service =
            behandlingServiceMedMocks(
                doedsdato = doedsdato,
                soeknadMottatt = soeknadMottatt,
                utlandstilknytning =
                    Utlandstilknytning(
                        UtlandstilknytningType.NASJONAL,
                        Grunnlagsopplysning.Saksbehandler.create("ident"),
                        "begrunnelse",
                    ),
            )

        val gyldigVirkningstidspunkt =
            runBlocking {
                service.erGyldigVirkningstidspunkt(BEHANDLINGS_ID, TOKEN, request)
            }

        assertFalse(gyldigVirkningstidspunkt)
    }

    @Test
    fun `erGyldigVirkningstidspunkt hvis tidspunkt er tre aar foer mottatt soeknad`() {
        val bodyVirkningstidspunkt = Tidspunkt.parse("2017-01-01T00:00:00Z")
        val bodyBegrunnelse = "begrunnelse"
        val request = VirkningstidspunktRequest(bodyVirkningstidspunkt.toString(), bodyBegrunnelse)

        val soeknadMottatt = LocalDateTime.parse("2020-01-01T00:00:00.000000000")
        val doedsdato = LocalDate.parse("2016-12-30")

        val service =
            behandlingServiceMedMocks(
                doedsdato = doedsdato,
                soeknadMottatt = soeknadMottatt,
                utlandstilknytning =
                    Utlandstilknytning(
                        UtlandstilknytningType.NASJONAL,
                        Grunnlagsopplysning.Saksbehandler.create("ident"),
                        "begrunnelse",
                    ),
            )

        val gyldigVirkningstidspunkt =
            runBlocking {
                service.erGyldigVirkningstidspunkt(BEHANDLINGS_ID, TOKEN, request)
            }

        assertFalse(gyldigVirkningstidspunkt)
    }

    @Test
    fun `hentSenestIverksatteBehandling() returnerer seneste iverksatte behandlingen`() {
        val behandling1 = foerstegangsbehandling(sakId = 1, status = BehandlingStatus.IVERKSATT)
        val behandling2 =
            revurdering(
                sakId = 1,
                status = BehandlingStatus.BEREGNET,
                revurderingAarsak = Revurderingaarsak.REGULERING,
            )
        val behandlingDaoMock =
            mockk<BehandlingDao> {
                every { alleBehandlingerISak(any()) } returns listOf(behandling1, behandling2)
            }

        val service =
            lagRealGenerellBehandlingService(
                behandlingDao = behandlingDaoMock,
            )

        assertEquals(behandling1, service.hentSisteIverksatte(1))
    }

    @Test
    fun `skal hente behandlinger i sak hvor sak har enhet og brukeren har enhet`() {
        every {
            user.enheter()
        } returns listOf(Enheter.PORSGRUNN.enhetNr)

        val behandlingHendelserKafkaProducerMock = mockk<BehandlingHendelserKafkaProducer>()
        val behandlingDaoMock =
            mockk<BehandlingDao> {
                every { alleBehandlingerISak(1) } returns
                    listOf(
                        revurdering(
                            sakId = 1,
                            revurderingAarsak = Revurderingaarsak.REGULERING,
                            enhet = Enheter.PORSGRUNN.enhetNr,
                        ),
                        foerstegangsbehandling(sakId = 1, enhet = Enheter.PORSGRUNN.enhetNr),
                    )
            }
        val hendelserMock = mockk<HendelseDao>()

        val sut =
            BehandlingServiceImpl(
                behandlingDao = behandlingDaoMock,
                behandlingHendelser = behandlingHendelserKafkaProducerMock,
                grunnlagsendringshendelseDao = mockk(),
                hendelseDao = hendelserMock,
                grunnlagKlient = mockk(),
                behandlingRequestLogger = mockk(),
                kommerBarnetTilGodeDao = mockk(),
                oppgaveService = mockk(),
                grunnlagService = mockk(),
            )

        val behandlinger = sut.hentBehandlingerForSak(1)

        assertAll(
            "skal hente behandlinger",
            { assertEquals(2, behandlinger.size) },
            { assertEquals(1, behandlinger.filterIsInstance<Foerstegangsbehandling>().size) },
            { assertEquals(1, behandlinger.filterIsInstance<Revurdering>().size) },
        )
    }

    @Test
    fun `skal ikke hente behandlinger i sak hvor sak har enhet og brukeren har ikke enhet`() {
        every {
            user.enheter()
        } returns listOf(Enheter.EGNE_ANSATTE.enhetNr)

        val behandlingHendelserKafkaProducerMock = mockk<BehandlingHendelserKafkaProducer>()
        val behandlingDaoMock =
            mockk<BehandlingDao> {
                every { alleBehandlingerISak(1) } returns
                    listOf(
                        revurdering(
                            sakId = 1,
                            revurderingAarsak = Revurderingaarsak.REGULERING,
                            enhet = Enheter.PORSGRUNN.enhetNr,
                        ),
                        foerstegangsbehandling(sakId = 1, enhet = Enheter.PORSGRUNN.enhetNr),
                    )
            }
        val hendelserMock = mockk<HendelseDao>()

        val sut =
            BehandlingServiceImpl(
                behandlingDao = behandlingDaoMock,
                behandlingHendelser = behandlingHendelserKafkaProducerMock,
                grunnlagsendringshendelseDao = mockk(),
                hendelseDao = hendelserMock,
                grunnlagKlient = mockk(),
                behandlingRequestLogger = mockk(),
                kommerBarnetTilGodeDao = mockk(),
                oppgaveService = mockk(),
                grunnlagService = mockk(),
            )

        val behandlinger = sut.hentBehandlingerForSak(1)

        assertEquals(0, behandlinger.size)
    }

    @Test
    fun `kan oppdatere bodd eller arbeidet i utlandet`() {
        every {
            user.enheter()
        } returns listOf(Enheter.PORSGRUNN.enhetNr)

        val uuid = UUID.randomUUID()

        val slot = slot<BoddEllerArbeidetUtlandet>()

        val behandlingDaoMock =
            mockk<BehandlingDao> {
                every { hentBehandling(any()) } returns
                    foerstegangsbehandling(
                        id = uuid,
                        sakId = 1,
                        enhet = Enheter.PORSGRUNN.enhetNr,
                    )

                every { lagreBoddEllerArbeidetUtlandet(any(), capture(slot)) } just runs

                every { lagreStatus(any()) } just runs
            }

        val sut =
            BehandlingServiceImpl(
                behandlingDaoMock,
                mockk(),
                mockk(),
                mockk(),
                mockk(),
                mockk(),
                kommerBarnetTilGodeDao = mockk(),
                oppgaveService = mockk(),
                grunnlagService = mockk(),
            )

        inTransaction {
            sut.oppdaterBoddEllerArbeidetUtlandet(
                uuid,
                BoddEllerArbeidetUtlandet(
                    true,
                    Grunnlagsopplysning.Saksbehandler.create("ident"),
                    "Test",
                ),
            )
        }

        assertEquals(true, slot.captured.boddEllerArbeidetUtlandet)
        assertEquals("Test", slot.captured.begrunnelse)
        assertEquals("ident", (slot.captured.kilde as Grunnlagsopplysning.Saksbehandler).ident)
    }

    private fun behandlingServiceMedMocks(
        doedsdato: LocalDate?,
        soeknadMottatt: LocalDateTime,
        utlandstilknytning: Utlandstilknytning? = null,
    ): BehandlingServiceImpl {
        val behandling =
            foerstegangsbehandling(
                id = BEHANDLINGS_ID,
                sakId = SAK_ID,
                soeknadMottattDato = soeknadMottatt,
                utlandstilknytning = utlandstilknytning,
            )
        val personopplysning = personOpplysning(doedsdato = doedsdato)
        val grunnlagsopplysningMedPersonopplysning = grunnlagsOpplysningMedPersonopplysning(personopplysning)
        val grunnlagKlient =
            mockk<GrunnlagKlientTest> {
                coEvery {
                    finnPersonOpplysning(behandling.id, Opplysningstype.AVDOED_PDL_V1, TOKEN)
                } returns grunnlagsopplysningMedPersonopplysning
                coEvery { hentPersongalleri(behandling.id, any()) } answers { callOriginal() }
            }

        return lagRealGenerellBehandlingService(
            behandlingDao =
                mockk {
                    every {
                        hentBehandling(BEHANDLINGS_ID)
                    } returns behandling
                },
            grunnlagKlient = grunnlagKlient,
        )
    }

    private fun lagRealGenerellBehandlingService(
        behandlingDao: BehandlingDao? = null,
        behandlingHendelserKafkaProducer: BehandlingHendelserKafkaProducer? = null,
        hendelseDao: HendelseDao? = null,
        grunnlagsendringshendelseDao: GrunnlagsendringshendelseDao = mockk(),
        grunnlagKlient: GrunnlagKlient? = null,
        oppgaveService: OppgaveService = mockk(),
        featureToggleService: FeatureToggleService = mockk<FeatureToggleService>(),
    ): BehandlingServiceImpl =
        BehandlingServiceImpl(
            behandlingDao = behandlingDao ?: mockk(),
            behandlingHendelser = behandlingHendelserKafkaProducer ?: mockk(),
            grunnlagsendringshendelseDao = grunnlagsendringshendelseDao,
            hendelseDao = hendelseDao ?: mockk(),
            grunnlagKlient = grunnlagKlient ?: mockk(),
            behandlingRequestLogger = mockk(),
            kommerBarnetTilGodeDao = mockk(),
            oppgaveService = oppgaveService,
            grunnlagService = mockk(),
        )

    companion object {
        const val SAK_ID = 1L
        val BEHANDLINGS_ID: UUID = UUID.randomUUID()
        val TOKEN = BrukerTokenInfo.of("a", "b", null, null, null)
    }
}
