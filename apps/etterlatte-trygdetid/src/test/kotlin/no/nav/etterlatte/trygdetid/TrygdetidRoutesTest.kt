package no.nav.etterlatte.trygdetid

import io.kotest.matchers.shouldBe
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.ApplicationTestBuilder
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import no.nav.etterlatte.ktor.runServer
import no.nav.etterlatte.ktor.startRandomPort
import no.nav.etterlatte.ktor.token.issueSaksbehandlerToken
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.trygdetid.StatusOppdatertDto
import no.nav.etterlatte.trygdetid.klienter.BehandlingKlient
import no.nav.security.mock.oauth2.MockOAuth2Server
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.util.UUID.randomUUID

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class TrygdetidRoutesTest {
    private val mockOAuth2Server = MockOAuth2Server()
    private val behandlingKlient = mockk<BehandlingKlient>()
    private val trygdetidService = mockk<TrygdetidService>()

    @BeforeAll
    fun beforeAll() {
        mockOAuth2Server.startRandomPort()
        coEvery { behandlingKlient.harTilgangTilBehandling(any(), any(), any()) } returns true
    }

    @AfterAll
    fun afterAll() {
        mockOAuth2Server.shutdown()
    }

    @Test
    fun `skal returnere 204 naar trygdetid ikke finnes`() {
        coEvery { trygdetidService.hentTrygdetiderIBehandling(any(), any()) } returns emptyList()

        testApplication {
            val response =
                client.get("/api/trygdetid_v2/${randomUUID()}") {
                    header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    header(HttpHeaders.Authorization, "Bearer $token")
                }

            response.status shouldBe HttpStatusCode.NoContent
        }

        coVerify {
            behandlingKlient.harTilgangTilBehandling(any(), any(), any())
            trygdetidService.hentTrygdetiderIBehandling(any(), any())
        }
    }

    @Test
    fun `skal returnere 200 og status ved kall mot oppdater-status`() {
        coEvery { trygdetidService.sjekkGyldighetOgOppdaterBehandlingStatus(any(), any()) } returns true

        testApplication {
            val response =
                client.post("/api/trygdetid_v2/${randomUUID()}/oppdater-status") {
                    header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    header(HttpHeaders.Authorization, "Bearer $token")
                }

            val dto = objectMapper.readValue(response.bodyAsText(), StatusOppdatertDto::class.java)

            response.status shouldBe HttpStatusCode.OK
            dto.statusOppdatert shouldBe true
        }

        coVerify {
            behandlingKlient.harTilgangTilBehandling(any(), any(), any())
            trygdetidService.sjekkGyldighetOgOppdaterBehandlingStatus(any(), any())
        }
    }

    @Test
    fun `skal returnere 200 ved kall til kopier-grunnlag`() {
        val behandlingId = randomUUID()
        val kildeBehandlingId = randomUUID()
        coEvery { trygdetidService.kopierTrygdetidsgrunnlag(behandlingId, kildeBehandlingId, any()) } just runs

        testApplication {
            val response =
                client.post("/api/trygdetid_v2/$behandlingId/kopier-grunnlag/$kildeBehandlingId") {
                    header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    header(HttpHeaders.Authorization, "Bearer $token")
                }
            response.status shouldBe HttpStatusCode.OK
        }
        coVerify { trygdetidService.kopierTrygdetidsgrunnlag(behandlingId, kildeBehandlingId, any()) }
    }

    @Test
    fun `skal returnere behandling-id for annen behandling med trygdetid for samme avdøde`() {
        val behandlingId = randomUUID()
        val kildeBehandlingId = randomUUID()
        coEvery {
            trygdetidService.finnBehandlingMedTrygdetidForSammeAvdoede(behandlingId, any())
        } returns kildeBehandlingId

        testApplication {
            val response =
                client.get("/api/trygdetid_v2/$behandlingId/behandling-med-trygdetid-for-avdoede") {
                    header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    header(HttpHeaders.Authorization, "Bearer $token")
                }
            response.status shouldBe HttpStatusCode.OK
            response.bodyAsText() shouldBe kildeBehandlingId.toString()
        }
        coVerify { trygdetidService.kopierTrygdetidsgrunnlag(behandlingId, kildeBehandlingId, any()) }
    }

    private fun testApplication(block: suspend ApplicationTestBuilder.() -> Unit) {
        io.ktor.server.testing.testApplication {
            runServer(mockOAuth2Server) {
                trygdetid(trygdetidService, behandlingKlient)
            }
            block(this)
        }
    }

    private val token: String by lazy { mockOAuth2Server.issueSaksbehandlerToken() }
}
