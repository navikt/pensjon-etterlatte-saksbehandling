package behandling.migrering

import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.serialization.jackson.jackson
import io.ktor.server.testing.testApplication
import no.nav.etterlatte.BehandlingIntegrationTest
import no.nav.etterlatte.behandling.omregning.MigreringResponse
import no.nav.etterlatte.libs.common.behandling.Persongalleri
import no.nav.etterlatte.libs.common.person.Folkeregisteridentifikator
import no.nav.etterlatte.module
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import rapidsandrivers.migrering.Enhet
import rapidsandrivers.migrering.MigreringRequest
import rapidsandrivers.migrering.PesysId
import java.time.LocalDateTime

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MigreringRoutesTest : BehandlingIntegrationTest() {

    @BeforeAll
    fun start() = startServer()

    @AfterAll
    fun shutdown() = afterAll()

    @Test
    fun `migrering oppretter sak og behandling`() {
        testApplication {
            environment {
                config = hoconApplicationConfig
            }
            val client = createClient {
                install(ContentNegotiation) {
                    jackson { registerModule(JavaTimeModule()) }
                }
            }
            application { module(beanFactory) }
            val fnr = Folkeregisteridentifikator.of("08071272487")
            val request = MigreringRequest(
                pesysId = PesysId("1"),
                enhet = Enhet("4808"),
                fnr = fnr,
                mottattDato = LocalDateTime.now(),
                persongalleri = Persongalleri(fnr.value, "innsender", emptyList(), emptyList(), emptyList())
            )

            val response: MigreringResponse = client.post("/migrering") {
                addAuthToken(tokenSaksbehandler)
                contentType(ContentType.Application.Json)
                setBody(request)
            }.apply {
                Assertions.assertEquals(HttpStatusCode.Created, status)
            }.body()

            client.get("/saker/${response.sakId}") {
                addAuthToken(tokenSaksbehandler)
            }.apply {
                Assertions.assertEquals(HttpStatusCode.OK, status)
            }
        }
    }
}