package no.nav.etterlatte.behandling

import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.config.HoconApplicationConfig
import io.ktor.server.testing.testApplication
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import no.nav.etterlatte.behandling.domain.Behandling
import no.nav.etterlatte.config.ApplicationContext
import no.nav.etterlatte.libs.common.behandling.BehandlingStatus
import no.nav.etterlatte.libs.ktor.AZURE_ISSUER
import no.nav.etterlatte.module
import no.nav.etterlatte.tilgangsstyring.AzureGroup
import no.nav.etterlatte.token.Claims
import no.nav.security.mock.oauth2.MockOAuth2Server
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import testsupport.buildTestApplicationConfigurationForOauth
import java.util.*

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class BehandlingsstatusRoutesTest {

    private val applicationContext: ApplicationContext = mockk(relaxed = true)
    private val server: MockOAuth2Server = MockOAuth2Server()
    private lateinit var hoconApplicationConfig: HoconApplicationConfig

    private val azureAdAttestantClaim: String by lazy {
        "0af3955f-df85-4eb0-b5b2-45bf2c8aeb9e"
    }

    private val azureAdSaksbehandlerClaim: String by lazy {
        "63f46f74-84a8-4d1c-87a8-78532ab3ae60"
    }

    @BeforeAll
    fun before() {
        server.start()
        val httpServer = server.config.httpServer
        hoconApplicationConfig = buildTestApplicationConfigurationForOauth(httpServer.port(), AZURE_ISSUER, CLIENT_ID)

        val azureAdGroupIds = mapOf(
            Pair(AzureGroup.ATTESTANT, azureAdAttestantClaim),
            Pair(AzureGroup.SAKSBEHANDLER, azureAdSaksbehandlerClaim)
        )

        every { applicationContext.saksbehandlerGroupIdsByKey } returns azureAdGroupIds

        every { applicationContext.tilgangService } returns mockk {
            every { harTilgangTilBehandling(any(), any()) } returns true
        }
    }

    @AfterAll
    fun after() {
        server.shutdown()
    }

    @Test
    fun `skal returnere 200 OK hvis behandlingstatus kan settes til vilkaarsvurdert`() {
        every { applicationContext.behandlingsStatusService } returns mockk {
            every { settVilkaarsvurdert(any(), any()) } just runs
        }

        testApplication {
            environment {
                config = hoconApplicationConfig
            }
            application {
                module(applicationContext)
            }

            val response = client.get("/behandlinger/$behandlingId/vilkaarsvurder") {
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
        every { applicationContext.behandlingsStatusService } returns mockk {
            every { sjekkOmKanAttestere(any()) } just runs
        }

        testApplication {
            environment {
                config = hoconApplicationConfig
            }
            application {
                module(applicationContext)
            }

            val response = client.get("/behandlinger/$behandlingId/attester") {
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
        every { applicationContext.behandlingsStatusService } returns mockk {
            every { sjekkOmKanAttestere(any()) } just runs
        }

        testApplication {
            environment {
                config = hoconApplicationConfig
            }
            application {
                module(applicationContext)
            }

            val response = client.get("/behandlinger/$behandlingId/attester") {
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                header(HttpHeaders.Authorization, "Bearer $tokenSaksbehandler")
            }

            assertEquals(HttpStatusCode.Unauthorized, response.status)
            assertEquals("Mangler attestantrolle", response.bodyAsText())
        }
    }

    @Test
    fun `skal returnere 409 Conflict naar behandlingstatus ikke kan settes til vilkaarsvurdert`() {
        every { applicationContext.behandlingsStatusService } returns mockk {
            every {
                settVilkaarsvurdert(
                    any(),
                    any()
                )
            } throws Behandling.BehandlingStoetterIkkeStatusEndringException(BehandlingStatus.VILKAARSVURDERT)
        }

        testApplication {
            environment {
                config = hoconApplicationConfig
            }
            application {
                module(applicationContext)
            }

            val response = client.get("/behandlinger/$behandlingId/vilkaarsvurder") {
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                header(HttpHeaders.Authorization, "Bearer $tokenSaksbehandler")
            }

            assertEquals(HttpStatusCode.Conflict, response.status)
        }
    }

    private val tokenSaksbehandler: String by lazy {
        server.issueToken(
            issuerId = AZURE_ISSUER,
            audience = CLIENT_ID,
            claims = mapOf(
                "navn" to "John Doe",
                Claims.NAVident.toString() to "Saksbehandler01",
                "groups" to listOf(
                    azureAdSaksbehandlerClaim
                )
            )
        ).serialize()
    }

    private val tokenAttestant: String by lazy {
        server.issueToken(
            issuerId = AZURE_ISSUER,
            audience = CLIENT_ID,
            claims = mapOf(
                "navn" to "John Doe",
                Claims.NAVident.toString() to "Saksbehandler02",
                "groups" to listOf(
                    azureAdAttestantClaim
                )
            )
        ).serialize()
    }

    private companion object {
        val behandlingId: UUID = UUID.randomUUID()
        const val CLIENT_ID = "mock-client-id"
    }
}