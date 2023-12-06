package no.nav.etterlatte.oppgaveGosys

import com.nimbusds.jwt.JWTClaimsSet
import io.kotest.matchers.collections.shouldHaveSize
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.Context
import no.nav.etterlatte.DatabaseKontekst
import no.nav.etterlatte.Kontekst
import no.nav.etterlatte.SaksbehandlerMedEnheterOgRoller
import no.nav.etterlatte.User
import no.nav.etterlatte.common.Enheter
import no.nav.etterlatte.common.klienter.PdlKlient
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.tilgangsstyring.AzureGroup
import no.nav.etterlatte.tilgangsstyring.SaksbehandlerMedRoller
import no.nav.etterlatte.token.BrukerTokenInfo
import no.nav.etterlatte.token.Saksbehandler
import no.nav.security.token.support.core.jwt.JwtTokenClaims
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.sql.Connection
import java.time.LocalDate
import java.time.temporal.ChronoUnit

class GosysOppgaveServiceImplTest {
    private val gosysOppgaveKlient = mockk<GosysOppgaveKlient>()
    private val pdlKlient = mockk<PdlKlient>()
    private val brukerTokenInfo = mockk<BrukerTokenInfo>()

    private val service = GosysOppgaveServiceImpl(gosysOppgaveKlient, pdlKlient)
    private val saksbehandler = mockk<SaksbehandlerMedEnheterOgRoller>()

    private fun setNewKontekstWithMockUser(userMock: User) {
        Kontekst.set(
            Context(
                userMock,
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

    private val saksbehandlerRolleDev = "8bb9b8d1-f46a-4ade-8ee8-5895eccdf8cf"
    private val strengtfortroligDev = "5ef775f2-61f8-4283-bf3d-8d03f428aa14"
    private val attestantRolleDev = "63f46f74-84a8-4d1c-87a8-78532ab3ae60"

    val azureGroupToGroupIDMap =
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

    @BeforeEach
    fun beforeEach() {
        val saksbehandlerRoller = generateSaksbehandlerMedRoller(AzureGroup.SAKSBEHANDLER)
        every { saksbehandler.enheter() } returns Enheter.nasjonalTilgangEnheter()

        setNewKontekstWithMockUser(saksbehandler)

        every { saksbehandler.saksbehandlerMedRoller } returns saksbehandlerRoller
    }

    @Test
    fun `skal hente oppgaver og deretter folkeregisterIdent for unike identer`() {
        coEvery { gosysOppgaveKlient.hentOppgaver(any(), brukerTokenInfo) } returns
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
                            opprettetTidspunkt = Tidspunkt.now(),
                            tildeltEnhetsnr = Enheter.PORSGRUNN.enhetNr,
                            tilordnetRessurs = null,
                            aktoerId = "53771238272763",
                            beskrivelse = "Beskrivelse av oppgaven",
                            status = "NY",
                            fristFerdigstillelse = LocalDate.now().plusDays(7),
                        ),
                        GosysApiOppgave(
                            id = 2,
                            versjon = 4,
                            tema = "EYB",
                            behandlingstema = "",
                            oppgavetype = "",
                            opprettetTidspunkt = Tidspunkt.now().minus(5L, ChronoUnit.DAYS),
                            tildeltEnhetsnr = Enheter.PORSGRUNN.enhetNr,
                            tilordnetRessurs = "A123456",
                            aktoerId = "53771238272763",
                            beskrivelse = "Beskrivelse av oppgave med id 2",
                            status = "TIL_ATTESTERING",
                            fristFerdigstillelse = LocalDate.now().plusDays(14),
                        ),
                        GosysApiOppgave(
                            id = 3,
                            versjon = 1,
                            tema = "EYO",
                            behandlingstema = "",
                            oppgavetype = "",
                            opprettetTidspunkt = Tidspunkt.now().minus(3L, ChronoUnit.DAYS),
                            tildeltEnhetsnr = Enheter.PORSGRUNN.enhetNr,
                            tilordnetRessurs = null,
                            aktoerId = "78324720383742",
                            beskrivelse = "Omstillingsstønad oppgavebeskrivelse",
                            status = "NY",
                            fristFerdigstillelse = LocalDate.now().plusDays(4),
                        ),
                    ),
            )
        every { pdlKlient.hentFolkeregisterIdenterForAktoerIdBolk(setOf("53771238272763", "78324720383742")) } returns
            mapOf(
                "53771238272763" to "01010812345",
                "78324720383742" to "29048012345",
            )

        val resultat =
            runBlocking {
                service.hentOppgaver(brukerTokenInfo)
            }

        resultat shouldHaveSize 3
        resultat.filter { it.fnr == "01010812345" } shouldHaveSize 2
        resultat.filter { it.fnr == "29048012345" } shouldHaveSize 1
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
        every { saksbehandler.enheter() } returns Enheter.nasjonalTilgangEnheter()

        setNewKontekstWithMockUser(saksbehandler)

        every { saksbehandler.saksbehandlerMedRoller } returns saksbehandlerRoller

        coEvery { gosysOppgaveKlient.hentOppgaver(Enheter.STRENGT_FORTROLIG.enhetNr, brukerTokenInfo) } returns
            enhetsfiltrererGosysOppgaver(
                Enheter.STRENGT_FORTROLIG.enhetNr,
                listOf(
                    GosysApiOppgave(
                        id = 1,
                        versjon = 1,
                        tema = "EYB",
                        behandlingstema = "",
                        oppgavetype = "",
                        opprettetTidspunkt = Tidspunkt.now(),
                        tildeltEnhetsnr = Enheter.PORSGRUNN.enhetNr,
                        tilordnetRessurs = null,
                        aktoerId = "53771238272763",
                        beskrivelse = "Beskrivelse av oppgaven",
                        status = "NY",
                        fristFerdigstillelse = LocalDate.now().plusDays(7),
                    ),
                    GosysApiOppgave(
                        id = 2,
                        versjon = 4,
                        tema = "EYB",
                        behandlingstema = "",
                        oppgavetype = "",
                        opprettetTidspunkt = Tidspunkt.now().minus(5L, ChronoUnit.DAYS),
                        tildeltEnhetsnr = Enheter.PORSGRUNN.enhetNr,
                        tilordnetRessurs = "A123456",
                        aktoerId = "53771238272763",
                        beskrivelse = "Beskrivelse av oppgave med id 2",
                        status = "TIL_ATTESTERING",
                        fristFerdigstillelse = LocalDate.now().plusDays(14),
                    ),
                    GosysApiOppgave(
                        id = 3,
                        versjon = 1,
                        tema = "EYO",
                        behandlingstema = "",
                        oppgavetype = "",
                        opprettetTidspunkt = Tidspunkt.now().minus(3L, ChronoUnit.DAYS),
                        tildeltEnhetsnr = Enheter.STRENGT_FORTROLIG.enhetNr,
                        tilordnetRessurs = null,
                        aktoerId = "78324720383742",
                        beskrivelse = "Omstillingsstønad oppgavebeskrivelse",
                        status = "NY",
                        fristFerdigstillelse = LocalDate.now().plusDays(4),
                    ),
                ),
            )

        every { pdlKlient.hentFolkeregisterIdenterForAktoerIdBolk(setOf("78324720383742")) } returns
            mapOf(
                "78324720383742" to "29048012345",
            )

        val resultat =
            runBlocking {
                service.hentOppgaver(brukerTokenInfo)
            }

        resultat shouldHaveSize 1
        resultat.filter { it.fnr == "01010812345" } shouldHaveSize 0
        resultat.filter { it.fnr == "29048012345" } shouldHaveSize 1
    }

    @Test
    fun `kalle gosys-klient med riktige params`() {
        coEvery {
            gosysOppgaveKlient.tildelOppgaveTilSaksbehandler(
                oppgaveId = "123", oppgaveVersjon = 2L, tildeles = "A012345", brukerTokenInfo,
            )
        } returns Unit

        runBlocking {
            service.tildelOppgaveTilSaksbehandler(oppgaveId = "123", oppgaveVersjon = 2L, "A012345", brukerTokenInfo)
        }
    }
}
