package no.nav.etterlatte.brev

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import io.mockk.Called
import io.mockk.Runs
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import no.nav.etterlatte.behandling.randomSakId
import no.nav.etterlatte.brev.model.Adresse
import no.nav.etterlatte.brev.model.Brev
import no.nav.etterlatte.brev.model.BrevProsessType
import no.nav.etterlatte.brev.model.Mottaker
import no.nav.etterlatte.brev.model.Pdf
import no.nav.etterlatte.brev.model.Spraak
import no.nav.etterlatte.brev.model.Status
import no.nav.etterlatte.brev.vedtaksbrev.VedtaksbrevService
import no.nav.etterlatte.brev.vedtaksbrev.vedtaksbrevRoute
import no.nav.etterlatte.ktor.runServer
import no.nav.etterlatte.ktor.startRandomPort
import no.nav.etterlatte.ktor.token.issueSaksbehandlerToken
import no.nav.etterlatte.libs.common.deserialize
import no.nav.etterlatte.libs.common.person.MottakerFoedselsnummer
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.ktor.route.Tilgangssjekker
import no.nav.security.mock.oauth2.MockOAuth2Server
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.util.UUID
import kotlin.random.Random

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class VedtaksbrevRouteTest {
    private val mockOAuth2Server = MockOAuth2Server()
    private val vedtaksbrevService = mockk<VedtaksbrevService>()
    private val tilgangssjekker = mockk<Tilgangssjekker>()

    companion object {
        private val STOR_SNERK = MottakerFoedselsnummer("11057523044")
        private val BEHANDLING_ID = UUID.randomUUID()
        private val SAK_ID = randomSakId()
    }

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
    fun `Endepunkt for henting av vedtaksbrev - brev finnes`() {
        coEvery { vedtaksbrevService.hentVedtaksbrev(any()) } returns opprettBrev()
        coEvery { tilgangssjekker.harTilgangTilBehandling(any(), any(), any()) } returns true

        testApplication {
            val client = httpClient()

            val response =
                client.get("/api/brev/behandling/$BEHANDLING_ID/vedtak") {
                    header(HttpHeaders.Authorization, "Bearer $accessToken")
                    contentType(ContentType.Application.Json)
                }

            assertEquals(HttpStatusCode.OK, response.status)
        }

        coVerify { vedtaksbrevService.hentVedtaksbrev(BEHANDLING_ID) }
    }

    @Test
    fun `Endepunkt for henting av vedtaksbrev - brev finnes ikke`() {
        coEvery { vedtaksbrevService.hentVedtaksbrev(any()) } returns null
        coEvery { tilgangssjekker.harTilgangTilBehandling(any(), any(), any()) } returns true

        testApplication {
            val client = httpClient()

            val response =
                client.get("/api/brev/behandling/$BEHANDLING_ID/vedtak") {
                    header(HttpHeaders.Authorization, "Bearer $accessToken")
                    contentType(ContentType.Application.Json)
                }

            assertEquals(HttpStatusCode.NoContent, response.status)
        }

        coVerify {
            vedtaksbrevService.hentVedtaksbrev(BEHANDLING_ID)
            tilgangssjekker.harTilgangTilBehandling(any(), any(), any())
        }
    }

    @Test
    fun `Endepunkt for oppretting av vedtaksbrev`() {
        coEvery { vedtaksbrevService.opprettVedtaksbrev(any(), any(), any()) } returns opprettBrev()
        coEvery { tilgangssjekker.harTilgangTilBehandling(any(), any(), any()) } returns true

        testApplication {
            val client = httpClient()

            val response =
                client.post("/api/brev/behandling/$BEHANDLING_ID/vedtak") {
                    parameter("sakId", SAK_ID)
                    header(HttpHeaders.Authorization, "Bearer $accessToken")
                    contentType(ContentType.Application.Json)
                }

            assertEquals(HttpStatusCode.Created, response.status)
        }

        coVerify(exactly = 1) {
            vedtaksbrevService.opprettVedtaksbrev(SAK_ID, BEHANDLING_ID, any())
            tilgangssjekker.harTilgangTilBehandling(any(), any(), any())
        }
    }

    @Test
    fun `Endepunkt for generering av pdf`() {
        val brevId = Random.nextLong()
        val pdf = Pdf("Hello world".toByteArray())

        coEvery { vedtaksbrevService.genererPdf(any(), any()) } returns pdf
        coEvery { tilgangssjekker.harTilgangTilBehandling(any(), any(), any()) } returns true

        testApplication {
            val client = httpClient()

            val response =
                client.get("/api/brev/behandling/$BEHANDLING_ID/vedtak/pdf") {
                    parameter("brevId", brevId)
                    header(HttpHeaders.Authorization, "Bearer $accessToken")
                    contentType(ContentType.Application.Json)
                }

            val responseAsPdf: Pdf = deserialize(response.body())
            assertEquals(HttpStatusCode.OK, response.status)
            assertArrayEquals(pdf.bytes, responseAsPdf.bytes)
        }

        coVerify(exactly = 1) {
            vedtaksbrevService.genererPdf(brevId, any())
            tilgangssjekker.harTilgangTilBehandling(any(), any(), any())
        }
    }

    @Test
    fun `Endepunkt for ferdigstilling av vedtaksbrev`() {
        coEvery { vedtaksbrevService.ferdigstillVedtaksbrev(any(), any()) } just Runs
        coEvery { tilgangssjekker.harTilgangTilBehandling(any(), any(), any()) } returns true

        testApplication {
            val client = httpClient()

            val response =
                client.post("/api/brev/behandling/$BEHANDLING_ID/vedtak/ferdigstill") {
                    header(HttpHeaders.Authorization, "Bearer $accessToken")
                    contentType(ContentType.Application.Json)
                }

            assertEquals(HttpStatusCode.OK, response.status)
        }

        coVerify(exactly = 1) {
            vedtaksbrevService.ferdigstillVedtaksbrev(BEHANDLING_ID, any())
            tilgangssjekker.harTilgangTilBehandling(any(), any(), any())
        }
    }

    @Test
    fun `Endepunkt som ikke finnes`() {
        coEvery { tilgangssjekker.harTilgangTilBehandling(any(), any(), any()) } returns true

        testApplication {
            val client = httpClient()

            val response =
                client.get("/api/brev/finnesikke") {
                    header(HttpHeaders.Authorization, "Bearer $accessToken")
                }

            assertEquals(HttpStatusCode.NotFound, response.status)
        }

        verify {
            vedtaksbrevService wasNot Called
            tilgangssjekker wasNot Called
        }
    }

    @Test
    fun `Mangler auth header`() {
        testApplication {
            runServer(mockOAuth2Server, "api") {
                vedtaksbrevRoute(
                    vedtaksbrevService,
                    mockk(),
                    tilgangssjekker,
                )
            }

            val response = client.post("/api/brev/behandling/${UUID.randomUUID()}/vedtak")

            assertEquals(HttpStatusCode.Unauthorized, response.status)
        }

        verify {
            vedtaksbrevService wasNot Called
            tilgangssjekker wasNot Called
        }
    }

    val accessToken: String by lazy { mockOAuth2Server.issueSaksbehandlerToken() }

    private fun opprettBrev() =
        Brev(
            1,
            randomSakId(),
            BEHANDLING_ID,
            "tittel",
            Spraak.NB,
            BrevProsessType.AUTOMATISK,
            "soeker_fnr",
            Status.OPPRETTET,
            Tidspunkt.now(),
            Tidspunkt.now(),
            listOf(
                Mottaker(
                    UUID.randomUUID(),
                    "Stor Snerk",
                    STOR_SNERK,
                    null,
                    Adresse(adresseType = "NORSKPOSTADRESSE", "Testgaten 13", "1234", "OSLO", land = "Norge", landkode = "NOR"),
                ),
            ),
            brevtype = Brevtype.INFORMASJON,
            brevkoder = Brevkoder.TOMT_INFORMASJONSBREV,
        )

    private fun ApplicationTestBuilder.httpClient(): HttpClient =
        runServer(mockOAuth2Server, "api") {
            vedtaksbrevRoute(
                vedtaksbrevService,
                mockk(),
                tilgangssjekker,
            )
        }
}
