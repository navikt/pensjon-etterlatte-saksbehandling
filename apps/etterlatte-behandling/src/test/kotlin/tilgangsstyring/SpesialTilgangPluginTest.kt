package tilgangsstyring

import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.testing.testApplication
import no.nav.etterlatte.ktor.runServer
import no.nav.etterlatte.ktor.startRandomPort
import no.nav.etterlatte.ktor.token.issueSaksbehandlerToken
import no.nav.etterlatte.libs.ktor.route.BEHANDLINGID_CALL_PARAMETER
import no.nav.etterlatte.tilgangsstyring.SpesialTilgangPlugin
import no.nav.etterlatte.tilgangsstyring.TILGANG_ROUTE_PATH
import no.nav.etterlatte.tilgangsstyring.erTilgangsrute
import no.nav.security.mock.oauth2.MockOAuth2Server
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import java.util.UUID

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SpesialTilgangPluginTest {
    private val mockOAuth2Server = MockOAuth2Server()
    private val behandlingId = UUID.randomUUID()

    @BeforeAll
    fun before() {
        mockOAuth2Server.startRandomPort()
    }

    @AfterAll
    fun after() {
        mockOAuth2Server.shutdown()
    }

    @ParameterizedTest(name = "{0} skal avvises når saksbehandler mangler tilgang")
    @ValueSource(
        strings = [
            "/api/behandling/{id}",
            "/api/behandling/{id}?x=tilgang",
            "/api/behandling/{id}?tilgang=true",
            "/api/behandling/{id}/tilgang",
        ],
    )
    fun `plugin kan ikke omgaas via query eller path`(sti: String) {
        assertEquals(HttpStatusCode.Forbidden, kall(sti.replace("{id}", behandlingId.toString())))
    }

    @Test
    fun `plugin kan ikke omgaas med path traversal fra tilgang-prefiks`() {
        assertNotEquals(HttpStatusCode.OK, kall("/tilgang/../api/behandling/$behandlingId"))
    }

    @Test
    fun `tilgangsrutene slipper gjennom pluginen`() {
        assertEquals(HttpStatusCode.OK, kall("/$TILGANG_ROUTE_PATH/behandling/$behandlingId"))
    }

    @Test
    fun `erTilgangsrute matcher kun foerste path-segment`() {
        assertTrue(erTilgangsrute("/tilgang/behandling/$behandlingId"))
        assertTrue(erTilgangsrute("/tilgang"))
        assertFalse(erTilgangsrute("/api/behandling/$behandlingId/tilgang"))
        assertFalse(erTilgangsrute("/api/tilgangsstyring/$behandlingId"))
        assertFalse(erTilgangsrute("/tilgangX/behandling/$behandlingId"))
        assertFalse(erTilgangsrute("/tilgang/../api/behandling/$behandlingId"))
        assertFalse(erTilgangsrute("/tilgang/%2e%2e/api/behandling/$behandlingId"))
    }

    private fun kall(sti: String): HttpStatusCode {
        var status: HttpStatusCode? = null
        testApplication {
            val client =
                runServer(mockOAuth2Server) {
                    install(SpesialTilgangPlugin) {
                        harTilgangBehandling = { _, _ -> false }
                    }
                    get("/api/behandling/{$BEHANDLINGID_CALL_PARAMETER}") { call.respond("OK") }
                    get("/api/behandling/{$BEHANDLINGID_CALL_PARAMETER}/tilgang") { call.respond("OK") }
                    get("/$TILGANG_ROUTE_PATH/behandling/{$BEHANDLINGID_CALL_PARAMETER}") { call.respond("OK") }
                }
            status =
                client
                    .get(sti) {
                        header(HttpHeaders.Authorization, "Bearer ${mockOAuth2Server.issueSaksbehandlerToken()}")
                    }.status
        }
        return status!!
    }
}
