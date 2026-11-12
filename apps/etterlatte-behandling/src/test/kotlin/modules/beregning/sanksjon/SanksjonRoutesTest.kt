package no.nav.etterlatte.modules.beregning.sanksjon

import io.kotest.matchers.shouldBe
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import io.mockk.coEvery
import io.mockk.mockk
import no.nav.etterlatte.behandling.domain.Behandling.BehandlingStoetterIkkeStatusEndringException
import no.nav.etterlatte.behandling.domain.TilstandException
import no.nav.etterlatte.ktor.runServer
import no.nav.etterlatte.ktor.token.issueSaksbehandlerToken
import no.nav.etterlatte.libs.common.behandling.BehandlingStatus
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.toJson
import no.nav.etterlatte.libs.ktor.route.BehandlingTilgangsSjekk
import no.nav.etterlatte.modules.beregning.regler.lagreSanksjon
import no.nav.etterlatte.modules.beregning.sanksjon.SanksjonService
import no.nav.etterlatte.modules.beregning.sanksjon.sanksjon
import no.nav.security.mock.oauth2.MockOAuth2Server
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import java.util.UUID

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class SanksjonRoutesTest {
    private val behandlingTilgangsSjekk = mockk<BehandlingTilgangsSjekk>()
    private val server: MockOAuth2Server = MockOAuth2Server()
    private val sanksjonService = mockk<SanksjonService>()

    @BeforeAll
    fun setup() {
        server.start()
        coEvery { behandlingTilgangsSjekk.harTilgangTilBehandling(any(), any(), any()) } returns true
    }

    @AfterAll
    fun teardown() {
        server.shutdown()
    }

    @Test
    fun `Skal returnere 204 naar sanksjon ikke finnes`() {
        coEvery { sanksjonService.hentSanksjon(any()) } returns emptyList()

        testApplication {
            runServer(server) {
                sanksjon(sanksjonService, behandlingTilgangsSjekk)
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
                sanksjon(sanksjonService, behandlingTilgangsSjekk)
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
                sanksjon(sanksjonService, behandlingTilgangsSjekk)
            }

            val response =
                client.delete("/api/beregning/sanksjon/${UUID.randomUUID()}/${UUID.randomUUID()}") {
                    header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    header(HttpHeaders.Authorization, "Bearer ${server.issueSaksbehandlerToken()}")
                }

            response.status shouldBe HttpStatusCode.OK
        }
    }

    @ParameterizedTest
    @CsvSource("true,true", "true,false", "false,true", "false,false")
    fun `skal beholde statusfeilen for endring av sanksjoner`(
        opprett: Boolean,
        ustoettetStatusendring: Boolean,
    ) {
        val behandlingId = UUID.randomUUID()
        val sanksjonId = UUID.randomUUID()
        val feil =
            if (ustoettetStatusendring) {
                BehandlingStoetterIkkeStatusEndringException(BehandlingStatus.BEREGNET)
            } else {
                TilstandException.UgyldigTilstand("Ugyldig status")
            }
        coEvery { sanksjonService.opprettEllerOppdaterSanksjon(behandlingId, any(), any()) } throws feil
        coEvery { sanksjonService.slettSanksjon(behandlingId, sanksjonId, any()) } throws feil

        testApplication {
            runServer(server) {
                sanksjon(sanksjonService, behandlingTilgangsSjekk)
            }

            val response =
                if (opprett) {
                    client.post("/api/beregning/sanksjon/$behandlingId") {
                        header(HttpHeaders.Authorization, "Bearer ${server.issueSaksbehandlerToken()}")
                        header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                        setBody(lagreSanksjon(sanksjonId).toJson())
                    }
                } else {
                    client.delete("/api/beregning/sanksjon/$behandlingId/$sanksjonId") {
                        header(HttpHeaders.Authorization, "Bearer ${server.issueSaksbehandlerToken()}")
                    }
                }

            response.status shouldBe HttpStatusCode.Forbidden
            objectMapper.readTree(response.bodyAsText())["code"].asString() shouldBe "BEHANDLINGEN_KAN_IKKE_ENDRES"
        }
    }
}
