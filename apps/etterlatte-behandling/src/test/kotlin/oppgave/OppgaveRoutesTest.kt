package no.nav.etterlatte.oppgave

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.testing.testApplication
import no.nav.etterlatte.BehandlingIntegrationTest
import no.nav.etterlatte.funksjonsbrytere.DummyFeatureToggleService
import no.nav.etterlatte.ktor.runServerWithModule
import no.nav.etterlatte.libs.common.behandling.PaaVentAarsak
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.dbutils.Tidspunkt
import no.nav.etterlatte.libs.common.oppgave.NyOppgaveDto
import no.nav.etterlatte.libs.common.oppgave.OppgaveIntern
import no.nav.etterlatte.libs.common.oppgave.OppgaveKilde
import no.nav.etterlatte.libs.common.oppgave.OppgaveType
import no.nav.etterlatte.libs.common.oppgave.RedigerFristRequest
import no.nav.etterlatte.libs.common.oppgave.SaksbehandlerEndringDto
import no.nav.etterlatte.libs.common.oppgave.Status
import no.nav.etterlatte.libs.common.oppgave.VentefristGaarUtRequest
import no.nav.etterlatte.libs.common.sak.Sak
import no.nav.etterlatte.libs.ktor.route.FoedselsnummerDTO
import no.nav.etterlatte.libs.testdata.grunnlag.AVDOED_FOEDSELSNUMMER
import no.nav.etterlatte.module
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.util.UUID

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class OppgaveRoutesTest : BehandlingIntegrationTest() {
    @BeforeAll
    fun start() = startServer(featureToggleService = DummyFeatureToggleService())

    @AfterAll
    fun shutdown() = afterAll()

    @Test
    fun verdikjedetest() {
        val fnr = AVDOED_FOEDSELSNUMMER.value

        testApplication {
            val client =
                runServerWithModule(mockOAuth2Server) {
                    module(applicationContext)
                }

            val sak: Sak =
                client
                    .post("/personer/saker/${SakType.BARNEPENSJON}") {
                        addAuthToken(tokenSaksbehandler)
                        contentType(ContentType.Application.Json)
                        setBody(FoedselsnummerDTO(fnr))
                    }.apply {
                        assertEquals(HttpStatusCode.OK, status)
                    }.body()

            val referanse = UUID.randomUUID().toString()
            val oppgave =
                client
                    .post("/oppgaver/sak/${sak.id}/opprett") {
                        val dto =
                            NyOppgaveDto(OppgaveKilde.EKSTERN, OppgaveType.JOURNALFOERING, "Mottatt journalpost", referanse)

                        addAuthToken(this@OppgaveRoutesTest.systemBruker)
                        header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                        setBody(dto)
                    }.let {
                        assertEquals(HttpStatusCode.OK, it.status)
                        val lestOppgave: OppgaveIntern = it.body()
                        assertEquals(fnr, lestOppgave.fnr)
                        assertEquals(referanse, lestOppgave.referanse)
                        assertEquals(OppgaveType.JOURNALFOERING, lestOppgave.type)
                        lestOppgave
                    }

            hentOppgave(client, oppgave.id)

            client
                .post("/api/oppgaver/${oppgave.id}/tildel-saksbehandler") {
                    val dto = SaksbehandlerEndringDto(this@OppgaveRoutesTest.systemBruker)
                    addAuthToken(this@OppgaveRoutesTest.systemBruker)
                    header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    setBody(dto)
                }.also {
                    assertEquals(HttpStatusCode.OK, it.status)
                }

            client
                .put("/api/oppgaver/${oppgave.id}/frist") {
                    val dto = RedigerFristRequest(Tidspunkt.now().plus(28, ChronoUnit.DAYS))
                    addAuthToken(this@OppgaveRoutesTest.systemBruker)
                    header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    setBody(dto)
                }.also {
                    assertEquals(HttpStatusCode.OK, it.status)
                }

            client
                .post("/api/oppgaver/${oppgave.id}/sett-paa-vent") {
                    val dto = EndrePaaVentRequest(PaaVentAarsak.OPPLYSNING_FRA_ANDRE, "", true)
                    addAuthToken(this@OppgaveRoutesTest.systemBruker)
                    header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    setBody(dto)
                }.also {
                    assertEquals(HttpStatusCode.OK, it.status)
                }
            assertEquals(hentOppgave(client, oppgave.id).status, Status.PAA_VENT)

            client
                .put("/oppgaver/ventefrist-gaar-ut") {
                    val dto =
                        VentefristGaarUtRequest(
                            dato = LocalDate.now().plusMonths(3),
                            type = setOf(OppgaveType.JOURNALFOERING),
                            oppgaveKilde = setOf(OppgaveKilde.EKSTERN),
                            oppgaver = listOf(),
                        )
                    addAuthToken(this@OppgaveRoutesTest.systemBruker)
                    header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    setBody(dto)
                }.also {
                    assertEquals(HttpStatusCode.OK, it.status)
                }
            assertEquals(hentOppgave(client, oppgave.id).status, Status.UNDER_BEHANDLING)
        }
    }

    private suspend fun hentOppgave(
        client: HttpClient,
        oppgaveId: UUID,
    ): OppgaveIntern =
        client
            .get("/api/oppgaver/$oppgaveId") {
                addAuthToken(this@OppgaveRoutesTest.systemBruker)
            }.let {
                assertEquals(HttpStatusCode.OK, it.status)
                it.body()
            }
}
