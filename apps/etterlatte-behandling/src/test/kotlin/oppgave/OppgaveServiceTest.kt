package no.nav.etterlatte.oppgave

import io.kotest.matchers.shouldBe
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import no.nav.etterlatte.ConnectionAutoclosingTest
import no.nav.etterlatte.DatabaseContextTest
import no.nav.etterlatte.DatabaseExtension
import no.nav.etterlatte.SaksbehandlerMedEnheterOgRoller
import no.nav.etterlatte.SystemUser
import no.nav.etterlatte.azureAdAttestantClaim
import no.nav.etterlatte.azureAdSaksbehandlerClaim
import no.nav.etterlatte.azureAdStrengtFortroligClaim
import no.nav.etterlatte.behandling.BehandlingHendelserKafkaProducer
import no.nav.etterlatte.behandling.hendelse.HendelseDao
import no.nav.etterlatte.common.Enheter
import no.nav.etterlatte.grunnlagsendring.SakMedEnhet
import no.nav.etterlatte.ktor.token.simpleAttestant
import no.nav.etterlatte.ktor.token.simpleSaksbehandler
import no.nav.etterlatte.libs.common.behandling.BehandlingHendelseType
import no.nav.etterlatte.libs.common.behandling.PaaVentAarsak
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.feilhaandtering.InternfeilException
import no.nav.etterlatte.libs.common.feilhaandtering.UgyldigForespoerselException
import no.nav.etterlatte.libs.common.oppgave.OppgaveKilde
import no.nav.etterlatte.libs.common.oppgave.OppgaveType
import no.nav.etterlatte.libs.common.oppgave.Status
import no.nav.etterlatte.libs.common.person.AdressebeskyttelseGradering
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.tidspunkt.toLocalDatetimeUTC
import no.nav.etterlatte.libs.common.tidspunkt.toTidspunkt
import no.nav.etterlatte.libs.ktor.token.Claims
import no.nav.etterlatte.nyKontekstMedBruker
import no.nav.etterlatte.nyKontekstMedBrukerOgDatabaseContext
import no.nav.etterlatte.sak.SakLesDao
import no.nav.etterlatte.sak.SakSkrivDao
import no.nav.etterlatte.sak.SakendringerDao
import no.nav.etterlatte.tilgangsstyring.AzureGroup
import no.nav.etterlatte.tilgangsstyring.SaksbehandlerMedRoller
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import java.util.UUID
import javax.sql.DataSource

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(DatabaseExtension::class)
internal class OppgaveServiceTest(
    val dataSource: DataSource,
) {
    private val sakLesDao: SakLesDao = SakLesDao(ConnectionAutoclosingTest(dataSource))
    private val sakSkrivDao =
        SakSkrivDao(SakendringerDao(ConnectionAutoclosingTest(dataSource)) { sakLesDao.hentSak(it) })
    private val oppgaveDao: OppgaveDao = spyk(OppgaveDaoImpl(ConnectionAutoclosingTest(dataSource)))
    private val hendelser: BehandlingHendelserKafkaProducer = mockk(relaxed = true)
    private val hendelseDao = mockk<HendelseDao>()
    private val oppgaveDaoMedEndringssporing: OppgaveDaoMedEndringssporing =
        OppgaveDaoMedEndringssporingImpl(oppgaveDao, ConnectionAutoclosingTest(dataSource))
    private val oppgaveService: OppgaveService =
        OppgaveService(oppgaveDaoMedEndringssporing, sakLesDao, hendelseDao, hendelser)
    private val saksbehandler = mockk<SaksbehandlerMedEnheterOgRoller>()

    private val azureGroupToGroupIDMap =
        mapOf(
            AzureGroup.SAKSBEHANDLER to azureAdSaksbehandlerClaim,
            AzureGroup.ATTESTANT to azureAdAttestantClaim,
            AzureGroup.STRENGT_FORTROLIG to azureAdStrengtFortroligClaim,
        )

    private fun generateSaksbehandlerMedRoller(azureGroup: AzureGroup): SaksbehandlerMedRoller {
        val groupId = azureGroupToGroupIDMap[azureGroup]!!
        return SaksbehandlerMedRoller(
            simpleSaksbehandler(ident = azureGroup.name, claims = mapOf(Claims.groups to groupId)),
            mapOf(azureGroup to groupId),
        )
    }

    private fun mockForSaksbehandlerMedRoller(
        userSaksbehandler: SaksbehandlerMedEnheterOgRoller,
        saksbehandlerMedRoller: SaksbehandlerMedRoller,
    ) {
        every { userSaksbehandler.saksbehandlerMedRoller } returns saksbehandlerMedRoller
    }

    @BeforeEach
    fun beforeEach() {
        val saksbehandlerRoller = generateSaksbehandlerMedRoller(AzureGroup.SAKSBEHANDLER)
        every { saksbehandler.enheter() } returns Enheter.enheterForVanligSaksbehandlere()
        every { saksbehandler.name() } returns "ident"

        nyKontekstMedBrukerOgDatabaseContext(saksbehandler, DatabaseContextTest(dataSource))

        every { saksbehandler.saksbehandlerMedRoller } returns saksbehandlerRoller
    }

    @AfterEach
    fun afterEach() {
        dataSource.connection.use {
            it.prepareStatement("TRUNCATE oppgave CASCADE;").execute()
        }
        clearAllMocks()
    }

    @Test
    fun `Hent oppgave med id som ikke finnes`() {
        assertThrows<InternfeilException> {
            oppgaveService.hentOppgave(UUID.randomUUID())
        }
    }

    @Test
    fun `skal kunne tildele oppgave uten saksbehandler`() {
        val opprettetSak = sakSkrivDao.opprettSak("fnr", SakType.BARNEPENSJON, Enheter.AALESUND.enhetNr)
        val nyOppgave =
            oppgaveService.opprettOppgave(
                "referanse",
                opprettetSak.id,
                OppgaveKilde.BEHANDLING,
                OppgaveType.FOERSTEGANGSBEHANDLING,
                null,
            )
        val nysaksbehandler = "nysaksbehandler"
        oppgaveService.tildelSaksbehandler(nyOppgave.id, nysaksbehandler)

        val oppgaveMedNySaksbehandler = oppgaveService.hentOppgave(nyOppgave.id)
        assertEquals(nysaksbehandler, oppgaveMedNySaksbehandler.saksbehandler?.ident)
    }

    @Test
    fun `skal tildele attesteringsoppgave hvis systembruker og fatte`() {
        val systemBruker = mockk<SystemUser> { every { name() } returns "name" }
        nyKontekstMedBruker(systemBruker)

        val opprettetSak = sakSkrivDao.opprettSak("fnr", SakType.BARNEPENSJON, Enheter.AALESUND.enhetNr)
        val referanse = "referanse"
        val nyOppgave =
            oppgaveService.opprettOppgave(
                referanse,
                opprettetSak.id,
                OppgaveKilde.BEHANDLING,
                OppgaveType.FOERSTEGANGSBEHANDLING,
                null,
            )

        val systembruker = "systembruker"
        oppgaveService.tildelSaksbehandler(nyOppgave.id, systembruker)

        val sakIdOgReferanse =
            oppgaveService.tilAttestering(
                referanse = referanse,
                type = OppgaveType.FOERSTEGANGSBEHANDLING,
                merknad = null,
            )

        oppgaveService.tildelSaksbehandler(sakIdOgReferanse.id, systembruker)
        val systembrukerOppgave = oppgaveService.hentOppgave(sakIdOgReferanse.id)
        assertEquals(systembruker, systembrukerOppgave.saksbehandler!!.ident)
    }

    @Test
    fun `skal tildele attesteringsoppgave hvis rolle attestering finnes`() {
        val opprettetSak = sakSkrivDao.opprettSak("fnr", SakType.BARNEPENSJON, Enheter.AALESUND.enhetNr)
        val referanse = "referanse"
        val nyOppgave =
            oppgaveService.opprettOppgave(
                referanse,
                opprettetSak.id,
                OppgaveKilde.BEHANDLING,
                OppgaveType.FOERSTEGANGSBEHANDLING,
                null,
            )

        val vanligSaksbehandler = saksbehandler.saksbehandlerMedRoller.saksbehandler
        oppgaveService.tildelSaksbehandler(nyOppgave.id, vanligSaksbehandler.ident)

        val sakIdOgReferanse =
            oppgaveService.tilAttestering(
                referanse,
                OppgaveType.FOERSTEGANGSBEHANDLING,
                null,
            )

        val attestantSaksbehandler = mockk<SaksbehandlerMedEnheterOgRoller> { every { name() } returns "ident" }
        nyKontekstMedBruker(attestantSaksbehandler)
        val attestantmedRoller = generateSaksbehandlerMedRoller(AzureGroup.ATTESTANT)
        mockForSaksbehandlerMedRoller(attestantSaksbehandler, attestantmedRoller)

        oppgaveService.tildelSaksbehandler(sakIdOgReferanse.id, attestantmedRoller.saksbehandler.ident)
        val attestantTildeltOppgave = oppgaveService.hentOppgave(sakIdOgReferanse.id)
        assertEquals(attestantmedRoller.saksbehandler.ident, attestantTildeltOppgave.saksbehandler!!.ident)
    }

    @Test
    fun `skal ikke tildele attesteringsoppgave hvis rolle saksbehandler`() {
        val opprettetSak = sakSkrivDao.opprettSak("fnr", SakType.BARNEPENSJON, Enheter.AALESUND.enhetNr)
        val referanse = "referanse"
        val nyOppgave =
            oppgaveService.opprettOppgave(
                referanse,
                opprettetSak.id,
                OppgaveKilde.BEHANDLING,
                OppgaveType.FOERSTEGANGSBEHANDLING,
                null,
            )

        val vanligSaksbehandler = saksbehandler.saksbehandlerMedRoller.saksbehandler
        oppgaveService.tildelSaksbehandler(nyOppgave.id, vanligSaksbehandler.ident)

        val sakIdOgReferanse =
            oppgaveService.tilAttestering(
                referanse,
                OppgaveType.FOERSTEGANGSBEHANDLING,
                null,
            )

        val saksbehandlerto = mockk<SaksbehandlerMedEnheterOgRoller> { every { name() } returns vanligSaksbehandler.ident }
        nyKontekstMedBruker(saksbehandlerto)
        val saksbehandlerMedRoller = generateSaksbehandlerMedRoller(AzureGroup.SAKSBEHANDLER)
        mockForSaksbehandlerMedRoller(saksbehandlerto, saksbehandlerMedRoller)

        assertThrows<BrukerManglerAttestantRolleException> {
            oppgaveService.tildelSaksbehandler(sakIdOgReferanse.id, saksbehandlerMedRoller.saksbehandler.ident)
        }
    }

    @Test
    fun `skal tildele attesteringsoppgave hvis rolle attestant og innsender tildeler seg selv`() {
        val opprettetSak = sakSkrivDao.opprettSak("fnr", SakType.BARNEPENSJON, Enheter.AALESUND.enhetNr)
        val referanse = "referanse"
        val nyOppgave =
            oppgaveService.opprettOppgave(
                referanse,
                opprettetSak.id,
                OppgaveKilde.BEHANDLING,
                OppgaveType.FOERSTEGANGSBEHANDLING,
                null,
            )

        val vanligSaksbehandler = saksbehandler.saksbehandlerMedRoller.saksbehandler
        oppgaveService.tildelSaksbehandler(nyOppgave.id, vanligSaksbehandler.ident)

        val sakIdOgReferanse =
            oppgaveService.tilAttestering(
                referanse,
                OppgaveType.FOERSTEGANGSBEHANDLING,
                null,
            )

        val saksbehandlerto = mockk<SaksbehandlerMedEnheterOgRoller> { every { name() } returns vanligSaksbehandler.ident }
        nyKontekstMedBruker(saksbehandlerto)
        val saksbehandlerMedRoller = generateSaksbehandlerMedRoller(AzureGroup.ATTESTANT)
        mockForSaksbehandlerMedRoller(saksbehandlerto, saksbehandlerMedRoller)

        oppgaveService.tildelSaksbehandler(sakIdOgReferanse.id, saksbehandlerMedRoller.saksbehandler.ident)
    }

    @Test
    fun `skal tildele attesteringsoppgave hvis innsender tildeler annen sb`() {
        val opprettetSak = sakSkrivDao.opprettSak("fnr", SakType.BARNEPENSJON, Enheter.AALESUND.enhetNr)
        val referanse = "referanse"
        val nyOppgave =
            oppgaveService.opprettOppgave(
                referanse,
                opprettetSak.id,
                OppgaveKilde.BEHANDLING,
                OppgaveType.FOERSTEGANGSBEHANDLING,
                null,
            )

        val vanligSaksbehandler = saksbehandler.saksbehandlerMedRoller.saksbehandler
        oppgaveService.tildelSaksbehandler(nyOppgave.id, "annen")

        val sakIdOgReferanse =
            oppgaveService.tilAttestering(
                referanse,
                OppgaveType.FOERSTEGANGSBEHANDLING,
                null,
            )

        val saksbehandlerto = mockk<SaksbehandlerMedEnheterOgRoller> { every { name() } returns vanligSaksbehandler.ident }
        nyKontekstMedBruker(saksbehandlerto)
        val saksbehandlerMedRoller = generateSaksbehandlerMedRoller(AzureGroup.ATTESTANT)
        mockForSaksbehandlerMedRoller(saksbehandlerto, saksbehandlerMedRoller)

        oppgaveService.tildelSaksbehandler(sakIdOgReferanse.id, saksbehandlerMedRoller.saksbehandler.ident)
    }

    @Test
    fun `skal ikke kunne tildele hvis oppgaven ikke finnes`() {
        val nysaksbehandler = "nysaksbehandler"
        val err =
            assertThrows<OppgaveIkkeFunnet> {
                oppgaveService.tildelSaksbehandler(UUID.randomUUID(), nysaksbehandler)
            }
        assertTrue(err.message!!.startsWith("Oppgaven finnes ikke"))
    }

    @Test
    fun `skal ikke kunne tildele en lukket oppgave`() {
        val opprettetSak = sakSkrivDao.opprettSak("fnr", SakType.BARNEPENSJON, Enheter.AALESUND.enhetNr)
        val nyOppgave =
            oppgaveService.opprettOppgave(
                "referanse",
                opprettetSak.id,
                OppgaveKilde.BEHANDLING,
                OppgaveType.FOERSTEGANGSBEHANDLING,
                null,
            )
        oppgaveDao.endreStatusPaaOppgave(nyOppgave.id, Status.FERDIGSTILT)
        val nysaksbehandler = "nysaksbehandler"
        assertThrows<OppgaveKanIkkeEndres> {
            oppgaveService.tildelSaksbehandler(nyOppgave.id, nysaksbehandler)
        }
    }

    @Test
    fun `avbrytAapneOppgaverMedReferanse setter alle åpne oppgaver for behandling til avbrutt`() {
        val sak = sakSkrivDao.opprettSak("fnr", SakType.BARNEPENSJON, Enheter.AALESUND.enhetNr)
        val behandlingsId = UUID.randomUUID().toString()
        val oppgaveBehandling =
            oppgaveService.opprettOppgave(
                referanse = behandlingsId,
                sakId = sak.id,
                kilde = OppgaveKilde.BEHANDLING,
                type = OppgaveType.FOERSTEGANGSBEHANDLING,
                merknad = null,
            )
        val oppgaveAttestering =
            oppgaveService.opprettOppgave(
                referanse = behandlingsId,
                sakId = sak.id,
                kilde = OppgaveKilde.BEHANDLING,
                type = OppgaveType.FOERSTEGANGSBEHANDLING,
                merknad = null,
            )
        oppgaveService.tildelSaksbehandler(oppgaveBehandling.id, "saksbehandler")
        oppgaveService.avbrytAapneOppgaverMedReferanse(behandlingsId)
        val oppgaveBehandlingEtterAvbryt = oppgaveService.hentOppgave(oppgaveBehandling.id)
        val oppgaveAttesteringEtterAvbryt = oppgaveService.hentOppgave(oppgaveAttestering.id)

        assertEquals(Status.AVBRUTT, oppgaveBehandlingEtterAvbryt.status)
        assertEquals(Status.AVBRUTT, oppgaveAttesteringEtterAvbryt.status)
    }

    @Test
    fun `avbrytAapneOppgaverMedReferanse endrer ikke avsluttede oppgaver eller oppgaver til andre behandlinger`() {
        val sak = sakSkrivDao.opprettSak("fnr", SakType.BARNEPENSJON, Enheter.AALESUND.enhetNr)
        val behandlingId = UUID.randomUUID().toString()
        val annenBehandlingId = UUID.randomUUID().toString()
        val saksbehandler = simpleSaksbehandler()

        val oppgaveFerdigstilt =
            oppgaveService.opprettOppgave(
                referanse = behandlingId,
                sakId = sak.id,
                kilde = OppgaveKilde.BEHANDLING,
                type = OppgaveType.FOERSTEGANGSBEHANDLING,
                merknad = null,
            )
        oppgaveService.tildelSaksbehandler(oppgaveFerdigstilt.id, saksbehandler.ident)
        oppgaveService.ferdigStillOppgaveUnderBehandling(
            behandlingId,
            OppgaveType.FOERSTEGANGSBEHANDLING,
            saksbehandler,
        )

        val annenbehandlingfoerstegangs =
            oppgaveService.opprettOppgave(
                referanse = annenBehandlingId,
                sakId = sak.id,
                kilde = OppgaveKilde.BEHANDLING,
                type = OppgaveType.FOERSTEGANGSBEHANDLING,
                merknad = null,
            )
        val saksbehandlerforstegangs = simpleSaksbehandler()
        oppgaveService.tildelSaksbehandler(annenbehandlingfoerstegangs.id, saksbehandlerforstegangs.ident)
        oppgaveService.ferdigStillOppgaveUnderBehandling(
            annenBehandlingId,
            OppgaveType.FOERSTEGANGSBEHANDLING,
            saksbehandlerforstegangs,
        )
        val oppgaveUnderBehandlingAnnenBehandling =
            oppgaveService.opprettOppgave(
                referanse = annenBehandlingId,
                sakId = sak.id,
                kilde = OppgaveKilde.BEHANDLING,
                type = OppgaveType.FOERSTEGANGSBEHANDLING,
                merknad = null,
            )

        opprettAttestantKontekst()
        oppgaveService.tildelSaksbehandler(oppgaveUnderBehandlingAnnenBehandling.id, saksbehandler.ident)
        oppgaveService.avbrytAapneOppgaverMedReferanse(behandlingId)

        val oppgaveFerdigstiltEtterAvbryt = oppgaveService.hentOppgave(oppgaveFerdigstilt.id)
        val oppgaveUnderBehandlingEtterAvbryt = oppgaveService.hentOppgave(oppgaveUnderBehandlingAnnenBehandling.id)
        assertEquals(Status.FERDIGSTILT, oppgaveFerdigstiltEtterAvbryt.status)
        assertEquals(Status.UNDER_BEHANDLING, oppgaveUnderBehandlingEtterAvbryt.status)
    }

    @Test
    fun `skal kunne bytte oppgave med saksbehandler`() {
        val opprettetSak = sakSkrivDao.opprettSak("fnr", SakType.BARNEPENSJON, Enheter.AALESUND.enhetNr)
        val nyOppgave =
            oppgaveService.opprettOppgave(
                "referanse",
                opprettetSak.id,
                OppgaveKilde.BEHANDLING,
                OppgaveType.FOERSTEGANGSBEHANDLING,
                null,
            )
        val nysaksbehandler = "nysaksbehandler"
        oppgaveService.tildelSaksbehandler(nyOppgave.id, nysaksbehandler)

        val oppgaveMedNySaksbehandler = oppgaveService.hentOppgave(nyOppgave.id)
        assertEquals(nysaksbehandler, oppgaveMedNySaksbehandler.saksbehandler?.ident)
    }

    @Test
    fun `skal ikke kunne bytte saksbehandler på lukket oppgave`() {
        val opprettetSak = sakSkrivDao.opprettSak("fnr", SakType.BARNEPENSJON, Enheter.AALESUND.enhetNr)
        val nyOppgave =
            oppgaveService.opprettOppgave(
                "referanse",
                opprettetSak.id,
                OppgaveKilde.BEHANDLING,
                OppgaveType.FOERSTEGANGSBEHANDLING,
                null,
            )
        oppgaveDao.endreStatusPaaOppgave(nyOppgave.id, Status.FERDIGSTILT)
        val nysaksbehandler = "nysaksbehandler"
        assertThrows<OppgaveKanIkkeEndres> {
            oppgaveService.tildelSaksbehandler(nyOppgave.id, nysaksbehandler)
        }
        val oppgaveMedNySaksbehandler = oppgaveService.hentOppgave(nyOppgave.id)
        assertEquals(nyOppgave.saksbehandler, oppgaveMedNySaksbehandler.saksbehandler?.ident)
    }

    @Test
    fun `skal ikke kunne bytte saksbehandler på en ikke eksisterende sak`() {
        val nysaksbehandler = "nysaksbehandler"
        val err =
            assertThrows<OppgaveIkkeFunnet> {
                oppgaveService.tildelSaksbehandler(UUID.randomUUID(), nysaksbehandler)
            }
        assertTrue(err.message!!.startsWith("Oppgaven finnes ikke"))
    }

    @Test
    fun `skal kunne fjerne saksbehandler fra oppgave`() {
        val opprettetSak = sakSkrivDao.opprettSak("fnr", SakType.BARNEPENSJON, Enheter.AALESUND.enhetNr)
        val nyOppgave =
            oppgaveService.opprettOppgave(
                "referanse",
                opprettetSak.id,
                OppgaveKilde.BEHANDLING,
                OppgaveType.FOERSTEGANGSBEHANDLING,
                null,
            )
        val nysaksbehandler = "nysaksbehandler"
        oppgaveService.tildelSaksbehandler(nyOppgave.id, nysaksbehandler)
        oppgaveService.fjernSaksbehandler(nyOppgave.id)
        val oppgaveUtenSaksbehandler = oppgaveService.hentOppgave(nyOppgave.id)
        Assertions.assertNotNull(oppgaveUtenSaksbehandler.id)
        assertNull(oppgaveUtenSaksbehandler.saksbehandler)
        assertEquals(Status.UNDER_BEHANDLING, oppgaveUtenSaksbehandler.status)
    }

    @Test
    fun `fjerning av saksbehandling ppå oppgave hvor sb mangler bare ignoreres`() {
        val opprettetSak = sakSkrivDao.opprettSak("fnr", SakType.BARNEPENSJON, Enheter.AALESUND.enhetNr)
        val nyOppgave =
            oppgaveService.opprettOppgave(
                "referanse",
                opprettetSak.id,
                OppgaveKilde.BEHANDLING,
                OppgaveType.FOERSTEGANGSBEHANDLING,
                null,
            )

        oppgaveService.fjernSaksbehandler(nyOppgave.id)

        verify(exactly = 0) {
            oppgaveDao.fjernSaksbehandler(any())
        }
    }

    @Test
    fun `skal ikke kunne fjerne saksbehandler på en lukket oppgave`() {
        val opprettetSak = sakSkrivDao.opprettSak("fnr", SakType.BARNEPENSJON, Enheter.AALESUND.enhetNr)
        val nyOppgave =
            oppgaveService.opprettOppgave(
                "referanse",
                opprettetSak.id,
                OppgaveKilde.BEHANDLING,
                OppgaveType.FOERSTEGANGSBEHANDLING,
                null,
            )
        val saksbehandler = "saksbehandler"
        oppgaveService.tildelSaksbehandler(nyOppgave.id, saksbehandler)
        oppgaveDao.endreStatusPaaOppgave(nyOppgave.id, Status.FERDIGSTILT)
        assertThrows<OppgaveKanIkkeEndres> {
            oppgaveService.fjernSaksbehandler(nyOppgave.id)
        }
        val lagretOppgave = oppgaveService.hentOppgave(nyOppgave.id)

        assertEquals(lagretOppgave.saksbehandler?.ident, saksbehandler)
    }

    @Test
    fun `kan redigere frist`() {
        val opprettetSak = sakSkrivDao.opprettSak("fnr", SakType.BARNEPENSJON, Enheter.AALESUND.enhetNr)
        val nyOppgave =
            oppgaveService.opprettOppgave(
                "referanse",
                opprettetSak.id,
                OppgaveKilde.BEHANDLING,
                OppgaveType.FOERSTEGANGSBEHANDLING,
                null,
            )
        oppgaveService.tildelSaksbehandler(nyOppgave.id, "nysaksbehandler")
        val nyFrist =
            Tidspunkt
                .now()
                .toLocalDatetimeUTC()
                .plusMonths(4L)
                .toTidspunkt()
        oppgaveService.redigerFrist(nyOppgave.id, nyFrist)
        val oppgaveMedNyFrist = oppgaveService.hentOppgave(nyOppgave.id)
        assertEquals(nyFrist, oppgaveMedNyFrist.frist)
    }

    @Test
    fun `Kan ikke sette oppgave på vente om man mangler årsak`() {
        every { hendelser.sendMeldingForHendelsePaaVent(any(), any(), any()) } returns Unit

        val opprettetSak = sakSkrivDao.opprettSak("fnr", SakType.BARNEPENSJON, Enheter.AALESUND.enhetNr)
        val nyOppgave =
            oppgaveService.opprettOppgave(
                UUID.randomUUID().toString(),
                opprettetSak.id,
                OppgaveKilde.BEHANDLING,
                OppgaveType.FOERSTEGANGSBEHANDLING,
                null,
            )
        oppgaveService.tildelSaksbehandler(nyOppgave.id, "nysaksbehandler")

        assertThrows<UgyldigForespoerselException> {
            oppgaveService.endrePaaVent(oppgaveId = nyOppgave.id, merknad = "test", paavent = true, aarsak = null)
        }
    }

    @Test
    fun `kan sette og fjerne oppgave paa vent`() {
        every { hendelser.sendMeldingForHendelsePaaVent(any(), any(), any()) } returns Unit
        every { hendelser.sendMeldingForHendelseAvVent(any(), any()) } returns Unit

        val opprettetSak = sakSkrivDao.opprettSak("fnr", SakType.BARNEPENSJON, Enheter.AALESUND.enhetNr)
        val nyOppgave =
            oppgaveService.opprettOppgave(
                UUID.randomUUID().toString(),
                opprettetSak.id,
                OppgaveKilde.BEHANDLING,
                OppgaveType.FOERSTEGANGSBEHANDLING,
                null,
            )
        oppgaveService.tildelSaksbehandler(nyOppgave.id, "nysaksbehandler")
        oppgaveService.endrePaaVent(nyOppgave.id, merknad = "test", paavent = true, aarsak = PaaVentAarsak.ANNET)
        val oppgavePaaVent = oppgaveService.hentOppgave(nyOppgave.id)
        assertEquals(Status.PAA_VENT, oppgavePaaVent.status)
        verify {
            hendelser.sendMeldingForHendelsePaaVent(
                UUID.fromString(nyOppgave.referanse),
                BehandlingHendelseType.PAA_VENT,
                PaaVentAarsak.ANNET,
            )
        }

        oppgaveService.endrePaaVent(nyOppgave.id, merknad = "", paavent = false, aarsak = null)
        val oppgaveTattAvVent = oppgaveService.hentOppgave(oppgavePaaVent.id)
        assertEquals(Status.UNDER_BEHANDLING, oppgaveTattAvVent.status)
        verify {
            hendelser.sendMeldingForHendelseAvVent(
                UUID.fromString(nyOppgave.referanse),
                BehandlingHendelseType.AV_VENT,
            )
        }
    }

    @Test
    fun `kan ikke redigere frist tilbake i tid`() {
        val opprettetSak = sakSkrivDao.opprettSak("fnr", SakType.BARNEPENSJON, Enheter.AALESUND.enhetNr)
        val nyOppgave =
            oppgaveService.opprettOppgave(
                "referanse",
                opprettetSak.id,
                OppgaveKilde.BEHANDLING,
                OppgaveType.FOERSTEGANGSBEHANDLING,
                null,
            )
        oppgaveService.tildelSaksbehandler(nyOppgave.id, "nysaksbehandler")
        val nyFrist =
            Tidspunkt
                .now()
                .toLocalDatetimeUTC()
                .minusMonths(1L)
                .toTidspunkt()

        assertThrows<FristTilbakeITid> {
            oppgaveService.redigerFrist(nyOppgave.id, nyFrist)
        }
    }

    @Test
    fun `kan ikke redigere frist på en lukket oppgave`() {
        val opprettetSak = sakSkrivDao.opprettSak("fnr", SakType.BARNEPENSJON, Enheter.AALESUND.enhetNr)
        val nyOppgave =
            oppgaveService.opprettOppgave(
                "referanse",
                opprettetSak.id,
                OppgaveKilde.BEHANDLING,
                OppgaveType.FOERSTEGANGSBEHANDLING,
                null,
            )

        oppgaveDao.endreStatusPaaOppgave(nyOppgave.id, Status.FERDIGSTILT)
        assertThrows<OppgaveKanIkkeEndres> {
            oppgaveService.redigerFrist(
                oppgaveId = nyOppgave.id,
                frist =
                    Tidspunkt
                        .now()
                        .toLocalDatetimeUTC()
                        .plusMonths(1L)
                        .toTidspunkt(),
            )
        }
        val lagretOppgave = oppgaveService.hentOppgave(nyOppgave.id)
        assertEquals(nyOppgave.frist, lagretOppgave.frist)
    }

    @Test
    fun `Kan sende til attestering`() {
        val opprettetSak = sakSkrivDao.opprettSak("fnr", SakType.BARNEPENSJON, Enheter.AALESUND.enhetNr)
        val referanse = "referanse"
        val nyOppgave =
            oppgaveService.opprettOppgave(
                referanse,
                opprettetSak.id,
                OppgaveKilde.BEHANDLING,
                OppgaveType.FOERSTEGANGSBEHANDLING,
                null,
            )

        val saksbehandler1 = simpleSaksbehandler()
        oppgaveService.tildelSaksbehandler(nyOppgave.id, saksbehandler1.ident)

        val sakIdOgReferanse =
            oppgaveService.tilAttestering(
                referanse,
                OppgaveType.FOERSTEGANGSBEHANDLING,
                null,
            )

        val saksbehandlerOppgave = oppgaveService.hentOppgave(nyOppgave.id)
        assertNull(saksbehandlerOppgave.saksbehandler)
        assertEquals(saksbehandler1.ident, saksbehandlerOppgave.forrigeSaksbehandlerIdent)
        assertEquals(Status.ATTESTERING, saksbehandlerOppgave.status)
        assertEquals(referanse, sakIdOgReferanse.referanse)
    }

    @Test
    fun `Kan underkjenne behandling - oppgave`() {
        val opprettetSak = sakSkrivDao.opprettSak("fnr", SakType.BARNEPENSJON, Enheter.AALESUND.enhetNr)
        val referanse = "referanse"
        val nyOppgave =
            oppgaveService.opprettOppgave(
                referanse,
                opprettetSak.id,
                OppgaveKilde.BEHANDLING,
                OppgaveType.FOERSTEGANGSBEHANDLING,
                null,
            )

        val saksbehandler1 = simpleSaksbehandler()
        oppgaveService.tildelSaksbehandler(nyOppgave.id, saksbehandler1.ident)
        oppgaveService.tilAttestering(
            referanse,
            OppgaveType.FOERSTEGANGSBEHANDLING,
            null,
        )

        val underkjentOppgave = oppgaveService.tilUnderkjent(referanse, OppgaveType.FOERSTEGANGSBEHANDLING, null)

        assertEquals(Status.UNDERKJENT, underkjentOppgave.status)
        assertEquals(saksbehandler1.ident, underkjentOppgave.saksbehandler?.ident)
        assertNull(underkjentOppgave.forrigeSaksbehandlerIdent)
    }

    @Test
    fun `Kan ikke lukke oppgave hvis man ikke eier oppgaven under behandling`() {
        val opprettetSak = sakSkrivDao.opprettSak("fnr", SakType.BARNEPENSJON, Enheter.AALESUND.enhetNr)
        val referanse = "referanse"
        val nyOppgave =
            oppgaveService.opprettOppgave(
                referanse,
                opprettetSak.id,
                OppgaveKilde.BEHANDLING,
                OppgaveType.FOERSTEGANGSBEHANDLING,
                null,
            )

        val saksbehandler1 = "saksbehandler"
        oppgaveService.tildelSaksbehandler(nyOppgave.id, saksbehandler1)
        assertThrows<OppgaveTilhoererAnnenSaksbehandler> {
            oppgaveService.ferdigstillOppgave(
                nyOppgave.id,
                simpleSaksbehandler(ident = "Feilsaksbehandler"),
                null,
            )
        }
    }

    @Test
    fun `Skal ikke kunne attestere vedtak hvis ingen oppgaver er under behandling altså tildelt en saksbehandler`() {
        val opprettetSak = sakSkrivDao.opprettSak("fnr", SakType.BARNEPENSJON, Enheter.AALESUND.enhetNr)
        val referanse = "referanse"
        oppgaveService.opprettOppgave(
            referanse,
            opprettetSak.id,
            OppgaveKilde.BEHANDLING,
            OppgaveType.FOERSTEGANGSBEHANDLING,
            null,
        )

        assertThrows<ManglerOppgaveUnderBehandling> {
            oppgaveService.tilAttestering(
                referanse,
                OppgaveType.FOERSTEGANGSBEHANDLING,
                null,
            )
        }
    }

    @Test
    fun `kan ikke attestere uten at det finnes en oppgave på behandlingen`() {
        assertThrows<ManglerOppgaveUnderBehandling> {
            oppgaveService.tilAttestering(
                UUID.randomUUID().toString(),
                OppgaveType.FOERSTEGANGSBEHANDLING,
                null,
            )
        }
    }

    @Test
    fun `Skal ikke kunne attestere vedtak hvis det finnes flere oppgaver under behandling for behandlingen`() {
        val opprettetSak = sakSkrivDao.opprettSak("fnr", SakType.BARNEPENSJON, Enheter.AALESUND.enhetNr)
        val referanse = "referanse"
        val oppgaveEn =
            oppgaveService.opprettOppgave(
                referanse,
                opprettetSak.id,
                OppgaveKilde.BEHANDLING,
                OppgaveType.FOERSTEGANGSBEHANDLING,
                null,
            )

        val oppgaveTo =
            oppgaveService.opprettOppgave(
                referanse,
                opprettetSak.id,
                OppgaveKilde.BEHANDLING,
                OppgaveType.FOERSTEGANGSBEHANDLING,
                null,
            )
        val saksbehandler1 = simpleSaksbehandler()
        oppgaveService.tildelSaksbehandler(oppgaveEn.id, saksbehandler1.ident)
        oppgaveService.tildelSaksbehandler(oppgaveTo.id, saksbehandler1.ident)

        assertThrows<ForMangeOppgaverUnderBehandling> {
            oppgaveService.tilAttestering(
                referanse,
                OppgaveType.FOERSTEGANGSBEHANDLING,
                null,
            )
        }
    }

    @Test
    fun `kan ikke fjerne saksbehandler hvis oppgaven ikke finnes`() {
        val err =
            assertThrows<OppgaveIkkeFunnet> {
                oppgaveService.fjernSaksbehandler(UUID.randomUUID())
            }
        assertTrue(err.message!!.startsWith("Oppgaven finnes ikke"))
    }

    @Test
    fun `Skal kun få saker som ikke er adressebeskyttet tilbake hvis saksbehandler ikke har spesialroller`() {
        val opprettetSak = sakSkrivDao.opprettSak("fnr", SakType.BARNEPENSJON, Enheter.AALESUND.enhetNr)
        val nyOppgave =
            oppgaveService.opprettOppgave(
                "referanse",
                opprettetSak.id,
                OppgaveKilde.BEHANDLING,
                OppgaveType.FOERSTEGANGSBEHANDLING,
                null,
            )

        val adressebeskyttetSak = sakSkrivDao.opprettSak("fnr", SakType.BARNEPENSJON, Enheter.AALESUND.enhetNr)
        oppgaveService.opprettOppgave(
            "referanse",
            adressebeskyttetSak.id,
            OppgaveKilde.BEHANDLING,
            OppgaveType.FOERSTEGANGSBEHANDLING,
            null,
        )

        sakSkrivDao.oppdaterAdresseBeskyttelse(adressebeskyttetSak.id, AdressebeskyttelseGradering.STRENGT_FORTROLIG)

        val saksbehandlerRoller = generateSaksbehandlerMedRoller(AzureGroup.SAKSBEHANDLER)
        every { saksbehandler.enheter() } returns listOf(Enheter.AALESUND.enhetNr)
        every { saksbehandler.saksbehandlerMedRoller } returns saksbehandlerRoller

        val oppgaver = oppgaveService.finnOppgaverForBruker(saksbehandler, Status.entries.map { it.name })
        assertEquals(1, oppgaver.size)
        val oppgaveUtenbeskyttelse = oppgaver[0]
        assertEquals(nyOppgave.id, oppgaveUtenbeskyttelse.id)
        assertEquals(nyOppgave.sakId, opprettetSak.id)
    }

    @Test
    fun `Skal kunne endre enhet for oppgaver tilknyttet sak`() {
        val opprettetSak = sakSkrivDao.opprettSak("fnr", SakType.BARNEPENSJON, Enheter.AALESUND.enhetNr)
        oppgaveService.opprettOppgave(
            referanse = "referanse",
            sakId = opprettetSak.id,
            kilde = OppgaveKilde.BEHANDLING,
            type = OppgaveType.FOERSTEGANGSBEHANDLING,
            merknad = null,
        )

        val saksbehandlerMedRoller = generateSaksbehandlerMedRoller(AzureGroup.SAKSBEHANDLER)
        every { saksbehandler.enheter() } returns listOf(Enheter.AALESUND.enhetNr, Enheter.STEINKJER.enhetNr)
        every { saksbehandler.saksbehandlerMedRoller } returns saksbehandlerMedRoller

        val oppgaverUtenEndring = oppgaveService.finnOppgaverForBruker(saksbehandler, Status.entries.map { it.name })
        assertEquals(1, oppgaverUtenEndring.size)
        assertEquals(Enheter.AALESUND.enhetNr, oppgaverUtenEndring[0].enhet)

        oppgaveService.oppdaterEnhetForRelaterteOppgaver(
            listOf(
                SakMedEnhet(
                    oppgaverUtenEndring[0].sakId,
                    Enheter.STEINKJER.enhetNr,
                ),
            ),
        )
        val oppgaverMedEndring = oppgaveService.finnOppgaverForBruker(saksbehandler, Status.entries.map { it.name })

        assertEquals(1, oppgaverMedEndring.size)
        assertEquals(Enheter.STEINKJER.enhetNr, oppgaverMedEndring[0].enhet)
    }

    @Test
    fun `Endre enhet skal ikke endre status på oppgave hvis den ikke er under behandling`() {
        val opprettetSak = sakSkrivDao.opprettSak("fnr", SakType.BARNEPENSJON, Enheter.AALESUND.enhetNr)
        oppgaveService.opprettOppgave(
            referanse = UUID.randomUUID().toString(),
            sakId = opprettetSak.id,
            kilde = OppgaveKilde.BEHANDLING,
            type = OppgaveType.FOERSTEGANGSBEHANDLING,
            merknad = null,
        )

        val saksbehandlerMedRoller = generateSaksbehandlerMedRoller(AzureGroup.SAKSBEHANDLER)
        every { saksbehandler.enheter() } returns listOf(Enheter.AALESUND.enhetNr, Enheter.STEINKJER.enhetNr)
        every { saksbehandler.saksbehandlerMedRoller } returns saksbehandlerMedRoller

        val oppgaverUtenEndring = oppgaveService.finnOppgaverForBruker(saksbehandler, Status.entries.map { it.name })
        assertEquals(1, oppgaverUtenEndring.size)
        val oppgaveUtenEndring = oppgaverUtenEndring.first()
        assertEquals(Enheter.AALESUND.enhetNr, oppgaveUtenEndring.enhet)
        oppgaveService.tildelSaksbehandler(oppgaveUtenEndring.id, saksbehandlerMedRoller.saksbehandler.ident())
        oppgaveService.oppdaterStatusOgMerknad(oppgaveUtenEndring.id, "settes til ferdigstilt", Status.FERDIGSTILT)
        oppgaveService.oppdaterEnhetForRelaterteOppgaver(
            listOf(
                SakMedEnhet(
                    oppgaveUtenEndring.sakId,
                    Enheter.STEINKJER.enhetNr,
                ),
            ),
        )
        val oppgaverMedEndring = oppgaveService.finnOppgaverForBruker(saksbehandler, Status.entries.map { it.name })

        assertEquals(1, oppgaverMedEndring.size)
        assertEquals(Enheter.STEINKJER.enhetNr, oppgaverMedEndring.first().enhet)
        assertEquals(Status.FERDIGSTILT, oppgaverMedEndring.first().status)
    }

    @Test
    fun `Skal endre status til ny ved endring av enhet`() {
        val opprettetSak = sakSkrivDao.opprettSak("fnr", SakType.BARNEPENSJON, Enheter.AALESUND.enhetNr)
        oppgaveService.opprettOppgave(
            referanse = UUID.randomUUID().toString(),
            sakId = opprettetSak.id,
            kilde = OppgaveKilde.BEHANDLING,
            type = OppgaveType.FOERSTEGANGSBEHANDLING,
            merknad = null,
        )

        val saksbehandlerMedRoller = generateSaksbehandlerMedRoller(AzureGroup.SAKSBEHANDLER)
        every { saksbehandler.enheter() } returns listOf(Enheter.AALESUND.enhetNr, Enheter.STEINKJER.enhetNr)
        every { saksbehandler.saksbehandlerMedRoller } returns saksbehandlerMedRoller

        val oppgaverUtenEndring = oppgaveService.finnOppgaverForBruker(saksbehandler, Status.entries.map { it.name })
        assertEquals(1, oppgaverUtenEndring.size)
        assertEquals(Enheter.AALESUND.enhetNr, oppgaverUtenEndring[0].enhet)
        oppgaveService.tildelSaksbehandler(oppgaverUtenEndring[0].id, saksbehandlerMedRoller.saksbehandler.ident())
        oppgaveService.endrePaaVent(
            oppgaverUtenEndring[0].id,
            merknad = "test",
            paavent = true,
            aarsak = PaaVentAarsak.ANNET,
        )
        oppgaveService.oppdaterEnhetForRelaterteOppgaver(
            listOf(
                SakMedEnhet(
                    oppgaverUtenEndring[0].sakId,
                    Enheter.STEINKJER.enhetNr,
                ),
            ),
        )
        val oppgaverMedEndring = oppgaveService.finnOppgaverForBruker(saksbehandler, Status.entries.map { it.name })

        assertEquals(1, oppgaverMedEndring.size)
        assertEquals(Enheter.STEINKJER.enhetNr, oppgaverMedEndring[0].enhet)
        assertEquals(Status.NY, oppgaverMedEndring[0].status)
    }

    @Test
    fun `Skal kun få saker som  er strengt fotrolig tilbake hvis saksbehandler har rolle strengt fortrolig`() {
        val opprettetSak = sakSkrivDao.opprettSak("fnr", SakType.BARNEPENSJON, Enheter.AALESUND.enhetNr)
        oppgaveService.opprettOppgave(
            "referanse",
            opprettetSak.id,
            OppgaveKilde.BEHANDLING,
            OppgaveType.FOERSTEGANGSBEHANDLING,
            null,
        )

        val adressebeskyttetSak = sakSkrivDao.opprettSak("fnr", SakType.BARNEPENSJON, Enheter.AALESUND.enhetNr)
        val adressebeskyttetOppgave =
            oppgaveService.opprettOppgave(
                "referanse",
                adressebeskyttetSak.id,
                OppgaveKilde.BEHANDLING,
                OppgaveType.FOERSTEGANGSBEHANDLING,
                null,
            )

        sakSkrivDao.oppdaterAdresseBeskyttelse(adressebeskyttetSak.id, AdressebeskyttelseGradering.STRENGT_FORTROLIG)
        val saksbehandlerMedRollerStrengtFortrolig = generateSaksbehandlerMedRoller(AzureGroup.STRENGT_FORTROLIG)
        every { saksbehandler.enheter() } returns listOf(Enheter.STRENGT_FORTROLIG.enhetNr)
        every { saksbehandler.saksbehandlerMedRoller } returns saksbehandlerMedRollerStrengtFortrolig

        val oppgaver = oppgaveService.finnOppgaverForBruker(saksbehandler, Status.entries.map { it.name })
        assertEquals(1, oppgaver.size)
        val strengtFortroligOppgave = oppgaver[0]
        assertEquals(adressebeskyttetOppgave.id, strengtFortroligOppgave.id)
        assertEquals(adressebeskyttetOppgave.sakId, adressebeskyttetSak.id)
    }

    @Test
    fun `saksbehandler med attestant rolle skal få attestant oppgaver`() {
        val opprettetSak = sakSkrivDao.opprettSak("fnr", SakType.BARNEPENSJON, Enheter.AALESUND.enhetNr)
        val oppgave =
            oppgaveService.opprettOppgave(
                "referanse",
                opprettetSak.id,
                OppgaveKilde.BEHANDLING,
                OppgaveType.FOERSTEGANGSBEHANDLING,
                null,
            )

        oppgaveService.tildelSaksbehandler(oppgave.id, "saksbehandler")
        oppgaveService.tilAttestering("referanse", OppgaveType.FOERSTEGANGSBEHANDLING, "Klar for att.")

        val attestant =
            mockk<SaksbehandlerMedEnheterOgRoller> {
                every { enheter() } returns listOf(Enheter.AALESUND.enhetNr)
                every { saksbehandlerMedRoller } returns generateSaksbehandlerMedRoller(AzureGroup.ATTESTANT)
            }

        val attestantSaksbehandler = mockk<SaksbehandlerMedEnheterOgRoller> { every { name() } returns "ident" }
        nyKontekstMedBruker(attestantSaksbehandler)
        val attestantmedRoller = generateSaksbehandlerMedRoller(AzureGroup.ATTESTANT)
        mockForSaksbehandlerMedRoller(attestantSaksbehandler, attestantmedRoller)

        oppgaveService.tildelSaksbehandler(oppgave.id, attestantmedRoller.saksbehandler.ident)

        val oppgaver = oppgaveService.finnOppgaverForBruker(attestant, Status.entries.map { it.name })
        assertEquals(1, oppgaver.size)
        val attesteringsoppgave = oppgaver[0]
        assertEquals(oppgave.id, attesteringsoppgave.id)
        assertEquals(oppgave.sakId, attesteringsoppgave.sakId)
    }

    @Test
    fun `skal tracke at en tildeling av saksbehandler blir lagret med oppgaveendringer`() {
        val opprettetSak = sakSkrivDao.opprettSak("fnr", SakType.BARNEPENSJON, Enheter.AALESUND.enhetNr)
        val nyOppgave =
            oppgaveService.opprettOppgave(
                "referanse",
                opprettetSak.id,
                OppgaveKilde.BEHANDLING,
                OppgaveType.FOERSTEGANGSBEHANDLING,
                null,
            )
        val nysaksbehandler = "nysaksbehandler"
        oppgaveService.tildelSaksbehandler(nyOppgave.id, nysaksbehandler)

        val oppgaveMedNySaksbehandler = oppgaveService.hentOppgave(nyOppgave.id)
        assertEquals(nysaksbehandler, oppgaveMedNySaksbehandler.saksbehandler?.ident)

        val hentEndringerForOppgave = oppgaveDaoMedEndringssporing.hentEndringerForOppgave(nyOppgave.id)
        assertEquals(2, hentEndringerForOppgave.size)

        val endringTildelt = hentEndringerForOppgave[0]
        assertNull(endringTildelt.oppgaveFoer.saksbehandler)
        assertEquals("nysaksbehandler", endringTildelt.oppgaveEtter.saksbehandler?.ident)

        val endringStatus = hentEndringerForOppgave[1]
        assertEquals(Status.NY, endringStatus.oppgaveFoer.status)
        assertEquals(Status.UNDER_BEHANDLING, endringStatus.oppgaveEtter.status)
    }

    @Test
    fun `skal ferdigstille en oppgave hivs det finnes kun en som er under behandling`() {
        val opprettetSak = sakSkrivDao.opprettSak("fnr", SakType.BARNEPENSJON, Enheter.AALESUND.enhetNr)
        val behandlingsref = UUID.randomUUID().toString()
        val oppgave =
            oppgaveService.opprettOppgave(
                behandlingsref,
                opprettetSak.id,
                OppgaveKilde.BEHANDLING,
                OppgaveType.FOERSTEGANGSBEHANDLING,
                null,
            )

        val saksbehandler1 = simpleSaksbehandler()
        oppgaveService.tildelSaksbehandler(oppgave.id, saksbehandler1.ident)
        oppgaveService.ferdigStillOppgaveUnderBehandling(
            behandlingsref,
            OppgaveType.FOERSTEGANGSBEHANDLING,
            saksbehandler1,
        )
        val ferdigstiltOppgave = oppgaveService.hentOppgave(oppgave.id)
        assertEquals(Status.FERDIGSTILT, ferdigstiltOppgave.status)
    }

    @Test
    fun `Kan ikke ferdigstille oppgave under behandling om man ikke eier den`() {
        val opprettetSak = sakSkrivDao.opprettSak("fnr", SakType.BARNEPENSJON, Enheter.AALESUND.enhetNr)
        val behandlingsref = UUID.randomUUID().toString()
        val oppgave =
            oppgaveService.opprettOppgave(
                behandlingsref,
                opprettetSak.id,
                OppgaveKilde.BEHANDLING,
                OppgaveType.FOERSTEGANGSBEHANDLING,
                null,
            )

        val saksbehandler1 = "saksbehandler01"
        oppgaveService.tildelSaksbehandler(oppgave.id, saksbehandler1)
        assertThrows<OppgaveTilhoererAnnenSaksbehandler> {
            oppgaveService.ferdigStillOppgaveUnderBehandling(
                behandlingsref,
                OppgaveType.FOERSTEGANGSBEHANDLING,
                simpleSaksbehandler(ident = "feilSaksbehandler"),
            )
        }
    }

    @Test
    fun `skal lukke nye ikke ferdige eller feilregistrerte oppgaver hvis ny søknad kommer inn`() {
        val opprettetSak = sakSkrivDao.opprettSak("fnr", SakType.BARNEPENSJON, Enheter.AALESUND.enhetNr)
        val behandlingsref = UUID.randomUUID().toString()
        val oppgaveSomSkalBliAvbrutt =
            oppgaveService.opprettOppgave(
                behandlingsref,
                opprettetSak.id,
                OppgaveKilde.BEHANDLING,
                OppgaveType.FOERSTEGANGSBEHANDLING,
                null,
            )
        oppgaveService.tildelSaksbehandler(oppgaveSomSkalBliAvbrutt.id, "saksbehandler01")

        oppgaveService.opprettFoerstegangsbehandlingsOppgaveForInnsendtSoeknad(behandlingsref, opprettetSak.id)

        val alleOppgaver = oppgaveDao.hentOppgaverForReferanse(behandlingsref)
        assertEquals(2, alleOppgaver.size)
        val avbruttOppgave = oppgaveDao.hentOppgave(oppgaveSomSkalBliAvbrutt.id)!!
        assertEquals(avbruttOppgave.status, Status.AVBRUTT)
    }

    @Test
    fun `Skal filtrere bort oppgaver med annen enhet`() {
        nyKontekstMedBrukerOgDatabaseContext(saksbehandler, DatabaseContextTest(dataSource))

        val aalesundSak = sakSkrivDao.opprettSak("fnr", SakType.BARNEPENSJON, Enheter.AALESUND.enhetNr)
        val behandlingsref = UUID.randomUUID().toString()
        val oppgaveAalesund =
            oppgaveService.opprettOppgave(
                behandlingsref,
                aalesundSak.id,
                OppgaveKilde.BEHANDLING,
                OppgaveType.FOERSTEGANGSBEHANDLING,
                null,
            )
        val saksbehandlerid = "saksbehandler01"
        oppgaveService.tildelSaksbehandler(oppgaveAalesund.id, saksbehandlerid)

        val saksteinskjer = sakSkrivDao.opprettSak("fnr", SakType.BARNEPENSJON, Enheter.STEINKJER.enhetNr)
        val behrefsteinkjer = UUID.randomUUID().toString()
        val oppgavesteinskjer =
            oppgaveService.opprettOppgave(
                behrefsteinkjer,
                saksteinskjer.id,
                OppgaveKilde.BEHANDLING,
                OppgaveType.FOERSTEGANGSBEHANDLING,
                null,
            )

        oppgaveService.tildelSaksbehandler(oppgavesteinskjer.id, saksbehandlerid)

        val saksbehandlerMedRoller = generateSaksbehandlerMedRoller(AzureGroup.SAKSBEHANDLER)
        every { saksbehandler.enheter() } returns listOf(Enheter.AALESUND.enhetNr)
        every { saksbehandler.saksbehandlerMedRoller } returns saksbehandlerMedRoller

        val finnOppgaverForBruker = oppgaveService.finnOppgaverForBruker(saksbehandler, Status.entries.map { it.name })

        assertEquals(1, finnOppgaverForBruker.size)
        val aalesundfunnetOppgave = finnOppgaverForBruker[0]
        assertEquals(Enheter.AALESUND.enhetNr, aalesundfunnetOppgave.enhet)
    }

    @Test
    fun `kan hente saksbehandler på en oppgave tilknyttet behandling som er under arbeid`() {
        val opprettetSak = sakSkrivDao.opprettSak("fnr", SakType.BARNEPENSJON, Enheter.AALESUND.enhetNr)
        val behandlingId = UUID.randomUUID().toString()
        val nyOppgave =
            oppgaveService.opprettOppgave(
                behandlingId,
                opprettetSak.id,
                OppgaveKilde.BEHANDLING,
                OppgaveType.FOERSTEGANGSBEHANDLING,
                null,
            )
        val saksbehandlerIdent = "saksbehandler"

        oppgaveService.tildelSaksbehandler(nyOppgave.id, saksbehandlerIdent)

        val saksbehandlerHentet = oppgaveService.hentOppgaveUnderBehandling(behandlingId)?.saksbehandler

        assertEquals(saksbehandlerIdent, saksbehandlerHentet!!.ident)
    }

    @Test
    fun `kan hente saksbehandler på en oppgave fra revurdering`() {
        val opprettetSak = sakSkrivDao.opprettSak("fnr", SakType.BARNEPENSJON, Enheter.AALESUND.enhetNr)
        val revurderingId = UUID.randomUUID().toString()

        val nyOppgave =
            oppgaveService.opprettOppgave(
                revurderingId,
                opprettetSak.id,
                OppgaveKilde.BEHANDLING,
                OppgaveType.REVURDERING,
                null,
            )
        val saksbehandlerIdent = "saksbehandler"

        oppgaveService.tildelSaksbehandler(nyOppgave.id, saksbehandlerIdent)

        val saksbehandlerHentet = oppgaveService.hentOppgaveUnderBehandling(revurderingId)?.saksbehandler

        assertEquals(saksbehandlerIdent, saksbehandlerHentet!!.ident)
    }

    @Test
    fun `Skal kunne hente saksbehandler på oppgave for behandling selvom den er ferdigstilt med attestering`() {
        val opprettetSak = sakSkrivDao.opprettSak("fnr", SakType.BARNEPENSJON, Enheter.AALESUND.enhetNr)
        val behandlingId = UUID.randomUUID().toString()
        val foerstegangsbehandling =
            oppgaveService.opprettOppgave(
                behandlingId,
                opprettetSak.id,
                OppgaveKilde.BEHANDLING,
                OppgaveType.FOERSTEGANGSBEHANDLING,
                null,
            )
        val saksbehandler = simpleSaksbehandler()

        oppgaveService.tildelSaksbehandler(foerstegangsbehandling.id, saksbehandler.ident)

        opprettAttestantKontekst()

        oppgaveService.tilAttestering(behandlingId, OppgaveType.FOERSTEGANGSBEHANDLING, null)
        oppgaveService.tildelSaksbehandler(foerstegangsbehandling.id, "attestant")

        val saksbehandlerHentet = oppgaveService.hentOppgaveUnderBehandling(behandlingId)?.saksbehandler

        assertEquals("attestant", saksbehandlerHentet!!.ident)
    }

    @Test
    fun `Får null saksbehandler ved henting på behandling hvis saksbehandler ikke satt`() {
        val opprettetSak = sakSkrivDao.opprettSak("fnr", SakType.BARNEPENSJON, Enheter.AALESUND.enhetNr)
        val behandlingId = UUID.randomUUID().toString()
        oppgaveService.opprettOppgave(
            behandlingId,
            opprettetSak.id,
            OppgaveKilde.BEHANDLING,
            OppgaveType.FOERSTEGANGSBEHANDLING,
            null,
        )

        val saksbehandlerHentet = oppgaveService.hentOppgaveUnderBehandling(behandlingId)?.saksbehandler

        assertNull(saksbehandlerHentet?.ident)
    }

    @Test
    fun `skal kunne endre kilde og sette referanse paa tildelt oppgave som ikke er ferdigstilt`() {
        val opprettetSak = sakSkrivDao.opprettSak("fnr", SakType.BARNEPENSJON, Enheter.AALESUND.enhetNr)
        val oppgave =
            oppgaveService.opprettOppgave(
                "dummy",
                opprettetSak.id,
                OppgaveKilde.HENDELSE,
                OppgaveType.REVURDERING,
                "Aldersovergang",
            )

        oppgaveService.tildelSaksbehandler(oppgave.id, "Z123456")

        val nyReferanse = UUID.randomUUID().toString()
        oppgaveService.endreTilKildeBehandlingOgOppdaterReferanse(oppgaveId = oppgave.id, referanse = nyReferanse)

        with(oppgaveService.hentOppgave(oppgave.id)) {
            kilde shouldBe OppgaveKilde.BEHANDLING
            referanse shouldBe nyReferanse
        }
    }

    @Test
    fun `Oppgaven skal tildeles opprinnelig saksbehandler etter attestering`() {
        val saksbehandler = simpleSaksbehandler()
        val attestant = simpleAttestant()

        val behandlingId = UUID.randomUUID().toString()
        val opprettetSak = sakSkrivDao.opprettSak("123", SakType.OMSTILLINGSSTOENAD, Enheter.PORSGRUNN.enhetNr)
        val oppgave =
            oppgaveService.opprettOppgave(
                referanse = behandlingId,
                sakId = opprettetSak.id,
                kilde = OppgaveKilde.BEHANDLING,
                type = OppgaveType.FOERSTEGANGSBEHANDLING,
                merknad = null,
            )

        oppgaveService.tildelSaksbehandler(oppgave.id, saksbehandler.ident())

        val attestertOppgave =
            oppgaveService.tilAttestering(behandlingId, OppgaveType.FOERSTEGANGSBEHANDLING, "innvilget")
        assertNull(attestertOppgave.saksbehandler)

        opprettAttestantKontekst(attestant.ident())
        oppgaveService.tildelSaksbehandler(oppgave.id, attestant.ident())
        val oppgaveTilAttestering = oppgaveService.hentOppgave(oppgave.id)
        assertEquals(attestant.ident(), oppgaveTilAttestering.saksbehandler?.ident)

        val ferdigstiltOppgave =
            oppgaveService.ferdigStillOppgaveUnderBehandling(
                behandlingId,
                OppgaveType.FOERSTEGANGSBEHANDLING,
                attestant,
            )
        assertEquals(saksbehandler.ident(), ferdigstiltOppgave.saksbehandler?.ident)
    }

    private fun opprettAttestantKontekst(ident: String = "ident"): SaksbehandlerMedEnheterOgRoller {
        val attestantmock = mockk<SaksbehandlerMedEnheterOgRoller> { every { name() } returns ident }
        nyKontekstMedBruker(attestantmock)
        mockForSaksbehandlerMedRoller(attestantmock, generateSaksbehandlerMedRoller(AzureGroup.ATTESTANT))
        return attestantmock
    }

    @Test
    fun `kan oppdatere status uten aa ha tildelt`() {
        val behandlingId = UUID.randomUUID().toString()
        val opprettetSak = sakSkrivDao.opprettSak("123", SakType.OMSTILLINGSSTOENAD, Enheter.PORSGRUNN.enhetNr)
        val oppgave =
            oppgaveService.opprettOppgave(
                referanse = behandlingId,
                sakId = opprettetSak.id,
                kilde = OppgaveKilde.BEHANDLING,
                type = OppgaveType.FOERSTEGANGSBEHANDLING,
                merknad = null,
            )
        oppgaveService.oppdaterStatusOgMerknad(oppgaveId = oppgave.id, merknad = "", status = Status.PAA_VENT)
        oppgaveService.oppdaterStatusOgMerknad(oppgaveId = oppgave.id, merknad = "", status = Status.UNDER_BEHANDLING)
    }

    @Test
    fun `skal hente forrige status naar kun en statusendring er utfoert`() {
        val opprettetSak = sakSkrivDao.opprettSak("fnr", SakType.BARNEPENSJON, Enheter.AALESUND.enhetNr)
        val oppgave =
            oppgaveService.opprettOppgave(
                UUID.randomUUID().toString(),
                opprettetSak.id,
                OppgaveKilde.TILBAKEKREVING,
                OppgaveType.TILBAKEKREVING,
                null,
            )

        val oppdatertOppgave =
            oppgaveService.endrePaaVent(
                oppgaveId = oppgave.id,
                aarsak = PaaVentAarsak.ANNET,
                merknad = "merknad",
                paavent = true,
            )
        val forrigeStatus = oppgaveService.hentForrigeStatus(oppgave.id)

        oppdatertOppgave.status shouldBe Status.PAA_VENT
        forrigeStatus shouldBe Status.NY
    }

    @Test
    fun `skal hente forrige status hvis flere statusendringer er gjort`() {
        val opprettetSak = sakSkrivDao.opprettSak("fnr", SakType.BARNEPENSJON, Enheter.AALESUND.enhetNr)
        val oppgave =
            oppgaveService.opprettOppgave(
                UUID.randomUUID().toString(),
                opprettetSak.id,
                OppgaveKilde.TILBAKEKREVING,
                OppgaveType.TILBAKEKREVING,
                null,
            )

        oppgaveService.endrePaaVent(oppgave.id, true, "merknad", PaaVentAarsak.ANNET)
        oppgaveService.oppdaterStatusOgMerknad(oppgave.id, "en ny merknad", Status.UNDER_BEHANDLING)
        oppgaveService.oppdaterStatusOgMerknad(oppgave.id, "enda en ny merknad", Status.FERDIGSTILT)
        val oppdatertOppgave = oppgaveService.hentOppgave(oppgave.id)
        val forrigeStatus = oppgaveService.hentForrigeStatus(oppgave.id)

        oppdatertOppgave.status shouldBe Status.FERDIGSTILT
        forrigeStatus shouldBe Status.UNDER_BEHANDLING
    }
}
