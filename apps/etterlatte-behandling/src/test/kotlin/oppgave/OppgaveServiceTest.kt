package no.nav.etterlatte.oppgave

import com.nimbusds.jwt.JWTClaimsSet
import io.kotest.matchers.shouldBe
import io.ktor.server.plugins.BadRequestException
import io.mockk.every
import io.mockk.mockk
import no.nav.etterlatte.ConnectionAutoclosingTest
import no.nav.etterlatte.Context
import no.nav.etterlatte.DatabaseContextTest
import no.nav.etterlatte.DatabaseExtension
import no.nav.etterlatte.Kontekst
import no.nav.etterlatte.SaksbehandlerMedEnheterOgRoller
import no.nav.etterlatte.SystemUser
import no.nav.etterlatte.User
import no.nav.etterlatte.common.Enheter
import no.nav.etterlatte.grunnlagsendring.GrunnlagsendringshendelseService
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.oppgave.OppgaveKilde
import no.nav.etterlatte.libs.common.oppgave.OppgaveType
import no.nav.etterlatte.libs.common.oppgave.SakIdOgReferanse
import no.nav.etterlatte.libs.common.oppgave.Status
import no.nav.etterlatte.libs.common.person.AdressebeskyttelseGradering
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.tidspunkt.toLocalDatetimeUTC
import no.nav.etterlatte.libs.common.tidspunkt.toTidspunkt
import no.nav.etterlatte.sak.SakDao
import no.nav.etterlatte.tilgangsstyring.AzureGroup
import no.nav.etterlatte.tilgangsstyring.SaksbehandlerMedRoller
import no.nav.etterlatte.token.BrukerTokenInfo
import no.nav.etterlatte.token.Fagsaksystem
import no.nav.etterlatte.token.Saksbehandler
import no.nav.security.token.support.core.jwt.JwtTokenClaims
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import java.util.UUID
import javax.sql.DataSource

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(DatabaseExtension::class)
internal class OppgaveServiceTest(val dataSource: DataSource) {
    private val sakDao: SakDao = SakDao(ConnectionAutoclosingTest(dataSource))
    private val oppgaveDao: OppgaveDao = OppgaveDaoImpl(ConnectionAutoclosingTest(dataSource))
    private val oppgaveDaoMedEndringssporing: OppgaveDaoMedEndringssporing =
        OppgaveDaoMedEndringssporingImpl(oppgaveDao, ConnectionAutoclosingTest(dataSource))
    private val oppgaveService: OppgaveService = OppgaveService(oppgaveDaoMedEndringssporing, sakDao)
    private val saksbehandlerRolleDev = "8bb9b8d1-f46a-4ade-8ee8-5895eccdf8cf"
    private val strengtfortroligDev = "5ef775f2-61f8-4283-bf3d-8d03f428aa14"
    private val attestantRolleDev = "63f46f74-84a8-4d1c-87a8-78532ab3ae60"
    private val saksbehandler = mockk<SaksbehandlerMedEnheterOgRoller>()

    private val azureGroupToGroupIDMap =
        mapOf(
            AzureGroup.SAKSBEHANDLER to saksbehandlerRolleDev,
            AzureGroup.ATTESTANT to attestantRolleDev,
            AzureGroup.STRENGT_FORTROLIG to strengtfortroligDev,
        )

    private fun generateSaksbehandlerMedRoller(azureGroup: AzureGroup): SaksbehandlerMedRoller {
        val groupId = azureGroupToGroupIDMap[azureGroup]!!
        val jwtclaimsSaksbehandler = JWTClaimsSet.Builder().claim("groups", groupId).build()
        return SaksbehandlerMedRoller(
            Saksbehandler("", azureGroup.name, JwtTokenClaims(jwtclaimsSaksbehandler)),
            mapOf(azureGroup to groupId),
        )
    }

    private fun setNewKontekstWithMockUser(userMock: User) {
        Kontekst.set(
            Context(
                userMock,
                DatabaseContextTest(dataSource),
            ),
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
        every { saksbehandler.enheter() } returns Enheter.enheterMedLesetilgang().toList()
        every { saksbehandler.name() } returns "ident"
        every { saksbehandler.erSuperbruker() } returns false

        setNewKontekstWithMockUser(saksbehandler)

        every { saksbehandler.saksbehandlerMedRoller } returns saksbehandlerRoller
    }

    @AfterEach
    fun afterEach() {
        dataSource.connection.use {
            it.prepareStatement("TRUNCATE oppgave CASCADE;").execute()
        }
    }

    @Test
    fun `skal kunne tildele oppgave uten saksbehandler`() {
        val opprettetSak = sakDao.opprettSak("fnr", SakType.BARNEPENSJON, Enheter.AALESUND.enhetNr)
        val nyOppgave =
            oppgaveService.opprettNyOppgaveMedSakOgReferanse(
                "referanse",
                opprettetSak.id,
                OppgaveKilde.BEHANDLING,
                OppgaveType.FOERSTEGANGSBEHANDLING,
                null,
            )
        val nysaksbehandler = "nysaksbehandler"
        oppgaveService.tildelSaksbehandler(nyOppgave.id, nysaksbehandler)

        val oppgaveMedNySaksbehandler = oppgaveService.hentOppgave(nyOppgave.id)
        assertEquals(nysaksbehandler, oppgaveMedNySaksbehandler?.saksbehandler?.ident)
    }

    @Test
    fun `skal kunne tildele seg oppgave som er tildelt systembruker`() {
        val systemBruker = mockk<SystemUser> { every { name() } returns Fagsaksystem.EY.navn }
        setNewKontekstWithMockUser(systemBruker)

        val opprettetSak = sakDao.opprettSak("fnr", SakType.BARNEPENSJON, Enheter.AALESUND.enhetNr)
        val referanse = "referanse"
        val nyOppgave =
            oppgaveService.opprettNyOppgaveMedSakOgReferanse(
                referanse,
                opprettetSak.id,
                OppgaveKilde.BEHANDLING,
                OppgaveType.FOERSTEGANGSBEHANDLING,
                null,
            )

        val bruker = Saksbehandler("", "ident1", null)
        oppgaveService.tildelSaksbehandler(nyOppgave.id, Fagsaksystem.EY.navn)
        assertDoesNotThrow { oppgaveService.tildelSaksbehandler(nyOppgave.id, bruker.ident()) }
    }

    @Test
    fun `skal tildele attesteringsoppgave hvis systembruker og fatte`() {
        val systemBruker = mockk<SystemUser> { every { name() } returns "name" }
        setNewKontekstWithMockUser(systemBruker)

        val opprettetSak = sakDao.opprettSak("fnr", SakType.BARNEPENSJON, Enheter.AALESUND.enhetNr)
        val referanse = "referanse"
        val nyOppgave =
            oppgaveService.opprettNyOppgaveMedSakOgReferanse(
                referanse,
                opprettetSak.id,
                OppgaveKilde.BEHANDLING,
                OppgaveType.FOERSTEGANGSBEHANDLING,
                null,
            )

        val systembruker = "systembruker"
        val systembrukerTokenInfo = BrukerTokenInfo.of("a", systembruker, "c", "c", null)
        oppgaveService.tildelSaksbehandler(nyOppgave.id, systembruker)

        val sakIdOgReferanse =
            oppgaveService.ferdigstillOppgaveUnderbehandlingOgLagNyMedType(
                SakIdOgReferanse(opprettetSak.id, referanse),
                OppgaveType.ATTESTERING,
                null,
                systembrukerTokenInfo,
            )

        oppgaveService.tildelSaksbehandler(sakIdOgReferanse.id, systembruker)
        val systembrukerOppgave = oppgaveService.hentOppgave(sakIdOgReferanse.id)
        assertEquals(systembruker, systembrukerOppgave?.saksbehandler!!.ident)
    }

    @Test
    fun `skal tildele attesteringsoppgave hvis rolle attestering finnes`() {
        val opprettetSak = sakDao.opprettSak("fnr", SakType.BARNEPENSJON, Enheter.AALESUND.enhetNr)
        val referanse = "referanse"
        val nyOppgave =
            oppgaveService.opprettNyOppgaveMedSakOgReferanse(
                referanse,
                opprettetSak.id,
                OppgaveKilde.BEHANDLING,
                OppgaveType.FOERSTEGANGSBEHANDLING,
                null,
            )

        val vanligSaksbehandler = saksbehandler.saksbehandlerMedRoller.saksbehandler
        oppgaveService.tildelSaksbehandler(nyOppgave.id, vanligSaksbehandler.ident)

        val sakIdOgReferanse =
            oppgaveService.ferdigstillOppgaveUnderbehandlingOgLagNyMedType(
                SakIdOgReferanse(opprettetSak.id, referanse),
                OppgaveType.ATTESTERING,
                null,
                vanligSaksbehandler,
            )

        val attestantSaksbehandler = mockk<SaksbehandlerMedEnheterOgRoller> { every { name() } returns "ident" }
        setNewKontekstWithMockUser(attestantSaksbehandler)
        val attestantmedRoller = generateSaksbehandlerMedRoller(AzureGroup.ATTESTANT)
        mockForSaksbehandlerMedRoller(attestantSaksbehandler, attestantmedRoller)

        oppgaveService.tildelSaksbehandler(sakIdOgReferanse.id, attestantmedRoller.saksbehandler.ident)
        val attestantTildeltOppgave = oppgaveService.hentOppgave(sakIdOgReferanse.id)
        assertEquals(attestantmedRoller.saksbehandler.ident, attestantTildeltOppgave?.saksbehandler!!.ident)
    }

    @Test
    fun `skal ikke tildele attesteringsoppgave hvis rolle saksbehandler`() {
        val opprettetSak = sakDao.opprettSak("fnr", SakType.BARNEPENSJON, Enheter.AALESUND.enhetNr)
        val referanse = "referanse"
        val nyOppgave =
            oppgaveService.opprettNyOppgaveMedSakOgReferanse(
                referanse,
                opprettetSak.id,
                OppgaveKilde.BEHANDLING,
                OppgaveType.FOERSTEGANGSBEHANDLING,
                null,
            )

        val vanligSaksbehandler = saksbehandler.saksbehandlerMedRoller.saksbehandler
        oppgaveService.tildelSaksbehandler(nyOppgave.id, vanligSaksbehandler.ident)

        val sakIdOgReferanse =
            oppgaveService.ferdigstillOppgaveUnderbehandlingOgLagNyMedType(
                SakIdOgReferanse(opprettetSak.id, referanse),
                OppgaveType.ATTESTERING,
                null,
                vanligSaksbehandler,
            )

        val saksbehandlerto = mockk<SaksbehandlerMedEnheterOgRoller> { every { name() } returns "ident" }
        setNewKontekstWithMockUser(saksbehandlerto)
        val saksbehandlerMedRoller = generateSaksbehandlerMedRoller(AzureGroup.SAKSBEHANDLER)
        mockForSaksbehandlerMedRoller(saksbehandlerto, saksbehandlerMedRoller)

        assertThrows<BrukerManglerAttestantRolleException> {
            oppgaveService.tildelSaksbehandler(sakIdOgReferanse.id, saksbehandlerMedRoller.saksbehandler.ident)
        }
    }

    @Test
    fun `skal ikke kunne tildele oppgave med saksbehandler felt satt`() {
        val opprettetSak = sakDao.opprettSak("fnr", SakType.BARNEPENSJON, Enheter.AALESUND.enhetNr)
        val nyOppgave =
            oppgaveService.opprettNyOppgaveMedSakOgReferanse(
                "referanse",
                opprettetSak.id,
                OppgaveKilde.BEHANDLING,
                OppgaveType.FOERSTEGANGSBEHANDLING,
                null,
            )
        val nysaksbehandler = "nysaksbehandler"
        oppgaveService.tildelSaksbehandler(nyOppgave.id, nysaksbehandler)
        assertThrows<OppgaveAlleredeTildeltSaksbehandler> {
            oppgaveService.tildelSaksbehandler(nyOppgave.id, "enda en")
        }
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
        val opprettetSak = sakDao.opprettSak("fnr", SakType.BARNEPENSJON, Enheter.AALESUND.enhetNr)
        val nyOppgave =
            oppgaveService.opprettNyOppgaveMedSakOgReferanse(
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
    fun `avbrytAapneOppgaverForBehandling setter alle åpne oppgaver for behandling til avbrutt`() {
        val sak = sakDao.opprettSak("fnr", SakType.BARNEPENSJON, Enheter.AALESUND.enhetNr)
        val behandlingsId = UUID.randomUUID().toString()
        val oppgaveBehandling =
            oppgaveService.opprettNyOppgaveMedSakOgReferanse(
                referanse = behandlingsId,
                sakId = sak.id,
                oppgaveKilde = OppgaveKilde.BEHANDLING,
                oppgaveType = OppgaveType.FOERSTEGANGSBEHANDLING,
                merknad = null,
            )
        val oppgaveAttestering =
            oppgaveService.opprettNyOppgaveMedSakOgReferanse(
                referanse = behandlingsId,
                sakId = sak.id,
                oppgaveKilde = OppgaveKilde.BEHANDLING,
                oppgaveType = OppgaveType.ATTESTERING,
                merknad = null,
            )
        oppgaveService.tildelSaksbehandler(oppgaveBehandling.id, "saksbehandler")
        oppgaveService.avbrytAapneOppgaverForBehandling(behandlingsId)
        val oppgaveBehandlingEtterAvbryt = oppgaveService.hentOppgave(oppgaveBehandling.id)
        val oppgaveAttesteringEtterAvbryt = oppgaveService.hentOppgave(oppgaveAttestering.id)

        assertEquals(Status.AVBRUTT, oppgaveBehandlingEtterAvbryt?.status)
        assertEquals(Status.AVBRUTT, oppgaveAttesteringEtterAvbryt?.status)
    }

    @Test
    fun `avbrytAapneOppgaverForBehandling endrer ikke avsluttede oppgaver eller oppgaver til andre behandlinger`() {
        val sak = sakDao.opprettSak("fnr", SakType.BARNEPENSJON, Enheter.AALESUND.enhetNr)
        val behandlingId = UUID.randomUUID().toString()
        val annenBehandlingId = UUID.randomUUID().toString()
        val saksbehandler = Saksbehandler("", "saksbehandler", null)

        val oppgaveFerdigstilt =
            oppgaveService.opprettNyOppgaveMedSakOgReferanse(
                referanse = behandlingId,
                sakId = sak.id,
                oppgaveKilde = OppgaveKilde.BEHANDLING,
                oppgaveType = OppgaveType.FOERSTEGANGSBEHANDLING,
                merknad = null,
            )
        oppgaveService.tildelSaksbehandler(oppgaveFerdigstilt.id, saksbehandler.ident)
        oppgaveService.ferdigStillOppgaveUnderBehandling(behandlingId, saksbehandler)

        val annenbehandlingfoerstegangs =
            oppgaveService.opprettNyOppgaveMedSakOgReferanse(
                referanse = annenBehandlingId,
                sakId = sak.id,
                oppgaveKilde = OppgaveKilde.BEHANDLING,
                oppgaveType = OppgaveType.FOERSTEGANGSBEHANDLING,
                merknad = null,
            )
        val saksbehandlerforstegangs = Saksbehandler("", "forstegangssaksbehandler", null)
        oppgaveService.tildelSaksbehandler(annenbehandlingfoerstegangs.id, saksbehandlerforstegangs.ident)
        oppgaveService.ferdigStillOppgaveUnderBehandling(annenBehandlingId, saksbehandlerforstegangs)
        val oppgaveUnderBehandlingAnnenBehandling =
            oppgaveService.opprettNyOppgaveMedSakOgReferanse(
                referanse = annenBehandlingId,
                sakId = sak.id,
                oppgaveKilde = OppgaveKilde.BEHANDLING,
                oppgaveType = OppgaveType.ATTESTERING,
                merknad = null,
            )

        val attestantmock = mockk<SaksbehandlerMedEnheterOgRoller> { every { name() } returns "ident" }
        setNewKontekstWithMockUser(attestantmock)
        mockForSaksbehandlerMedRoller(attestantmock, generateSaksbehandlerMedRoller(AzureGroup.ATTESTANT))
        oppgaveService.tildelSaksbehandler(oppgaveUnderBehandlingAnnenBehandling.id, saksbehandler.ident)
        oppgaveService.avbrytAapneOppgaverForBehandling(behandlingId)

        val oppgaveFerdigstiltEtterAvbryt = oppgaveService.hentOppgave(oppgaveFerdigstilt.id)
        val oppgaveUnderBehandlingEtterAvbryt = oppgaveService.hentOppgave(oppgaveUnderBehandlingAnnenBehandling.id)
        assertEquals(Status.FERDIGSTILT, oppgaveFerdigstiltEtterAvbryt?.status)
        assertEquals(Status.UNDER_BEHANDLING, oppgaveUnderBehandlingEtterAvbryt?.status)
    }

    @Test
    fun `skal kunne bytte oppgave med saksbehandler`() {
        val opprettetSak = sakDao.opprettSak("fnr", SakType.BARNEPENSJON, Enheter.AALESUND.enhetNr)
        val nyOppgave =
            oppgaveService.opprettNyOppgaveMedSakOgReferanse(
                "referanse",
                opprettetSak.id,
                OppgaveKilde.BEHANDLING,
                OppgaveType.FOERSTEGANGSBEHANDLING,
                null,
            )
        val nysaksbehandler = "nysaksbehandler"
        oppgaveService.byttSaksbehandler(nyOppgave.id, nysaksbehandler)

        val oppgaveMedNySaksbehandler = oppgaveService.hentOppgave(nyOppgave.id)
        assertEquals(nysaksbehandler, oppgaveMedNySaksbehandler?.saksbehandler?.ident)
    }

    @Test
    fun `skal ikke kunne bytte saksbehandler på lukket oppgave`() {
        val opprettetSak = sakDao.opprettSak("fnr", SakType.BARNEPENSJON, Enheter.AALESUND.enhetNr)
        val nyOppgave =
            oppgaveService.opprettNyOppgaveMedSakOgReferanse(
                "referanse",
                opprettetSak.id,
                OppgaveKilde.BEHANDLING,
                OppgaveType.FOERSTEGANGSBEHANDLING,
                null,
            )
        oppgaveDao.endreStatusPaaOppgave(nyOppgave.id, Status.FERDIGSTILT)
        val nysaksbehandler = "nysaksbehandler"
        assertThrows<OppgaveKanIkkeEndres> {
            oppgaveService.byttSaksbehandler(nyOppgave.id, nysaksbehandler)
        }
        val oppgaveMedNySaksbehandler = oppgaveService.hentOppgave(nyOppgave.id)
        assertEquals(nyOppgave.saksbehandler, oppgaveMedNySaksbehandler?.saksbehandler?.ident)
    }

    @Test
    fun `skal ikke kunne bytte saksbehandler på en ikke eksisterende sak`() {
        val nysaksbehandler = "nysaksbehandler"
        val err =
            assertThrows<OppgaveIkkeFunnet> {
                oppgaveService.byttSaksbehandler(UUID.randomUUID(), nysaksbehandler)
            }
        assertTrue(err.message!!.startsWith("Oppgaven finnes ikke"))
    }

    @Test
    fun `skal kunne fjerne saksbehandler fra oppgave`() {
        val opprettetSak = sakDao.opprettSak("fnr", SakType.BARNEPENSJON, Enheter.AALESUND.enhetNr)
        val nyOppgave =
            oppgaveService.opprettNyOppgaveMedSakOgReferanse(
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
        Assertions.assertNotNull(oppgaveUtenSaksbehandler?.id)
        Assertions.assertNull(oppgaveUtenSaksbehandler?.saksbehandler)
        assertEquals(Status.NY, oppgaveUtenSaksbehandler?.status)
    }

    @Test
    fun `kan ikke fjerne saksbehandler hvis det ikke er satt på oppgaven`() {
        val opprettetSak = sakDao.opprettSak("fnr", SakType.BARNEPENSJON, Enheter.AALESUND.enhetNr)
        val nyOppgave =
            oppgaveService.opprettNyOppgaveMedSakOgReferanse(
                "referanse",
                opprettetSak.id,
                OppgaveKilde.BEHANDLING,
                OppgaveType.FOERSTEGANGSBEHANDLING,
                null,
            )

        assertThrows<OppgaveIkkeTildeltSaksbehandler> {
            oppgaveService.fjernSaksbehandler(nyOppgave.id)
        }
    }

    @Test
    fun `skal ikke kunne fjerne saksbehandler på en lukket oppgave`() {
        val opprettetSak = sakDao.opprettSak("fnr", SakType.BARNEPENSJON, Enheter.AALESUND.enhetNr)
        val nyOppgave =
            oppgaveService.opprettNyOppgaveMedSakOgReferanse(
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

        assertEquals(lagretOppgave?.saksbehandler?.ident, saksbehandler)
    }

    @Test
    fun `kan redigere frist`() {
        val opprettetSak = sakDao.opprettSak("fnr", SakType.BARNEPENSJON, Enheter.AALESUND.enhetNr)
        val nyOppgave =
            oppgaveService.opprettNyOppgaveMedSakOgReferanse(
                "referanse",
                opprettetSak.id,
                OppgaveKilde.BEHANDLING,
                OppgaveType.FOERSTEGANGSBEHANDLING,
                null,
            )
        oppgaveService.tildelSaksbehandler(nyOppgave.id, "nysaksbehandler")
        val nyFrist = Tidspunkt.now().toLocalDatetimeUTC().plusMonths(4L).toTidspunkt()
        oppgaveService.redigerFrist(nyOppgave.id, nyFrist)
        val oppgaveMedNyFrist = oppgaveService.hentOppgave(nyOppgave.id)
        assertEquals(nyFrist, oppgaveMedNyFrist?.frist)
    }

    @Test
    fun `kan sette og fjerne oppgave paa vent`() {
        val opprettetSak = sakDao.opprettSak("fnr", SakType.BARNEPENSJON, Enheter.AALESUND.enhetNr)
        val nyOppgave =
            oppgaveService.opprettNyOppgaveMedSakOgReferanse(
                "referanse",
                opprettetSak.id,
                OppgaveKilde.BEHANDLING,
                OppgaveType.FOERSTEGANGSBEHANDLING,
                null,
            )
        oppgaveService.tildelSaksbehandler(nyOppgave.id, "nysaksbehandler")
        oppgaveService.oppdaterStatusOgMerknad(
            nyOppgave.id,
            "test",
            if (nyOppgave.status == Status.PAA_VENT) Status.UNDER_BEHANDLING else Status.PAA_VENT,
        )
        val oppgavePaaVent = oppgaveService.hentOppgave(nyOppgave.id)
        assertEquals(Status.PAA_VENT, oppgavePaaVent?.status)
        oppgaveService.oppdaterStatusOgMerknad(
            oppgavePaaVent!!.id,
            "test",
            if (oppgavePaaVent.status == Status.PAA_VENT) Status.UNDER_BEHANDLING else Status.PAA_VENT,
        )
        val oppgaveTattAvVent = oppgaveService.hentOppgave(oppgavePaaVent.id)
        assertEquals(Status.UNDER_BEHANDLING, oppgaveTattAvVent?.status)
    }

    @Test
    fun `kan ikke redigere frist tilbake i tid`() {
        val opprettetSak = sakDao.opprettSak("fnr", SakType.BARNEPENSJON, Enheter.AALESUND.enhetNr)
        val nyOppgave =
            oppgaveService.opprettNyOppgaveMedSakOgReferanse(
                "referanse",
                opprettetSak.id,
                OppgaveKilde.BEHANDLING,
                OppgaveType.FOERSTEGANGSBEHANDLING,
                null,
            )
        oppgaveService.tildelSaksbehandler(nyOppgave.id, "nysaksbehandler")
        val nyFrist = Tidspunkt.now().toLocalDatetimeUTC().minusMonths(1L).toTidspunkt()

        assertThrows<FristTilbakeITid> {
            oppgaveService.redigerFrist(nyOppgave.id, nyFrist)
        }
    }

    @Test
    fun `kan ikke redigere frist på en lukket oppgave`() {
        val opprettetSak = sakDao.opprettSak("fnr", SakType.BARNEPENSJON, Enheter.AALESUND.enhetNr)
        val nyOppgave =
            oppgaveService.opprettNyOppgaveMedSakOgReferanse(
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
                frist = Tidspunkt.now().toLocalDatetimeUTC().plusMonths(1L).toTidspunkt(),
            )
        }
        val lagretOppgave = oppgaveService.hentOppgave(nyOppgave.id)
        assertEquals(nyOppgave.frist, lagretOppgave?.frist)
    }

    @Test
    fun `Håndtering av vedtaksfatting`() {
        val opprettetSak = sakDao.opprettSak("fnr", SakType.BARNEPENSJON, Enheter.AALESUND.enhetNr)
        val referanse = "referanse"
        val nyOppgave =
            oppgaveService.opprettNyOppgaveMedSakOgReferanse(
                referanse,
                opprettetSak.id,
                OppgaveKilde.BEHANDLING,
                OppgaveType.FOERSTEGANGSBEHANDLING,
                null,
            )

        val saksbehandler1 = Saksbehandler("", "saksbehandler", null)
        oppgaveService.tildelSaksbehandler(nyOppgave.id, saksbehandler1.ident)

        val sakIdOgReferanse =
            oppgaveService.ferdigstillOppgaveUnderbehandlingOgLagNyMedType(
                SakIdOgReferanse(opprettetSak.id, referanse),
                OppgaveType.ATTESTERING,
                null,
                saksbehandler1,
            )

        val saksbehandlerOppgave = oppgaveService.hentOppgave(nyOppgave.id)
        assertEquals(Status.FERDIGSTILT, saksbehandlerOppgave?.status)
        assertEquals(OppgaveType.ATTESTERING, sakIdOgReferanse.type)
        assertEquals(referanse, sakIdOgReferanse.referanse)
    }

    @Test
    fun `Kan ikke lukke oppgave hvis man ikke eier oppgaven under behandling`() {
        val opprettetSak = sakDao.opprettSak("fnr", SakType.BARNEPENSJON, Enheter.AALESUND.enhetNr)
        val referanse = "referanse"
        val nyOppgave =
            oppgaveService.opprettNyOppgaveMedSakOgReferanse(
                referanse,
                opprettetSak.id,
                OppgaveKilde.BEHANDLING,
                OppgaveType.FOERSTEGANGSBEHANDLING,
                null,
            )

        val saksbehandler1 = "saksbehandler"
        oppgaveService.tildelSaksbehandler(nyOppgave.id, saksbehandler1)
        assertThrows<OppgaveTilhoererAnnenSaksbehandler> {
            oppgaveService.ferdigstillOppgaveUnderbehandlingOgLagNyMedType(
                SakIdOgReferanse(opprettetSak.id, referanse),
                OppgaveType.ATTESTERING,
                null,
                Saksbehandler("", "Feilsaksbehandler", null),
            )
        }
    }

    @Test
    fun `Skal ikke kunne attestere vedtak hvis ingen oppgaver er under behandling altså tildelt en saksbehandler`() {
        val opprettetSak = sakDao.opprettSak("fnr", SakType.BARNEPENSJON, Enheter.AALESUND.enhetNr)
        val referanse = "referanse"
        oppgaveService.opprettNyOppgaveMedSakOgReferanse(
            referanse,
            opprettetSak.id,
            OppgaveKilde.BEHANDLING,
            OppgaveType.FOERSTEGANGSBEHANDLING,
            null,
        )

        val err =
            assertThrows<BadRequestException> {
                oppgaveService.ferdigstillOppgaveUnderbehandlingOgLagNyMedType(
                    SakIdOgReferanse(opprettetSak.id, referanse),
                    OppgaveType.ATTESTERING,
                    null,
                    Saksbehandler("", "saksbehandler", null),
                )
            }

        assertTrue(
            err.message!!.startsWith("Det må finnes en oppgave under behandling, gjelder behandling:"),
        )
    }

    @Test
    fun `kan ikke attestere uten at det finnes en oppgave på behandlingen`() {
        val opprettetSak = sakDao.opprettSak("fnr", SakType.BARNEPENSJON, Enheter.AALESUND.enhetNr)
        val referanse = "referanse"

        val err =
            assertThrows<BadRequestException> {
                oppgaveService.ferdigstillOppgaveUnderbehandlingOgLagNyMedType(
                    SakIdOgReferanse(opprettetSak.id, referanse),
                    OppgaveType.ATTESTERING,
                    null,
                    Saksbehandler("", "saksbehandler", null),
                )
            }

        assertEquals(
            "Må ha en oppgave for å kunne lage attesteringsoppgave",
            err.message!!,
        )
    }

    @Test
    fun `Skal ikke kunne attestere vedtak hvis det finnes flere oppgaver under behandling for behandlingen`() {
        val opprettetSak = sakDao.opprettSak("fnr", SakType.BARNEPENSJON, Enheter.AALESUND.enhetNr)
        val referanse = "referanse"
        val oppgaveEn =
            oppgaveService.opprettNyOppgaveMedSakOgReferanse(
                referanse,
                opprettetSak.id,
                OppgaveKilde.BEHANDLING,
                OppgaveType.FOERSTEGANGSBEHANDLING,
                null,
            )

        val oppgaveTo =
            oppgaveService.opprettNyOppgaveMedSakOgReferanse(
                referanse,
                opprettetSak.id,
                OppgaveKilde.BEHANDLING,
                OppgaveType.FOERSTEGANGSBEHANDLING,
                null,
            )
        val saksbehandler1 = Saksbehandler("", "saksbehandler", null)
        oppgaveService.tildelSaksbehandler(oppgaveEn.id, saksbehandler1.ident)
        oppgaveService.tildelSaksbehandler(oppgaveTo.id, saksbehandler1.ident)

        val err =
            assertThrows<BadRequestException> {
                oppgaveService.ferdigstillOppgaveUnderbehandlingOgLagNyMedType(
                    SakIdOgReferanse(opprettetSak.id, referanse),
                    OppgaveType.ATTESTERING,
                    null,
                    saksbehandler1,
                )
            }

        assertTrue(
            err.message!!.startsWith("Skal kun ha en oppgave under behandling, gjelder behandling:"),
        )
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
        val opprettetSak = sakDao.opprettSak("fnr", SakType.BARNEPENSJON, Enheter.AALESUND.enhetNr)
        val nyOppgave =
            oppgaveService.opprettNyOppgaveMedSakOgReferanse(
                "referanse",
                opprettetSak.id,
                OppgaveKilde.BEHANDLING,
                OppgaveType.FOERSTEGANGSBEHANDLING,
                null,
            )

        val adressebeskyttetSak = sakDao.opprettSak("fnr", SakType.BARNEPENSJON, Enheter.AALESUND.enhetNr)
        oppgaveService.opprettNyOppgaveMedSakOgReferanse(
            "referanse",
            adressebeskyttetSak.id,
            OppgaveKilde.BEHANDLING,
            OppgaveType.FOERSTEGANGSBEHANDLING,
            null,
        )

        sakDao.oppdaterAdresseBeskyttelse(adressebeskyttetSak.id, AdressebeskyttelseGradering.STRENGT_FORTROLIG)

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
        val opprettetSak = sakDao.opprettSak("fnr", SakType.BARNEPENSJON, Enheter.AALESUND.enhetNr)
        oppgaveService.opprettNyOppgaveMedSakOgReferanse(
            referanse = "referanse",
            sakId = opprettetSak.id,
            oppgaveKilde = OppgaveKilde.BEHANDLING,
            oppgaveType = OppgaveType.FOERSTEGANGSBEHANDLING,
            merknad = null,
        )

        val saksbehandlerMedRoller = generateSaksbehandlerMedRoller(AzureGroup.SAKSBEHANDLER)
        every { saksbehandler.enheter() } returns listOf(Enheter.AALESUND.enhetNr, Enheter.STEINKJER.enhetNr)
        every { saksbehandler.saksbehandlerMedRoller } returns saksbehandlerMedRoller

        val oppgaverUtenEndring = oppgaveService.finnOppgaverForBruker(saksbehandler, Status.entries.map { it.name })
        assertEquals(1, oppgaverUtenEndring.size)
        assertEquals(Enheter.AALESUND.enhetNr, oppgaverUtenEndring[0].enhet)

        oppgaveService.oppdaterEnhetForRelaterteOppgaver(
            listOf(GrunnlagsendringshendelseService.SakMedEnhet(oppgaverUtenEndring[0].sakId, Enheter.STEINKJER.enhetNr)),
        )
        val oppgaverMedEndring = oppgaveService.finnOppgaverForBruker(saksbehandler, Status.entries.map { it.name })

        assertEquals(1, oppgaverMedEndring.size)
        assertEquals(Enheter.STEINKJER.enhetNr, oppgaverMedEndring[0].enhet)
    }

    @Test
    fun `Skal kun få saker som  er strengt fotrolig tilbake hvis saksbehandler har rolle strengt fortrolig`() {
        val opprettetSak = sakDao.opprettSak("fnr", SakType.BARNEPENSJON, Enheter.AALESUND.enhetNr)
        oppgaveService.opprettNyOppgaveMedSakOgReferanse(
            "referanse",
            opprettetSak.id,
            OppgaveKilde.BEHANDLING,
            OppgaveType.FOERSTEGANGSBEHANDLING,
            null,
        )

        val adressebeskyttetSak = sakDao.opprettSak("fnr", SakType.BARNEPENSJON, Enheter.AALESUND.enhetNr)
        val adressebeskyttetOppgave =
            oppgaveService.opprettNyOppgaveMedSakOgReferanse(
                "referanse",
                adressebeskyttetSak.id,
                OppgaveKilde.BEHANDLING,
                OppgaveType.FOERSTEGANGSBEHANDLING,
                null,
            )

        sakDao.oppdaterAdresseBeskyttelse(adressebeskyttetSak.id, AdressebeskyttelseGradering.STRENGT_FORTROLIG)
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
        val opprettetSak = sakDao.opprettSak("fnr", SakType.BARNEPENSJON, Enheter.AALESUND.enhetNr)
        oppgaveService.opprettNyOppgaveMedSakOgReferanse(
            "referanse",
            opprettetSak.id,
            OppgaveKilde.BEHANDLING,
            OppgaveType.FOERSTEGANGSBEHANDLING,
            null,
        )

        val attestantSak = sakDao.opprettSak("fnr", SakType.BARNEPENSJON, Enheter.AALESUND.enhetNr)
        val attestantOppgave =
            oppgaveService.opprettNyOppgaveMedSakOgReferanse(
                "referanse",
                attestantSak.id,
                OppgaveKilde.BEHANDLING,
                OppgaveType.ATTESTERING,
                null,
            )

        val saksbehandlerMedRollerAttestant = generateSaksbehandlerMedRoller(AzureGroup.ATTESTANT)
        every { saksbehandler.enheter() } returns listOf(Enheter.AALESUND.enhetNr)
        every { saksbehandler.saksbehandlerMedRoller } returns saksbehandlerMedRollerAttestant

        val oppgaver = oppgaveService.finnOppgaverForBruker(saksbehandler, Status.entries.map { it.name })
        assertEquals(1, oppgaver.size)
        val attesteringsoppgave = oppgaver[0]
        assertEquals(attestantOppgave.id, attesteringsoppgave.id)
        assertEquals(attestantOppgave.sakId, attestantSak.id)
    }

    @Test
    fun `Superbruker kan se oppgave på en annen enhet unntatt strengt fortrolig`() {
        val randomenhet = "1111"
        val opprettetSak = sakDao.opprettSak("fnr", SakType.BARNEPENSJON, randomenhet)
        oppgaveService.opprettNyOppgaveMedSakOgReferanse(
            "referanse",
            opprettetSak.id,
            OppgaveKilde.BEHANDLING,
            OppgaveType.FOERSTEGANGSBEHANDLING,
            null,
        )

        val attestantSak = sakDao.opprettSak("fnr", SakType.BARNEPENSJON, randomenhet)
        val attestantOppgave =
            oppgaveService.opprettNyOppgaveMedSakOgReferanse(
                "referanse",
                attestantSak.id,
                OppgaveKilde.BEHANDLING,
                OppgaveType.ATTESTERING,
                null,
            )

        val saksbehandlerMedRollerAttestant = generateSaksbehandlerMedRoller(AzureGroup.ATTESTANT)
        every { saksbehandler.enheter() } returns listOf(Enheter.AALESUND.enhetNr) // må ikke endres
        every { saksbehandler.erSuperbruker() } returns true
        every { saksbehandler.saksbehandlerMedRoller } returns saksbehandlerMedRollerAttestant

        val oppgaver = oppgaveService.finnOppgaverForBruker(saksbehandler, Status.entries.map { it.name })
        assertEquals(1, oppgaver.size)
        val attesteringsoppgave = oppgaver[0]
        assertEquals(attestantOppgave.id, attesteringsoppgave.id)
        assertEquals(attestantOppgave.sakId, attestantSak.id)
    }

    @Test
    fun `skal tracke at en tildeling av saksbehandler blir lagret med oppgaveendringer`() {
        val opprettetSak = sakDao.opprettSak("fnr", SakType.BARNEPENSJON, Enheter.AALESUND.enhetNr)
        val nyOppgave =
            oppgaveService.opprettNyOppgaveMedSakOgReferanse(
                "referanse",
                opprettetSak.id,
                OppgaveKilde.BEHANDLING,
                OppgaveType.FOERSTEGANGSBEHANDLING,
                null,
            )
        val nysaksbehandler = "nysaksbehandler"
        oppgaveService.tildelSaksbehandler(nyOppgave.id, nysaksbehandler)

        val oppgaveMedNySaksbehandler = oppgaveService.hentOppgave(nyOppgave.id)
        assertEquals(nysaksbehandler, oppgaveMedNySaksbehandler?.saksbehandler?.ident)

        val hentEndringerForOppgave = oppgaveDaoMedEndringssporing.hentEndringerForOppgave(nyOppgave.id)
        assertEquals(1, hentEndringerForOppgave.size)
        val endringPaaOppgave = hentEndringerForOppgave[0]
        Assertions.assertNull(endringPaaOppgave.oppgaveFoer.saksbehandler)
        assertEquals("nysaksbehandler", endringPaaOppgave.oppgaveEtter.saksbehandler?.ident)
        assertEquals(Status.NY, endringPaaOppgave.oppgaveFoer.status)
        assertEquals(Status.UNDER_BEHANDLING, endringPaaOppgave.oppgaveEtter.status)
    }

    @Test
    fun `skal ferdigstille en oppgave hivs det finnes kun en som er under behandling`() {
        val opprettetSak = sakDao.opprettSak("fnr", SakType.BARNEPENSJON, Enheter.AALESUND.enhetNr)
        val behandlingsref = UUID.randomUUID().toString()
        val oppgave =
            oppgaveService.opprettNyOppgaveMedSakOgReferanse(
                behandlingsref,
                opprettetSak.id,
                OppgaveKilde.BEHANDLING,
                OppgaveType.FOERSTEGANGSBEHANDLING,
                null,
            )

        val saksbehandler1 = Saksbehandler("", "saksbehandler01", null)
        oppgaveService.tildelSaksbehandler(oppgave.id, saksbehandler1.ident)
        oppgaveService.ferdigStillOppgaveUnderBehandling(behandlingsref, saksbehandler1)
        val ferdigstiltOppgave = oppgaveService.hentOppgave(oppgave.id)
        assertEquals(Status.FERDIGSTILT, ferdigstiltOppgave?.status)
    }

    @Test
    fun `Kan ikke ferdigstille oppgave under behandling om man ikke eier den`() {
        val opprettetSak = sakDao.opprettSak("fnr", SakType.BARNEPENSJON, Enheter.AALESUND.enhetNr)
        val behandlingsref = UUID.randomUUID().toString()
        val oppgave =
            oppgaveService.opprettNyOppgaveMedSakOgReferanse(
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
                Saksbehandler("", "feilSaksbehandler", null),
            )
        }
    }

    @Test
    fun `skal lukke nye ikke ferdige eller feilregistrerte oppgaver hvis ny søknad kommer inn`() {
        val opprettetSak = sakDao.opprettSak("fnr", SakType.BARNEPENSJON, Enheter.AALESUND.enhetNr)
        val behandlingsref = UUID.randomUUID().toString()
        val oppgaveSomSkalBliAvbrutt =
            oppgaveService.opprettNyOppgaveMedSakOgReferanse(
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
        Kontekst.set(
            Context(
                saksbehandler,
                DatabaseContextTest(dataSource),
            ),
        )

        val aalesundSak = sakDao.opprettSak("fnr", SakType.BARNEPENSJON, Enheter.AALESUND.enhetNr)
        val behandlingsref = UUID.randomUUID().toString()
        val oppgaveAalesund =
            oppgaveService.opprettNyOppgaveMedSakOgReferanse(
                behandlingsref,
                aalesundSak.id,
                OppgaveKilde.BEHANDLING,
                OppgaveType.FOERSTEGANGSBEHANDLING,
                null,
            )
        val saksbehandlerid = "saksbehandler01"
        oppgaveService.tildelSaksbehandler(oppgaveAalesund.id, saksbehandlerid)

        val saksteinskjer = sakDao.opprettSak("fnr", SakType.BARNEPENSJON, Enheter.STEINKJER.enhetNr)
        val behrefsteinkjer = UUID.randomUUID().toString()
        val oppgavesteinskjer =
            oppgaveService.opprettNyOppgaveMedSakOgReferanse(
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
        val opprettetSak = sakDao.opprettSak("fnr", SakType.BARNEPENSJON, Enheter.AALESUND.enhetNr)
        val behandlingId = UUID.randomUUID().toString()
        val nyOppgave =
            oppgaveService.opprettNyOppgaveMedSakOgReferanse(
                behandlingId,
                opprettetSak.id,
                OppgaveKilde.BEHANDLING,
                OppgaveType.FOERSTEGANGSBEHANDLING,
                null,
            )
        val saksbehandlerIdent = "saksbehandler"

        oppgaveService.tildelSaksbehandler(nyOppgave.id, saksbehandlerIdent)

        val saksbehandlerHentet =
            oppgaveService.hentSisteSaksbehandlerIkkeAttestertOppgave(behandlingId)

        assertEquals(saksbehandlerIdent, saksbehandlerHentet!!.ident)
    }

    @Test
    fun `Skal kaste om ingen oppgave som ikke er attestering finnes`() {
        val opprettetSak = sakDao.opprettSak("fnr", SakType.BARNEPENSJON, Enheter.AALESUND.enhetNr)
        val behandlingId = UUID.randomUUID().toString()
        oppgaveService.opprettNyOppgaveMedSakOgReferanse(
            behandlingId,
            opprettetSak.id,
            OppgaveKilde.BEHANDLING,
            OppgaveType.ATTESTERING,
            null,
        )

        assertThrows<ManglerSaksbehandlerException> {
            oppgaveService.hentSisteSaksbehandlerIkkeAttestertOppgave(behandlingId)
        }

        val attestertoppgave = oppgaveService.hentOppgaverForReferanse(behandlingId)
        assertEquals(1, attestertoppgave.size)
    }

    @Test
    fun `kan hente saksbehandler på en oppgave fra revurdering`() {
        val opprettetSak = sakDao.opprettSak("fnr", SakType.BARNEPENSJON, Enheter.AALESUND.enhetNr)
        val revurderingId = UUID.randomUUID().toString()
        val nyOppgave =
            oppgaveService.opprettNyOppgaveMedSakOgReferanse(
                revurderingId,
                opprettetSak.id,
                OppgaveKilde.BEHANDLING,
                OppgaveType.REVURDERING,
                null,
            )
        val saksbehandlerIdent = "saksbehandler"

        oppgaveService.tildelSaksbehandler(nyOppgave.id, saksbehandlerIdent)

        val saksbehandlerHentet =
            oppgaveService.hentSisteSaksbehandlerIkkeAttestertOppgave(revurderingId)

        assertEquals(saksbehandlerIdent, saksbehandlerHentet!!.ident)
    }

    @Test
    fun `Skal kunne hente saksbehandler på oppgave for behandling selvom den er ferdigstilt med attestering`() {
        val opprettetSak = sakDao.opprettSak("fnr", SakType.BARNEPENSJON, Enheter.AALESUND.enhetNr)
        val behandlingId = UUID.randomUUID().toString()
        val foerstegangsbehandling =
            oppgaveService.opprettNyOppgaveMedSakOgReferanse(
                behandlingId,
                opprettetSak.id,
                OppgaveKilde.BEHANDLING,
                OppgaveType.FOERSTEGANGSBEHANDLING,
                null,
            )
        val saksbehandler = Saksbehandler("", "saksbehandler", null)

        oppgaveService.tildelSaksbehandler(foerstegangsbehandling.id, saksbehandler.ident)
        oppgaveService.ferdigStillOppgaveUnderBehandling(behandlingId, saksbehandler)
        val attestertBehandlingsoppgave =
            oppgaveService.opprettNyOppgaveMedSakOgReferanse(
                behandlingId,
                opprettetSak.id,
                OppgaveKilde.BEHANDLING,
                OppgaveType.ATTESTERING,
                null,
            )

        val attestantmock = mockk<SaksbehandlerMedEnheterOgRoller> { every { name() } returns "ident" }
        setNewKontekstWithMockUser(attestantmock)
        mockForSaksbehandlerMedRoller(attestantmock, generateSaksbehandlerMedRoller(AzureGroup.ATTESTANT))
        oppgaveService.tildelSaksbehandler(attestertBehandlingsoppgave.id, "attestant")

        val saksbehandlerHentet =
            oppgaveService.hentSisteSaksbehandlerIkkeAttestertOppgave(behandlingId)

        assertEquals(saksbehandler.ident, saksbehandlerHentet!!.ident)
    }

    @Test
    fun `Får null saksbehandler ved henting på behandling hvis saksbehandler ikke satt`() {
        val opprettetSak = sakDao.opprettSak("fnr", SakType.BARNEPENSJON, Enheter.AALESUND.enhetNr)
        val behandlingId = UUID.randomUUID().toString()
        oppgaveService.opprettNyOppgaveMedSakOgReferanse(
            behandlingId,
            opprettetSak.id,
            OppgaveKilde.BEHANDLING,
            OppgaveType.FOERSTEGANGSBEHANDLING,
            null,
        )
        val saksbehandlerHentet =
            oppgaveService.hentSisteSaksbehandlerIkkeAttestertOppgave(behandlingId)

        Assertions.assertNull(saksbehandlerHentet?.ident)
    }

    @Test
    fun `skal kunne endre kilde og sette referanse paa tildelt oppgave som ikke er ferdigstilt`() {
        val opprettetSak = sakDao.opprettSak("fnr", SakType.BARNEPENSJON, Enheter.AALESUND.enhetNr)
        val oppgave =
            oppgaveService.opprettNyOppgaveMedSakOgReferanse(
                "dummy",
                opprettetSak.id,
                OppgaveKilde.HENDELSE,
                OppgaveType.REVURDERING,
                "Aldersovergang",
            )

        oppgaveService.tildelSaksbehandler(oppgave.id, "Z123456")

        val nyReferanse = UUID.randomUUID().toString()
        oppgaveService.endreTilKildeBehandlingOgOppdaterReferanse(oppgaveId = oppgave.id, referanse = nyReferanse)

        with(oppgaveService.hentOppgave(oppgave.id)!!) {
            kilde shouldBe OppgaveKilde.BEHANDLING
            referanse shouldBe nyReferanse
        }
    }
}
