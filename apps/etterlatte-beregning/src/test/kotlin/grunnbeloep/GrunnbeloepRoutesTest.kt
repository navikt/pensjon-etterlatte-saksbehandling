package no.nav.etterlatte.beregning.regler.grunnbeloep

import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import no.nav.etterlatte.grunnbeloep.Grunnbeloep
import no.nav.etterlatte.grunnbeloep.GrunnbeloepRepository
import no.nav.etterlatte.grunnbeloep.grunnbeloep
import no.nav.etterlatte.ktor.runServer
import no.nav.etterlatte.ktor.token.issueSaksbehandlerToken
import no.nav.etterlatte.no.nav.etterlatte.grunnbeloep.GrunnbeloepService
import no.nav.security.mock.oauth2.MockOAuth2Server
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class GrunnbeloepRoutesTest {
    private val server: MockOAuth2Server = MockOAuth2Server()

    @BeforeAll
    fun setup() {
        server.start()
    }

    @AfterAll
    fun teardown() {
        server.shutdown()
    }

    @Test
    fun `kan serialisere grunnbeloep`() {
        testApplication {
            val service = GrunnbeloepService(repository = GrunnbeloepRepository)
            val client =
                runServer(server) {
                    grunnbeloep(service)
                }

            val response =
                client.get("/api/beregning/grunnbeloep") {
                    header(HttpHeaders.Authorization, "Bearer ${server.issueSaksbehandlerToken()}")
                }
            Assertions.assertEquals(HttpStatusCode.OK, response.status)
            val grunnbeloep = response.body<Grunnbeloep>()
            Assertions.assertNotNull(grunnbeloep)
            Assertions.assertTrue(grunnbeloep.grunnbeloep >= 118620)
        }
    }
}
