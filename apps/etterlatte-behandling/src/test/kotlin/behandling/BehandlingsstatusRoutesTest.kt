package no.nav.etterlatte.behandling

import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import no.nav.etterlatte.azureAdAttestantClaim
import no.nav.etterlatte.azureAdSaksbehandlerClaim
import no.nav.etterlatte.behandling.domain.Behandling
import no.nav.etterlatte.config.ApplicationContext
import no.nav.etterlatte.ktor.runServerWithModule
import no.nav.etterlatte.ktor.token.issueSaksbehandlerToken
import no.nav.etterlatte.libs.common.behandling.BehandlingStatus
import no.nav.etterlatte.module
import no.nav.etterlatte.tilgangsstyring.AzureGroup
import no.nav.security.mock.oauth2.MockOAuth2Server
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.util.UUID

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class BehandlingsstatusRoutesTest {
    private val applicationContext: ApplicationContext = mockk(relaxed = true)
    private val server: MockOAuth2Server = MockOAuth2Server()

    @BeforeAll
    fun before() {
        server.start()

        val azureAdGroupIds =
            mapOf(
                Pair(AzureGroup.ATTESTANT, azureAdAttestantClaim),
                Pair(AzureGroup.SAKSBEHANDLER, azureAdSaksbehandlerClaim),
            )

        every { applicationContext.saksbehandlerGroupIdsByKey } returns azureAdGroupIds

        every { applicationContext.tilgangService } returns
            mockk {
                every { harTilgangTilBehandling(any(), any()) } returns true
            }
    }

    @AfterAll
    fun after() {
        applicationContext.close()
        server.shutdown()
    }

    @Test
    fun `skal returnere 200 OK hvis behandlingstatus kan settes til vilkaarsvurdert`() {
        every { applicationContext.behandlingsStatusService } returns
            mockk {
                every { settVilkaarsvurdert(any(), any(), any()) } just runs
            }

        testApplication {
            runServerWithModule(server) {
                module(applicationContext)
            }

            val response =
                client.get("/behandlinger/$behandlingId/vilkaarsvurder") {
                    header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    header(HttpHeaders.Authorization, "Bearer $tokenSaksbehandler")
                }

            val operasjonGyldig: OperasjonGyldig =
                objectMapper.readValue(response.bodyAsText(), OperasjonGyldig::class.java)

            assertEquals(HttpStatusCode.OK, response.status)
            assertEquals(true, operasjonGyldig.gyldig)
        }
    }

    @Test
    fun `skal kunne attestere med attestant rolle`() {
        every { applicationContext.behandlingsStatusService } returns
            mockk {
                every { sjekkOmKanAttestere(any()) } just runs
            }

        testApplication {
            runServerWithModule(server) {
                module(applicationContext)
            }

            val response =
                client.get("/behandlinger/$behandlingId/attester") {
                    header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    header(HttpHeaders.Authorization, "Bearer $tokenAttestant")
                }

            val operasjonGyldig: OperasjonGyldig =
                objectMapper.readValue(response.bodyAsText(), OperasjonGyldig::class.java)

            assertEquals(HttpStatusCode.OK, response.status)
            assertEquals(true, operasjonGyldig.gyldig)
        }
    }

    @Test
    fun `skal ikke kunne attestere med kun saksbehandler rolle`() {
        every { applicationContext.behandlingsStatusService } returns
            mockk {
                every { sjekkOmKanAttestere(any()) } just runs
            }

        testApplication {
            runServerWithModule(server) {
                module(applicationContext)
            }

            val response =
                client.get("/behandlinger/$behandlingId/attester") {
                    header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    header(HttpHeaders.Authorization, "Bearer $tokenSaksbehandler")
                }

            assertEquals(HttpStatusCode.Unauthorized, response.status)
        }
    }

    @Test
    fun `skal returnere 409 Conflict naar behandlingstatus ikke kan settes til vilkaarsvurdert`() {
        every { applicationContext.behandlingsStatusService } returns
            mockk {
                every {
                    settVilkaarsvurdert(
                        any(),
                        any(),
                        any(),
                    )
                } throws Behandling.BehandlingStoetterIkkeStatusEndringException(BehandlingStatus.VILKAARSVURDERT)
            }

        testApplication {
            runServerWithModule(server) {
                module(applicationContext)
            }

            val response =
                client.get("/behandlinger/$behandlingId/vilkaarsvurder") {
                    header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    header(HttpHeaders.Authorization, "Bearer $tokenSaksbehandler")
                }

            assertEquals(HttpStatusCode.Conflict, response.status)
        }
    }

    private val tokenSaksbehandler: String by lazy { server.issueSaksbehandlerToken(groups = listOf(azureAdSaksbehandlerClaim)) }

    private val tokenAttestant: String by lazy {
        server.issueSaksbehandlerToken(navIdent = "Saksbehandler02", groups = listOf(azureAdAttestantClaim))
    }

    private companion object {
        val behandlingId: UUID = UUID.randomUUID()
    }
}
