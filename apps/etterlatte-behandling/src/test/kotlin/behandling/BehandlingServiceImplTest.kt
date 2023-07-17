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
import no.nav.etterlatte.libs.common.behandling.BehandlingStatus
import no.nav.etterlatte.libs.common.behandling.BoddEllerArbeidetUtlandet
import no.nav.etterlatte.libs.common.behandling.RevurderingAarsak
import no.nav.etterlatte.libs.common.behandling.Saksrolle
import no.nav.etterlatte.libs.common.behandling.Utenlandstilsnitt
import no.nav.etterlatte.libs.common.behandling.UtenlandstilsnittType
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstype
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.personOpplysning
import no.nav.etterlatte.revurdering
import no.nav.etterlatte.token.BrukerTokenInfo
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
import java.util.*

class BehandlingServiceImplTest {
    private val user = mockk<SaksbehandlerMedEnheterOgRoller>()

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
                }
            )
        )
    }

    @Test
    fun `skal hente behandlinger i sak`() {
        val behandlingHendelser = mockk<BehandlingHendelserKafkaProducer>()
        val behandlingDaoMock = mockk<BehandlingDao> {
            every { alleBehandlingerISak(1) } returns listOf(
                revurdering(sakId = 1, revurderingAarsak = RevurderingAarsak.REGULERING),
                foerstegangsbehandling(sakId = 1)
            )
        }
        val hendelserMock = mockk<HendelseDao>()

        val featureToggleService = mockk<FeatureToggleService>()
        every { featureToggleService.isEnabled(BehandlingServiceFeatureToggle.FiltrerMedEnhetId, false) } returns false

        val sut = BehandlingServiceImpl(
            behandlingDao = behandlingDaoMock,
            behandlingHendelser = behandlingHendelser,
            grunnlagsendringshendelseDao = mockk(),
            hendelseDao = hendelserMock,
            grunnlagKlient = mockk(),
            sporingslogg = mockk(),
            featureToggleService = featureToggleService,
            kommerBarnetTilGodeDao = mockk()
        )

        val behandlinger = sut.hentBehandlingerISak(1)

        assertAll(
            "skal hente behandlinger",
            { assertEquals(2, behandlinger.size) },
            { assertEquals(1, behandlinger.filterIsInstance<Foerstegangsbehandling>().size) },
            { assertEquals(1, behandlinger.filterIsInstance<Revurdering>().size) }
        )
    }

    @Test
    fun `avbrytBehandling sjekker om behandlingsstatusen er gyldig for avbrudd`() {
        val sakId = 1L
        val avbruttBehandling = foerstegangsbehandling(sakId = sakId, status = BehandlingStatus.AVBRUTT)
        val attestertBehandling = foerstegangsbehandling(sakId = sakId, status = BehandlingStatus.ATTESTERT)
        val iverksattBehandling = foerstegangsbehandling(sakId = sakId, status = BehandlingStatus.IVERKSATT)
        val nyFoerstegangsbehandling = foerstegangsbehandling(sakId = sakId)

        val behandlingDaoMock = mockk<BehandlingDao> {
            every { hentBehandling(avbruttBehandling.id) } returns avbruttBehandling
            every { hentBehandling(attestertBehandling.id) } returns attestertBehandling
            every { hentBehandling(iverksattBehandling.id) } returns iverksattBehandling
            every { hentBehandling(nyFoerstegangsbehandling.id) } returns nyFoerstegangsbehandling
            every { avbrytBehandling(nyFoerstegangsbehandling.id) } just runs
        }
        val hendelserMock = mockk<HendelseDao> {
            every { behandlingAvbrutt(any(), any()) } returns Unit
        }
        val hendelseskanalMock = mockk<BehandlingHendelserKafkaProducer> {
            every { sendMeldingForHendelse(any(), any()) } returns Unit
        }
        val grunnlagsendringshendelseDaoMock = mockk<GrunnlagsendringshendelseDao> {
            every { kobleGrunnlagsendringshendelserFraBehandlingId(any()) } just runs
        }

        val featureToggleService = mockk<FeatureToggleService>()
        every { featureToggleService.isEnabled(BehandlingServiceFeatureToggle.FiltrerMedEnhetId, false) } returns false

        val behandlingService =
            lagRealGenerellBehandlingService(
                behandlingDao = behandlingDaoMock,
                behandlingHendelserKafkaProducer = hendelseskanalMock,
                hendelseDao = hendelserMock,
                grunnlagsendringshendelseDao = grunnlagsendringshendelseDaoMock,
                featureToggleService = featureToggleService
            )

        assertThrows<IllegalStateException> {
            behandlingService.avbrytBehandling(avbruttBehandling.id, "")
        }

        assertThrows<IllegalStateException> {
            behandlingService.avbrytBehandling(iverksattBehandling.id, "")
        }

        assertThrows<IllegalStateException> {
            behandlingService.avbrytBehandling(attestertBehandling.id, "")
        }
        assertDoesNotThrow {
            behandlingService.avbrytBehandling(nyFoerstegangsbehandling.id, "")
        }
    }

    @Test
    fun `avbrytBehandling registrer en avbruddshendelse`() {
        val sakId = 1L
        val nyFoerstegangsbehandling = foerstegangsbehandling(sakId = sakId)

        val behandlingDaoMock = mockk<BehandlingDao> {
            every { hentBehandling(nyFoerstegangsbehandling.id) } returns nyFoerstegangsbehandling
            every { avbrytBehandling(nyFoerstegangsbehandling.id) } just runs
        }
        val hendelserMock = mockk<HendelseDao> {
            every { behandlingAvbrutt(any(), any()) } returns Unit
        }
        val behandlingHendelserKafkaProducer = mockk<BehandlingHendelserKafkaProducer> {
            every { sendMeldingForHendelse(any(), any()) } returns Unit
        }
        val grunnlagsendringshendelseDaoMock = mockk<GrunnlagsendringshendelseDao> {
            every { kobleGrunnlagsendringshendelserFraBehandlingId(any()) } just runs
        }

        val featureToggleService = mockk<FeatureToggleService>()
        every { featureToggleService.isEnabled(BehandlingServiceFeatureToggle.FiltrerMedEnhetId, false) } returns false

        val behandlingService =
            lagRealGenerellBehandlingService(
                behandlingDao = behandlingDaoMock,
                behandlingHendelserKafkaProducer = behandlingHendelserKafkaProducer,
                grunnlagsendringshendelseDao = grunnlagsendringshendelseDaoMock,
                hendelseDao = hendelserMock,
                featureToggleService = featureToggleService
            )

        behandlingService.avbrytBehandling(nyFoerstegangsbehandling.id, "")
        verify {
            hendelserMock.behandlingAvbrutt(any(), any())
        }
    }

    @Test
    fun `avbrytBehandling sender en kafka-melding`() {
        val sakId = 1L
        val nyFoerstegangsbehandling = foerstegangsbehandling(sakId = sakId)

        val behandlingDaoMock = mockk<BehandlingDao> {
            every { hentBehandling(nyFoerstegangsbehandling.id) } returns nyFoerstegangsbehandling
            every { avbrytBehandling(nyFoerstegangsbehandling.id) } just runs
        }
        val hendelserMock = mockk<HendelseDao> {
            every { behandlingAvbrutt(any(), any()) } returns Unit
        }
        val behandlingHendelserKafkaProducerMock = mockk<BehandlingHendelserKafkaProducer> {
            every {
                sendMeldingForHendelse(
                    nyFoerstegangsbehandling,
                    BehandlingHendelseType.AVBRUTT
                )
            } returns Unit
        }
        val grunnlagsendringshendelseDaoMock = mockk<GrunnlagsendringshendelseDao> {
            every { kobleGrunnlagsendringshendelserFraBehandlingId(any()) } just runs
        }

        val featureToggleService = mockk<FeatureToggleService>()
        every { featureToggleService.isEnabled(BehandlingServiceFeatureToggle.FiltrerMedEnhetId, false) } returns false

        val behandlingService =
            lagRealGenerellBehandlingService(
                behandlingDao = behandlingDaoMock,
                behandlingHendelserKafkaProducer = behandlingHendelserKafkaProducerMock,
                grunnlagsendringshendelseDao = grunnlagsendringshendelseDaoMock,
                hendelseDao = hendelserMock,
                featureToggleService = featureToggleService
            )

        behandlingService.avbrytBehandling(nyFoerstegangsbehandling.id, "")
        verify {
            behandlingHendelserKafkaProducerMock.sendMeldingForHendelse(
                nyFoerstegangsbehandling,
                BehandlingHendelseType.AVBRUTT
            )
        }
    }

    @Test
    fun `avbryt behandling setter koblede grunnlagsendringshendelser tilbake til ingen kobling`() {
        val sakId = 1L
        val nyFoerstegangsbehandling = foerstegangsbehandling(sakId = sakId)

        val behandlingDaoMock = mockk<BehandlingDao> {
            every { hentBehandling(nyFoerstegangsbehandling.id) } returns nyFoerstegangsbehandling
            every { avbrytBehandling(nyFoerstegangsbehandling.id) } just runs
        }
        val hendelserMock = mockk<HendelseDao> {
            every { behandlingAvbrutt(any(), any()) } returns Unit
        }
        val behandlingHendelserKafkaProducer = mockk<BehandlingHendelserKafkaProducer> {
            every {
                sendMeldingForHendelse(
                    nyFoerstegangsbehandling,
                    BehandlingHendelseType.AVBRUTT
                )
            } returns Unit
        }
        val grunnlagsendringshendelseDaoMock = mockk<GrunnlagsendringshendelseDao> {
            every { kobleGrunnlagsendringshendelserFraBehandlingId(any()) } just runs
        }

        val featureToggleService = mockk<FeatureToggleService>()
        every { featureToggleService.isEnabled(BehandlingServiceFeatureToggle.FiltrerMedEnhetId, false) } returns false

        val behandlingService =
            lagRealGenerellBehandlingService(
                behandlingDao = behandlingDaoMock,
                behandlingHendelserKafkaProducer = behandlingHendelserKafkaProducer,
                grunnlagsendringshendelseDao = grunnlagsendringshendelseDaoMock,
                hendelseDao = hendelserMock,
                featureToggleService = featureToggleService
            )

        behandlingService.avbrytBehandling(nyFoerstegangsbehandling.id, "")
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
    fun `hentBehandlingMedEnkelPersonopplysning henter behandlingsinfo og etterspurt personopplysning`() {
        val soeknadMottatDato = LocalDateTime.parse("2020-01-01T00:00:00")
        val behandling = foerstegangsbehandling(
            id = BEHANDLINGS_ID,
            sakId = SAK_ID,
            soeknadMottattDato = soeknadMottatDato
        )
        val opplysningstype = Opplysningstype.AVDOED_PDL_V1
        val doedsdato = LocalDate.parse("2020-01-01")

        val personopplysning = personOpplysning(doedsdato = doedsdato)
        val grunnlagsopplysningMedPersonopplysning = grunnlagsOpplysningMedPersonopplysning(personopplysning)

        val service = lagRealGenerellBehandlingService(
            behandlingDao = mockk {
                every { hentBehandling(BEHANDLINGS_ID) } returns behandling
            },
            grunnlagKlient = mockk {
                coEvery {
                    finnPersonOpplysning(SAK_ID, opplysningstype, TOKEN)
                } returns grunnlagsopplysningMedPersonopplysning
            },
            featureToggleService = mockk {
                every { isEnabled(BehandlingServiceFeatureToggle.FiltrerMedEnhetId, false) } returns false
            }
        )
        val behandlingMedPersonopplsning = runBlocking {
            service.hentBehandlingMedEnkelPersonopplysning(
                BEHANDLINGS_ID,
                TOKEN,
                opplysningstype
            )
        }

        assertEquals(soeknadMottatDato, behandlingMedPersonopplsning.soeknadMottattDato)
        assertEquals(doedsdato, behandlingMedPersonopplsning.personopplysning?.opplysning?.doedsdato)
    }

    @Test
    fun `erGyldigVirkningstidspunkt hvis tidspunkt er maaned etter doedsfall og maks tre aar foer mottatt soeknad`() {
        val bodyVirkningstidspunkt = Tidspunkt.parse("2017-02-01T00:00:00Z")
        val bodyBegrunnelse = "begrunnelse"
        val request = VirkningstidspunktRequest(bodyVirkningstidspunkt.toString(), bodyBegrunnelse)

        val soeknadMottatt = LocalDateTime.parse("2020-01-01T00:00:00.000000000")
        val doedsdato = LocalDate.parse("2016-12-30")

        val service = behandlingServiceMedMocks(doedsdato, soeknadMottatt)

        val gyldigVirkningstidspunkt = runBlocking {
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

        val service = behandlingServiceMedMocks(doedsdato, soeknadMottatt)

        val gyldigVirkningstidspunkt = runBlocking {
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

        val service = behandlingServiceMedMocks(doedsdato, soeknadMottatt)

        val gyldigVirkningstidspunkt = runBlocking {
            service.erGyldigVirkningstidspunkt(BEHANDLINGS_ID, TOKEN, request)
        }

        assertFalse(gyldigVirkningstidspunkt)
    }

    @Test
    fun `hentSenestIverksatteBehandling() returnerer seneste iverksatte behandlingen`() {
        val behandling1 = foerstegangsbehandling(sakId = 1, status = BehandlingStatus.IVERKSATT)
        val behandling2 = revurdering(
            sakId = 1,
            status = BehandlingStatus.BEREGNET,
            revurderingAarsak = RevurderingAarsak.REGULERING
        )
        val behandlingDaoMock = mockk<BehandlingDao> {
            every { alleBehandlingerISak(any()) } returns listOf(behandling1, behandling2)
        }

        val featureToggleService = mockk<FeatureToggleService>()
        every { featureToggleService.isEnabled(BehandlingServiceFeatureToggle.FiltrerMedEnhetId, false) } returns false

        val service = lagRealGenerellBehandlingService(
            behandlingDao = behandlingDaoMock,
            featureToggleService = featureToggleService
        )

        assertEquals(behandling1, service.hentSisteIverksatte(1))
    }

    @Test
    fun `skal hente behandlinger i sak hvor sak har enhet og brukeren har enhet`() {
        every {
            user.enheter()
        } returns listOf(Enheter.PORSGRUNN.enhetNr)

        val behandlingHendelserKafkaProducerMock = mockk<BehandlingHendelserKafkaProducer>()
        val behandlingDaoMock = mockk<BehandlingDao> {
            every { alleBehandlingerISak(1) } returns listOf(
                revurdering(
                    sakId = 1,
                    revurderingAarsak = RevurderingAarsak.REGULERING,
                    enhet = Enheter.PORSGRUNN.enhetNr
                ),
                foerstegangsbehandling(sakId = 1, enhet = Enheter.PORSGRUNN.enhetNr)
            )
        }
        val hendelserMock = mockk<HendelseDao>()

        val featureToggleService = mockk<FeatureToggleService>()
        every { featureToggleService.isEnabled(BehandlingServiceFeatureToggle.FiltrerMedEnhetId, false) } returns true

        val sut = BehandlingServiceImpl(
            behandlingDao = behandlingDaoMock,
            behandlingHendelser = behandlingHendelserKafkaProducerMock,
            grunnlagsendringshendelseDao = mockk(),
            hendelseDao = hendelserMock,
            grunnlagKlient = mockk(),
            sporingslogg = mockk(),
            featureToggleService = featureToggleService,
            kommerBarnetTilGodeDao = mockk()
        )

        val behandlinger = sut.hentBehandlingerISak(1)

        assertAll(
            "skal hente behandlinger",
            { assertEquals(2, behandlinger.size) },
            { assertEquals(1, behandlinger.filterIsInstance<Foerstegangsbehandling>().size) },
            { assertEquals(1, behandlinger.filterIsInstance<Revurdering>().size) }
        )
    }

    @Test
    fun `skal ikke hente behandlinger i sak hvor sak har enhet og brukeren har ikke enhet`() {
        every {
            user.enheter()
        } returns listOf(Enheter.EGNE_ANSATTE.enhetNr)

        val behandlingHendelserKafkaProducerMock = mockk<BehandlingHendelserKafkaProducer>()
        val behandlingDaoMock = mockk<BehandlingDao> {
            every { alleBehandlingerISak(1) } returns listOf(
                revurdering(
                    sakId = 1,
                    revurderingAarsak = RevurderingAarsak.REGULERING,
                    enhet = Enheter.PORSGRUNN.enhetNr
                ),
                foerstegangsbehandling(sakId = 1, enhet = Enheter.PORSGRUNN.enhetNr)
            )
        }
        val hendelserMock = mockk<HendelseDao>()

        val featureToggleService = mockk<FeatureToggleService>()
        every { featureToggleService.isEnabled(BehandlingServiceFeatureToggle.FiltrerMedEnhetId, false) } returns true

        val sut = BehandlingServiceImpl(
            behandlingDao = behandlingDaoMock,
            behandlingHendelser = behandlingHendelserKafkaProducerMock,
            grunnlagsendringshendelseDao = mockk(),
            hendelseDao = hendelserMock,
            grunnlagKlient = mockk(),
            sporingslogg = mockk(),
            featureToggleService = featureToggleService,
            kommerBarnetTilGodeDao = mockk()
        )

        val behandlinger = sut.hentBehandlingerISak(1)

        assertEquals(0, behandlinger.size)
    }

    @Test
    fun `kan oppdatere utenlandstilsnitt`() {
        every {
            user.enheter()
        } returns listOf(Enheter.PORSGRUNN.enhetNr)

        val uuid = UUID.randomUUID()

        val slot = slot<Utenlandstilsnitt>()

        val behandlingDaoMock = mockk<BehandlingDao> {
            every { hentBehandling(any()) } returns
                foerstegangsbehandling(
                    id = uuid,
                    sakId = 1,
                    enhet = Enheter.PORSGRUNN.enhetNr
                )

            every { lagreUtenlandstilsnitt(any(), capture(slot)) } just runs

            every { lagreStatus(any()) } just runs
        }

        val featureToggleService = mockk<FeatureToggleService>()
        every { featureToggleService.isEnabled(BehandlingServiceFeatureToggle.FiltrerMedEnhetId, false) } returns true

        val sut = BehandlingServiceImpl(
            behandlingDao = behandlingDaoMock,
            behandlingHendelser = mockk(),
            grunnlagsendringshendelseDao = mockk(),
            hendelseDao = mockk(),
            grunnlagKlient = mockk(),
            sporingslogg = mockk(),
            featureToggleService = featureToggleService,
            kommerBarnetTilGodeDao = mockk()
        )

        sut.oppdaterUtenlandstilsnitt(
            uuid,
            Utenlandstilsnitt(
                UtenlandstilsnittType.BOSATT_UTLAND,
                Grunnlagsopplysning.Saksbehandler.create("ident"),
                "Test"
            )
        )

        assertEquals(UtenlandstilsnittType.BOSATT_UTLAND, slot.captured.type)
        assertEquals("Test", slot.captured.begrunnelse)
        assertEquals("ident", slot.captured.kilde.ident)
    }

    @Test
    fun `kan oppdatere bodd eller arbeidet i utlandet`() {
        every {
            user.enheter()
        } returns listOf(Enheter.PORSGRUNN.enhetNr)

        val uuid = UUID.randomUUID()

        val slot = slot<BoddEllerArbeidetUtlandet>()

        val behandlingDaoMock = mockk<BehandlingDao> {
            every { hentBehandling(any()) } returns
                foerstegangsbehandling(
                    id = uuid,
                    sakId = 1,
                    enhet = Enheter.PORSGRUNN.enhetNr
                )

            every { lagreBoddEllerArbeidetUtlandet(any(), capture(slot)) } just runs

            every { lagreStatus(any()) } just runs
        }

        val featureToggleService = mockk<FeatureToggleService>()
        every { featureToggleService.isEnabled(BehandlingServiceFeatureToggle.FiltrerMedEnhetId, false) } returns true

        val sut = BehandlingServiceImpl(
            behandlingDaoMock,
            mockk(),
            mockk(),
            mockk(),
            mockk(),
            mockk(),
            featureToggleService,
            kommerBarnetTilGodeDao = mockk()
        )

        sut.oppdaterBoddEllerArbeidetUtlandet(
            uuid,
            BoddEllerArbeidetUtlandet(
                true,
                Grunnlagsopplysning.Saksbehandler.create("ident"),
                "Test"
            )
        )

        assertEquals(true, slot.captured.boddEllerArbeidetUtlandet)
        assertEquals("Test", slot.captured.begrunnelse)
        assertEquals("ident", slot.captured.kilde.ident)
    }

    private fun behandlingServiceMedMocks(
        doedsdato: LocalDate?,
        soeknadMottatt: LocalDateTime
    ): BehandlingServiceImpl {
        val behandling = foerstegangsbehandling(
            id = BEHANDLINGS_ID,
            sakId = SAK_ID,
            soeknadMottattDato = soeknadMottatt
        )
        val personopplysning = personOpplysning(doedsdato = doedsdato)
        val grunnlagsopplysningMedPersonopplysning = grunnlagsOpplysningMedPersonopplysning(personopplysning)

        return lagRealGenerellBehandlingService(
            behandlingDao = mockk {
                every {
                    hentBehandling(BEHANDLINGS_ID)
                } returns behandling
            },
            grunnlagKlient = mockk {
                coEvery {
                    finnPersonOpplysning(SAK_ID, Opplysningstype.AVDOED_PDL_V1, TOKEN)
                } returns grunnlagsopplysningMedPersonopplysning
            },
            featureToggleService = mockk {
                every { isEnabled(BehandlingServiceFeatureToggle.FiltrerMedEnhetId, false) } returns false
            }
        )
    }

    private fun lagRealGenerellBehandlingService(
        behandlingDao: BehandlingDao? = null,
        behandlingHendelserKafkaProducer: BehandlingHendelserKafkaProducer? = null,
        hendelseDao: HendelseDao? = null,
        grunnlagsendringshendelseDao: GrunnlagsendringshendelseDao = mockk(),
        grunnlagKlient: GrunnlagKlient? = null,
        featureToggleService: FeatureToggleService
    ): BehandlingServiceImpl = BehandlingServiceImpl(
        behandlingDao = behandlingDao ?: mockk(),
        behandlingHendelser = behandlingHendelserKafkaProducer ?: mockk(),
        grunnlagsendringshendelseDao = grunnlagsendringshendelseDao,
        hendelseDao = hendelseDao ?: mockk(),
        grunnlagKlient = grunnlagKlient ?: mockk(),
        sporingslogg = mockk(),
        featureToggleService = featureToggleService,
        kommerBarnetTilGodeDao = mockk()
    )

    companion object {
        const val SAK_ID = 1L
        val BEHANDLINGS_ID: UUID = UUID.randomUUID()
        val TOKEN = BrukerTokenInfo.of("a", "b", null, null, null)
    }
}