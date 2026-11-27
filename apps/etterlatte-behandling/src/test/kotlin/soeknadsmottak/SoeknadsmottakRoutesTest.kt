package no.nav.etterlatte.soeknadsmottak

import io.kotest.matchers.shouldBe
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.testing.testApplication
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.etterlatte.attachMockContextWithDb
import no.nav.etterlatte.common.Enheter
import no.nav.etterlatte.ktor.runServer
import no.nav.etterlatte.ktor.startRandomPort
import no.nav.etterlatte.ktor.token.issueSaksbehandlerToken
import no.nav.etterlatte.ktor.token.issueSystembrukerToken
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.soeknadsmottak.JournalfoeringskontekstResponse
import no.nav.etterlatte.mockSaksbehandler
import no.nav.security.mock.oauth2.MockOAuth2Server
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class SoeknadsmottakRoutesTest {
    private val mockOAuth2Server = MockOAuth2Server()
    private val service = mockk<SoeknadsmottakService>()

    @BeforeAll
    fun before() {
        mockOAuth2Server.startRandomPort()
        every { service.journalfoeringskontekst(any()) } returns
            JournalfoeringskontekstResponse(true, null, SakType.OMSTILLINGSSTOENAD, "EYO", Enheter.defaultEnhet.enhetNr)
    }

    @AfterAll
    fun after() = mockOAuth2Server.shutdown()

    @Test
    fun `bare etterlatte-api som systembruker slipper inn`() {
        testApplication {
            runServer(mockOAuth2Server) {
                attachMockContextWithDb(mockSaksbehandler("Z1"), mockk(relaxed = true))
                soeknadsmottakRoutes(service)
            }

            suspend fun kall(token: String) =
                client
                    .post("/api/soeknadsmottak/journalfoeringskontekst") {
                        header(HttpHeaders.Authorization, "Bearer $token")
                        contentType(ContentType.Application.Json)
                        setBody("""{"soeknadType":"UTVIDET_OMSTILLINGSSTOENAD","foedselsnummer":null}""")
                    }.status

            kall(mockOAuth2Server.issueSaksbehandlerToken()) shouldBe HttpStatusCode.NotFound
            kall(mockOAuth2Server.issueSystembrukerToken(mittsystem = "dev-gcp:etterlatte:innsendt-soeknad")) shouldBe
                HttpStatusCode.Forbidden
            kall(mockOAuth2Server.issueSystembrukerToken(mittsystem = "dev-gcp:etterlatte:etterlatte-api")) shouldBe HttpStatusCode.OK
            verify(exactly = 1) { service.journalfoeringskontekst(any()) }
        }
    }

    @Test
    fun `ukjent soeknadstype avvises`() {
        testApplication {
            runServer(mockOAuth2Server) {
                attachMockContextWithDb(mockSaksbehandler("Z1"), mockk(relaxed = true))
                soeknadsmottakRoutes(service)
            }

            client
                .post("/api/soeknadsmottak/overlevering") {
                    header(
                        HttpHeaders.Authorization,
                        "Bearer ${mockOAuth2Server.issueSystembrukerToken(mittsystem = "dev-gcp:etterlatte:etterlatte-api")}",
                    )
                    contentType(ContentType.Application.Json)
                    setBody(
                        """{"mottaksnoekkel":"innsendt-soeknad:1","soeknadType":"BARNEPENSJON","sakId":1,"journalpostId":"1","dokumentInfoIder":[]}""",
                    )
                }.status shouldBe HttpStatusCode.BadRequest
        }
    }
}
