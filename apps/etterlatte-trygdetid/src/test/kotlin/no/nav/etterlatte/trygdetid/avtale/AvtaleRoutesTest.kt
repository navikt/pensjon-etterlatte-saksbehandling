package no.nav.etterlatte.trygdetid.avtale

import io.kotest.matchers.shouldBe
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import no.nav.etterlatte.ktor.runServer
import no.nav.etterlatte.ktor.startRandomPort
import no.nav.etterlatte.ktor.token.issueSaksbehandlerToken
import no.nav.etterlatte.libs.common.trygdetid.avtale.Trygdeavtale
import no.nav.etterlatte.libs.common.trygdetid.avtale.TrygdetidAvtale
import no.nav.etterlatte.libs.common.trygdetid.avtale.TrygdetidAvtaleKriteria
import no.nav.etterlatte.trygdetid.klienter.BehandlingKlient
import no.nav.etterlatte.trygdetid.trygdeavtale
import no.nav.security.mock.oauth2.MockOAuth2Server
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.time.LocalDate
import java.time.Month
import java.util.UUID.randomUUID

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class AvtaleRoutesTest {
    private val repository = mockk<AvtaleRepository>()
    private val behandlingKlient = mockk<BehandlingKlient>()

    private val service = AvtaleService(repository)

    private val mockOAuth2Server = MockOAuth2Server()

    @BeforeAll
    fun beforeAll() {
        mockOAuth2Server.startRandomPort()
    }

    @AfterAll
    fun afterAll() {
        mockOAuth2Server.shutdown()
    }

    @BeforeEach
    fun beforeEach() {
        clearAllMocks()
        coEvery { behandlingKlient.harTilgangTilBehandling(any(), any(), any()) } returns true
    }

    @AfterEach
    fun afterEach() {
        confirmVerified()
    }

    @Test
    fun `skal levere informasjon om avtaler`() {
        testApplication { client ->
            val response =
                client.get("/api/trygdetid/avtaler") {
                    header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    header(HttpHeaders.Authorization, "Bearer $token")
                }

            response.status shouldBe HttpStatusCode.OK

            val avtaler = response.body<List<TrygdetidAvtale>>()

            avtaler.size shouldBe 26
            with(avtaler.first { it.kode == "EOS_NOR" }) {
                this.beskrivelse shouldBe "Eøs-Avtalen/Nordisk Konvensjon"
                this.fraDato shouldBe LocalDate.of(1899, Month.DECEMBER, 31)
            }

            with(avtaler.first { it.kode == "BIH" }) {
                this.beskrivelse shouldBe "Bosnia-Hercegovina"
                this.fraDato shouldBe LocalDate.of(1899, Month.DECEMBER, 31)

                val trygdetidDato = this.datoer.first()

                trygdetidDato.kode shouldBe "BIH1992"
                trygdetidDato.beskrivelse shouldBe "01.03.1992"
                trygdetidDato.fraDato shouldBe LocalDate.of(1899, Month.DECEMBER, 31)
            }
        }
    }

    @Test
    fun `skal levere informasjon om avtale kriterier`() {
        testApplication { client ->
            val response =
                client.get("/api/trygdetid/avtaler/kriteria") {
                    header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    header(HttpHeaders.Authorization, "Bearer $token")
                }

            response.status shouldBe HttpStatusCode.OK

            val avtaler = response.body<List<TrygdetidAvtaleKriteria>>()

            avtaler.size shouldBe 7
            with(avtaler.first { it.kode == "YRK_MEDL" }) {
                this.beskrivelse shouldBe "Yrkesaktiv i Norge eller EØS, ett års medlemskap i Norge"
                this.fraDato shouldBe LocalDate.of(1899, Month.DECEMBER, 31)
            }
        }
    }

    @Test
    fun `skal levere avtaler for behandlinger`() {
        testApplication { client ->
            val behandlingId = randomUUID()

            every { repository.hentAvtale(behandlingId) } returns trygdeavtale(behandlingId, avtaleKode = "TEST")

            val response =
                client.get("/api/trygdetid/avtaler/$behandlingId") {
                    header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    header(HttpHeaders.Authorization, "Bearer $token")
                }

            response.status shouldBe HttpStatusCode.OK

            val avtale = response.body<Trygdeavtale>()

            avtale.avtaleKode shouldBe "TEST"

            coVerify(exactly = 1) { behandlingKlient.harTilgangTilBehandling(any(), any(), any()) }
            verify(exactly = 1) { repository.hentAvtale(any()) }
        }
    }

    @Test
    fun `skal opprette avtaler for behandlinger`() {
        testApplication { client ->
            val behandlingId = randomUUID()

            every { repository.opprettAvtale(any()) } just runs

            val response =
                client.post("/api/trygdetid/avtaler/$behandlingId") {
                    header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    header(HttpHeaders.Authorization, "Bearer $token")
                    setBody(
                        TrygdeavtaleRequest(
                            id = null,
                            avtaleKode = "TEST",
                            avtaleDatoKode = "TESTDATO",
                            avtaleKriteriaKode = "TESTKRITERIA",
                            personKrets = null,
                            arbInntekt1G = null,
                            arbInntekt1GKommentar = null,
                            beregArt50 = null,
                            beregArt50Kommentar = null,
                            nordiskTrygdeAvtale = null,
                            nordiskTrygdeAvtaleKommentar = null,
                        ),
                    )
                }

            response.status shouldBe HttpStatusCode.OK

            val avtale = response.body<Trygdeavtale>()

            avtale.avtaleKode shouldBe "TEST"
            avtale.avtaleDatoKode shouldBe "TESTDATO"
            avtale.avtaleKriteriaKode shouldBe "TESTKRITERIA"
            avtale.behandlingId shouldBe behandlingId
            avtale.kilde.ident shouldBe "Saksbehandler01"

            coVerify(exactly = 1) { behandlingKlient.harTilgangTilBehandling(any(), any(), any()) }
            verify(exactly = 1) { repository.opprettAvtale(any()) }
        }
    }

    @Test
    fun `skal oppdatere avtaler for behandlinger`() {
        testApplication { client ->
            val behandlingId = randomUUID()
            val id = randomUUID()

            every { repository.lagreAvtale(any()) } just runs

            val response =
                client.post("/api/trygdetid/avtaler/$behandlingId") {
                    header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    header(HttpHeaders.Authorization, "Bearer $token")
                    setBody(
                        TrygdeavtaleRequest(
                            id = id,
                            avtaleKode = "TEST",
                            avtaleDatoKode = "TESTDATO",
                            avtaleKriteriaKode = "TESTKRITERIA",
                            personKrets = null,
                            arbInntekt1G = null,
                            arbInntekt1GKommentar = null,
                            beregArt50 = null,
                            beregArt50Kommentar = null,
                            nordiskTrygdeAvtale = null,
                            nordiskTrygdeAvtaleKommentar = null,
                        ),
                    )
                }

            response.status shouldBe HttpStatusCode.OK

            val avtale = response.body<Trygdeavtale>()

            avtale.avtaleKode shouldBe "TEST"
            avtale.avtaleDatoKode shouldBe "TESTDATO"
            avtale.avtaleKriteriaKode shouldBe "TESTKRITERIA"
            avtale.behandlingId shouldBe behandlingId
            avtale.kilde.ident shouldBe "Saksbehandler01"

            coVerify(exactly = 1) { behandlingKlient.harTilgangTilBehandling(any(), any(), any()) }
            verify(exactly = 1) { repository.lagreAvtale(any()) }
        }
    }

    private fun testApplication(block: suspend (client: HttpClient) -> Unit) {
        io.ktor.server.testing.testApplication {
            val client =
                runServer(mockOAuth2Server) {
                    avtale(service, behandlingKlient)
                }

            block(client)
        }
    }

    private val token: String by lazy { mockOAuth2Server.issueSaksbehandlerToken() }
}
