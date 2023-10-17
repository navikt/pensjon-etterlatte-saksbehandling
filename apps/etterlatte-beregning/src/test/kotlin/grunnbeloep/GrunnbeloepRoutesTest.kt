package no.nav.etterlatte.beregning.regler.grunnbeloep

import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.jackson.jackson
import io.ktor.server.application.log
import io.ktor.server.config.HoconApplicationConfig
import io.ktor.server.testing.testApplication
import no.nav.etterlatte.grunnbeloep.Grunnbeloep
import no.nav.etterlatte.grunnbeloep.GrunnbeloepRepository
import no.nav.etterlatte.grunnbeloep.grunnbeloep
import no.nav.etterlatte.libs.ktor.AZURE_ISSUER
import no.nav.etterlatte.libs.ktor.restModule
import no.nav.etterlatte.no.nav.etterlatte.grunnbeloep.GrunnbeloepService
import no.nav.security.mock.oauth2.MockOAuth2Server
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import testsupport.buildTestApplicationConfigurationForOauth

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class GrunnbeloepRoutesTest {
    private val server: MockOAuth2Server = MockOAuth2Server()
    private lateinit var applicationConfig: HoconApplicationConfig
    private val clientId = "mock-client-id"

    @BeforeAll
    fun setup() {
        server.start()
        applicationConfig =
            buildTestApplicationConfigurationForOauth(server.config.httpServer.port(), AZURE_ISSUER, clientId)
    }

    @AfterAll
    fun teardown() {
        server.shutdown()
    }

    @Test
    fun `kan serialisere grunnbeloep`() {
        testApplication {
            environment { config = applicationConfig }
            val client =
                createClient {
                    install(ContentNegotiation) {
                        jackson { registerModule(JavaTimeModule()) }
                    }
                }
            val service = GrunnbeloepService(repository = GrunnbeloepRepository)
            application { restModule(log) { grunnbeloep(service) } }

            val response =
                client.get("/api/beregning/grunnbeloep") {
                    header(HttpHeaders.Authorization, "Bearer $token")
                }
            Assertions.assertEquals(HttpStatusCode.OK, response.status)
            val grunnbeloep = response.body<Grunnbeloep>()
            Assertions.assertNotNull(grunnbeloep)
            Assertions.assertTrue(grunnbeloep.grunnbeloep >= 118620)
        }
    }

    private val token: String by lazy {
        server.issueToken(
            issuerId = AZURE_ISSUER,
            audience = clientId,
            claims = mapOf("navn" to "John Doe", "NAVident" to "Saksbehandler01"),
        ).serialize()
    }
}
