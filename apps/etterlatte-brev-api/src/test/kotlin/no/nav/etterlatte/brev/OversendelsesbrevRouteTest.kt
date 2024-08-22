package no.nav.etterlatte.brev

import io.kotest.matchers.shouldBe
import io.ktor.client.HttpClient
import io.ktor.client.request.delete
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import no.nav.etterlatte.brev.model.Adresse
import no.nav.etterlatte.brev.model.Brev
import no.nav.etterlatte.brev.model.BrevProsessType
import no.nav.etterlatte.brev.model.Mottaker
import no.nav.etterlatte.brev.model.Spraak
import no.nav.etterlatte.brev.model.Status
import no.nav.etterlatte.brev.oversendelsebrev.OversendelseBrevService
import no.nav.etterlatte.brev.oversendelsebrev.oversendelseBrevRoute
import no.nav.etterlatte.ktor.runServer
import no.nav.etterlatte.ktor.startRandomPort
import no.nav.etterlatte.ktor.token.issueSaksbehandlerToken
import no.nav.etterlatte.libs.common.person.MottakerFoedselsnummer
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.ktor.route.Tilgangssjekker
import no.nav.security.mock.oauth2.MockOAuth2Server
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.util.UUID

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class OversendelsesbrevRouteTest {
    private val mockOAuth2Server = MockOAuth2Server()
    private val oversendelseBrevService = mockk<OversendelseBrevService>()
    private val tilgangssjekker = mockk<Tilgangssjekker>()

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

    private fun ApplicationTestBuilder.runServerAndGetClient(): HttpClient =
        runServer(mockOAuth2Server, "api") {
            oversendelseBrevRoute(oversendelseBrevService, tilgangssjekker)
        }

    @Test
    fun `Endepunkt for oppretting av oversendelsesbrev`() {
        coEvery { oversendelseBrevService.opprettOversendelseBrev(any(), any()) } returns opprettBrev()
        coEvery { tilgangssjekker.harTilgangTilBehandling(any(), any(), any()) } returns true

        testApplication {
            val client = runServerAndGetClient()

            val response =
                client.post("/api/brev/behandling/$BEHANDLING_ID/oversendelse") {
                    header(HttpHeaders.Authorization, "Bearer $accessToken")
                    contentType(ContentType.Application.Json)
                }
            response.status shouldBe HttpStatusCode.OK
        }
        coVerify { oversendelseBrevService.opprettOversendelseBrev(BEHANDLING_ID, any()) }
    }

    @Test
    fun `Endepunkt for sletting av oversendelsesbrev`() {
        coEvery { oversendelseBrevService.slettOversendelseBrev(any(), any()) } just runs
        coEvery { tilgangssjekker.harTilgangTilBehandling(any(), any(), any()) } returns true

        testApplication {
            val client = runServerAndGetClient()

            val response =
                client.delete("/api/brev/behandling/$BEHANDLING_ID/oversendelse") {
                    header(HttpHeaders.Authorization, "Bearer $accessToken")
                }
            response.status shouldBe HttpStatusCode.NoContent
        }
        coVerify { oversendelseBrevService.slettOversendelseBrev(BEHANDLING_ID, any()) }
    }

    private val accessToken: String by lazy { mockOAuth2Server.issueSaksbehandlerToken() }

    private fun opprettBrev() =
        Brev(
            1,
            41,
            BEHANDLING_ID,
            "tittel",
            Spraak.NB,
            BrevProsessType.AUTOMATISK,
            "soeker_fnr",
            Status.OPPRETTET,
            Tidspunkt.now(),
            Tidspunkt.now(),
            Mottaker(
                "Stor Snerk",
                STOR_SNERK,
                null,
                Adresse(adresseType = "NORSKPOSTADRESSE", "Testgaten 13", "1234", "OSLO", land = "Norge", landkode = "NOR"),
            ),
            brevtype = Brevtype.OVERSENDELSE_KLAGE,
            brevkoder = Brevkoder.TOMT_INFORMASJONSBREV,
        )

    companion object {
        private val STOR_SNERK = MottakerFoedselsnummer("11057523044")
        private val BEHANDLING_ID = UUID.randomUUID()
    }
}
