package no.nav.etterlatte.oppgaveGosys

import io.kotest.matchers.collections.shouldHaveSize
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.SaksbehandlerMedEnheterOgRoller
import no.nav.etterlatte.azureAdAttestantClaim
import no.nav.etterlatte.azureAdSaksbehandlerClaim
import no.nav.etterlatte.azureAdStrengtFortroligClaim
import no.nav.etterlatte.behandling.randomSakId
import no.nav.etterlatte.common.Enheter
import no.nav.etterlatte.common.klienter.PdlTjenesterKlient
import no.nav.etterlatte.ktor.token.simpleSaksbehandler
import no.nav.etterlatte.libs.common.Enhetsnummer
import no.nav.etterlatte.libs.common.oppgave.OppgaveKilde
import no.nav.etterlatte.libs.common.oppgave.OppgaveType
import no.nav.etterlatte.libs.common.person.PdlIdentifikator
import no.nav.etterlatte.libs.common.sak.SakId
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.ktor.token.BrukerTokenInfo
import no.nav.etterlatte.libs.ktor.token.Claims
import no.nav.etterlatte.nyKontekstMedBruker
import no.nav.etterlatte.oppgave.OppgaveService
import no.nav.etterlatte.saksbehandler.Saksbehandler
import no.nav.etterlatte.saksbehandler.SaksbehandlerInfoDao
import no.nav.etterlatte.saksbehandler.SaksbehandlerService
import no.nav.etterlatte.tilgangsstyring.AzureGroup
import no.nav.etterlatte.tilgangsstyring.SaksbehandlerMedRoller
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.time.LocalDate
import java.time.LocalTime
import java.time.temporal.ChronoUnit
import kotlin.random.Random

@TestInstance(TestInstance.Lifecycle.PER_METHOD)
class GosysOppgaveServiceImplTest {
    private val gosysOppgaveKlient = mockk<GosysOppgaveKlient>()
    private val oppgaveService = mockk<OppgaveService>()
    private val sbident = "SB1234"
    private val brukerTokenInfo =
        mockk<BrukerTokenInfo> {
            every { ident() } returns sbident
        }
    private val saksbehandlerService =
        mockk<SaksbehandlerService> {
            every { hentKomplettSaksbehandler(sbident) } returns
                Saksbehandler(
                    sbident,
                    "Ola Nordmann",
                    listOf(Enheter.PORSGRUNN.enhetNr),
                    false,
                    listOf(Enheter.PORSGRUNN.enhetNr),
                    true,
                )
        }

    private val saksbehandlerInfoDao =
        mockk<SaksbehandlerInfoDao> {
            every { hentAlleSaksbehandlere() } returns emptyList()
        }
    private val pdltjenesterKlientMock = mockk<PdlTjenesterKlient>()
    private val service =
        GosysOppgaveServiceImpl(
            gosysOppgaveKlient,
            oppgaveService,
            saksbehandlerService,
            saksbehandlerInfoDao,
            pdltjenesterKlientMock,
        )

    private val saksbehandler = mockk<SaksbehandlerMedEnheterOgRoller>().also { every { it.name() } returns this::class.java.simpleName }

    val azureGroupToGroupIDMap =
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

    @BeforeEach
    fun beforeEach() {
        val saksbehandlerRoller = generateSaksbehandlerMedRoller(AzureGroup.SAKSBEHANDLER)
        every { saksbehandler.enheter() } returns Enheter.enheterForVanligSaksbehandlere()

        nyKontekstMedBruker(saksbehandler)

        every { saksbehandler.saksbehandlerMedRoller } returns saksbehandlerRoller
    }

    @AfterEach
    fun afterEach() {
        confirmVerified(saksbehandlerInfoDao, oppgaveService, saksbehandlerService, gosysOppgaveKlient)
        clearAllMocks()
    }

    @Test
    fun `skal hente oppgaver og deretter folkeregisterIdent for unike identer`() {
        val saksbehandlerRoller = generateSaksbehandlerMedRoller(AzureGroup.SAKSBEHANDLER)
        every { saksbehandler.enheter() } returns Enheter.enheterForVanligSaksbehandlere()
        every { saksbehandler.name() } returns "ident"

        nyKontekstMedBruker(saksbehandler)

        every { saksbehandler.saksbehandlerMedRoller } returns saksbehandlerRoller

        coEvery { gosysOppgaveKlient.hentOppgaver(null, null, listOf("EYO", "EYB"), any(), null, brukerTokenInfo) } returns
            GosysOppgaver(
                antallTreffTotalt = 3,
                oppgaver =
                    listOf(
                        GosysApiOppgave(
                            id = 1,
                            versjon = 1,
                            tema = "EYB",
                            behandlingstema = "",
                            oppgavetype = "",
                            journalpostId = null,
                            opprettetTidspunkt = Tidspunkt.now(),
                            tildeltEnhetsnr = Enheter.PORSGRUNN.enhetNr,
                            tilordnetRessurs = null,
                            beskrivelse = "Beskrivelse av oppgaven",
                            status = "NY",
                            fristFerdigstillelse = LocalDate.now().plusDays(7),
                            bruker = GosysOppgaveBruker("01010812345", GosysOppgaveBruker.Type.PERSON),
                        ),
                        GosysApiOppgave(
                            id = 2,
                            versjon = 4,
                            tema = "EYB",
                            behandlingstema = "",
                            oppgavetype = "",
                            journalpostId = null,
                            opprettetTidspunkt = Tidspunkt.now().minus(5L, ChronoUnit.DAYS),
                            tildeltEnhetsnr = Enheter.PORSGRUNN.enhetNr,
                            tilordnetRessurs = "A123456",
                            beskrivelse = "Beskrivelse av oppgave med id 2",
                            status = "TIL_ATTESTERING",
                            fristFerdigstillelse = LocalDate.now().plusDays(14),
                            bruker = GosysOppgaveBruker("01010812345", GosysOppgaveBruker.Type.PERSON),
                        ),
                        GosysApiOppgave(
                            id = 3,
                            versjon = 1,
                            tema = "EYO",
                            behandlingstema = "",
                            oppgavetype = "",
                            journalpostId = null,
                            opprettetTidspunkt = Tidspunkt.now().minus(3L, ChronoUnit.DAYS),
                            tildeltEnhetsnr = Enheter.PORSGRUNN.enhetNr,
                            tilordnetRessurs = null,
                            beskrivelse = "Omstillingsstønad oppgavebeskrivelse",
                            status = "NY",
                            fristFerdigstillelse = LocalDate.now().plusDays(4),
                            bruker = GosysOppgaveBruker("29048012345", GosysOppgaveBruker.Type.PERSON),
                        ),
                    ),
            )

        val resultat =
            runBlocking {
                service.hentOppgaver(null, null, null, null, brukerTokenInfo)
            }

        verify(exactly = 1) { saksbehandlerService.hentKomplettSaksbehandler(sbident) }
        coVerify(exactly = 1) {
            gosysOppgaveKlient.hentOppgaver(
                null,
                null,
                listOf("EYO", "EYB"),
                any(),
                null,
                brukerTokenInfo,
            )
        }
        verify(exactly = 1) { saksbehandlerInfoDao.hentAlleSaksbehandlere() }

        resultat shouldHaveSize 3
        resultat.filter { it.bruker?.ident == "01010812345" } shouldHaveSize 2
        resultat.filter { it.bruker?.ident == "29048012345" } shouldHaveSize 1
    }

    fun enhetsfiltrererGosysOppgaver(
        enhetsnr: Enhetsnummer,
        oppgaverFraGosys: List<GosysApiOppgave>,
    ): GosysOppgaver {
        val enhetsfiltrerteOppgaver = oppgaverFraGosys.filter { it.tildeltEnhetsnr == enhetsnr }
        return GosysOppgaver(antallTreffTotalt = enhetsfiltrerteOppgaver.size, oppgaver = enhetsfiltrerteOppgaver)
    }

    @Test
    fun `skal kun returnere vikafossen enhetsnummer relaterte oppgaver for ad-rolle strengt fortrolige`() {
        val saksbehandlerRoller = generateSaksbehandlerMedRoller(AzureGroup.STRENGT_FORTROLIG)
        every { saksbehandler.enheter() } returns listOf(Enheter.STRENGT_FORTROLIG.enhetNr)
        every { saksbehandler.name() } returns "ident"

        nyKontekstMedBruker(saksbehandler)

        every { saksbehandler.saksbehandlerMedRoller } returns saksbehandlerRoller

        coEvery {
            gosysOppgaveKlient.hentOppgaver(
                null,
                any(),
                any(),
                Enheter.STRENGT_FORTROLIG.enhetNr,
                null,
                brukerTokenInfo,
            )
        } returns
            enhetsfiltrererGosysOppgaver(
                Enheter.STRENGT_FORTROLIG.enhetNr,
                listOf(
                    GosysApiOppgave(
                        id = 1,
                        versjon = 1,
                        tema = "EYB",
                        behandlingstema = "",
                        oppgavetype = "",
                        journalpostId = null,
                        opprettetTidspunkt = Tidspunkt.now(),
                        tildeltEnhetsnr = Enheter.PORSGRUNN.enhetNr,
                        tilordnetRessurs = null,
                        beskrivelse = "Beskrivelse av oppgaven",
                        status = "NY",
                        fristFerdigstillelse = LocalDate.now().plusDays(7),
                        bruker = null,
                    ),
                    GosysApiOppgave(
                        id = 2,
                        versjon = 4,
                        tema = "EYB",
                        behandlingstema = "",
                        oppgavetype = "",
                        journalpostId = null,
                        opprettetTidspunkt = Tidspunkt.now().minus(5L, ChronoUnit.DAYS),
                        tildeltEnhetsnr = Enheter.PORSGRUNN.enhetNr,
                        tilordnetRessurs = "A123456",
                        beskrivelse = "Beskrivelse av oppgave med id 2",
                        status = "TIL_ATTESTERING",
                        fristFerdigstillelse = LocalDate.now().plusDays(14),
                        bruker = null,
                    ),
                    GosysApiOppgave(
                        id = 3,
                        versjon = 1,
                        tema = "EYO",
                        behandlingstema = "",
                        oppgavetype = "",
                        journalpostId = null,
                        opprettetTidspunkt = Tidspunkt.now().minus(3L, ChronoUnit.DAYS),
                        tildeltEnhetsnr = Enheter.STRENGT_FORTROLIG.enhetNr,
                        tilordnetRessurs = null,
                        beskrivelse = "Omstillingsstønad oppgavebeskrivelse",
                        status = "NY",
                        fristFerdigstillelse = LocalDate.now().plusDays(4),
                        bruker = GosysOppgaveBruker("29048012345", GosysOppgaveBruker.Type.PERSON),
                    ),
                ),
            )

        val resultat =
            runBlocking {
                service.hentOppgaver(null, null, null, null, brukerTokenInfo)
            }

        coVerify(exactly = 1) {
            gosysOppgaveKlient.hentOppgaver(
                null,
                any(),
                any(),
                Enheter.STRENGT_FORTROLIG.enhetNr,
                null,
                brukerTokenInfo,
            )
        }

        resultat shouldHaveSize 1
        resultat.filter { it.bruker?.ident == "01010812345" } shouldHaveSize 0
        resultat.filter { it.bruker?.ident == "29048012345" } shouldHaveSize 1
    }

    @Test
    fun `kalle gosys-klient med riktige params`() {
        val oppgaveId = "123"
        val tildeles = "A012345"
        val oppgaveVersjon = 2L
        coEvery {
            gosysOppgaveKlient.tildelOppgaveTilSaksbehandler(
                oppgaveId = oppgaveId,
                oppgaveVersjon = oppgaveVersjon,
                tildeles = tildeles,
                brukerTokenInfo,
            )
        } returns mockGosysOppgave("EYO", "GEN")

        runBlocking {
            service.tildelOppgaveTilSaksbehandler(oppgaveId = oppgaveId, oppgaveVersjon = oppgaveVersjon, tildeles, brukerTokenInfo)
        }
        coVerify { gosysOppgaveKlient.tildelOppgaveTilSaksbehandler(oppgaveId, oppgaveVersjon, tildeles, brukerTokenInfo) }
    }

    @Test
    fun `Flytt oppgave til Gjenny`() {
        val sakId = randomSakId()
        val gosysOppgave = mockGosysOppgave("EYO", "JFR", Random.nextLong().toString())
        val brukerTokenInfo = simpleSaksbehandler("Z123456")
        val request =
            FlyttOppgavetilGjennyRequest(
                sakid = sakId.toString().toLong(),
                enhetsnr = "9999",
            )

        coEvery { gosysOppgaveKlient.hentOppgave(any(), any()) } returns gosysOppgave
        coEvery { gosysOppgaveKlient.feilregistrer(any(), any(), any()) } returns gosysOppgave
        coEvery {
            oppgaveService.opprettOppgave(any(), any(), any(), any(), any(), any(), any())
        } returns mockk()

        runBlocking {
            service.flyttTilGjenny(gosysOppgave.id, request, brukerTokenInfo)
        }

        verify(exactly = 1) {
            oppgaveService.opprettOppgave(
                gosysOppgave.journalpostId!!,
                sakId,
                OppgaveKilde.SAKSBEHANDLER,
                OppgaveType.JOURNALFOERING,
                gosysOppgave.beskrivelse,
                Tidspunkt.ofNorskTidssone(gosysOppgave.fristFerdigstillelse!!, LocalTime.MIDNIGHT),
                brukerTokenInfo.ident(),
            )
        }
        coVerify(exactly = 1) { gosysOppgaveKlient.hentOppgave(any(), brukerTokenInfo) }
        coVerify(exactly = 1) {
            gosysOppgaveKlient.feilregistrer(
                id = gosysOppgave.id.toString(),
                request =
                    EndreStatusRequest(
                        versjon = gosysOppgave.versjon.toString(),
                        status = "FEILREGISTRERT",
                        beskrivelse = "Oppgave overført til Gjenny",
                        endretAvEnhetsnr = request.enhetsnr,
                    ),
                brukerTokenInfo = brukerTokenInfo,
            )
        }
        verify(exactly = 1) { saksbehandlerInfoDao.hentAlleSaksbehandlere() }
    }

    @Test
    fun `Hent oppgaver for person`() {
        val foedselsnummer = "ident"
        val aktoerId = Random.nextLong().toString()

        coEvery { pdltjenesterKlientMock.hentAktoerId(foedselsnummer) } returns PdlIdentifikator.AktoerId(aktoerId)
        coEvery { gosysOppgaveKlient.hentOppgaver(aktoerId, any(), any(), any(), any(), any()) } returns
            GosysOppgaver(
                1,
                listOf(mockGosysOppgave("EYB", "GEN", null, aktoerId)),
            )

        runBlocking {
            service.hentOppgaverForPerson(foedselsnummer, brukerTokenInfo)
        }

        coVerify(exactly = 1) {
            saksbehandlerInfoDao.hentAlleSaksbehandlere()
            pdltjenesterKlientMock.hentAktoerId(foedselsnummer)
            gosysOppgaveKlient.hentOppgaver(aktoerId, null, emptyList(), null, null, brukerTokenInfo)
        }
    }

    private fun mockGosysOppgave(
        tema: String,
        oppgavetype: String,
        journalpostId: String? = null,
        aktoerId: String? = null,
    ) = GosysApiOppgave(
        id = Random.nextLong(),
        versjon = Random.nextLong(),
        tema = tema,
        behandlingstema = "",
        oppgavetype = oppgavetype,
        journalpostId = journalpostId,
        opprettetTidspunkt = Tidspunkt.now().minus(3L, ChronoUnit.DAYS),
        tildeltEnhetsnr = Enheter.STEINKJER.enhetNr,
        tilordnetRessurs = "A012345",
        beskrivelse = "Beskrivelse for oppgaven",
        status = "OPPRETTET",
        fristFerdigstillelse = LocalDate.now().plusDays(4),
        bruker = aktoerId?.let { GosysOppgaveBruker(aktoerId, GosysOppgaveBruker.Type.PERSON) },
    )
}
