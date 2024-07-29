package no.nav.etterlatte.behandling

import io.kotest.matchers.shouldBe
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.DatabaseKontekst
import no.nav.etterlatte.behandling.domain.Foerstegangsbehandling
import no.nav.etterlatte.behandling.domain.Revurdering
import no.nav.etterlatte.behandling.hendelse.HendelseDao
import no.nav.etterlatte.behandling.klienter.GrunnlagKlient
import no.nav.etterlatte.common.Enheter
import no.nav.etterlatte.foerstegangsbehandling
import no.nav.etterlatte.grunnlagsOpplysningMedPersonopplysning
import no.nav.etterlatte.grunnlagsendring.GrunnlagsendringshendelseDao
import no.nav.etterlatte.inTransaction
import no.nav.etterlatte.ktor.token.simpleSaksbehandler
import no.nav.etterlatte.libs.common.Vedtaksloesning
import no.nav.etterlatte.libs.common.behandling.BehandlingHendelseType
import no.nav.etterlatte.libs.common.behandling.BehandlingStatus
import no.nav.etterlatte.libs.common.behandling.BehandlingType
import no.nav.etterlatte.libs.common.behandling.BoddEllerArbeidetUtlandet
import no.nav.etterlatte.libs.common.behandling.Persongalleri
import no.nav.etterlatte.libs.common.behandling.Revurderingaarsak
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.behandling.Utlandstilknytning
import no.nav.etterlatte.libs.common.behandling.UtlandstilknytningType
import no.nav.etterlatte.libs.common.behandling.Virkningstidspunkt
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstype
import no.nav.etterlatte.libs.common.oppgave.OppgaveIntern
import no.nav.etterlatte.libs.common.sak.Sak
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.toObjectNode
import no.nav.etterlatte.mockSaksbehandler
import no.nav.etterlatte.nyKontekstMedBruker
import no.nav.etterlatte.nyKontekstMedBrukerOgDatabaseContext
import no.nav.etterlatte.oppgave.OppgaveService
import no.nav.etterlatte.personOpplysning
import no.nav.etterlatte.revurdering
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertAll
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.testcontainers.shaded.org.apache.commons.lang3.NotImplementedException
import java.sql.Connection
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.Month
import java.time.YearMonth
import java.util.UUID
import kotlin.random.Random

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class BehandlingServiceImplTest {
    private val behandlingDaoMock = mockk<BehandlingDao>()
    private val behandlingHendelser = mockk<BehandlingHendelserKafkaProducer>()
    private val grunnlagsendringshendelseDaoMock = mockk<GrunnlagsendringshendelseDao>()
    private val hendelseDaoMock = mockk<HendelseDao>()
    private val grunnlagKlientMock = mockk<GrunnlagKlient>()
    private val oppgaveServiceMock = mockk<OppgaveService>()

    private val behandlingService =
        BehandlingServiceImpl(
            behandlingDao = behandlingDaoMock,
            behandlingHendelser = behandlingHendelser,
            grunnlagsendringshendelseDao = grunnlagsendringshendelseDaoMock,
            hendelseDao = hendelseDaoMock,
            grunnlagKlient = grunnlagKlientMock,
            behandlingRequestLogger = mockk(),
            kommerBarnetTilGodeDao = mockk(),
            oppgaveService = oppgaveServiceMock,
            grunnlagService = mockk(),
            beregningKlient = mockk(),
        )

    @AfterEach
    fun after() {
        clearAllMocks()
    }

    @Test
    fun `Kan hente egne ansatte behandlínger som egen ansatt saksbehandler`() {
        nyKontekstMedBruker(mockSaksbehandler(harRolleEgenAnsatt = true))

        every { behandlingDaoMock.hentBehandlingerForSak(1) } returns
            listOf(
                revurdering(
                    sakId = 1,
                    revurderingAarsak = Revurderingaarsak.REGULERING,
                    enhet = Enheter.EGNE_ANSATTE.enhetNr,
                ),
                foerstegangsbehandling(sakId = 1, enhet = Enheter.EGNE_ANSATTE.enhetNr),
            )

        val behandlinger = behandlingService.hentBehandlingerForSak(1)

        assertAll(
            "skal hente behandlinger",
            { assertEquals(2, behandlinger.size) },
            { assertEquals(1, behandlinger.filterIsInstance<Foerstegangsbehandling>().size) },
            { assertEquals(1, behandlinger.filterIsInstance<Revurdering>().size) },
        )
    }

    @Test
    fun `Kan hente strengt fortrolig behandlínger som streng fortrolig saksbehandler`() {
        nyKontekstMedBruker(mockSaksbehandler("ident", harRolleStrengtFortrolig = true))

        every { behandlingDaoMock.hentBehandlingerForSak(1) } returns
            listOf(
                revurdering(
                    sakId = 1,
                    revurderingAarsak = Revurderingaarsak.REGULERING,
                    enhet = Enheter.STRENGT_FORTROLIG.enhetNr,
                ),
                foerstegangsbehandling(sakId = 1, enhet = Enheter.STRENGT_FORTROLIG.enhetNr),
            )

        val behandlinger = behandlingService.hentBehandlingerForSak(1)

        assertAll(
            "skal hente behandlinger",
            { assertEquals(2, behandlinger.size) },
            { assertEquals(1, behandlinger.filterIsInstance<Foerstegangsbehandling>().size) },
            { assertEquals(1, behandlinger.filterIsInstance<Revurdering>().size) },
        )
    }

    @Test
    fun `Kan ikke hente strengt fortrolig behandlínger som vanlig saksbehandler`() {
        nyKontekstMedBruker(mockSaksbehandler())

        every { behandlingDaoMock.hentBehandlingerForSak(1) } returns
            listOf(
                revurdering(
                    sakId = 1,
                    revurderingAarsak = Revurderingaarsak.REGULERING,
                    enhet = Enheter.STRENGT_FORTROLIG.enhetNr,
                ),
                foerstegangsbehandling(sakId = 1, enhet = Enheter.STRENGT_FORTROLIG.enhetNr),
            )

        val behandlinger = behandlingService.hentBehandlingerForSak(1)

        assertAll(
            "skal hente behandlinger",
            { assertEquals(0, behandlinger.size) },
            { assertEquals(0, behandlinger.filterIsInstance<Foerstegangsbehandling>().size) },
            { assertEquals(0, behandlinger.filterIsInstance<Revurdering>().size) },
        )
    }

    @Test
    fun `Kan ikke hente egne ansatte behandlínger som vanlig saksbehandler`() {
        nyKontekstMedBruker(mockSaksbehandler())

        every { behandlingDaoMock.hentBehandlingerForSak(1) } returns
            listOf(
                revurdering(
                    sakId = 1,
                    revurderingAarsak = Revurderingaarsak.REGULERING,
                    enhet = Enheter.EGNE_ANSATTE.enhetNr,
                ),
                foerstegangsbehandling(sakId = 1, enhet = Enheter.EGNE_ANSATTE.enhetNr),
            )

        val behandlinger = behandlingService.hentBehandlingerForSak(1)

        assertAll(
            "skal hente behandlinger",
            { assertEquals(0, behandlinger.size) },
            { assertEquals(0, behandlinger.filterIsInstance<Foerstegangsbehandling>().size) },
            { assertEquals(0, behandlinger.filterIsInstance<Revurdering>().size) },
        )
    }

    @Test
    fun `skal hente behandlinger i sak`() {
        nyKontekstMedBruker(mockSaksbehandler())

        every { behandlingDaoMock.hentBehandlingerForSak(1) } returns
            listOf(
                revurdering(sakId = 1, revurderingAarsak = Revurderingaarsak.REGULERING),
                foerstegangsbehandling(sakId = 1),
            )

        val behandlinger = behandlingService.hentBehandlingerForSak(1)

        assertAll(
            "skal hente behandlinger",
            { assertEquals(2, behandlinger.size) },
            { assertEquals(1, behandlinger.filterIsInstance<Foerstegangsbehandling>().size) },
            { assertEquals(1, behandlinger.filterIsInstance<Revurdering>().size) },
        )
    }

    @Test
    fun `avbrytBehandling sjekker om behandlingsstatusen er gyldig for avbrudd`() {
        nyKontekstMedBruker(mockSaksbehandler())

        val sakId = 1L
        val avbruttBehandling = foerstegangsbehandling(sakId = sakId, status = BehandlingStatus.AVBRUTT)
        val attestertBehandling = foerstegangsbehandling(sakId = sakId, status = BehandlingStatus.ATTESTERT)
        val iverksattBehandling = foerstegangsbehandling(sakId = sakId, status = BehandlingStatus.IVERKSATT)
        val nyFoerstegangsbehandling = foerstegangsbehandling(sakId = sakId)

        every { behandlingDaoMock.hentBehandling(avbruttBehandling.id) } returns avbruttBehandling
        every { behandlingDaoMock.hentBehandling(attestertBehandling.id) } returns attestertBehandling
        every { behandlingDaoMock.hentBehandling(iverksattBehandling.id) } returns iverksattBehandling
        every { behandlingDaoMock.hentBehandling(nyFoerstegangsbehandling.id) } returns nyFoerstegangsbehandling
        every { behandlingDaoMock.avbrytBehandling(nyFoerstegangsbehandling.id) } just runs

        every { hendelseDaoMock.behandlingAvbrutt(any(), any()) } returns Unit

        every { behandlingHendelser.sendMeldingForHendelseStatisitkk(any(), any()) } returns Unit

        every { grunnlagsendringshendelseDaoMock.kobleGrunnlagsendringshendelserFraBehandlingId(any()) } just runs
        every { grunnlagsendringshendelseDaoMock.hentGrunnlagsendringshendelseSomErTattMedIBehandling(any()) } returns emptyList()

        every { oppgaveServiceMock.avbrytOppgaveUnderBehandling(any(), any()) } returns mockk<OppgaveIntern>()

        coEvery { grunnlagKlientMock.hentPersongalleri(any(), any()) } returns mockPersongalleri()

        val saksbehandler = simpleSaksbehandler()
        assertThrows<BehandlingKanIkkeAvbrytesException> {
            behandlingService.avbrytBehandling(avbruttBehandling.id, saksbehandler)
        }

        assertThrows<BehandlingKanIkkeAvbrytesException> {
            behandlingService.avbrytBehandling(iverksattBehandling.id, saksbehandler)
        }

        assertThrows<BehandlingKanIkkeAvbrytesException> {
            behandlingService.avbrytBehandling(attestertBehandling.id, saksbehandler)
        }
        assertDoesNotThrow {
            behandlingService.avbrytBehandling(nyFoerstegangsbehandling.id, saksbehandler)
        }
    }

    @Test
    fun `avbrytBehandling registrerer en avbruddshendelse`() {
        nyKontekstMedBruker(mockSaksbehandler())

        val sakId = 1L
        val nyFoerstegangsbehandling = foerstegangsbehandling(sakId = sakId)

        every { behandlingDaoMock.hentBehandling(nyFoerstegangsbehandling.id) } returns nyFoerstegangsbehandling
        every { behandlingDaoMock.avbrytBehandling(nyFoerstegangsbehandling.id) } just runs
        every { hendelseDaoMock.behandlingAvbrutt(any(), any()) } returns Unit
        every { behandlingHendelser.sendMeldingForHendelseStatisitkk(any(), any()) } returns Unit
        every { grunnlagsendringshendelseDaoMock.kobleGrunnlagsendringshendelserFraBehandlingId(any()) } just runs
        every { grunnlagsendringshendelseDaoMock.hentGrunnlagsendringshendelseSomErTattMedIBehandling(any()) } returns emptyList()
        every { oppgaveServiceMock.avbrytOppgaveUnderBehandling(any(), any()) } returns mockk<OppgaveIntern>()
        coEvery { grunnlagKlientMock.hentPersongalleri(any(), any()) } returns mockPersongalleri()

        behandlingService.avbrytBehandling(nyFoerstegangsbehandling.id, simpleSaksbehandler())

        verify {
            hendelseDaoMock.behandlingAvbrutt(any(), any())
        }
    }

    @Test
    fun `avbrytBehandling ruller tilbake alt ved exception i intransaction`() {
        var didRollback = false
        nyKontekstMedBrukerOgDatabaseContext(
            mockSaksbehandler("ident"),
            object : DatabaseKontekst {
                override fun activeTx(): Connection = throw IllegalArgumentException()

                override fun harIntransaction(): Boolean = throw NotImplementedException("not implemented")

                override fun <T> inTransaction(block: () -> T): T {
                    try {
                        return block()
                    } catch (ex: Throwable) {
                        didRollback = true
                        throw ex
                    }
                }
            },
        )

        val sakId = 1L
        val nyFoerstegangsbehandling = foerstegangsbehandling(sakId = sakId)

        every { behandlingDaoMock.hentBehandling(nyFoerstegangsbehandling.id) } returns nyFoerstegangsbehandling
        every { behandlingDaoMock.avbrytBehandling(nyFoerstegangsbehandling.id) } just runs
        every { hendelseDaoMock.behandlingAvbrutt(any(), any()) } returns Unit
        every { behandlingHendelser.sendMeldingForHendelseStatisitkk(any(), any()) } returns Unit
        every { grunnlagsendringshendelseDaoMock.kobleGrunnlagsendringshendelserFraBehandlingId(any()) } throws
            RuntimeException(
                "Alt må rulles tilbake",
            )
        every { grunnlagsendringshendelseDaoMock.hentGrunnlagsendringshendelseSomErTattMedIBehandling(any()) } returns emptyList()

        every { oppgaveServiceMock.avbrytOppgaveUnderBehandling(any(), any()) } returns mockk<OppgaveIntern>()

        assertFalse(didRollback)
        assertThrows<RuntimeException> {
            inTransaction {
                behandlingService.avbrytBehandling(
                    nyFoerstegangsbehandling.id,
                    simpleSaksbehandler(),
                )
            }
        }

        assertTrue(didRollback)
    }

    @Test
    fun `avbrytBehandling sender en kafka-melding`() {
        nyKontekstMedBruker(mockSaksbehandler())

        val sakId = 1L
        val nyFoerstegangsbehandling = foerstegangsbehandling(sakId = sakId)

        every { behandlingDaoMock.hentBehandling(nyFoerstegangsbehandling.id) } returns nyFoerstegangsbehandling
        every { behandlingDaoMock.avbrytBehandling(nyFoerstegangsbehandling.id) } just runs
        every { hendelseDaoMock.behandlingAvbrutt(any(), any()) } returns Unit
        every {
            behandlingHendelser.sendMeldingForHendelseStatisitkk(
                any(),
                BehandlingHendelseType.AVBRUTT,
            )
        } returns Unit
        every { grunnlagsendringshendelseDaoMock.kobleGrunnlagsendringshendelserFraBehandlingId(any()) } just runs
        every { grunnlagsendringshendelseDaoMock.hentGrunnlagsendringshendelseSomErTattMedIBehandling(any()) } returns emptyList()
        every { oppgaveServiceMock.avbrytOppgaveUnderBehandling(any(), any()) } returns mockk<OppgaveIntern>()
        coEvery { grunnlagKlientMock.hentPersongalleri(any(), any()) } returns mockPersongalleri()

        behandlingService.avbrytBehandling(nyFoerstegangsbehandling.id, simpleSaksbehandler())

        verify {
            behandlingHendelser.sendMeldingForHendelseStatisitkk(
                any(),
                BehandlingHendelseType.AVBRUTT,
            )
        }
    }

    @Test
    fun `avbryt behandling setter koblede grunnlagsendringshendelser tilbake til ingen kobling`() {
        nyKontekstMedBruker(mockSaksbehandler())

        val sakId = 1L
        val nyFoerstegangsbehandling = foerstegangsbehandling(sakId = sakId)

        every { behandlingDaoMock.hentBehandling(nyFoerstegangsbehandling.id) } returns nyFoerstegangsbehandling
        every { behandlingDaoMock.avbrytBehandling(nyFoerstegangsbehandling.id) } just runs
        every { hendelseDaoMock.behandlingAvbrutt(any(), any()) } returns Unit
        every {
            behandlingHendelser.sendMeldingForHendelseStatisitkk(
                any(),
                BehandlingHendelseType.AVBRUTT,
            )
        } returns Unit
        every { grunnlagsendringshendelseDaoMock.kobleGrunnlagsendringshendelserFraBehandlingId(any()) } just runs
        every { grunnlagsendringshendelseDaoMock.hentGrunnlagsendringshendelseSomErTattMedIBehandling(any()) } returns emptyList()
        every { oppgaveServiceMock.avbrytOppgaveUnderBehandling(any(), any()) } returns mockk<OppgaveIntern>()
        coEvery { grunnlagKlientMock.hentPersongalleri(any(), any()) } returns mockPersongalleri()

        behandlingService.avbrytBehandling(nyFoerstegangsbehandling.id, simpleSaksbehandler())
        verify(exactly = 1) {
            grunnlagsendringshendelseDaoMock.kobleGrunnlagsendringshendelserFraBehandlingId(nyFoerstegangsbehandling.id)
        }
    }

    @ParameterizedTest
    @EnumSource(SakType::class, names = ["OMSTILLINGSSTOENAD", "BARNEPENSJON"], mode = EnumSource.Mode.INCLUDE)
    fun `skal feile dersom virkningstidspunkt ikke har satt utlandstilknytning`(sakType: SakType) {
        assertThrows<VirkningstidspunktMaaHaUtenlandstilknytning> {
            sjekkOmVirkningstidspunktErGyldig(
                sakType = sakType,
                utlandstilknytningType = null,
                virkningstidspunkt = Tidspunkt.parse("2015-02-01T00:00:00Z"),
                soeknadMottatt = LocalDateTime.parse("2020-01-01T00:00:00"),
                doedsdato = LocalDate.parse("2014-01-01"),
            )
        }
    }

    @ParameterizedTest
    @EnumSource(SakType::class, names = ["OMSTILLINGSSTOENAD", "BARNEPENSJON"], mode = EnumSource.Mode.INCLUDE)
    fun `skal feile dersom kravdato ikke er med ved bosatt utland`(sakType: SakType) {
        assertThrows<KravdatoMaaFinnesHvisBosattutland> {
            sjekkOmVirkningstidspunktErGyldig(
                sakType = sakType,
                utlandstilknytningType = UtlandstilknytningType.BOSATT_UTLAND,
                virkningstidspunkt = Tidspunkt.parse("2015-02-01T00:00:00Z"),
                soeknadMottatt = LocalDateTime.parse("2020-01-01T00:00:00"),
                doedsdato = LocalDate.parse("2014-01-01"),
            )
        }
    }

    @ParameterizedTest
    @EnumSource(SakType::class, names = ["OMSTILLINGSSTOENAD", "BARNEPENSJON"], mode = EnumSource.Mode.INCLUDE)
    fun `skal legge til grunn kravdato i stedet for soeknadMottattDato ved bosatt utland`(sakType: SakType) {
        val gyldigVirkningstidspunkt =
            sjekkOmVirkningstidspunktErGyldig(
                sakType = sakType,
                utlandstilknytningType = UtlandstilknytningType.BOSATT_UTLAND,
                virkningstidspunkt = Tidspunkt.parse("2015-02-01T00:00:00Z"),
                kravdato = Tidspunkt.parse("2017-02-01T00:00:00Z"),
                // brukes denne vil ikke virk være innenfor 3 år
                soeknadMottatt = LocalDateTime.parse("2020-01-01T00:00:00"),
                doedsdato = LocalDate.parse("2014-01-01"),
            )

        gyldigVirkningstidspunkt shouldBe true
    }

    @ParameterizedTest
    @EnumSource(SakType::class, names = ["OMSTILLINGSSTOENAD", "BARNEPENSJON"], mode = EnumSource.Mode.INCLUDE)
    fun `skal gi gyldig virkningstidspunkt hvis tidspunkt er en maaned etter doedsfall`(sakType: SakType) {
        val gyldigVirkningstidspunkt =
            sjekkOmVirkningstidspunktErGyldig(
                sakType = sakType,
                virkningstidspunkt = Tidspunkt.parse("2020-02-01T00:00:00Z"),
                soeknadMottatt = LocalDateTime.parse("2020-02-01T00:00:00"),
                doedsdato = LocalDate.parse("2020-01-01"),
            )

        gyldigVirkningstidspunkt shouldBe true
    }

    @ParameterizedTest
    @EnumSource(SakType::class, names = ["OMSTILLINGSSTOENAD", "BARNEPENSJON"], mode = EnumSource.Mode.INCLUDE)
    fun `skal gi gyldig virkningstidspunkt dersom doedsdato mangler`(sakType: SakType) {
        val gyldigVirkningstidspunkt =
            sjekkOmVirkningstidspunktErGyldig(
                sakType = sakType,
                virkningstidspunkt = Tidspunkt.parse("2020-02-01T00:00:00Z"),
                soeknadMottatt = LocalDateTime.parse("2020-02-01T00:00:00"),
                doedsdato = null,
            )

        gyldigVirkningstidspunkt shouldBe true
    }

    @ParameterizedTest
    @EnumSource(SakType::class, names = ["OMSTILLINGSSTOENAD", "BARNEPENSJON"], mode = EnumSource.Mode.INCLUDE)
    fun `skal gi ugyldig virkningstidspunkt hvis tidspunkt er foer en maaned etter doedsfall`(sakType: SakType) {
        val gyldigVirkningstidspunkt =
            sjekkOmVirkningstidspunktErGyldig(
                sakType = sakType,
                virkningstidspunkt = Tidspunkt.parse("2020-01-01T00:00:00Z"),
                soeknadMottatt = LocalDateTime.parse("2020-02-01T00:00:00"),
                doedsdato = LocalDate.parse("2020-01-01"),
            )

        gyldigVirkningstidspunkt shouldBe false
    }

    @Test
    fun `skal gi gyldig virkningstidspunkt hvis tidspunkt er inntil tre aar foer mottatt soeknad for barnepensjon`() {
        val gyldigVirkningstidspunkt =
            sjekkOmVirkningstidspunktErGyldig(
                sakType = SakType.BARNEPENSJON,
                virkningstidspunkt = Tidspunkt.parse("2017-01-01T00:00:00Z"),
                soeknadMottatt = LocalDateTime.parse("2020-01-15T00:00:00"),
                doedsdato = LocalDate.parse("2016-11-30"),
            )

        gyldigVirkningstidspunkt shouldBe true
    }

    @Test
    fun `skal gi gyldig virkningstidspunkt hvis tidspunkt er under tre aar foer mottatt soeknad for omstillingsstoenad`() {
        val gyldigVirkningstidspunkt =
            sjekkOmVirkningstidspunktErGyldig(
                sakType = SakType.OMSTILLINGSSTOENAD,
                virkningstidspunkt = Tidspunkt.parse("2017-02-01T00:00:00Z"),
                soeknadMottatt = LocalDateTime.parse("2020-01-15T00:00:00"),
                doedsdato = LocalDate.parse("2016-11-30"),
            )

        gyldigVirkningstidspunkt shouldBe true
    }

    @Test
    fun `skal gi ugyldig virkningstidspunkt hvis tidspunkt er over tre aar foer mottatt soeknad for barnepensjon`() {
        val gyldigVirkningstidspunkt =
            sjekkOmVirkningstidspunktErGyldig(
                sakType = SakType.BARNEPENSJON,
                virkningstidspunkt = Tidspunkt.parse("2016-12-01T00:00:00Z"),
                soeknadMottatt = LocalDateTime.parse("2020-01-15T00:00:00"),
                doedsdato = LocalDate.parse("2016-11-30"),
            )

        gyldigVirkningstidspunkt shouldBe false
    }

    @Test
    fun `skal gi ugyldig virkningstidspunkt hvis tidspunkt er tre aar foer mottatt soeknad for omstillingsstoenad`() {
        val gyldigVirkningstidspunkt =
            sjekkOmVirkningstidspunktErGyldig(
                sakType = SakType.OMSTILLINGSSTOENAD,
                virkningstidspunkt = Tidspunkt.parse("2016-01-01T00:00:00Z"),
                soeknadMottatt = LocalDateTime.parse("2020-01-15T00:00:00"),
                doedsdato = LocalDate.parse("2016-11-30"),
            )

        gyldigVirkningstidspunkt shouldBe false
    }

    @Test
    fun `Migreringer og gjenopprettinger skal ikke validere på mottatt soeknad tidspunkt fordi det ikke noen mottatt soeknad`() {
        val gyldigVirkningstidspunkt =
            sjekkOmVirkningstidspunktErGyldig(
                sakType = SakType.OMSTILLINGSSTOENAD,
                virkningstidspunkt = Tidspunkt.parse("2016-01-01T00:00:00Z"),
                soeknadMottatt = LocalDateTime.parse("2020-01-15T00:00:00"),
                doedsdato = LocalDate.parse("2016-11-30"),
                kilde = Vedtaksloesning.PESYS,
            )

        gyldigVirkningstidspunkt shouldBe true
    }

    @ParameterizedTest
    @EnumSource(SakType::class, names = ["OMSTILLINGSSTOENAD", "BARNEPENSJON"], mode = EnumSource.Mode.INCLUDE)
    fun `skal gi gyldig virkningstidspunkt for revurdering hvis virkningstidspunkt er paa foerste virk`(sakType: SakType) {
        val gyldigVirkningstidspunkt =
            sjekkOmVirkningstidspunktErGyldig(
                sakType = sakType,
                behandlingType = BehandlingType.REVURDERING,
                virkningstidspunkt = Tidspunkt.parse("2024-01-01T00:00:00Z"),
                foersteVirk = YearMonth.of(2024, Month.JANUARY),
            )

        gyldigVirkningstidspunkt shouldBe true
    }

    @ParameterizedTest
    @EnumSource(SakType::class, names = ["OMSTILLINGSSTOENAD", "BARNEPENSJON"], mode = EnumSource.Mode.INCLUDE)
    fun `skal gi gyldig virkningstidspunkt for revurdering hvis virkningstidspunkt er etter foerste virk`(sakType: SakType) {
        val gyldigVirkningstidspunkt =
            sjekkOmVirkningstidspunktErGyldig(
                sakType = sakType,
                behandlingType = BehandlingType.REVURDERING,
                virkningstidspunkt = Tidspunkt.parse("2024-02-01T00:00:00Z"),
                foersteVirk = YearMonth.of(2024, Month.JANUARY),
            )

        gyldigVirkningstidspunkt shouldBe true
    }

    @ParameterizedTest
    @EnumSource(SakType::class, names = ["OMSTILLINGSSTOENAD", "BARNEPENSJON"], mode = EnumSource.Mode.INCLUDE)
    fun `skal gi ugyldig virkningstidspunkt for revurdering hvis virkningstidspunkt er foer foerste virk`(sakType: SakType) {
        val gyldigVirkningstidspunkt =
            sjekkOmVirkningstidspunktErGyldig(
                sakType = sakType,
                behandlingType = BehandlingType.REVURDERING,
                virkningstidspunkt = Tidspunkt.parse("2023-12-01T00:00:00Z"),
                foersteVirk = YearMonth.of(2024, Month.JANUARY),
            )

        gyldigVirkningstidspunkt shouldBe false
    }

    private fun sjekkOmVirkningstidspunktErGyldig(
        sakType: SakType,
        behandlingType: BehandlingType = BehandlingType.FØRSTEGANGSBEHANDLING,
        utlandstilknytningType: UtlandstilknytningType? = UtlandstilknytningType.NASJONAL,
        virkningstidspunkt: Tidspunkt = Tidspunkt.parse("2016-01-01T00:00:00Z"),
        begrunnelse: String = "en begrunnelse",
        soeknadMottatt: LocalDateTime = LocalDateTime.parse("2020-01-15T00:00:00"),
        doedsdato: LocalDate? = LocalDate.parse("2016-11-30"),
        kravdato: Tidspunkt? = null,
        foersteVirk: YearMonth? = null,
        kilde: Vedtaksloesning = Vedtaksloesning.GJENNY,
    ): Boolean {
        initFellesMocks(
            sakType = sakType,
            behandlingType = behandlingType,
            doedsdato = doedsdato,
            soeknadMottatt = soeknadMottatt,
            foersteVirk = foersteVirk,
            utlandstilknytning =
                utlandstilknytningType?.let {
                    Utlandstilknytning(
                        utlandstilknytningType,
                        Grunnlagsopplysning.Saksbehandler.create("ident"),
                        "begrunnelse",
                    )
                },
            kilde = kilde,
        )

        val request = VirkningstidspunktRequest(virkningstidspunkt.toString(), begrunnelse, kravdato?.toLocalDate())

        return runBlocking {
            behandlingService.erGyldigVirkningstidspunkt(BEHANDLINGS_ID, TOKEN, request)
        }
    }

    @Test
    fun `hentSenestIverksatteBehandling() returnerer seneste iverksatte behandlingen`() {
        nyKontekstMedBruker(mockSaksbehandler())

        val behandling1 = foerstegangsbehandling(sakId = 1, status = BehandlingStatus.IVERKSATT)
        val behandling2 =
            revurdering(
                sakId = 1,
                status = BehandlingStatus.BEREGNET,
                revurderingAarsak = Revurderingaarsak.REGULERING,
            )

        every { behandlingDaoMock.hentBehandlingerForSak(any()) } returns listOf(behandling1, behandling2)

        assertEquals(behandling1, behandlingService.hentSisteIverksatte(1))
    }

    @Test
    fun `skal hente behandlinger i sak hvor sak har enhet og brukeren har enhet`() {
        nyKontekstMedBruker(mockSaksbehandler(enheter = listOf(Enheter.PORSGRUNN.enhetNr)))
        every { behandlingDaoMock.hentBehandlingerForSak(1) } returns
            listOf(
                revurdering(
                    sakId = 1,
                    revurderingAarsak = Revurderingaarsak.REGULERING,
                    enhet = Enheter.PORSGRUNN.enhetNr,
                ),
                foerstegangsbehandling(sakId = 1, enhet = Enheter.PORSGRUNN.enhetNr),
            )

        val behandlinger = behandlingService.hentBehandlingerForSak(1)

        assertAll(
            "skal hente behandlinger",
            { assertEquals(2, behandlinger.size) },
            { assertEquals(1, behandlinger.filterIsInstance<Foerstegangsbehandling>().size) },
            { assertEquals(1, behandlinger.filterIsInstance<Revurdering>().size) },
        )
    }

    @Test
    fun `kan oppdatere bodd eller arbeidet i utlandet`() {
        nyKontekstMedBruker(mockSaksbehandler(enheter = listOf(Enheter.PORSGRUNN.enhetNr)))

        val uuid = UUID.randomUUID()

        val slot = slot<BoddEllerArbeidetUtlandet>()

        every { behandlingDaoMock.hentBehandling(any()) } returns
            foerstegangsbehandling(
                id = uuid,
                sakId = 1,
                enhet = Enheter.PORSGRUNN.enhetNr,
            )

        every { behandlingDaoMock.lagreBoddEllerArbeidetUtlandet(any(), capture(slot)) } just runs
        every { behandlingDaoMock.lagreStatus(any()) } just runs

        inTransaction {
            behandlingService.oppdaterBoddEllerArbeidetUtlandet(
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

    @Test
    fun `hentSakMedBehandlinger - flere saker prioriteres korrekt`() {
        nyKontekstMedBruker(mockSaksbehandler())

        val sak1 = Sak("fnr", SakType.BARNEPENSJON, id = Random.nextLong(), "4808")
        val sak2 = Sak("fnr", SakType.OMSTILLINGSSTOENAD, id = Random.nextLong(), "4808")

        every { behandlingDaoMock.hentBehandlingerForSak(sak1.id) } returns
            listOf(
                foerstegangsbehandling(sakId = sak1.id, status = BehandlingStatus.AVBRUTT),
                foerstegangsbehandling(sakId = sak1.id, status = BehandlingStatus.AVBRUTT),
            )
        every { behandlingDaoMock.hentBehandlingerForSak(sak2.id) } returns
            listOf(
                foerstegangsbehandling(sakId = sak2.id, status = BehandlingStatus.IVERKSATT),
            )

        val sakMedBehandlinger = behandlingService.hentSakMedBehandlinger(listOf(sak1, sak2))

        assertEquals(sak2.id, sakMedBehandlinger.sak.id)
        assertEquals(1, sakMedBehandlinger.behandlinger.size)

        verify(exactly = 1) {
            behandlingDaoMock.hentBehandlingerForSak(sak1.id)
            behandlingDaoMock.hentBehandlingerForSak(sak2.id)
        }
    }

    @Test
    fun `hentSakMedBehandlinger - kun én sak`() {
        nyKontekstMedBruker(mockSaksbehandler())

        val sak = Sak("fnr", SakType.OMSTILLINGSSTOENAD, id = Random.nextLong(), "4808")

        every { behandlingDaoMock.hentBehandlingerForSak(sak.id) } returns
            listOf(
                foerstegangsbehandling(
                    sakId = sak.id,
                    status = BehandlingStatus.IVERKSATT,
                ),
            )

        val sakMedBehandlinger = behandlingService.hentSakMedBehandlinger(listOf(sak))

        assertEquals(sak.id, sakMedBehandlinger.sak.id)
        assertEquals(1, sakMedBehandlinger.behandlinger.size)

        verify(exactly = 1) { behandlingDaoMock.hentBehandlingerForSak(sak.id) }
    }

    @Test
    fun `Kan kun endre send brev for revurdering`() {
        nyKontekstMedBruker(mockSaksbehandler())
        val behandlingId = UUID.randomUUID()
        every { behandlingDaoMock.lagreSendeBrev(behandlingId, true) } just runs
        every {
            behandlingDaoMock.hentBehandling(behandlingId)
        } returns revurdering(sakId = 1L, revurderingAarsak = Revurderingaarsak.INNTEKTSENDRING)
        behandlingService.endreSkalSendeBrev(behandlingId, true)
        verify(exactly = 1) { behandlingDaoMock.lagreSendeBrev(behandlingId, true) }
    }

    @Test
    fun `Kan ikke endre send brev førstegangsbehandling revurdering`() {
        nyKontekstMedBruker(mockSaksbehandler())
        val behandlingId = UUID.randomUUID()
        every { behandlingDaoMock.lagreSendeBrev(behandlingId, true) } just runs
        every { behandlingDaoMock.hentBehandling(behandlingId) } returns foerstegangsbehandling(sakId = 1L)
        assertThrows<KanIkkeEndreSendeBrevForFoerstegangsbehandling> {
            behandlingService.endreSkalSendeBrev(behandlingId, true)
        }

        verify(exactly = 0) { behandlingDaoMock.lagreSendeBrev(behandlingId, true) }
    }

    private fun initFellesMocks(
        sakType: SakType = SakType.BARNEPENSJON,
        behandlingType: BehandlingType = BehandlingType.FØRSTEGANGSBEHANDLING,
        doedsdato: LocalDate?,
        soeknadMottatt: LocalDateTime,
        foersteVirk: YearMonth?,
        utlandstilknytning: Utlandstilknytning? = null,
        kilde: Vedtaksloesning = Vedtaksloesning.GJENNY,
    ) {
        nyKontekstMedBruker(mockSaksbehandler())

        val (behandling, tidligereBehandlinger) =
            when (behandlingType) {
                BehandlingType.FØRSTEGANGSBEHANDLING ->
                    Pair(
                        foerstegangsbehandling(
                            id = BEHANDLINGS_ID,
                            sakId = SAK_ID,
                            sakType = sakType,
                            soeknadMottattDato = soeknadMottatt,
                            utlandstilknytning = utlandstilknytning,
                            kilde = kilde,
                        ),
                        emptyList(),
                    )

                BehandlingType.REVURDERING ->
                    Pair(
                        revurdering(
                            id = BEHANDLINGS_ID,
                            sakId = SAK_ID,
                            revurderingAarsak = Revurderingaarsak.ANNEN,
                            utlandstilknytning = utlandstilknytning,
                        ),
                        listOf(
                            foerstegangsbehandling(
                                id = BEHANDLINGS_ID,
                                sakId = SAK_ID,
                                sakType = sakType,
                                status = BehandlingStatus.IVERKSATT,
                                virkningstidspunkt =
                                    Virkningstidspunkt.create(
                                        dato = foersteVirk!!,
                                        begrunnelse = "begrunnelse",
                                        saksbehandler = Grunnlagsopplysning.Saksbehandler.create("Z123456"),
                                    ),
                                soeknadMottattDato = soeknadMottatt,
                                utlandstilknytning = utlandstilknytning,
                            ),
                        ),
                    )
            }

        val personopplysning = personOpplysning(doedsdato = doedsdato)
        val grunnlagsopplysningMedPersonopplysning = grunnlagsOpplysningMedPersonopplysning(personopplysning)

        coEvery {
            grunnlagKlientMock.finnPersonOpplysning(behandling.id, Opplysningstype.AVDOED_PDL_V1, TOKEN)
        } returns grunnlagsopplysningMedPersonopplysning
        coEvery { grunnlagKlientMock.hentPersongalleri(behandling.id, any()) } answers { callOriginal() }

        every { behandlingDaoMock.hentBehandling(BEHANDLINGS_ID) } returns behandling
        every { behandlingDaoMock.hentBehandlingerForSak(any()) } returns tidligereBehandlinger
    }

    private fun mockPersongalleri() =
        Grunnlagsopplysning(
            id = UUID.randomUUID(),
            kilde = Grunnlagsopplysning.Privatperson("fnr", Tidspunkt.now()),
            meta = emptyMap<String, String>().toObjectNode(),
            opplysningType = Opplysningstype.PERSONGALLERI_V1,
            opplysning =
                Persongalleri(
                    "soeker",
                    "innsender",
                    listOf("soesken"),
                    listOf("avdoed"),
                    listOf("gjenlevende"),
                ),
        )

    companion object {
        const val SAK_ID = 1L
        val BEHANDLINGS_ID: UUID = UUID.randomUUID()
        val TOKEN = simpleSaksbehandler()
    }
}
