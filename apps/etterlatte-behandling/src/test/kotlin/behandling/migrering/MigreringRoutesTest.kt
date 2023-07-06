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
import no.nav.etterlatte.libs.common.behandling.DetaljertBehandling
import no.nav.etterlatte.libs.common.behandling.Persongalleri
import no.nav.etterlatte.libs.common.person.Folkeregisteridentifikator
import no.nav.etterlatte.libs.common.sak.BehandlingOgSak
import no.nav.etterlatte.module
import no.nav.etterlatte.rapidsandrivers.migrering.Enhet
import no.nav.etterlatte.rapidsandrivers.migrering.MigreringRequest
import no.nav.etterlatte.rapidsandrivers.migrering.PesysId
import no.nav.etterlatte.rapidsandrivers.migrering.Trygdetidsgrunnlag
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.time.LocalDate
import java.time.YearMonth
import java.util.*

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
            application { module(applicationContext) }
            val fnr = Folkeregisteridentifikator.of("08071272487")
            val request = MigreringRequest(
                pesysId = PesysId("1"),
                enhet = Enhet("4817"),
                fnr = fnr,
                persongalleri = Persongalleri(fnr.value, "innsender", emptyList(), emptyList(), emptyList()),
                virkningstidspunkt = YearMonth.now(),
                trygdetidsgrunnlag = Trygdetidsgrunnlag(
                    "Norge",
                    LocalDate.now().minusYears(40),
                    LocalDate.now(),
                    "derfor"
                )
            )

            val response: BehandlingOgSak = client.post("/migrering") {
                addAuthToken(tokenSaksbehandler)
                contentType(ContentType.Application.Json)
                setBody(request)
            }.apply {
                Assertions.assertEquals(HttpStatusCode.Created, status)
            }.body()

            val behandling = client.get("/behandlinger/${response.behandlingId}") {
                addAuthToken(tokenSaksbehandler)
            }.apply {
                Assertions.assertEquals(HttpStatusCode.OK, status)
            }.body<DetaljertBehandling>()

            Assertions.assertNotNull(behandling.virkningstidspunkt)

            client.get("/saker/${behandling.sak}") {
                addAuthToken(tokenSaksbehandler)
            }.apply {
                Assertions.assertEquals(HttpStatusCode.OK, status)
            }
        }
    }
}