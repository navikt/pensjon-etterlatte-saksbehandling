package no.nav.etterlatte.behandling

import com.fasterxml.jackson.databind.JsonNode
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.coVerify
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
import no.nav.etterlatte.common.Enheter
import no.nav.etterlatte.foerstegangsbehandling
import no.nav.etterlatte.grunnlag.GenerellKilde
import no.nav.etterlatte.grunnlag.GrunnlagService
import no.nav.etterlatte.grunnlag.Personopplysning
import no.nav.etterlatte.grunnlag.PersonopplysningerResponse
import no.nav.etterlatte.grunnlagsendring.GrunnlagsendringshendelseDao
import no.nav.etterlatte.inTransaction
import no.nav.etterlatte.ktor.token.simpleSaksbehandler
import no.nav.etterlatte.libs.common.Vedtaksloesning
import no.nav.etterlatte.libs.common.behandling.AarsakTilAvbrytelse
import no.nav.etterlatte.libs.common.behandling.AnnenForelder
import no.nav.etterlatte.libs.common.behandling.BehandlingHendelseType
import no.nav.etterlatte.libs.common.behandling.BehandlingStatus
import no.nav.etterlatte.libs.common.behandling.BehandlingType
import no.nav.etterlatte.libs.common.behandling.BoddEllerArbeidetUtlandet
import no.nav.etterlatte.libs.common.behandling.Persongalleri
import no.nav.etterlatte.libs.common.behandling.Revurderingaarsak
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.behandling.TidligereFamiliepleier
import no.nav.etterlatte.libs.common.behandling.Utlandstilknytning
import no.nav.etterlatte.libs.common.behandling.UtlandstilknytningType
import no.nav.etterlatte.libs.common.behandling.Virkningstidspunkt
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstype
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.oppgave.OppgaveIntern
import no.nav.etterlatte.libs.common.oppgave.OppgaveKilde
import no.nav.etterlatte.libs.common.oppgave.OppgaveType
import no.nav.etterlatte.libs.common.sak.Sak
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.toJson
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
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.EnumSource
import org.junit.jupiter.params.provider.MethodSource
import org.testcontainers.shaded.org.apache.commons.lang3.NotImplementedException
import java.sql.Connection
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.Month
import java.time.YearMonth
import java.util.UUID

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class BehandlingServiceImplTest {
    private val behandlingDaoMock = mockk<BehandlingDao>()
    private val behandlingHendelser = mockk<BehandlingHendelserKafkaProducer>()
    private val grunnlagsendringshendelseDaoMock = mockk<GrunnlagsendringshendelseDao>()
    private val hendelseDaoMock = mockk<HendelseDao>()
    private val grunnlagService = mockk<GrunnlagService>()
    private val oppgaveServiceMock = mockk<OppgaveService>()

    private val behandlingService =
        BehandlingServiceImpl(
            behandlingDao = behandlingDaoMock,
            behandlingHendelser = behandlingHendelser,
            grunnlagsendringshendelseDao = grunnlagsendringshendelseDaoMock,
            hendelseDao = hendelseDaoMock,
            kommerBarnetTilGodeDao = mockk(),
            oppgaveService = oppgaveServiceMock,
            grunnlagService = grunnlagService,
            beregningKlient = mockk(),
            etteroppgjoerTempService = mockk(),
        )

    @AfterEach
    fun after() {
        clearAllMocks()
    }

    @Test
    fun `Kan hente egne ansatte behandlínger som egen ansatt saksbehandler`() {
        nyKontekstMedBruker(mockSaksbehandler(harRolleEgenAnsatt = true))

        every { behandlingDaoMock.hentBehandlingerForSak(sakId1) } returns
            listOf(
                revurdering(
                    sakId = sakId1,
                    revurderingAarsak = Revurderingaarsak.REGULERING,
                    enhet = Enheter.EGNE_ANSATTE.enhetNr,
                ),
                foerstegangsbehandling(sakId = sakId1, enhet = Enheter.EGNE_ANSATTE.enhetNr),
            )

        val behandlinger = behandlingService.hentBehandlingerForSak(sakId1)

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

        every { behandlingDaoMock.hentBehandlingerForSak(sakId1) } returns
            listOf(
                revurdering(
                    sakId = sakId1,
                    revurderingAarsak = Revurderingaarsak.REGULERING,
                    enhet = Enheter.STRENGT_FORTROLIG.enhetNr,
                ),
                foerstegangsbehandling(sakId = sakId1, enhet = Enheter.STRENGT_FORTROLIG.enhetNr),
            )

        val behandlinger = behandlingService.hentBehandlingerForSak(sakId1)

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

        every { behandlingDaoMock.hentBehandlingerForSak(sakId1) } returns
            listOf(
                revurdering(
                    sakId = sakId1,
                    revurderingAarsak = Revurderingaarsak.REGULERING,
                    enhet = Enheter.STRENGT_FORTROLIG.enhetNr,
                ),
                foerstegangsbehandling(sakId = sakId1, enhet = Enheter.STRENGT_FORTROLIG.enhetNr),
            )

        val behandlinger = behandlingService.hentBehandlingerForSak(sakId1)

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

        every { behandlingDaoMock.hentBehandlingerForSak(sakId1) } returns
            listOf(
                revurdering(
                    sakId = sakId1,
                    revurderingAarsak = Revurderingaarsak.REGULERING,
                    enhet = Enheter.EGNE_ANSATTE.enhetNr,
                ),
                foerstegangsbehandling(sakId = sakId1, enhet = Enheter.EGNE_ANSATTE.enhetNr),
            )

        val behandlinger = behandlingService.hentBehandlingerForSak(sakId1)

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

        every { behandlingDaoMock.hentBehandlingerForSak(sakId1) } returns
            listOf(
                revurdering(sakId = sakId1, revurderingAarsak = Revurderingaarsak.REGULERING),
                foerstegangsbehandling(sakId = sakId1),
            )

        val behandlinger = behandlingService.hentBehandlingerForSak(sakId1)

        assertAll(
            "skal hente behandlinger",
            { assertEquals(2, behandlinger.size) },
            { assertEquals(1, behandlinger.filterIsInstance<Foerstegangsbehandling>().size) },
            { assertEquals(1, behandlinger.filterIsInstance<Revurdering>().size) },
        )
    }

    @Test
    fun `avbrytBehangling oppretter oppgave hvis omgjoeringsoppgaveForKlage eksisterer for revurdering `() {
        nyKontekstMedBruker(mockSaksbehandler())

        val relatertBehandlingsId = UUID.randomUUID()
        val revurderingbehandling =
            revurdering(
                sakId = sakId1,
                relatertBehandlingId = relatertBehandlingsId.toString(),
                revurderingAarsak = Revurderingaarsak.OMGJOERING_ETTER_KLAGE,
            )

        val oppgaveKlage = mockOppgaveIntern(relatertBehandlingsId)

        every { behandlingDaoMock.hentBehandling(revurderingbehandling.id) } returns revurderingbehandling
        every { behandlingDaoMock.avbrytBehandling(revurderingbehandling.id, any(), any()) } just runs
        every { hendelseDaoMock.behandlingAvbrutt(any(), any(), any(), any()) } returns Unit
        every {
            behandlingHendelser.sendMeldingForHendelseStatistikk(
                any(),
                BehandlingHendelseType.AVBRUTT,
            )
        } returns Unit

        every { grunnlagsendringshendelseDaoMock.kobleGrunnlagsendringshendelserFraBehandlingId(any()) } just runs
        every { grunnlagsendringshendelseDaoMock.hentGrunnlagsendringshendelseSomErTattMedIBehandling(any()) } returns emptyList()
        every { oppgaveServiceMock.avbrytAapneOppgaverMedReferanse(any(), any()) } just runs
        every { oppgaveServiceMock.hentOppgaverForSak(any(), any()) } returns listOf(oppgaveKlage)
        every { oppgaveServiceMock.opprettOppgave(any(), any(), any(), any(), any(), any(), any()) } returns oppgaveKlage

        every { grunnlagService.hentPersongalleri(any<UUID>()) } returns mockPersongalleri()

        behandlingService.avbrytBehandling(revurderingbehandling.id, simpleSaksbehandler(), AarsakTilAvbrytelse.ANNET, "")

        verify {
            oppgaveServiceMock.opprettOppgave(
                referanse = relatertBehandlingsId.toString(),
                sakId = sakId1,
                kilde = any(),
                type = any(),
                merknad = any(),
                frist = any(),
            )
        }
    }

    @Test
    fun `avbrytBehangling oppretter oppgave hvis omgjoeringsoppgaveForKlage eksisterer for førstegangsbehandling `() {
        nyKontekstMedBruker(mockSaksbehandler())

        val relatertBehandlingsId = UUID.randomUUID()
        val nyFoerstegangsbehandling = foerstegangsbehandling(sakId = sakId1, relatertBehandlingId = relatertBehandlingsId.toString())

        val oppgaveKlage = mockOppgaveIntern(relatertBehandlingsId)

        every { behandlingDaoMock.hentBehandling(nyFoerstegangsbehandling.id) } returns nyFoerstegangsbehandling
        every { behandlingDaoMock.avbrytBehandling(nyFoerstegangsbehandling.id, any(), any()) } just runs
        every { hendelseDaoMock.behandlingAvbrutt(any(), any(), any(), any()) } returns Unit
        every {
            behandlingHendelser.sendMeldingForHendelseStatistikk(
                any(),
                BehandlingHendelseType.AVBRUTT,
            )
        } returns Unit

        every { grunnlagsendringshendelseDaoMock.kobleGrunnlagsendringshendelserFraBehandlingId(any()) } just runs
        every { grunnlagsendringshendelseDaoMock.hentGrunnlagsendringshendelseSomErTattMedIBehandling(any()) } returns emptyList()
        every { oppgaveServiceMock.avbrytAapneOppgaverMedReferanse(any(), any()) } just runs
        every { oppgaveServiceMock.hentOppgaverForSak(any(), any()) } returns listOf(oppgaveKlage)
        every { oppgaveServiceMock.opprettOppgave(any(), any(), any(), any(), any(), any(), any()) } returns oppgaveKlage
        every { grunnlagService.hentPersongalleri(any<UUID>()) } returns mockPersongalleri()

        behandlingService.avbrytBehandling(nyFoerstegangsbehandling.id, simpleSaksbehandler(), AarsakTilAvbrytelse.ANNET, "")

        verify {
            oppgaveServiceMock.opprettOppgave(
                referanse = relatertBehandlingsId.toString(),
                sakId = sakId1,
                kilde = any(),
                type = any(),
                merknad = any(),
                frist = any(),
            )
        }
    }

    @Test
    fun `avbrytBehandling sjekker om behandlingsstatusen er gyldig for avbrudd`() {
        nyKontekstMedBruker(mockSaksbehandler())

        val avbruttBehandling = foerstegangsbehandling(sakId = sakId1, status = BehandlingStatus.AVBRUTT)
        val attestertBehandling = foerstegangsbehandling(sakId = sakId1, status = BehandlingStatus.ATTESTERT)
        val iverksattBehandling = foerstegangsbehandling(sakId = sakId1, status = BehandlingStatus.IVERKSATT)
        val nyFoerstegangsbehandling = foerstegangsbehandling(sakId = sakId1)

        every { behandlingDaoMock.hentBehandling(avbruttBehandling.id) } returns avbruttBehandling
        every { behandlingDaoMock.hentBehandling(attestertBehandling.id) } returns attestertBehandling
        every { behandlingDaoMock.hentBehandling(iverksattBehandling.id) } returns iverksattBehandling
        every { behandlingDaoMock.hentBehandling(nyFoerstegangsbehandling.id) } returns nyFoerstegangsbehandling
        every { behandlingDaoMock.avbrytBehandling(nyFoerstegangsbehandling.id, AarsakTilAvbrytelse.ANNET, "") } just runs

        every { hendelseDaoMock.behandlingAvbrutt(any(), any(), any(), any()) } returns Unit

        every { behandlingHendelser.sendMeldingForHendelseStatistikk(any(), any()) } returns Unit

        every { grunnlagsendringshendelseDaoMock.kobleGrunnlagsendringshendelserFraBehandlingId(any()) } just runs
        every { grunnlagsendringshendelseDaoMock.hentGrunnlagsendringshendelseSomErTattMedIBehandling(any()) } returns emptyList()

        every { oppgaveServiceMock.avbrytAapneOppgaverMedReferanse(any(), any()) } just runs

        every { grunnlagService.hentPersongalleri(any<UUID>()) } returns mockPersongalleri()

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
            behandlingService.avbrytBehandling(nyFoerstegangsbehandling.id, saksbehandler, AarsakTilAvbrytelse.ANNET, "")
        }
    }

    @Test
    fun `avbrytBehandling registrerer en avbruddshendelse`() {
        nyKontekstMedBruker(mockSaksbehandler())

        val nyFoerstegangsbehandling = foerstegangsbehandling(sakId = sakId1)

        every { behandlingDaoMock.hentBehandling(nyFoerstegangsbehandling.id) } returns nyFoerstegangsbehandling
        every { behandlingDaoMock.avbrytBehandling(nyFoerstegangsbehandling.id, AarsakTilAvbrytelse.ANNET, "test") } just runs
        every { hendelseDaoMock.behandlingAvbrutt(any(), any(), any(), any()) } returns Unit
        every { behandlingHendelser.sendMeldingForHendelseStatistikk(any(), any()) } returns Unit
        every { grunnlagsendringshendelseDaoMock.kobleGrunnlagsendringshendelserFraBehandlingId(any()) } just runs
        every { grunnlagsendringshendelseDaoMock.hentGrunnlagsendringshendelseSomErTattMedIBehandling(any()) } returns emptyList()
        every { oppgaveServiceMock.avbrytAapneOppgaverMedReferanse(any(), any()) } just runs
        every { grunnlagService.hentPersongalleri(any<UUID>()) } returns mockPersongalleri()

        behandlingService.avbrytBehandling(nyFoerstegangsbehandling.id, simpleSaksbehandler(), AarsakTilAvbrytelse.ANNET, "test")

        verify {
            hendelseDaoMock.behandlingAvbrutt(any(), any(), any(), any())
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

        val nyFoerstegangsbehandling = foerstegangsbehandling(sakId = sakId1)

        every { behandlingDaoMock.hentBehandling(nyFoerstegangsbehandling.id) } returns nyFoerstegangsbehandling
        every { behandlingDaoMock.avbrytBehandling(nyFoerstegangsbehandling.id, any(), any()) } just runs
        every { hendelseDaoMock.behandlingAvbrutt(any(), any()) } returns Unit
        every { behandlingHendelser.sendMeldingForHendelseStatistikk(any(), any()) } returns Unit
        every { grunnlagsendringshendelseDaoMock.kobleGrunnlagsendringshendelserFraBehandlingId(any()) } throws
            RuntimeException(
                "Alt må rulles tilbake",
            )
        every { grunnlagsendringshendelseDaoMock.hentGrunnlagsendringshendelseSomErTattMedIBehandling(any()) } returns emptyList()
        every { oppgaveServiceMock.avbrytAapneOppgaverMedReferanse(any(), any()) } just runs

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

        val nyFoerstegangsbehandling = foerstegangsbehandling(sakId = sakId1)

        every { behandlingDaoMock.hentBehandling(nyFoerstegangsbehandling.id) } returns nyFoerstegangsbehandling
        every { behandlingDaoMock.avbrytBehandling(nyFoerstegangsbehandling.id, any(), any()) } just runs
        every { hendelseDaoMock.behandlingAvbrutt(any(), any(), any(), any()) } returns Unit
        every {
            behandlingHendelser.sendMeldingForHendelseStatistikk(
                any(),
                BehandlingHendelseType.AVBRUTT,
            )
        } returns Unit
        every { grunnlagsendringshendelseDaoMock.kobleGrunnlagsendringshendelserFraBehandlingId(any()) } just runs
        every { grunnlagsendringshendelseDaoMock.hentGrunnlagsendringshendelseSomErTattMedIBehandling(any()) } returns emptyList()
        every { oppgaveServiceMock.avbrytAapneOppgaverMedReferanse(any(), any()) } just runs
        every { grunnlagService.hentPersongalleri(any<UUID>()) } returns mockPersongalleri()

        behandlingService.avbrytBehandling(nyFoerstegangsbehandling.id, simpleSaksbehandler(), AarsakTilAvbrytelse.ANNET, "")

        verify {
            behandlingHendelser.sendMeldingForHendelseStatistikk(
                any(),
                BehandlingHendelseType.AVBRUTT,
            )
        }
    }

    @Test
    fun `avbryt behandling setter koblede grunnlagsendringshendelser tilbake til ingen kobling`() {
        nyKontekstMedBruker(mockSaksbehandler())

        val nyFoerstegangsbehandling = foerstegangsbehandling(sakId = sakId1)

        every { behandlingDaoMock.hentBehandling(nyFoerstegangsbehandling.id) } returns nyFoerstegangsbehandling
        every { behandlingDaoMock.avbrytBehandling(nyFoerstegangsbehandling.id, any(), any()) } just runs
        every { hendelseDaoMock.behandlingAvbrutt(any(), any(), any(), any()) } returns Unit
        every {
            behandlingHendelser.sendMeldingForHendelseStatistikk(
                any(),
                BehandlingHendelseType.AVBRUTT,
            )
        } returns Unit
        every { grunnlagsendringshendelseDaoMock.kobleGrunnlagsendringshendelserFraBehandlingId(any()) } just runs
        every { grunnlagsendringshendelseDaoMock.hentGrunnlagsendringshendelseSomErTattMedIBehandling(any()) } returns emptyList()
        every { oppgaveServiceMock.avbrytAapneOppgaverMedReferanse(any(), any()) } just runs
        every { grunnlagService.hentPersongalleri(any<UUID>()) } returns mockPersongalleri()

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
                doedsdato = listOf(LocalDate.parse("2014-01-01")),
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
                doedsdato = listOf(LocalDate.parse("2014-01-01")),
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
                doedsdato = listOf(LocalDate.parse("2014-01-01")),
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
                doedsdato = listOf(LocalDate.parse("2020-01-01")),
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
                doedsdato = emptyList(),
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
                doedsdato = listOf(LocalDate.parse("2020-01-01")),
            )

        gyldigVirkningstidspunkt shouldBe false
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("flereAvdoedeVirkningstidspunktTestdata")
    fun `skal gi gyldig virkningstidspunkt med to avdoede hvis tidspunkt er foer siste doedsfall`(
        beskrivelse: String,
        virkningstidspunkt: Tidspunkt,
        doedsdatoer: List<LocalDate>,
        forventetGyldig: Boolean,
    ) {
        val gyldigVirkningstidspunkt =
            sjekkOmVirkningstidspunktErGyldig(
                virkningstidspunkt = virkningstidspunkt,
                doedsdato = doedsdatoer,
                sakType = SakType.BARNEPENSJON,
                soeknadMottatt = LocalDateTime.parse("2020-03-01T00:00:00"),
            )

        gyldigVirkningstidspunkt shouldBe forventetGyldig
    }

    /**
     * Gitt virk og dødsdatoer, er virk gyldig eller ikke?
     */
    private fun flereAvdoedeVirkningstidspunktTestdata() =
        listOf(
            Arguments.of(
                "Virk er før første dødsdato og er ugyldig",
                Tidspunkt.parse("2019-08-01T00:00:00Z"),
                listOf(LocalDate.parse("2019-10-15"), LocalDate.parse("2020-01-15")),
                false,
            ),
            Arguments.of(
                "Virk er i samme måned som første dødsdato og er ugyldig",
                Tidspunkt.parse("2019-10-01T00:00:00Z"),
                listOf(LocalDate.parse("2019-10-15"), LocalDate.parse("2020-01-15")),
                false,
            ),
            Arguments.of(
                "Virk er etter første dødsdato, før siste dødsdato og er gyldig",
                Tidspunkt.parse("2019-11-01T00:00:00Z"),
                listOf(LocalDate.parse("2019-10-15"), LocalDate.parse("2020-01-15")),
                true,
            ),
            Arguments.of(
                "Virk er etter siste dødsdato og er gyldig",
                Tidspunkt.parse("2020-02-01T00:00:00Z"),
                listOf(LocalDate.parse("2019-10-15"), LocalDate.parse("2020-01-15")),
                true,
            ),
        )

    @Test
    fun `skal gi ugyldig virkningstidspunkt med to avdoede hvis tidspunkt er foer siste doedsfall`() {
        val gyldigVirkningstidspunkt =
            sjekkOmVirkningstidspunktErGyldig(
                sakType = SakType.BARNEPENSJON,
                virkningstidspunkt = Tidspunkt.parse("2019-11-01T00:00:00Z"),
                soeknadMottatt = LocalDateTime.parse("2020-02-01T00:00:00"),
                doedsdato = listOf(LocalDate.parse("2019-10-15"), LocalDate.parse("2020-01-15")),
            )

        gyldigVirkningstidspunkt shouldBe true
    }

    @Test
    fun `skal gi gyldig virkningstidspunkt hvis tidspunkt er inntil tre aar foer mottatt soeknad for barnepensjon`() {
        val gyldigVirkningstidspunkt =
            sjekkOmVirkningstidspunktErGyldig(
                sakType = SakType.BARNEPENSJON,
                virkningstidspunkt = Tidspunkt.parse("2017-01-01T00:00:00Z"),
                soeknadMottatt = LocalDateTime.parse("2020-01-15T00:00:00"),
                doedsdato = listOf(LocalDate.parse("2016-11-30")),
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
                doedsdato = listOf(LocalDate.parse("2016-11-30")),
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
                doedsdato = listOf(LocalDate.parse("2016-11-30")),
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
                doedsdato = listOf(LocalDate.parse("2016-11-30")),
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
                doedsdato = listOf(LocalDate.parse("2016-11-30")),
                kilde = Vedtaksloesning.PESYS,
            )

        gyldigVirkningstidspunkt shouldBe true
    }

    @ParameterizedTest
    @EnumSource(SakType::class, names = ["OMSTILLINGSSTOENAD", "BARNEPENSJON"], mode = EnumSource.Mode.INCLUDE)
    fun `skal gi gyldig virkningstidspunkt hvis virkningstidspunkt er foer opphoer`(sakType: SakType) {
        val gyldigVirkningstidspunkt =
            sjekkOmVirkningstidspunktErGyldig(
                sakType = sakType,
                behandlingType = BehandlingType.FØRSTEGANGSBEHANDLING,
                virkningstidspunkt = Tidspunkt.parse("2024-06-01T00:00:00Z"),
                opphoerFraOgMed = YearMonth.of(2024, Month.DECEMBER),
                foersteVirk = YearMonth.of(2024, Month.JANUARY),
            )

        gyldigVirkningstidspunkt shouldBe true
    }

    @ParameterizedTest
    @EnumSource(SakType::class, names = ["OMSTILLINGSSTOENAD", "BARNEPENSJON"], mode = EnumSource.Mode.INCLUDE)
    fun `skal gi ugyldig virkningstidspunkt hvis virkningstidspunkt er etter opphoer`(sakType: SakType) {
        shouldThrow<VirkningstidspunktKanIkkeVaereEtterOpphoer> {
            sjekkOmVirkningstidspunktErGyldig(
                sakType = sakType,
                behandlingType = BehandlingType.FØRSTEGANGSBEHANDLING,
                virkningstidspunkt = Tidspunkt.parse("2024-12-01T00:00:00Z"),
                opphoerFraOgMed = YearMonth.of(2024, Month.MAY),
                foersteVirk = YearMonth.of(2024, Month.JANUARY),
            )
        }
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
        shouldThrow<VirkFoerIverksattVirk> {
            sjekkOmVirkningstidspunktErGyldig(
                sakType = sakType,
                behandlingType = BehandlingType.REVURDERING,
                virkningstidspunkt = Tidspunkt.parse("2023-12-01T00:00:00Z"),
                foersteVirk = YearMonth.of(2024, Month.JANUARY),
            )
        }
    }

    @ParameterizedTest
    @EnumSource(SakType::class, names = ["OMSTILLINGSSTOENAD", "BARNEPENSJON"], mode = EnumSource.Mode.INCLUDE)
    fun `skal gi gyldig virkningstidspunkt for revurdering hvis virkningstidspunkt er foer opphoer`(sakType: SakType) {
        val gyldigVirkningstidspunkt =
            sjekkOmVirkningstidspunktErGyldig(
                sakType = sakType,
                behandlingType = BehandlingType.REVURDERING,
                virkningstidspunkt = Tidspunkt.parse("2024-06-01T00:00:00Z"),
                opphoerFraOgMed = YearMonth.of(2024, Month.DECEMBER),
                foersteVirk = YearMonth.of(2024, Month.JANUARY),
            )

        gyldigVirkningstidspunkt shouldBe true
    }

    @ParameterizedTest
    @EnumSource(SakType::class, names = ["OMSTILLINGSSTOENAD", "BARNEPENSJON"], mode = EnumSource.Mode.INCLUDE)
    fun `skal gi ugyldig virkningstidspunkt for revurdering hvis virkningstidspunkt er etter opphoer`(sakType: SakType) {
        shouldThrow<VirkningstidspunktKanIkkeVaereEtterOpphoer> {
            sjekkOmVirkningstidspunktErGyldig(
                sakType = sakType,
                behandlingType = BehandlingType.REVURDERING,
                virkningstidspunkt = Tidspunkt.parse("2024-12-01T00:00:00Z"),
                opphoerFraOgMed = YearMonth.of(2024, Month.MAY),
                foersteVirk = YearMonth.of(2024, Month.JANUARY),
            )
        }
    }

    @ParameterizedTest
    @EnumSource(SakType::class, names = ["OMSTILLINGSSTOENAD", "BARNEPENSJON"], mode = EnumSource.Mode.INCLUDE)
    fun `skal gi ugyldig virkningstidspunkt for revurdering hvis foerste virk ikke finnes`(sakType: SakType) {
        shouldThrow<KanIkkeOppretteRevurderingUtenIverksattFoerstegangsbehandling> {
            sjekkOmVirkningstidspunktErGyldig(
                sakType = sakType,
                behandlingType = BehandlingType.REVURDERING,
                virkningstidspunkt = Tidspunkt.parse("2024-12-01T00:00:00Z"),
                foersteVirk = null,
            )
        }
    }

    @ParameterizedTest
    @EnumSource(SakType::class, names = ["BARNEPENSJON"], mode = EnumSource.Mode.INCLUDE)
    fun `saker innvilget i Pesys skal gi feimelding hvis virkningstidspunkt før januar`(sakType: SakType) {
        shouldThrow<VirkFoerOmsKildePesys> {
            sjekkOmVirkningstidspunktErGyldig(
                sakType = sakType,
                behandlingType = BehandlingType.REVURDERING,
                virkningstidspunkt = Tidspunkt.parse("2023-12-01T00:00:00Z"),
                foersteVirk = YearMonth.of(2024, 1),
                kilde = Vedtaksloesning.PESYS,
            )
        }
    }

    @ParameterizedTest
    @EnumSource(SakType::class, names = ["BARNEPENSJON"], mode = EnumSource.Mode.INCLUDE)
    fun `saker ikke innvilget i Pesys skal ikke feile hvis virkningstidspunkt før januar`(sakType: SakType) {
        sjekkOmVirkningstidspunktErGyldig(
            sakType = sakType,
            behandlingType = BehandlingType.REVURDERING,
            virkningstidspunkt = Tidspunkt.parse("2023-12-01T00:00:00Z"),
            foersteVirk = YearMonth.of(2023, 12),
        )
    }

    private fun sjekkOmVirkningstidspunktErGyldig(
        sakType: SakType,
        behandlingType: BehandlingType = BehandlingType.FØRSTEGANGSBEHANDLING,
        utlandstilknytningType: UtlandstilknytningType? = UtlandstilknytningType.NASJONAL,
        virkningstidspunkt: Tidspunkt = Tidspunkt.parse("2016-01-01T00:00:00Z"),
        begrunnelse: String = "en begrunnelse",
        soeknadMottatt: LocalDateTime = LocalDateTime.parse("2020-01-15T00:00:00"),
        doedsdato: List<LocalDate> = listOf(LocalDate.parse("2016-11-30")),
        kravdato: Tidspunkt? = null,
        foersteVirk: YearMonth? = null,
        kilde: Vedtaksloesning = Vedtaksloesning.GJENNY,
        opphoerFraOgMed: YearMonth? = null,
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
            opphoerFraOgMed = opphoerFraOgMed,
        )

        val request = VirkningstidspunktRequest(virkningstidspunkt.toString(), begrunnelse, kravdato?.toLocalDate())

        return runBlocking {
            behandlingService.erGyldigVirkningstidspunkt(BEHANDLINGS_ID, request, false)
        }
    }

    @Test
    fun `hentSenestIverksatteBehandling() returnerer seneste iverksatte behandlingen`() {
        nyKontekstMedBruker(mockSaksbehandler())

        val behandling1 = foerstegangsbehandling(sakId = sakId1, status = BehandlingStatus.IVERKSATT)
        val behandling2 =
            revurdering(
                sakId = sakId1,
                status = BehandlingStatus.BEREGNET,
                revurderingAarsak = Revurderingaarsak.REGULERING,
            )

        every { behandlingDaoMock.hentBehandlingerForSak(any()) } returns listOf(behandling1, behandling2)

        assertEquals(behandling1, behandlingService.hentSisteIverksatteBehandling(sakId1))
    }

    @Test
    fun `skal hente behandlinger i sak hvor sak har enhet og brukeren har enhet`() {
        nyKontekstMedBruker(mockSaksbehandler(enheter = listOf(Enheter.PORSGRUNN.enhetNr)))
        every { behandlingDaoMock.hentBehandlingerForSak(sakId1) } returns
            listOf(
                revurdering(
                    sakId = sakId1,
                    revurderingAarsak = Revurderingaarsak.REGULERING,
                    enhet = Enheter.PORSGRUNN.enhetNr,
                ),
                foerstegangsbehandling(sakId = sakId1, enhet = Enheter.PORSGRUNN.enhetNr),
            )

        val behandlinger = behandlingService.hentBehandlingerForSak(sakId1)

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
                sakId = sakId1,
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

        val sak1 = Sak("fnr", SakType.BARNEPENSJON, id = randomSakId(), Enheter.PORSGRUNN.enhetNr, null, false)
        val sak2 =
            Sak("fnr", SakType.OMSTILLINGSSTOENAD, id = randomSakId(), Enheter.PORSGRUNN.enhetNr, null, false)

        every { behandlingDaoMock.hentBehandlingerForSak(sak1.id) } returns
            listOf(
                foerstegangsbehandling(sakId = sak1.id, status = BehandlingStatus.AVBRUTT),
                foerstegangsbehandling(sakId = sak1.id, status = BehandlingStatus.AVBRUTT),
            )
        every { behandlingDaoMock.hentBehandlingerForSak(sak2.id) } returns
            listOf(
                foerstegangsbehandling(sakId = sak2.id, status = BehandlingStatus.IVERKSATT),
            )

        every { oppgaveServiceMock.hentOppgaverForSak(any()) } returns listOf(mockk())

        val sakMedBehandlinger = behandlingService.hentSakMedBehandlinger(listOf(sak1, sak2))

        assertEquals(sak2.id, sakMedBehandlinger.sak.id)
        assertEquals(1, sakMedBehandlinger.behandlinger.size)

        verify(exactly = 1) {
            behandlingDaoMock.hentBehandlingerForSak(sak1.id)
            behandlingDaoMock.hentBehandlingerForSak(sak2.id)
        }
    }

    @Test
    fun `start dato må være før opphørsdato tidligere familiepleier`() {
        nyKontekstMedBruker(mockSaksbehandler(enheter = listOf(Enheter.PORSGRUNN.enhetNr)))

        val uuid = UUID.randomUUID()

        val slot = slot<TidligereFamiliepleier>()

        every { behandlingDaoMock.hentBehandling(any()) } returns
            foerstegangsbehandling(
                id = uuid,
                sakId = randomSakId(),
                enhet = Enheter.PORSGRUNN.enhetNr,
            )

        every { behandlingDaoMock.lagreTidligereFamiliepleier(any(), capture(slot)) } just runs
        every { behandlingDaoMock.lagreStatus(any()) } just runs

        shouldThrow<PleieforholdMaaStarteFoerDetOpphoerer> {
            inTransaction {
                behandlingService.oppdaterTidligereFamiliepleier(
                    uuid,
                    TidligereFamiliepleier(
                        true,
                        Grunnlagsopplysning.Saksbehandler.create("ident"),
                        "123",
                        LocalDate.now(),
                        LocalDate.now(),
                        "Test",
                    ),
                )
            }
        }
    }

    @Test
    fun `kan oppdatere tidligere familiepleier`() {
        nyKontekstMedBruker(mockSaksbehandler(enheter = listOf(Enheter.PORSGRUNN.enhetNr)))

        val uuid = UUID.randomUUID()

        val slot = slot<TidligereFamiliepleier>()

        every { behandlingDaoMock.hentBehandling(any()) } returns
            foerstegangsbehandling(
                id = uuid,
                sakId = randomSakId(),
                enhet = Enheter.PORSGRUNN.enhetNr,
            )

        every { behandlingDaoMock.lagreTidligereFamiliepleier(any(), capture(slot)) } just runs
        every { behandlingDaoMock.lagreStatus(any()) } just runs

        inTransaction {
            behandlingService.oppdaterTidligereFamiliepleier(
                uuid,
                TidligereFamiliepleier(
                    true,
                    Grunnlagsopplysning.Saksbehandler.create("ident"),
                    "123",
                    LocalDate.of(1970, 1, 1),
                    LocalDate.now(),
                    "Test",
                ),
            )
        }

        assertEquals(true, slot.captured.svar)
        assertEquals("123", slot.captured.foedselsnummer)
        assertEquals("Test", slot.captured.begrunnelse)
        assertEquals("ident", (slot.captured.kilde as Grunnlagsopplysning.Saksbehandler).ident)
    }

    @Test
    fun `hentSakMedBehandlinger - kun én sak`() {
        nyKontekstMedBruker(mockSaksbehandler())

        val sak = Sak("fnr", SakType.OMSTILLINGSSTOENAD, id = randomSakId(), Enheter.PORSGRUNN.enhetNr, null, false)

        every { behandlingDaoMock.hentBehandlingerForSak(sak.id) } returns
            listOf(
                foerstegangsbehandling(
                    sakId = sak.id,
                    status = BehandlingStatus.IVERKSATT,
                ),
            )
        every { oppgaveServiceMock.hentOppgaverForSak(any()) } returns listOf(mockk())

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
        } returns revurdering(sakId = sakId1, revurderingAarsak = Revurderingaarsak.INNTEKTSENDRING)
        behandlingService.endreSkalSendeBrev(behandlingId, true)
        verify(exactly = 1) { behandlingDaoMock.lagreSendeBrev(behandlingId, true) }
    }

    @Test
    fun `Kan ikke endre send brev førstegangsbehandling revurdering`() {
        nyKontekstMedBruker(mockSaksbehandler())
        val behandlingId = UUID.randomUUID()
        every { behandlingDaoMock.lagreSendeBrev(behandlingId, true) } just runs
        every { behandlingDaoMock.hentBehandling(behandlingId) } returns foerstegangsbehandling(sakId = sakId1)
        assertThrows<KanIkkeEndreSendeBrevForFoerstegangsbehandling> {
            behandlingService.endreSkalSendeBrev(behandlingId, true)
        }

        verify(exactly = 0) { behandlingDaoMock.lagreSendeBrev(behandlingId, true) }
    }

    @Test
    fun `Kan lagre annen forelder`() {
        nyKontekstMedBruker(mockSaksbehandler())
        val behandling = foerstegangsbehandling(sakId = randomSakId(), id = UUID.randomUUID())
        every { behandlingDaoMock.hentBehandling(behandling.id) } returns behandling
        every { grunnlagService.hentPersongalleri(behandling.id) } returns mockPersongalleri()
        every { grunnlagService.lagreNyeSaksopplysninger(any(), behandling.id, any()) } just runs
        coEvery { grunnlagService.oppdaterGrunnlag(behandling.id, behandling.sak.id, any()) } just runs
        every { behandlingDaoMock.lagreStatus(any()) } just runs
        every { behandlingDaoMock.lagreStatus(any()) } just runs

        val annenForelderInRequest =
            AnnenForelder(
                vurdering = AnnenForelder.AnnenForelderVurdering.FORELDER_UTEN_IDENT_I_PDL,
                foedselsdato = LocalDate.now(),
            )
        runBlocking {
            behandlingService.lagreAnnenForelder(
                behandling.id,
                TOKEN,
                annenForelderInRequest,
            )
        }
        val slot = slot<List<Grunnlagsopplysning<JsonNode>>>()
        coVerify {
            grunnlagService.oppdaterGrunnlag(behandling.id, behandling.sak.id, any())
        }
        verify {
            grunnlagService.lagreNyeSaksopplysninger(any(), behandling.id, capture(slot))
        }

        val grunnlagsopplysning = slot.captured.single()
        assertEquals(Opplysningstype.PERSONGALLERI_V1, grunnlagsopplysning.opplysningType)

        val persongalleri = objectMapper.readValue(grunnlagsopplysning.opplysning.toJson(), Persongalleri::class.java)
        assertEquals(annenForelderInRequest, persongalleri.annenForelder)
    }

    private fun initFellesMocks(
        sakType: SakType = SakType.BARNEPENSJON,
        behandlingType: BehandlingType = BehandlingType.FØRSTEGANGSBEHANDLING,
        doedsdato: List<LocalDate>,
        soeknadMottatt: LocalDateTime,
        foersteVirk: YearMonth?,
        utlandstilknytning: Utlandstilknytning? = null,
        kilde: Vedtaksloesning = Vedtaksloesning.GJENNY,
        opphoerFraOgMed: YearMonth? = null,
    ) {
        nyKontekstMedBruker(mockSaksbehandler())

        val foerstegangsbehandling =
            foerstegangsbehandling(
                id = BEHANDLINGS_ID,
                sakId = SAK_ID,
                sakType = sakType,
                soeknadMottattDato = soeknadMottatt,
                utlandstilknytning = utlandstilknytning,
                kilde = kilde,
                opphoerFraOgMed = opphoerFraOgMed,
                virkningstidspunkt =
                    foersteVirk?.let {
                        Virkningstidspunkt(it, Grunnlagsopplysning.Saksbehandler("", Tidspunkt.now()), "")
                    },
            )
        val (behandling, tidligereBehandlinger) =
            when (behandlingType) {
                BehandlingType.FØRSTEGANGSBEHANDLING ->
                    Pair(
                        foerstegangsbehandling,
                        emptyList(),
                    )

                BehandlingType.REVURDERING ->
                    Pair(
                        revurdering(
                            id = BEHANDLINGS_ID,
                            sakId = SAK_ID,
                            revurderingAarsak = Revurderingaarsak.ANNEN,
                            utlandstilknytning = utlandstilknytning,
                            opphoerFraOgMed = opphoerFraOgMed,
                        ),
                        if (foersteVirk != null) {
                            listOf(
                                foerstegangsbehandling(
                                    id = BEHANDLINGS_ID,
                                    sakId = SAK_ID,
                                    sakType = sakType,
                                    status = BehandlingStatus.IVERKSATT,
                                    virkningstidspunkt =
                                        Virkningstidspunkt.create(
                                            dato = foersteVirk,
                                            begrunnelse = "begrunnelse",
                                            saksbehandler = Grunnlagsopplysning.Saksbehandler.create("Z123456"),
                                        ),
                                    soeknadMottattDato = soeknadMottatt,
                                    utlandstilknytning = utlandstilknytning,
                                ),
                            )
                        } else {
                            emptyList()
                        },
                    )
            }

        every {
            grunnlagService.hentPersonopplysninger(behandling.id, sakType)
        } returns
            PersonopplysningerResponse(
                avdoede =
                    doedsdato.map { datoDoed ->
                        Personopplysning(
                            Opplysningstype.AVDOED_PDL_V1,
                            UUID.randomUUID(),
                            GenerellKilde("", Tidspunkt.now(), ""),
                            personOpplysning(doedsdato = datoDoed),
                        )
                    },
                gjenlevende = emptyList(),
                innsender = null,
                soeker = null,
                annenForelder = null,
            )
        every { grunnlagService.hentPersongalleri(behandling.id) } answers { callOriginal() }

        every { behandlingDaoMock.hentBehandling(BEHANDLINGS_ID) } returns behandling
        every { behandlingDaoMock.hentBehandlingerForSak(any()) } returns tidligereBehandlinger // TODO fjern?
        every { behandlingDaoMock.hentInnvilgaFoerstegangsbehandling(behandling.sak.id) } returns foerstegangsbehandling
    }

    private fun mockOppgaveIntern(relatertBehandlingsId: UUID): OppgaveIntern {
        val oppgaveKlage = mockk<OppgaveIntern>()
        every { oppgaveKlage.type } returns OppgaveType.KLAGE
        every { oppgaveKlage.id } returns relatertBehandlingsId
        every { oppgaveKlage.referanse } returns relatertBehandlingsId.toString()
        every { oppgaveKlage.merknad } returns ""
        every { oppgaveKlage.frist } returns mockk<Tidspunkt>()
        every { oppgaveKlage.sakId } returns sakId1
        every { oppgaveKlage.kilde } returns mockk<OppgaveKilde>()

        return oppgaveKlage
    }

    private fun mockPersongalleri() =
        Persongalleri(
            "soeker",
            "innsender",
            listOf("soesken"),
            listOf("avdoed"),
            listOf("gjenlevende"),
        )

    companion object {
        val SAK_ID = sakId1
        val BEHANDLINGS_ID: UUID = UUID.randomUUID()
        val TOKEN = simpleSaksbehandler()
    }
}
