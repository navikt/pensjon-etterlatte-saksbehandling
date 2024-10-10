package no.nav.etterlatte.brev.dokument

import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import io.mockk.Called
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.verify
import no.nav.etterlatte.brev.dokarkiv.DokarkivService
import no.nav.etterlatte.ktor.runServer
import no.nav.etterlatte.ktor.startRandomPort
import no.nav.etterlatte.ktor.token.issueSaksbehandlerToken
import no.nav.etterlatte.libs.ktor.route.Tilgangssjekker
import no.nav.security.mock.oauth2.MockOAuth2Server
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class DokumentRouteTest {
    private val mockOAuth2Server = MockOAuth2Server()
    private val tilgangssjekker = mockk<Tilgangssjekker>()
    private val safService = mockk<SafService>()
    private val dokarkivService = mockk<DokarkivService>()
    private val accessToken: String by lazy { mockOAuth2Server.issueSaksbehandlerToken() }

    @BeforeAll
    fun before() {
        mockOAuth2Server.startRandomPort()
    }

    @AfterEach
    fun afterEach() {
        clearAllMocks()
    }

    @AfterAll
    fun after() {
        mockOAuth2Server.shutdown()
    }

    @Test
    fun `Endepunkt for uthenting av bestemt dokument`() {
        coEvery { safService.hentDokumentPDF(any(), any(), any()) } returns "dokument".toByteArray()

        val journalpostId = "111"
        val dokumentInfoId = "333"

        testApplication {
            runServer(mockOAuth2Server, "api") {
                dokumentRoute(
                    safService,
                    dokarkivService,
                    tilgangssjekker,
                )
            }

            val response =
                client.get("/api/dokumenter/$journalpostId/$dokumentInfoId") {
                    header(HttpHeaders.Authorization, "Bearer $accessToken")
                }

            assertEquals(HttpStatusCode.OK, response.status)
        }

        coVerify(exactly = 1) { safService.hentDokumentPDF(journalpostId, dokumentInfoId, any()) }
    }

    @Test
    fun `Endepunkt som ikke finnes`() {
        testApplication {
            runServer(mockOAuth2Server, "api") {
                dokumentRoute(
                    safService,
                    dokarkivService,
                    tilgangssjekker,
                )
            }

            val response =
                client.get("/api/dokument/finnesikke") {
                    header(HttpHeaders.Authorization, "Bearer $accessToken")
                }

            assertEquals(HttpStatusCode.NotFound, response.status)
        }

        verify { safService wasNot Called }
    }
}
