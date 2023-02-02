package no.nav.etterlatte.behandling

import com.typesafe.config.ConfigFactory
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.install
import io.ktor.server.auth.Authentication
import io.ktor.server.config.HoconApplicationConfig
import io.ktor.server.testing.testApplication
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import no.nav.etterlatte.BeanFactory
import no.nav.etterlatte.behandling.domain.Behandling
import no.nav.etterlatte.libs.common.behandling.BehandlingStatus
import no.nav.etterlatte.module
import no.nav.security.mock.oauth2.MockOAuth2Server
import no.nav.security.token.support.v2.tokenValidationSupport
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.util.*

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class BehandlingsstatusRoutesTest {

    private val beanFactory: BeanFactory = mockk() {
        every { generellBehandlingService() } returns mockk()
        every { sakService() } returns mockk()
        every { revurderingService() } returns mockk()
        every { manueltOpphoerService() } returns mockk()
        every { behandlingHendelser() } returns mockk()
        every { behandlingHendelser().start() } returns mockk()
        every { foerstegangsbehandlingService() } returns mockk()
        every { grunnlagsendringshendelseService() } returns mockk()
        every { datasourceBuilder().dataSource } returns mockk()
        every { datasourceBuilder().migrate() } returns mockk()

        every { behandlingsStatusService() } returns mockk {
            every { settVilkaarsvurdert(any(), any(), any()) } just runs
        }
    }

    private val server = MockOAuth2Server()

    private val token: String by lazy {
        server.issueToken(
            issuerId = ISSUER_ID,
            audience = CLIENT_ID,
            claims = mapOf(
                "navn" to "John Doe",
                "NAVident" to "Saksbehandler01"
            )
        ).serialize()
    }

    @BeforeAll
    fun before() {
        server.start()
        System.setProperty("AZURE_APP_WELL_KNOWN_URL", server.wellKnownUrl(ISSUER_ID).toString())
        System.setProperty("AZURE_APP_CLIENT_ID", CLIENT_ID)
    }

    @AfterAll
    fun after() {
        server.shutdown()
    }

    @Test
    fun `skal returnere 200 OK hvis behandlingstatus kan settes til vilkaarsvurdert`() {
        every { beanFactory.behandlingsStatusService() } returns mockk {
            every { settVilkaarsvurdert(any(), any(), any()) } just runs
        }

        testApplication {
            application {
                module(beanFactory)
                install(Authentication) {
                    tokenValidationSupport(config = HoconApplicationConfig(ConfigFactory.load()))
                }
            }

            val response = client.get("/behandlinger/$behandlingId/vilkaarsvurder") {
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                header(HttpHeaders.Authorization, "Bearer $token")
            }

            val operasjonGyldig: OperasjonGyldig =
                objectMapper.readValue(response.bodyAsText(), OperasjonGyldig::class.java)

            assertEquals(HttpStatusCode.OK, response.status)
            assertEquals(true, operasjonGyldig.gyldig)
        }
    }

    @Test
    fun `skal returnere 409 Conflict naar behandlingstatus ikke kan settes til vilkaarsvurdert`() {
        every { beanFactory.behandlingsStatusService() } returns mockk {
            every {
                settVilkaarsvurdert(
                    any(),
                    any(),
                    any()
                )
            } throws Behandling.BehandlingStoetterIkkeStatusEndringException(BehandlingStatus.VILKAARSVURDERT)
        }

        testApplication {
            application {
                module(beanFactory)
                install(Authentication) {
                    tokenValidationSupport(config = HoconApplicationConfig(ConfigFactory.load()))
                }
            }

            val response = client.get("/behandlinger/$behandlingId/vilkaarsvurder") {
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                header(HttpHeaders.Authorization, "Bearer $token")
            }

            assertEquals(HttpStatusCode.Conflict, response.status)
        }
    }

    private companion object {
        val behandlingId: UUID = UUID.randomUUID()
        const val ISSUER_ID = "azure"
        const val CLIENT_ID = "azure-id for saksbehandler"
    }
}