package no.nav.etterlatte.beregning.regler.sanksjon

import io.kotest.matchers.shouldBe
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import io.mockk.coEvery
import io.mockk.mockk
import no.nav.etterlatte.beregning.regler.lagreSanksjon
import no.nav.etterlatte.klienter.BehandlingKlient
import no.nav.etterlatte.ktor.runServer
import no.nav.etterlatte.ktor.token.issueSaksbehandlerToken
import no.nav.etterlatte.libs.common.toJson
import no.nav.etterlatte.sanksjon.SanksjonService
import no.nav.etterlatte.sanksjon.sanksjon
import no.nav.security.mock.oauth2.MockOAuth2Server
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.util.UUID

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class SanksjonRoutesTest {
    private val server: MockOAuth2Server = MockOAuth2Server()
    private val behandlingKlient = mockk<BehandlingKlient>()
    private val sanksjonService = mockk<SanksjonService>()

    @BeforeAll
    fun setup() {
        server.start()
        coEvery { behandlingKlient.harTilgangTilBehandling(any(), any(), any()) } returns true
    }

    @AfterAll
    fun teardown() {
        server.shutdown()
    }

    @Test
    fun `Skal returnere 204 naar sanksjon ikke finnes`() {
        coEvery { sanksjonService.hentSanksjon(any()) } returns null

        testApplication {
            runServer(server) {
                sanksjon(sanksjonService, behandlingKlient)
            }

            val response =
                client.get("/api/beregning/sanksjon/${UUID.randomUUID()}") {
                    header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    header(HttpHeaders.Authorization, "Bearer ${server.issueSaksbehandlerToken()}")
                }

            response.status shouldBe HttpStatusCode.NoContent
        }
    }

    @Test
    fun `Skal kunne lagre en sanksjon`() {
        val sanksjonId: UUID = UUID.randomUUID()
        val sanksjon = lagreSanksjon(sanksjonId)

        coEvery { sanksjonService.opprettEllerOppdaterSanksjon(any(), any(), any()) } returns Unit

        testApplication {
            runServer(server) {
                sanksjon(sanksjonService, behandlingKlient)
            }

            val response =
                client.post("/api/beregning/sanksjon/${UUID.randomUUID()}") {
                    setBody(sanksjon.toJson())
                    header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    header(HttpHeaders.Authorization, "Bearer ${server.issueSaksbehandlerToken()}")
                }

            response.status shouldBe HttpStatusCode.OK
        }
    }

    @Test
    fun `Skal returnere 200 naar sanksjon slettes`() {
        coEvery { sanksjonService.slettSanksjon(any(), any(), any()) } returns Unit

        testApplication {
            runServer(server) {
                sanksjon(sanksjonService, behandlingKlient)
            }

            val response =
                client.delete("/api/beregning/sanksjon/${UUID.randomUUID()}/${UUID.randomUUID()}") {
                    header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    header(HttpHeaders.Authorization, "Bearer ${server.issueSaksbehandlerToken()}")
                }

            response.status shouldBe HttpStatusCode.OK
        }
    }
}
