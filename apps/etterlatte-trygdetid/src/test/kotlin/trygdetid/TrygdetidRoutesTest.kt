package no.nav.etterlatte.trygdetid

import io.kotest.matchers.shouldBe
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.log
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.trygdetid.StatusOppdatertDto
import no.nav.etterlatte.libs.ktor.AZURE_ISSUER
import no.nav.etterlatte.libs.ktor.restModule
import no.nav.etterlatte.trygdetid.klienter.BehandlingKlient
import no.nav.security.mock.oauth2.MockOAuth2Server
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import testsupport.buildTestApplicationConfigurationForOauth
import java.util.UUID.randomUUID

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class TrygdetidRoutesTest {
    private val server = MockOAuth2Server()
    private val behandlingKlient = mockk<BehandlingKlient>()
    private val trygdetidService = mockk<TrygdetidService>()

    @BeforeAll
    fun beforeAll() {
        server.start()
        coEvery { behandlingKlient.harTilgangTilBehandling(any(), any()) } returns true
    }

    @AfterAll
    fun afterAll() {
        server.shutdown()
    }

    @Test
    fun `skal returnere 204 naar trygdetid ikke finnes`() {
        coEvery { trygdetidService.hentTrygdetid(any(), any()) } returns null

        testApplication(server.config.httpServer.port()) {
            val response =
                client.get("/api/trygdetid/${randomUUID()}") {
                    header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    header(HttpHeaders.Authorization, "Bearer $token")
                }

            response.status shouldBe HttpStatusCode.NoContent
        }

        coVerify {
            behandlingKlient.harTilgangTilBehandling(any(), any())
            trygdetidService.hentTrygdetid(any(), any())
        }
    }

    @Test
    fun `skal returnere 200 og status ved kall mot oppdater-status`() {
        coEvery { trygdetidService.sjekkGyldighetOgOppdaterBehandlingStatus(any(), any()) } returns true

        testApplication(server.config.httpServer.port()) {
            val response =
                client.post("/api/trygdetid/${randomUUID()}/oppdater-status") {
                    header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    header(HttpHeaders.Authorization, "Bearer $token")
                }

            val dto = objectMapper.readValue(response.bodyAsText(), StatusOppdatertDto::class.java)

            response.status shouldBe HttpStatusCode.OK
            dto.statusOppdatert shouldBe true
        }

        coVerify {
            behandlingKlient.harTilgangTilBehandling(any(), any())
            trygdetidService.sjekkGyldighetOgOppdaterBehandlingStatus(any(), any())
        }
    }

    private fun testApplication(
        port: Int,
        block: suspend ApplicationTestBuilder.() -> Unit,
    ) {
        testApplication {
            environment {
                config = buildTestApplicationConfigurationForOauth(port, AZURE_ISSUER, AZURE_CLIENT_ID)
            }
            application { restModule(log) { trygdetid(trygdetidService, behandlingKlient) } }

            block(this)
        }
    }

    private val token: String by lazy {
        server.issueToken(
            issuerId = AZURE_ISSUER,
            audience = AZURE_CLIENT_ID,
            claims = mapOf("navn" to "John Doe", "NAVident" to "Saksbehandler01"),
        ).serialize()
    }

    private companion object {
        const val AZURE_CLIENT_ID: String = "azure-id"
    }
}
