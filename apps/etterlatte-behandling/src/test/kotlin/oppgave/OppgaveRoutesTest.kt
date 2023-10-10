package no.nav.etterlatte.oppgave

import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.serialization.jackson.jackson
import io.ktor.server.testing.testApplication
import io.mockk.every
import io.mockk.mockk
import no.nav.etterlatte.BehandlingIntegrationTest
import no.nav.etterlatte.funksjonsbrytere.FeatureToggleService
import no.nav.etterlatte.libs.common.FoedselsnummerDTO
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.oppgave.NyOppgaveDto
import no.nav.etterlatte.libs.common.oppgave.OppgaveIntern
import no.nav.etterlatte.libs.common.oppgave.OppgaveKilde
import no.nav.etterlatte.libs.common.oppgave.OppgaveListe
import no.nav.etterlatte.libs.common.oppgave.OppgaveType
import no.nav.etterlatte.libs.common.person.Folkeregisteridentifikator
import no.nav.etterlatte.libs.common.sak.Sak
import no.nav.etterlatte.module
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class OppgaveRoutesTest : BehandlingIntegrationTest() {
    @BeforeAll
    fun start() =
        startServer(
            // En enkel mock som skrur på alle toggles for verdikjedetesten
            // overstyr egne ønskede toggles her om behovet oppstår
            featureToggleService =
                mockk<FeatureToggleService> {
                    every { isEnabled(any(), any()) } returns true
                },
        )

    @AfterAll
    fun shutdown() = afterAll()

    @Test
    fun verdikjedetest() {
        val fnr = Folkeregisteridentifikator.of("08071272487").value

        testApplication {
            environment {
                config = hoconApplicationConfig
            }
            val client =
                createClient {
                    install(ContentNegotiation) {
                        jackson { registerModule(JavaTimeModule()) }
                    }
                }
            application { module(applicationContext) }

            val sak: Sak =
                client.post("/personer/saker/${SakType.BARNEPENSJON}") {
                    addAuthToken(tokenSaksbehandler)
                    contentType(ContentType.Application.Json)
                    setBody(FoedselsnummerDTO(fnr))
                }.apply {
                    assertEquals(HttpStatusCode.OK, status)
                }.body()

            client.post("/oppgaver/sak/${sak.id}/oppgaver") {
                val dto =
                    NyOppgaveDto("01018912345", OppgaveKilde.EKSTERN, OppgaveType.MANUELL_JOURNALFOERING, "abc")
                addAuthToken(systemBruker)
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                setBody(dto)
            }.also {
                assertEquals(HttpStatusCode.OK, it.status)
                val lestOppgave: OppgaveIntern = it.body()
                assertEquals(fnr, lestOppgave.fnr)
                assertEquals(OppgaveType.MANUELL_JOURNALFOERING, lestOppgave.type)
            }

            client.get("/api/oppgaver/sak/${sak.id}/oppgaver") {
                addAuthToken(systemBruker)
            }.also {
                assertEquals(HttpStatusCode.OK, it.status)
                val liste: OppgaveListe = it.body()
                assertEquals(1, liste.oppgaver.size)
                assertEquals(fnr, liste.oppgaver.first().fnr)
            }
        }
    }
}
