package avkorting

import io.kotest.matchers.shouldBe
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.log
import io.ktor.server.testing.ApplicationTestBuilder
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.slot
import no.nav.etterlatte.avkorting.Avkorting
import no.nav.etterlatte.avkorting.AvkortingGrunnlag
import no.nav.etterlatte.avkorting.AvkortingService
import no.nav.etterlatte.avkorting.avkorting
import no.nav.etterlatte.beregning.regler.avkortetYtelse
import no.nav.etterlatte.beregning.regler.avkortinggrunnlag
import no.nav.etterlatte.beregning.regler.avkortingsperiode
import no.nav.etterlatte.klienter.BehandlingKlient
import no.nav.etterlatte.libs.common.beregning.AvkortetYtelseDto
import no.nav.etterlatte.libs.common.beregning.AvkortingDto
import no.nav.etterlatte.libs.common.beregning.AvkortingGrunnlagDto
import no.nav.etterlatte.libs.common.beregning.AvkortingGrunnlagKildeDto
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.periode.Periode
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.toJson
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
        coEvery { avkortingService.hentAvkorting(any(), any()) } returns null

        testApplication(server.config.httpServer.port()) {
            val response = client.get("/api/beregning/avkorting/${UUID.randomUUID()}") {
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                header(HttpHeaders.Authorization, "Bearer $token")
            }

            response.status shouldBe HttpStatusCode.NotFound
        }
    }

    @Test
    fun `skal motta og returnere avkorting`() {
        val behandlingsId = UUID.randomUUID()
        val avkortingsgrunnlagId = UUID.randomUUID()
        val dato = YearMonth.now()
        val tidspunkt = Tidspunkt.now()
        val avkortingsgrunnlag = avkortinggrunnlag(
            id = avkortingsgrunnlagId,
            periode = Periode(fom = dato, tom = dato),
            kilde = Grunnlagsopplysning.Saksbehandler("Saksbehandler01", tidspunkt)
        )
        val avkorting = Avkorting(
            behandlingId = behandlingsId,
            avkortingGrunnlag = mutableListOf(avkortingsgrunnlag),
            avkortingsperioder = mutableListOf(avkortingsperiode()),
            avkortetYtelse = mutableListOf(avkortetYtelse(periode = Periode(fom = dato, tom = dato)))
        )
        val dto = AvkortingDto(
            behandlingId = behandlingsId,
            avkortingGrunnlag = listOf(
                AvkortingGrunnlagDto(
                    id = avkortingsgrunnlagId,
                    fom = dato,
                    tom = dato,
                    aarsinntekt = 100000,
                    fratrekkInnUt = 10000,
                    spesifikasjon = "Spesifikasjon",
                    kilde = AvkortingGrunnlagKildeDto(
                        tidspunkt = tidspunkt.toString(),
                        ident = "Saksbehandler01"
                    )
                )
            ),
            avkortetYtelse = listOf(
                AvkortetYtelseDto(
                    fom = dato.atDay(1),
                    tom = dato.atEndOfMonth(),
                    avkortingsbeloep = 200,
                    ytelseEtterAvkorting = 100
                )
            )
        )
        val avkortingsgrunnlagSlot = slot<AvkortingGrunnlag>()
        coEvery { avkortingService.lagreAvkorting(any(), any(), capture(avkortingsgrunnlagSlot)) } returns avkorting

        testApplication(server.config.httpServer.port()) {
            val response = client.post("/api/beregning/avkorting/$behandlingsId") {
                setBody(dto.avkortingGrunnlag[0].toJson())
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                header(HttpHeaders.Authorization, "Bearer $token")
            }

            response.status shouldBe HttpStatusCode.OK
            val result = objectMapper.readValue(response.bodyAsText(), AvkortingDto::class.java)
            result shouldBe dto
            with(avkortingsgrunnlagSlot.captured) {
                periode shouldBe avkortingsgrunnlag.periode
                aarsinntekt shouldBe avkortingsgrunnlag.aarsinntekt
                fratrekkInnUt shouldBe avkortingsgrunnlag.fratrekkInnUt
                spesifikasjon shouldBe avkortingsgrunnlag.spesifikasjon
                kilde.ident shouldBe avkortingsgrunnlag.kilde.ident
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