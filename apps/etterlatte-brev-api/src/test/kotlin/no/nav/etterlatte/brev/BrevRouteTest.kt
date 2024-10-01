package no.nav.etterlatte.brev

import com.fasterxml.jackson.module.kotlin.readValue
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import io.mockk.Called
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.verify
import no.nav.etterlatte.behandling.randomSakId
import no.nav.etterlatte.brev.BrevService.BrevPayload
import no.nav.etterlatte.brev.distribusjon.Brevdistribuerer
import no.nav.etterlatte.brev.hentinformasjon.behandling.BehandlingService
import no.nav.etterlatte.brev.hentinformasjon.grunnlag.GrunnlagService
import no.nav.etterlatte.brev.model.Adresse
import no.nav.etterlatte.brev.model.Brev
import no.nav.etterlatte.brev.model.BrevID
import no.nav.etterlatte.brev.model.BrevProsessType
import no.nav.etterlatte.brev.model.Mottaker
import no.nav.etterlatte.brev.model.Pdf
import no.nav.etterlatte.brev.model.Slate
import no.nav.etterlatte.brev.model.Spraak
import no.nav.etterlatte.brev.model.Status
import no.nav.etterlatte.brev.pdf.PDFService
import no.nav.etterlatte.ktor.runServer
import no.nav.etterlatte.ktor.startRandomPort
import no.nav.etterlatte.ktor.token.issueSaksbehandlerToken
import no.nav.etterlatte.libs.common.objectMapper
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
import kotlin.random.Random

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class BrevRouteTest {
    private val mockOAuth2Server = MockOAuth2Server()
    private val brevService = mockk<BrevService>()
    private val pdfService = mockk<PDFService>()
    private val brevdistribuerer = mockk<Brevdistribuerer>()
    private val tilgangssjekker = mockk<Tilgangssjekker>()
    private val grunnlagService = mockk<GrunnlagService>()
    private val behandlingService = mockk<BehandlingService>()

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
        val brevId = Random.nextLong()

        coEvery { brevService.hentBrev(any()) } returns opprettBrev(brevId)
        coEvery { tilgangssjekker.harTilgangTilSak(any(), any(), any()) } returns true

        testApplication {
            val client = httpClient()

            val response =
                client.get("/api/brev/$brevId") {
                    parameter("sakId", SAK_ID)
                    header(HttpHeaders.Authorization, "Bearer $accessToken")
                    contentType(ContentType.Application.Json)
                }

            assertEquals(HttpStatusCode.OK, response.status)
        }

        coVerify { brevService.hentBrev(brevId) }
    }

    @Test
    fun `Endepunkt for generering av pdf`() {
        val brevId = Random.nextLong()
        val pdf = Pdf("Hello world".toByteArray())

        coEvery { brevService.genererPdf(any(), any()) } returns pdf
        coEvery { tilgangssjekker.harTilgangTilSak(any(), any(), any()) } returns true

        testApplication {
            val client = httpClient()

            val response =
                client.get("/api/brev/$brevId/pdf") {
                    parameter("sakId", SAK_ID)
                    header(HttpHeaders.Authorization, "Bearer $accessToken")
                    contentType(ContentType.Application.Json)
                }

            assertEquals(HttpStatusCode.OK, response.status)
            assertArrayEquals(pdf.bytes, response.body())
        }

        coVerify(exactly = 1) {
            brevService.genererPdf(brevId, any())
            tilgangssjekker.harTilgangTilSak(any(), any(), any())
        }
    }

    @Test
    fun deserialiser() {
        val mottaker =
            """{
            "navn": "Peder Ås",
            "foedselsnummer": {
                "value": "25478323363"
            },
            "orgnummer": null,
            "adresse": {
                "adresseType": "123",
                "landkode": "NO",
                "land": "Norge"
            }
        }
            """.trimMargin()
        objectMapper.readValue<Mottaker>(mottaker)
    }

    @Test
    fun `Endepunkt for henting av manuelt brev`() {
        val brevId = Random.nextLong()

        coEvery { brevService.hentBrevPayload(any()) } returns BrevPayload(Slate(), null)
        coEvery { tilgangssjekker.harTilgangTilSak(any(), any(), any()) } returns true

        testApplication {
            val client = httpClient()

            val response =
                client.get("/api/brev/$brevId/payload") {
                    parameter("sakId", SAK_ID)
                    header(HttpHeaders.Authorization, "Bearer $accessToken")
                    contentType(ContentType.Application.Json)
                }

            assertEquals(HttpStatusCode.OK, response.status)
        }

        coVerify(exactly = 1) {
            brevService.hentBrevPayload(brevId)
            tilgangssjekker.harTilgangTilSak(any(), any(), any())
        }
    }

    @Test
    fun `Endepunkt for lagring av manuelt brev`() {
        val brevId = Random.nextLong()
        coEvery { brevService.lagreBrevPayload(any(), any()) } returns 1
        coEvery { brevService.lagreBrevPayloadVedlegg(any(), any()) } returns 1
        coEvery { tilgangssjekker.harTilgangTilSak(any(), any(), any()) } returns true

        testApplication {
            val client = httpClient()

            val response =
                client.post("/api/brev/$brevId/payload") {
                    parameter("sakId", SAK_ID)
                    header(HttpHeaders.Authorization, "Bearer $accessToken")
                    contentType(ContentType.Application.Json)
                    setBody(OppdaterPayloadRequest(Slate(), listOf()))
                }

            assertEquals(HttpStatusCode.OK, response.status)
        }

        coVerify(exactly = 1) {
            brevService.lagreBrevPayload(brevId, any())
            tilgangssjekker.harTilgangTilSak(any(), any(), any())
        }
    }

    @Test
    fun `Endepunkt som ikke finnes`() {
        coEvery { tilgangssjekker.harTilgangTilSak(any(), any(), any()) } returns true

        testApplication {
            val client = httpClient()

            val response =
                client.get("/api/brev/123/baretull") {
                    header(HttpHeaders.Authorization, "Bearer $accessToken")
                }

            assertEquals(HttpStatusCode.NotFound, response.status)
        }

        verify {
            brevService wasNot Called
            tilgangssjekker wasNot Called
        }
    }

    @Test
    fun `Mangler auth header`() {
        testApplication {
            val client = httpClient()

            val response = client.get("/api/brev/123")

            assertEquals(HttpStatusCode.Unauthorized, response.status)
        }

        verify {
            brevService wasNot Called
            tilgangssjekker wasNot Called
        }
    }

    private val accessToken: String by lazy { mockOAuth2Server.issueSaksbehandlerToken() }

    private fun opprettBrev(id: BrevID) =
        Brev(
            id = id,
            sakId = randomSakId(),
            behandlingId = null,
            tittel = null,
            spraak = Spraak.NB,
            prosessType = BrevProsessType.AUTOMATISK,
            soekerFnr = "soeker_fnr",
            status = Status.OPPRETTET,
            statusEndret = Tidspunkt.now(),
            opprettet = Tidspunkt.now(),
            mottaker =
                Mottaker(
                    "Stor Snerk",
                    STOR_SNERK,
                    null,
                    Adresse(
                        adresseType = "NORSKPOSTADRESSE",
                        "Testgaten 13",
                        "1234",
                        "OSLO",
                        land = "Norge",
                        landkode = "NOR",
                    ),
                ),
            brevtype = Brevtype.INFORMASJON,
            brevkoder = Brevkoder.TOMT_INFORMASJONSBREV,
        )

    private fun ApplicationTestBuilder.httpClient(): HttpClient =
        runServer(mockOAuth2Server, "api") {
            brevRoute(
                brevService,
                pdfService,
                brevdistribuerer,
                tilgangssjekker,
                grunnlagService,
                behandlingService,
            )
        }

    companion object {
        private val STOR_SNERK = MottakerFoedselsnummer("11057523044")
        private val SAK_ID = Random.nextLong(1000)
    }
}
