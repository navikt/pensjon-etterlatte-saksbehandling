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
import io.mockk.mockkStatic
import io.mockk.runs
import io.mockk.unmockkStatic
import no.nav.etterlatte.BeanFactory
import no.nav.etterlatte.behandling.domain.Behandling
import no.nav.etterlatte.libs.common.behandling.BehandlingStatus
import no.nav.etterlatte.libs.database.DataSourceBuilder
import no.nav.etterlatte.libs.database.migrate
import no.nav.etterlatte.module
import no.nav.security.mock.oauth2.MockOAuth2Server
import no.nav.security.token.support.v2.tokenValidationSupport
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.util.*
import javax.sql.DataSource

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class BehandlingsstatusRoutesTest {

    private val beanFactory: BeanFactory = mockk(relaxed = true)
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

        mockkStatic("${DataSourceBuilder.javaClass.canonicalName}Kt")
        every { any<DataSource>().migrate() } returns mockk()
    }

    @AfterAll
    fun after() {
        server.shutdown()

        unmockkStatic("${DataSourceBuilder.javaClass.canonicalName}Kt")
    }

    @Test
    fun `skal returnere 200 OK hvis behandlingstatus kan settes til vilkaarsvurdert`() {
        every { beanFactory.behandlingsStatusService() } returns mockk {
            every { settVilkaarsvurdert(any(), any(), any()) } just runs
        }

        testApplication {
            application {
                install(Authentication) {
                    tokenValidationSupport(config = HoconApplicationConfig(ConfigFactory.load()))
                }
                module(beanFactory)
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
                install(Authentication) {
                    tokenValidationSupport(config = HoconApplicationConfig(ConfigFactory.load()))
                }
                module(beanFactory)
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