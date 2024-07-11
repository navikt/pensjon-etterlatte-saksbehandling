package no.nav.etterlatte.oppgaveGosys

import com.nimbusds.jwt.JWTClaimsSet
import io.kotest.matchers.collections.shouldHaveSize
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.SaksbehandlerMedEnheterOgRoller
import no.nav.etterlatte.azureAdAttestantClaim
import no.nav.etterlatte.azureAdSaksbehandlerClaim
import no.nav.etterlatte.azureAdStrengtFortroligClaim
import no.nav.etterlatte.common.Enheter
import no.nav.etterlatte.ktor.simpleSaksbehandlerMedIdent
import no.nav.etterlatte.libs.common.oppgave.OppgaveKilde
import no.nav.etterlatte.libs.common.oppgave.OppgaveType
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.ktor.token.BrukerTokenInfo
import no.nav.etterlatte.libs.ktor.token.Saksbehandler
import no.nav.etterlatte.nyKontekstMedBruker
import no.nav.etterlatte.oppgave.OppgaveService
import no.nav.etterlatte.saksbehandler.SaksbehandlerService
import no.nav.etterlatte.tilgangsstyring.AzureGroup
import no.nav.etterlatte.tilgangsstyring.SaksbehandlerMedRoller
import no.nav.security.token.support.core.jwt.JwtTokenClaims
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
                no.nav.etterlatte.saksbehandler.Saksbehandler(
                    sbident,
                    "Ola Nordmann",
                    listOf(Enheter.PORSGRUNN.enhetNr),
                    false,
                    listOf(Enheter.PORSGRUNN.enhetNr),
                    true,
                )
        }

    private val service = GosysOppgaveServiceImpl(gosysOppgaveKlient, oppgaveService, saksbehandlerService)
    private val saksbehandler = mockk<SaksbehandlerMedEnheterOgRoller>()

    val azureGroupToGroupIDMap =
        mapOf(
            AzureGroup.SAKSBEHANDLER to azureAdSaksbehandlerClaim,
            AzureGroup.ATTESTANT to azureAdAttestantClaim,
            AzureGroup.STRENGT_FORTROLIG to azureAdStrengtFortroligClaim,
        )

    private fun generateSaksbehandlerMedRoller(azureGroup: AzureGroup): SaksbehandlerMedRoller {
        val groupId = azureGroupToGroupIDMap[azureGroup]!!
        val jwtclaimsSaksbehandler = JWTClaimsSet.Builder().claim("groups", groupId).build()
        return SaksbehandlerMedRoller(
            Saksbehandler("", azureGroup.name, JwtTokenClaims(jwtclaimsSaksbehandler)),
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
        clearAllMocks()
    }

    @Test
    fun `skal hente oppgaver og deretter folkeregisterIdent for unike identer`() {
        val saksbehandlerRoller = generateSaksbehandlerMedRoller(AzureGroup.SAKSBEHANDLER)
        every { saksbehandler.enheter() } returns Enheter.enheterForVanligSaksbehandlere()
        every { saksbehandler.name() } returns "ident"

        nyKontekstMedBruker(saksbehandler)

        every { saksbehandler.saksbehandlerMedRoller } returns saksbehandlerRoller

        coEvery { gosysOppgaveKlient.hentOppgaver(null, listOf("EYO", "EYB"), any(), null, brukerTokenInfo) } returns
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

        resultat shouldHaveSize 3
        resultat.filter { it.bruker?.ident == "01010812345" } shouldHaveSize 2
        resultat.filter { it.bruker?.ident == "29048012345" } shouldHaveSize 1
    }

    fun enhetsfiltrererGosysOppgaver(
        enhetsnr: String,
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

        resultat shouldHaveSize 1
        resultat.filter { it.bruker?.ident == "01010812345" } shouldHaveSize 0
        resultat.filter { it.bruker?.ident == "29048012345" } shouldHaveSize 1
    }

    @Test
    fun `kalle gosys-klient med riktige params`() {
        coEvery {
            gosysOppgaveKlient.tildelOppgaveTilSaksbehandler(
                oppgaveId = "123",
                oppgaveVersjon = 2L,
                tildeles = "A012345",
                brukerTokenInfo,
            )
        } returns mockGosysOppgave("EYO", "GEN")

        runBlocking {
            service.tildelOppgaveTilSaksbehandler(oppgaveId = "123", oppgaveVersjon = 2L, "A012345", brukerTokenInfo)
        }
    }

    @Test
    fun `Flytt oppgave til Gjenny`() {
        val sakId = Random.nextLong()
        val gosysOppgave = mockGosysOppgave("EYO", "JFR", Random.nextLong().toString())
        val brukerTokenInfo = simpleSaksbehandlerMedIdent("Z123456")

        coEvery { gosysOppgaveKlient.hentOppgave(any(), any()) } returns gosysOppgave
        coEvery { gosysOppgaveKlient.feilregistrer(any(), any(), any()) } returns gosysOppgave
        coEvery {
            oppgaveService.opprettOppgave(any(), any(), any(), any(), any(), any(), any())
        } returns mockk()

        runBlocking {
            service.flyttTilGjenny(gosysOppgave.id, sakId, brukerTokenInfo)
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
        coVerify(exactly = 1) {
            gosysOppgaveKlient.feilregistrer(
                id = gosysOppgave.id.toString(),
                request =
                    EndreStatusRequest(
                        versjon = gosysOppgave.versjon.toString(),
                        status = "FEILREGISTRERT",
                        beskrivelse = "Oppgave overført til Gjenny",
                    ),
                brukerTokenInfo = brukerTokenInfo,
            )
        }
    }

    private fun mockGosysOppgave(
        tema: String,
        oppgavetype: String,
        journalpostId: String? = null,
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
        bruker = null,
    )
}
