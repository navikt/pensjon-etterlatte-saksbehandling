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
import io.mockk.coVerify
import io.mockk.mockk
import no.nav.etterlatte.avkorting.AvkortetYtelseType
import no.nav.etterlatte.avkorting.Avkorting
import no.nav.etterlatte.avkorting.AvkortingService
import no.nav.etterlatte.avkorting.Inntektsavkorting
import no.nav.etterlatte.avkorting.avkorting
import no.nav.etterlatte.avkorting.fromDto
import no.nav.etterlatte.beregning.regler.aarsoppgjoer
import no.nav.etterlatte.beregning.regler.avkortetYtelse
import no.nav.etterlatte.beregning.regler.avkortinggrunnlag
import no.nav.etterlatte.beregning.regler.bruker
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
        val dato = YearMonth.of(2023, 1)
        val tidspunkt = Tidspunkt.now()
        val avkortingsgrunnlag = avkortinggrunnlag(
            id = avkortingsgrunnlagId,
            periode = Periode(fom = dato, tom = dato),
            kilde = Grunnlagsopplysning.Saksbehandler("Saksbehandler01", tidspunkt),
            virkningstidspunkt = dato
        )
        val avkortetYtelseId = UUID.randomUUID()
        val avkortetYtelse = listOf(
            avkortetYtelse(
                id = avkortetYtelseId,
                type = AvkortetYtelseType.AARSOPPGJOER,
                periode = Periode(fom = dato, tom = dato)
            )
        )
        val inntektsavkorting = listOf(
            Inntektsavkorting(
                grunnlag = avkortingsgrunnlag
            )
        )
        val avkorting = Avkorting(
            aarsoppgjoer = aarsoppgjoer(
                inntektsavkorting = inntektsavkorting
            ),
            lopendeYtelse = avkortetYtelse,
            avkortetYtelseForrigeVedtak = avkortetYtelse,
        )
        val dto = AvkortingDto(
            avkortingGrunnlag = listOf(
                AvkortingGrunnlagDto(
                    id = avkortingsgrunnlagId,
                    fom = dato,
                    tom = dato,
                    aarsinntekt = 100000,
                    fratrekkInnAar = 10000,
                    relevanteMaanederInnAar = 12,
                    spesifikasjon = "Spesifikasjon",
                    kilde = AvkortingGrunnlagKildeDto(
                        tidspunkt = tidspunkt.toString(),
                        ident = "Saksbehandler01"
                    ),
                )
            ),
            avkortetYtelse = listOf(
                AvkortetYtelseDto(
                    id = avkortetYtelseId,
                    type = AvkortetYtelseType.AARSOPPGJOER.name,
                    fom = dato,
                    tom = dato,
                    ytelseFoerAvkorting = 300,
                    avkortingsbeloep = 200,
                    ytelseEtterAvkorting = 50,
                    restanse = 50
                )
            ),
            tidligereAvkortetYtelse = listOf(
                AvkortetYtelseDto(
                    id = avkortetYtelseId,
                    type = AvkortetYtelseType.AARSOPPGJOER.name,
                    fom = dato,
                    tom = dato,
                    ytelseFoerAvkorting = 300,
                    avkortingsbeloep = 200,
                    ytelseEtterAvkorting = 50,
                    restanse = 50
                )
            )
        )
        coEvery { avkortingService.lagreAvkorting(any(), any(), any()) } returns avkorting

        testApplication(server.config.httpServer.port()) {
            val response = client.post("/api/beregning/avkorting/$behandlingsId") {
                setBody(dto.avkortingGrunnlag[0].toJson())
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                header(HttpHeaders.Authorization, "Bearer $token")
            }

            response.status shouldBe HttpStatusCode.OK
            val result = objectMapper.readValue(response.bodyAsText(), AvkortingDto::class.java)
            result shouldBe dto
            coVerify {
                avkortingService.lagreAvkorting(
                    behandlingsId,
                    any(),
                    withArg {
                        it.periode shouldBe avkortingsgrunnlag.periode
                        it.aarsinntekt shouldBe avkortingsgrunnlag.aarsinntekt
                        it.fratrekkInnAar shouldBe avkortingsgrunnlag.fratrekkInnAar
                        it.relevanteMaanederInnAar shouldBe 12
                        it.spesifikasjon shouldBe avkortingsgrunnlag.spesifikasjon
                        it.kilde.ident shouldBe avkortingsgrunnlag.kilde.ident
                    }
                )
            }
        }
    }

    @Test
    fun `skal regne ut relevant antall maaneder inkludert innevaerende hvis det ikke finnes fra foer`() {
        val startenAvAaret = AvkortingGrunnlagDto(
            relevanteMaanederInnAar = null,
            id = UUID.randomUUID(),
            fom = YearMonth.of(2023, 1),
            tom = null,
            aarsinntekt = 100000,
            fratrekkInnAar = 10000,
            spesifikasjon = "Spesifikasjon",
            kilde = AvkortingGrunnlagKildeDto(
                tidspunkt = Tidspunkt.now().toString(),
                ident = "Saksbehandler01"
            )
        )
        val sluttenavAaret = startenAvAaret.copy(fom = YearMonth.of(2023, 12))

        startenAvAaret.fromDto(bruker).relevanteMaanederInnAar shouldBe 12
        sluttenavAaret.fromDto(bruker).relevanteMaanederInnAar shouldBe 1
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