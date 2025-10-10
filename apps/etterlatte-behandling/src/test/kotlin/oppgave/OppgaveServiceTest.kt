package no.nav.etterlatte.oppgave

import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import no.nav.etterlatte.ConnectionAutoclosingTest
import no.nav.etterlatte.DatabaseContextTest
import no.nav.etterlatte.DatabaseExtension
import no.nav.etterlatte.JOVIAL_LAMA
import no.nav.etterlatte.KONTANT_FOT
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
import no.nav.etterlatte.ktor.token.systembruker
import no.nav.etterlatte.libs.common.behandling.BehandlingHendelseType
import no.nav.etterlatte.libs.common.behandling.PaaVentAarsak
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.feilhaandtering.InternfeilException
import no.nav.etterlatte.libs.common.feilhaandtering.UgyldigForespoerselException
import no.nav.etterlatte.libs.common.oppgave.OppgaveIntern
import no.nav.etterlatte.libs.common.oppgave.OppgaveKilde
import no.nav.etterlatte.libs.common.oppgave.OppgaveType
import no.nav.etterlatte.libs.common.oppgave.Status
import no.nav.etterlatte.libs.common.person.AdressebeskyttelseGradering
import no.nav.etterlatte.libs.common.sak.SakId
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.tidspunkt.toLocalDatetimeUTC
import no.nav.etterlatte.libs.common.tidspunkt.toTidspunkt
import no.nav.etterlatte.libs.ktor.token.BrukerTokenInfo
import no.nav.etterlatte.libs.ktor.token.Claims
import no.nav.etterlatte.libs.testdata.grunnlag.SOEKER2_FOEDSELSNUMMER
import no.nav.etterlatte.libs.testdata.grunnlag.SOEKER_FOEDSELSNUMMER
import no.nav.etterlatte.nyKontekstMedBruker
import no.nav.etterlatte.nyKontekstMedBrukerOgDatabaseContext
import no.nav.etterlatte.sak.SakLesDao
import no.nav.etterlatte.sak.SakSkrivDao
import no.nav.etterlatte.sak.SakendringerDao
import no.nav.etterlatte.saksbehandler.SaksbehandlerService
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
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import java.util.UUID
import javax.sql.DataSource
import kotlin.random.Random

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(DatabaseExtension::class)
internal class OppgaveServiceTest(
    val dataSource: DataSource,
) {
    private val sakLesDao: SakLesDao = SakLesDao(ConnectionAutoclosingTest(dataSource))
    private val sakSkrivDao =
        SakSkrivDao(SakendringerDao(ConnectionAutoclosingTest(dataSource)))
    private val oppgaveDao: OppgaveDao = spyk(OppgaveDaoImpl(ConnectionAutoclosingTest(dataSource)))
    private val hendelser: BehandlingHendelserKafkaProducer = mockk(relaxed = true)
    private val hendelseDao = mockk<HendelseDao>()
    private val saksbehandlerService = mockk<SaksbehandlerService>()
    private val oppgaveDaoMedEndringssporing: OppgaveDaoMedEndringssporing =
        OppgaveDaoMedEndringssporingImpl(oppgaveDao, ConnectionAutoclosingTest(dataSource))
    private val oppgaveService: OppgaveService =
        OppgaveService(oppgaveDaoMedEndringssporing, sakLesDao, hendelseDao, hendelser, saksbehandlerService)
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

        val saksbehandlerto =
            mockk<SaksbehandlerMedEnheterOgRoller> { every { name() } returns vanligSaksbehandler.ident }
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

        val saksbehandlerto =
            mockk<SaksbehandlerMedEnheterOgRoller> { every { name() } returns vanligSaksbehandler.ident }
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

        val saksbehandlerto =
            mockk<SaksbehandlerMedEnheterOgRoller> { every { name() } returns vanligSaksbehandler.ident }
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
        oppgaveService.ferdigstillOppgaveUnderBehandling(
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
        oppgaveService.ferdigstillOppgaveUnderBehandling(
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
    fun `Underkjenne behandling skal ikke endre saksbehandler hvis forrige er EY`() {
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

        val systembruker = systembruker(mapOf(Claims.azp_name to "EY"))
        oppgaveService.tildelSaksbehandler(nyOppgave.id, systembruker.ident)
        oppgaveService.tilAttestering(
            referanse,
            OppgaveType.FOERSTEGANGSBEHANDLING,
            null,
        )

        val saksbehandler = simpleSaksbehandler()
        oppgaveService.tildelSaksbehandler(nyOppgave.id, saksbehandler.ident)
        val underkjentOppgave = oppgaveService.tilUnderkjent(referanse, OppgaveType.FOERSTEGANGSBEHANDLING, null)

        assertEquals(Status.UNDERKJENT, underkjentOppgave.status)
        assertEquals(saksbehandler.ident, underkjentOppgave.saksbehandler?.ident)
        assertEquals("EY", underkjentOppgave.forrigeSaksbehandlerIdent)
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

    @ParameterizedTest
    @EnumSource(OppgaveType::class)
    fun `oppgaver opprettes med standard frist hvis ikke angitt, og angitt frist ellers`(oppgaveType: OppgaveType) {
        val sak = sakSkrivDao.opprettSak("fnr", SakType.BARNEPENSJON, Enheter.defaultEnhet.enhetNr)
        val nyOppgaveUtenFrist =
            opprettOppgave(
                referanse = "",
                sakId = sak.id,
                kilde = OppgaveKilde.EKSTERN,
                type = oppgaveType,
                merknad = null,
                frist = null,
                saksbehandler = null,
                status = Status.NY,
            )
        if (oppgaveType == OppgaveType.JOURNALFOERING) {
            assertEquals(
                nyOppgaveUtenFrist.opprettet
                    .toLocalDatetimeUTC()
                    .plusDays(1L)
                    .toTidspunkt(),
                nyOppgaveUtenFrist.frist,
            )
        } else {
            assertEquals(
                nyOppgaveUtenFrist.opprettet
                    .toLocalDatetimeUTC()
                    .plusMonths(1L)
                    .toTidspunkt(),
                nyOppgaveUtenFrist.frist,
            )
        }
        val egenFrist = Tidspunkt.now()
        val nyOppgaveMedFrist =
            opprettOppgave(
                referanse = "",
                sakId = sak.id,
                kilde = OppgaveKilde.EKSTERN,
                type = oppgaveType,
                merknad = null,
                frist = egenFrist,
                saksbehandler = null,
                status = Status.NY,
            )
        assertEquals(egenFrist, nyOppgaveMedFrist.frist)
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
        val saksbehandlerRoller = generateSaksbehandlerMedRoller(AzureGroup.SAKSBEHANDLER)
        every { saksbehandler.enheter() } returns listOf(Enheter.AALESUND.enhetNr)
        every { saksbehandler.saksbehandlerMedRoller } returns saksbehandlerRoller

        val ikkeAdressebeskyttetSak = sakSkrivDao.opprettSak(SOEKER_FOEDSELSNUMMER.value, SakType.BARNEPENSJON, Enheter.AALESUND.enhetNr)
        val ikkeAdressebeskyttetOppgave =
            oppgaveService.opprettOppgave(
                "referanse1",
                ikkeAdressebeskyttetSak.id,
                OppgaveKilde.BEHANDLING,
                OppgaveType.FOERSTEGANGSBEHANDLING,
                null,
            )

        val adressebeskyttetSak = sakSkrivDao.opprettSak(SOEKER2_FOEDSELSNUMMER.value, SakType.BARNEPENSJON, Enheter.AALESUND.enhetNr)
        oppgaveService.opprettOppgave(
            "referanse",
            adressebeskyttetSak.id,
            OppgaveKilde.BEHANDLING,
            OppgaveType.FOERSTEGANGSBEHANDLING,
            null,
        )

        val sakMedEnhet = SakMedEnhet(adressebeskyttetSak.id, Enheter.STRENGT_FORTROLIG.enhetNr)
        sakSkrivDao.oppdaterAdresseBeskyttelse(adressebeskyttetSak.id, AdressebeskyttelseGradering.STRENGT_FORTROLIG)
        sakSkrivDao.oppdaterEnhet(sakMedEnhet)
        oppgaveService.oppdaterEnhetForRelaterteOppgaver(listOf(sakMedEnhet))

        val oppgaver = oppgaveService.finnOppgaverForBruker(saksbehandler, Status.entries.map { it.name })

        oppgaver shouldHaveSize 1
        ikkeAdressebeskyttetOppgave.id shouldBe oppgaver.first().id
        ikkeAdressebeskyttetOppgave.sakId shouldBe ikkeAdressebeskyttetSak.id
    }

    @Test
    fun `Skal få alle saker tilbake for enhet hvis saksbehandler har spesialroller`() {
        val saksbehandlerRoller = generateSaksbehandlerMedRoller(AzureGroup.SAKSBEHANDLER)
        every { saksbehandler.enheter() } returns listOf(Enheter.STRENGT_FORTROLIG_UTLAND.enhetNr)
        every { saksbehandler.saksbehandlerMedRoller } returns saksbehandlerRoller

        val ikkeAdressebeskyttetSak =
            sakSkrivDao.opprettSak(
                SOEKER_FOEDSELSNUMMER.value,
                SakType.BARNEPENSJON,
                Enheter.STRENGT_FORTROLIG_UTLAND.enhetNr,
            )
        oppgaveService.opprettOppgave(
            "referanse1",
            ikkeAdressebeskyttetSak.id,
            OppgaveKilde.BEHANDLING,
            OppgaveType.FOERSTEGANGSBEHANDLING,
            null,
        )

        val adressebeskyttetSak = sakSkrivDao.opprettSak(SOEKER2_FOEDSELSNUMMER.value, SakType.BARNEPENSJON, Enheter.AALESUND.enhetNr)
        oppgaveService.opprettOppgave(
            "referanse",
            adressebeskyttetSak.id,
            OppgaveKilde.BEHANDLING,
            OppgaveType.FOERSTEGANGSBEHANDLING,
            null,
        )

        val sakMedEnhet = SakMedEnhet(adressebeskyttetSak.id, Enheter.STRENGT_FORTROLIG.enhetNr)
        sakSkrivDao.oppdaterAdresseBeskyttelse(adressebeskyttetSak.id, AdressebeskyttelseGradering.STRENGT_FORTROLIG)
        sakSkrivDao.oppdaterEnhet(sakMedEnhet)
        oppgaveService.oppdaterEnhetForRelaterteOppgaver(listOf(sakMedEnhet))

        // Returnerer begge oppgavene, selv om den ene saken ikke har markert adressegradering
        val oppgaver = oppgaveService.finnOppgaverForBruker(saksbehandler, Status.entries.map { it.name })

        oppgaver shouldHaveSize 2
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
        val oppdatert = oppgaveService.hentOppgave(oppgaverUtenEndring[0].id)
        assertEquals(Status.UNDER_BEHANDLING, oppdatert.status)

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
    fun `attesteringsoppgave skal fortsette å være attesteringsoppgave selv om saken flyttes`() {
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
        val oppgaveUtenEndring = oppgaverUtenEndring.single()

        assertEquals(Enheter.AALESUND.enhetNr, oppgaveUtenEndring.enhet)
        oppgaveService.tildelSaksbehandler(oppgaveUtenEndring.id, saksbehandlerMedRoller.saksbehandler.ident())
        oppgaveService.tilAttestering(oppgaveUtenEndring.referanse, oppgaveUtenEndring.type, null)
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
        val oppgaveMedEndring = oppgaverMedEndring.single()
        assertEquals(Enheter.STEINKJER.enhetNr, oppgaveMedEndring.enhet)
        assertEquals(Status.ATTESTERING, oppgaveMedEndring.status)
        assertEquals(null, oppgaveMedEndring.saksbehandler)
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
        oppgaveService.ferdigstillOppgaveUnderBehandling(
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
            oppgaveService.ferdigstillOppgaveUnderBehandling(
                behandlingsref,
                OppgaveType.FOERSTEGANGSBEHANDLING,
                simpleSaksbehandler(ident = "feilSaksbehandler"),
            )
        }
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

        val saksbehandlerHentet = oppgaveService.hentOppgaveForAttesterbarBehandling(behandlingId)?.saksbehandler

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

        val saksbehandlerHentet = oppgaveService.hentOppgaveForAttesterbarBehandling(revurderingId)?.saksbehandler

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

        val saksbehandlerHentet = oppgaveService.hentOppgaveForAttesterbarBehandling(behandlingId)?.saksbehandler

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

        val saksbehandlerHentet = oppgaveService.hentOppgaveForAttesterbarBehandling(behandlingId)?.saksbehandler

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
        oppgaveService.endreTilKildeBehandlingOgOppdaterReferanseOgMerknad(oppgaveId = oppgave.id, referanse = nyReferanse)

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
            oppgaveService.ferdigstillOppgaveUnderBehandling(
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

    @ParameterizedTest
    @EnumSource(OppgaveType::class, names = ["AKTIVITETSPLIKT", "AKTIVITETSPLIKT_12MND"], mode = EnumSource.Mode.EXCLUDE)
    fun `kan ikke avbryte oppgave som ikke er aktivitetspliktoppgave`(oppgaveType: OppgaveType) {
        val brukerTokenInfo =
            mockk<BrukerTokenInfo> {
                every { ident() } returns "Z999999"
                every { kanEndreOppgaverFor(any()) } returns true
            }

        val opprettetSak = sakSkrivDao.opprettSak("123", SakType.OMSTILLINGSSTOENAD, Enheter.PORSGRUNN.enhetNr)
        val oppgave =
            oppgaveService.opprettOppgave(
                referanse = UUID.randomUUID().toString(),
                sakId = opprettetSak.id,
                kilde = OppgaveKilde.BEHANDLING,
                type = oppgaveType,
                merknad = null,
                saksbehandler = brukerTokenInfo.ident(),
            )

        assertThrows<InternfeilException> {
            oppgaveService.avbrytAktivitetspliktoppgave(oppgave.id, "merkand", brukerTokenInfo)
        }
    }

    @ParameterizedTest
    @EnumSource(OppgaveType::class, names = ["AKTIVITETSPLIKT", "AKTIVITETSPLIKT_12MND"], mode = EnumSource.Mode.INCLUDE)
    fun `kan avbryte aktivitetspliktoppgave med merknad `(oppgaveType: OppgaveType) {
        val brukerTokenInfo =
            mockk<BrukerTokenInfo> {
                every { ident() } returns "Z999999"
                every { kanEndreOppgaverFor(any()) } returns true
            }

        val opprettetSak = sakSkrivDao.opprettSak("123", SakType.OMSTILLINGSSTOENAD, Enheter.PORSGRUNN.enhetNr)
        val oppgave =
            oppgaveService.opprettOppgave(
                referanse = UUID.randomUUID().toString(),
                sakId = opprettetSak.id,
                kilde = OppgaveKilde.BEHANDLING,
                type = oppgaveType,
                merknad = null,
                saksbehandler = brukerTokenInfo.ident(),
            )

        val merkand = "oppgave avbrutt"
        oppgaveService.hentOppgave(oppgave.id).status shouldNotBe Status.AVBRUTT
        oppgaveService.avbrytAktivitetspliktoppgave(oppgave.id, merkand, brukerTokenInfo)

        val oppdatertOppgave = oppgaveService.hentOppgave(oppgave.id)
        oppdatertOppgave.status shouldBe Status.AVBRUTT
        oppdatertOppgave.merknad shouldBe merkand

        // negative ikke avbryte oppgave med status avbrutt
        assertThrows<InternfeilException> {
            oppgaveService.avbrytAktivitetspliktoppgave(oppgave.id, merkand, brukerTokenInfo)
        }

        // negative ikke avbryte oppgave med status ferdigstilt
        assertThrows<InternfeilException> {
            oppgaveService.ferdigstillOppgave(oppgave.id, brukerTokenInfo)
            oppgaveService.hentOppgave(oppgave.id).status shouldBe Status.FERDIGSTILT
            oppgaveService.avbrytAktivitetspliktoppgave(oppgave.id, merkand, brukerTokenInfo)
        }
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

    @Test
    fun `attesteringsoppgave satt på vent beholder status og merknad men når tas av vent blir under behandling`() {
        val saksbehandler = "Z123456"
        val attestant = "Z987654"
        val ident = KONTANT_FOT
        val sak =
            sakSkrivDao.opprettSak(
                fnr = ident.value,
                type = SakType.OMSTILLINGSSTOENAD,
                enhet = Enheter.defaultEnhet.enhetNr,
            )
        val behandlingId = UUID.randomUUID()

        val oppgave =
            opprettOppgave(sakId = sak.id, referanse = behandlingId.toString(), type = OppgaveType.REVURDERING, status = Status.NY)
        oppgaveService.tildelSaksbehandler(oppgaveId = oppgave.id, saksbehandler = saksbehandler)
        oppgaveService.tilAttestering(referanse = behandlingId.toString(), type = OppgaveType.REVURDERING, merknad = null)
        oppgaveService.tildelSaksbehandler(oppgaveId = oppgave.id, saksbehandler = attestant)
        oppgaveService.endrePaaVent(
            oppgaveId = oppgave.id,
            paavent = true,
            merknad = "Viktig merknad om hva vi venter på",
            aarsak = PaaVentAarsak.ANNET,
        )
        // Dette er oppgaven vi vil bevare tilstanden på
        val oppgavePaaVent = oppgaveService.hentOppgave(oppgave.id)

        // Tilbakestiller for saken
        oppgaveService.tilbakestillOppgaverUnderAttestering(listOf(sak.id))
        val oppgaveOppdatert = oppgaveService.hentOppgave(oppgave.id)
        assertEquals(oppgavePaaVent, oppgaveOppdatert)
        oppgaveService.endrePaaVent(oppgave.id, false, "", null)
        val oppgaveAvVent = oppgaveService.hentOppgave(oppgave.id)
        assertEquals(Status.UNDER_BEHANDLING, oppgaveAvVent.status)
    }

    @Test
    fun `Oppdater ident på oppgaver tilknyttet sak`() {
        val opprinneligIdent = KONTANT_FOT

        val sak =
            sakSkrivDao.opprettSak(
                fnr = opprinneligIdent.value,
                type = SakType.OMSTILLINGSSTOENAD,
                enhet = Enheter.defaultEnhet.enhetNr,
            )
        val sakId = sak.id

        // 5 oppgaver skal få ny ident
        opprettOppgave(sakId = sakId, type = OppgaveType.VURDER_KONSEKVENS, status = Status.NY)
        opprettOppgave(sakId = sakId, type = OppgaveType.VURDER_KONSEKVENS, status = Status.UNDER_BEHANDLING)
        opprettOppgave(sakId = sakId, type = OppgaveType.VURDER_KONSEKVENS, status = Status.UNDERKJENT)
        opprettOppgave(sakId = sakId, type = OppgaveType.VURDER_KONSEKVENS, status = Status.PAA_VENT)
        opprettOppgave(sakId = sakId, type = OppgaveType.VURDER_KONSEKVENS, status = Status.ATTESTERING)

        // 3 avsluttede oppgaver skal beholde gammel ident for historikk
        opprettOppgave(sakId = sakId, type = OppgaveType.VURDER_KONSEKVENS, status = Status.FEILREGISTRERT)
        opprettOppgave(sakId = sakId, type = OppgaveType.VURDER_KONSEKVENS, status = Status.FERDIGSTILT)
        opprettOppgave(sakId = sakId, type = OppgaveType.VURDER_KONSEKVENS, status = Status.AVBRUTT)

        val nyIdent = JOVIAL_LAMA
        sakSkrivDao.oppdaterIdent(sakId, nyIdent)

        val oppdatertSak = sakLesDao.hentSak(sakId)!!
        oppdatertSak.ident shouldBe nyIdent.value
        opprinneligIdent shouldNotBe nyIdent

        oppgaveService.oppdaterIdentForOppgaver(oppdatertSak)

        val oppgaver = oppgaveService.hentOppgaverForSak(sakId)

        oppgaver.size shouldBe 8

        oppgaver.forEach {
            if (it.erAvsluttet()) {
                it.fnr shouldBe opprinneligIdent.value
            } else {
                it.fnr shouldBe nyIdent.value
            }
        }
    }

    private fun opprettOppgave(
        referanse: String = UUID.randomUUID().toString(),
        sakId: SakId = SakId(Random.nextLong()),
        kilde: OppgaveKilde? = null,
        type: OppgaveType,
        merknad: String? = "en tilfeldig merknad",
        frist: Tidspunkt? = null,
        saksbehandler: String? = null,
        status: Status? = null,
    ): OppgaveIntern =
        oppgaveService
            .opprettOppgave(
                referanse = referanse,
                sakId = sakId,
                kilde = kilde,
                type = type,
                merknad = merknad,
                frist = frist,
                saksbehandler = saksbehandler,
            ).also {
                if (status != null) {
                    oppgaveService.oppdaterStatusOgMerknad(it.id, "ny merknad", status)
                }
            }
}
