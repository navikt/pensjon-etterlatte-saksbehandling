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
import no.nav.etterlatte.libs.common.FoedselsnummerDTO
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.oppgave.NyOppgaveDto
import no.nav.etterlatte.libs.common.oppgave.OppgaveIntern
import no.nav.etterlatte.libs.common.oppgave.OppgaveKilde
import no.nav.etterlatte.libs.common.oppgave.OppgaveListe
import no.nav.etterlatte.libs.common.oppgave.OppgaveType
import no.nav.etterlatte.libs.common.oppgave.SaksbehandlerEndringDto
import no.nav.etterlatte.libs.common.oppgave.SettPaaVentRequest
import no.nav.etterlatte.libs.common.oppgave.Status
import no.nav.etterlatte.libs.common.sak.Sak
import no.nav.etterlatte.libs.testdata.grunnlag.AVDOED_FOEDSELSNUMMER
import no.nav.etterlatte.module
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

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
                runServerWithModule(server) {
                    module(applicationContext)
                }

            val sak: Sak =
                client.post("/personer/saker/${SakType.BARNEPENSJON}") {
                    addAuthToken(tokenSaksbehandler)
                    contentType(ContentType.Application.Json)
                    setBody(FoedselsnummerDTO(fnr))
                }.apply {
                    assertEquals(HttpStatusCode.OK, status)
                }.body()

            val oppgave =
                client.post("/oppgaver/sak/${sak.id}/opprett") {
                    val dto =
                        NyOppgaveDto(OppgaveKilde.EKSTERN, OppgaveType.JOURNALFOERING, "Mottatt journalpost", "12345")

                    addAuthToken(systemBruker)
                    header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    setBody(dto)
                }.let {
                    assertEquals(HttpStatusCode.OK, it.status)
                    val lestOppgave: OppgaveIntern = it.body()
                    assertEquals(fnr, lestOppgave.fnr)
                    assertEquals("12345", lestOppgave.referanse)
                    assertEquals(OppgaveType.JOURNALFOERING, lestOppgave.type)
                    lestOppgave
                }

            hentOppgaver(client, sak, fnr)

            client.post("/api/oppgaver/${oppgave.id}/tildel-saksbehandler") {
                val dto = SaksbehandlerEndringDto(systemBruker)
                addAuthToken(systemBruker)
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                setBody(dto)
            }.also {
                assertEquals(HttpStatusCode.OK, it.status)
            }

            client.post("/api/oppgaver/${oppgave.id}/sett-paa-vent") {
                val dto = SettPaaVentRequest("", Status.UNDER_BEHANDLING)
                addAuthToken(systemBruker)
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                setBody(dto)
            }.also {
                assertEquals(HttpStatusCode.OK, it.status)
            }

            client.put("/api/oppgaver/${oppgave.id}/sett-paa-vent") {
                val dto = SettPaaVentRequest("", Status.UNDER_BEHANDLING)
                addAuthToken(systemBruker)
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                setBody(dto)
            }.also {
                assertEquals(HttpStatusCode.OK, it.status)
            }

            assertEquals(hentOppgaver(client, sak, fnr).first().status, Status.PAA_VENT)

            client.put("/oppgaver/${oppgave.id}/ventefrist-gaar-ut") {
                val dto = SettPaaVentRequest("", Status.UNDER_BEHANDLING)
                addAuthToken(systemBruker)
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                setBody(dto)
            }.also {
                assertEquals(HttpStatusCode.OK, it.status)
            }
        }
    }

    private suspend fun hentOppgaver(
        client: HttpClient,
        sak: Sak,
        fnr: String,
    ): List<OppgaveIntern> =
        client.get("/api/oppgaver/sak/${sak.id}/oppgaver") {
            addAuthToken(systemBruker)
        }.let {
            assertEquals(HttpStatusCode.OK, it.status)
            val liste: OppgaveListe = it.body()
            assertEquals(1, liste.oppgaver.size)
            assertEquals(fnr, liste.oppgaver.first().fnr)
            liste.oppgaver
        }
}
