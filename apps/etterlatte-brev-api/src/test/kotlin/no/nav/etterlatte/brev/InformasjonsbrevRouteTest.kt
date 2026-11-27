package no.nav.etterlatte.brev

import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.OutgoingContent
import io.ktor.http.contentType
import io.ktor.server.testing.testApplication
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import no.nav.etterlatte.brev.hentinformasjon.behandling.BehandlingService
import no.nav.etterlatte.ktor.runServer
import no.nav.etterlatte.ktor.startRandomPort
import no.nav.etterlatte.ktor.token.issueSaksbehandlerToken
import no.nav.etterlatte.ktor.token.issueSystembrukerToken
import no.nav.etterlatte.libs.common.sak.SakId
import no.nav.etterlatte.libs.ktor.route.Tilgangssjekker
import no.nav.security.mock.oauth2.MockOAuth2Server
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.util.UUID

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class InformasjonsbrevRouteTest {
    private val oauth = MockOAuth2Server()
    private val klient = "test:etterlatte:etterlatte-behandling"
    private val id = UUID.randomUUID()
    private val path = "/api/brev/strukturert/sak/123/utsending/$id"

    @BeforeAll
    fun start() = oauth.startRandomPort()

    @AfterAll
    fun stop() = oauth.shutdown()

    @Test
    fun `lesing krever eksplisitt tillatt systembruker og manglende utsending gir 204`() =
        testApplication {
            val service = mockk<InformasjonsbrevService>()
            val behandling = mockk<BehandlingService>()
            coEvery { behandling.hentSak(any(), any()) } returns mockk()
            every { service.hent(SakId(123), id, any()) } returns null
            val client =
                runServer(oauth, "api") {
                    informasjonsbrevRoute(service, mockk<Tilgangssjekker>(), behandling, BrevBatchKonfigurasjon(false, klient))
                }
            assertEquals(
                HttpStatusCode.Forbidden,
                client
                    .get(path) {
                        header(HttpHeaders.Authorization, "Bearer ${oauth.issueSaksbehandlerToken()}")
                    }.status,
            )
            assertEquals(
                HttpStatusCode.Forbidden,
                client
                    .get(path) {
                        header(HttpHeaders.Authorization, "Bearer ${oauth.issueSystembrukerToken("annen-klient")}")
                    }.status,
            )
            assertEquals(
                HttpStatusCode.NoContent,
                client
                    .get(path) {
                        header(HttpHeaders.Authorization, "Bearer ${oauth.issueSystembrukerToken(klient)}")
                    }.status,
            )
            coVerify(exactly = 1) { behandling.hentSak(SakId(123), any()) }
        }

    @Test
    fun `stoppbryter blokkerer opprettelse og send foer services kalles`() =
        testApplication {
            val service = mockk<InformasjonsbrevService>()
            val behandling = mockk<BehandlingService>()
            val client =
                runServer(oauth, "api") {
                    informasjonsbrevRoute(service, mockk(), behandling, BrevBatchKonfigurasjon(false, klient))
                }
            for (url in listOf(path, "$path/send")) {
                assertEquals(
                    HttpStatusCode.ServiceUnavailable,
                    client
                        .post(url) {
                            header(HttpHeaders.Authorization, "Bearer ${oauth.issueSystembrukerToken(klient)}")
                            contentType(ContentType.Application.Json)
                            setBody("{}")
                        }.status,
                )
            }
            coVerify(exactly = 0) { service.send(any(), any(), any()) }
            coVerify(exactly = 0) { service.opprett(any(), any(), any(), any()) }
            coVerify(exactly = 0) { behandling.hentSak(any(), any()) }
        }

    @Test
    fun `faktisk requeststoerrelse begrenses ogsaa uten content length foer deserialisering`() =
        testApplication {
            val service = mockk<InformasjonsbrevService>()
            val behandling = mockk<BehandlingService>()
            coEvery { behandling.hentSak(any(), any()) } returns mockk()
            val client =
                runServer(oauth, "api") {
                    informasjonsbrevRoute(service, mockk(), behandling, BrevBatchKonfigurasjon(true, klient))
                }
            val body =
                object : OutgoingContent.ByteArrayContent() {
                    override val contentType = ContentType.Application.Json

                    override fun bytes() = ByteArray(256 * 1024 + 1) { 'x'.code.toByte() }
                }
            assertEquals(
                HttpStatusCode.PayloadTooLarge,
                client
                    .post(path) {
                        header(HttpHeaders.Authorization, "Bearer ${oauth.issueSystembrukerToken(klient)}")
                        setBody(body)
                    }.status,
            )
            coVerify(exactly = 0) { service.opprett(any(), any(), any(), any()) }
        }
}
