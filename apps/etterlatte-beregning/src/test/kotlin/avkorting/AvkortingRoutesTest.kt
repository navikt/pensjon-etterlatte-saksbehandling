package avkorting

import io.kotest.matchers.shouldBe
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.log
import io.ktor.server.testing.ApplicationTestBuilder
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import no.nav.etterlatte.avkorting.Avkorting
import no.nav.etterlatte.avkorting.AvkortingService
import no.nav.etterlatte.avkorting.avkorting
import no.nav.etterlatte.beregning.regler.avkortetYtelse
import no.nav.etterlatte.beregning.regler.avkortinggrunnlag
import no.nav.etterlatte.klienter.BehandlingKlient
import no.nav.etterlatte.libs.common.beregning.AvkortingDto
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.periode.Periode
import no.nav.etterlatte.libs.ktor.AZURE_ISSUER
import no.nav.etterlatte.libs.ktor.restModule
import no.nav.security.mock.oauth2.MockOAuth2Server
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import testsupport.buildTestApplicationConfigurationForOauth
import java.time.YearMonth
import java.util.*

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AvkortingRoutesTest {

    private val server = MockOAuth2Server()
    private val behandlingKlient = mockk<BehandlingKlient>()
    private val avkortingService = mockk<AvkortingService>()

    @BeforeAll
    fun beforeAll() {
        server.start()
        coEvery { behandlingKlient.harTilgangTilBehandling(any(), any()) } returns true
    }

    @AfterAll
    fun afterAll() {
        server.shutdown()
    }

    @Test
    fun `skal returnere 404 naar avkorting ikke finnes`() {
        every { avkortingService.hentAvkorting(any()) } returns null

        testApplication(server.config.httpServer.port()) {
            val response = client.get("/api/beregning/avkorting/${UUID.randomUUID()}") {
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                header(HttpHeaders.Authorization, "Bearer $token")
            }

            response.status shouldBe HttpStatusCode.NotFound
        }
    }

    @Test
    fun `skal returnere avkorting med avkortingsgrunnlag og beregnet avkorting over flere perioder`() {
        every { avkortingService.hentAvkorting(any()) } returns Avkorting(
            behandlingId = UUID.randomUUID(),
            avkortingGrunnlag = listOf(avkortinggrunnlag(), avkortinggrunnlag()),
            avkortingsperioder = emptyList(),
            avkortetYtelse = listOf(
                avkortetYtelse(100, 1000, periode = Periode(YearMonth.of(2023, 1), YearMonth.of(2023, 1))),
                avkortetYtelse(200, 2000, periode = Periode(YearMonth.of(2023, 2), YearMonth.of(2023, 2))),
                avkortetYtelse(300, 2000, periode = Periode(YearMonth.of(2023, 4), YearMonth.of(2023, 2))),
                avkortetYtelse(400, 5000, periode = Periode(YearMonth.of(2023, 5), null))
            )
        )
        testApplication(server.config.httpServer.port()) {
            val response = client.get("/api/beregning/avkorting/${UUID.randomUUID()}") {
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                header(HttpHeaders.Authorization, "Bearer $token")
            }

            response.status shouldBe HttpStatusCode.OK

            val avkorting = objectMapper.readValue(response.bodyAsText(), AvkortingDto::class.java)
            with(avkorting.avkortetYtelse[0]) {
                avkortingsbeloep shouldBe 1000
                ytelseEtterAvkorting shouldBe 100
            }
            with(avkorting.avkortetYtelse[1]) {
                avkortingsbeloep shouldBe 2000
                ytelseEtterAvkorting shouldBe 200
            }
            with(avkorting.avkortetYtelse[2]) {
                avkortingsbeloep shouldBe 2000
                ytelseEtterAvkorting shouldBe 300
            }
            with(avkorting.avkortetYtelse[3]) {
                avkortingsbeloep shouldBe 5000
                ytelseEtterAvkorting shouldBe 400
            }
        }
    }

    private fun testApplication(port: Int, block: suspend ApplicationTestBuilder.() -> Unit) {
        io.ktor.server.testing.testApplication {
            environment {
                config = buildTestApplicationConfigurationForOauth(port, AZURE_ISSUER, AZURE_CLIENT_ID)
            }
            application { restModule(log) { avkorting(avkortingService, behandlingKlient) } }

            block(this)
        }
    }

    private val token: String by lazy {
        server.issueToken(
            issuerId = AZURE_ISSUER,
            audience = AZURE_CLIENT_ID,
            claims = mapOf("navn" to "John Doe", "NAVident" to "Saksbehandler01")
        ).serialize()
    }

    private companion object {
        const val AZURE_CLIENT_ID: String = "azure-id"
    }
}