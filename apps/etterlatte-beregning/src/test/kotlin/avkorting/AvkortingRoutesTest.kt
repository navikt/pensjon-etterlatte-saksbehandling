package no.nav.etterlatte.avkorting

import io.kotest.matchers.shouldBe
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.ApplicationTestBuilder
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import no.nav.etterlatte.avkorting.etteroppgjoer.EtteroppgjoerService
import no.nav.etterlatte.avkorting.inntektsjustering.AarligInntektsjusteringService
import no.nav.etterlatte.avkorting.inntektsjustering.MottattInntektsjusteringService
import no.nav.etterlatte.beregning.regler.avkortinggrunnlag
import no.nav.etterlatte.klienter.BehandlingKlient
import no.nav.etterlatte.ktor.runServer
import no.nav.etterlatte.ktor.startRandomPort
import no.nav.etterlatte.ktor.token.issueSaksbehandlerToken
import no.nav.etterlatte.libs.common.beregning.AvkortetYtelseDto
import no.nav.etterlatte.libs.common.beregning.AvkortingFrontendGammelDto
import no.nav.etterlatte.libs.common.beregning.AvkortingGrunnlagKildeDto
import no.nav.etterlatte.libs.common.beregning.AvkortingGrunnlagLagreDto
import no.nav.etterlatte.libs.common.beregning.ForventetInntektDto
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.periode.Periode
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.toJson
import no.nav.security.mock.oauth2.MockOAuth2Server
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.time.YearMonth
import java.util.UUID

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AvkortingRoutesTest {
    private val mockOAuth2Server = MockOAuth2Server()
    private val behandlingKlient = mockk<BehandlingKlient>()
    private val avkortingService = mockk<AvkortingService>()
    private val avkortingTidligAlderspensjonService = mockk<AvkortingTidligAlderspensjonService>()
    private val aarligInntektsjusteringService = mockk<AarligInntektsjusteringService>()
    private val mottattInntektsjusteringService = mockk<MottattInntektsjusteringService>()
    private val etteroppgjoerService = mockk<EtteroppgjoerService>()

    @BeforeAll
    fun beforeAll() {
        mockOAuth2Server.startRandomPort()
        coEvery { behandlingKlient.harTilgangTilBehandling(any(), any(), any()) } returns true
    }

    @AfterAll
    fun afterAll() {
        mockOAuth2Server.shutdown()
    }

    @Test
    fun `skal returnere 204 naar avkorting ikke finnes`() {
        coEvery { avkortingService.hentOpprettEllerReberegnAvkorting(any(), any()) } returns null

        testApplication {
            val response =
                client.get("/api/beregning/avkorting/${UUID.randomUUID()}") {
                    header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    header(HttpHeaders.Authorization, "Bearer ${mockOAuth2Server.issueSaksbehandlerToken()}")
                }

            response.status shouldBe HttpStatusCode.NoContent
        }
    }

    @Test
    fun `skal motta og returnere avkorting`() {
        val behandlingsId = UUID.randomUUID()
        val avkortingsgrunnlagId = UUID.randomUUID()
        val dato = YearMonth.of(2023, 1)
        val tidspunkt = Tidspunkt.now()
        val avkortingsgrunnlag =
            avkortinggrunnlag(
                id = avkortingsgrunnlagId,
                periode = Periode(fom = dato, tom = dato),
                kilde = Grunnlagsopplysning.Saksbehandler("Saksbehandler01", tidspunkt),
            )
        val avkortetYtelseId = UUID.randomUUID()
        val avkorting =
            AvkortingFrontendGammelDto(
                redigerbarForventetInntekt =
                    ForventetInntektDto(
                        id = avkortingsgrunnlagId,
                        fom = dato,
                        tom = dato,
                        inntektTom = 100000,
                        fratrekkInnAar = 10000,
                        spesifikasjon = "Spesifikasjon",
                        inntektUtlandTom = 0,
                        fratrekkInnAarUtland = 0,
                        innvilgaMaaneder = 12,
                        inntektInnvilgetPeriode = 90000,
                        kilde =
                            AvkortingGrunnlagKildeDto(
                                tidspunkt = tidspunkt.toString(),
                                ident = "Saksbehandler01",
                            ),
                    ),
                redigerbarForventetInntektNesteAar = null,
                avkortingGrunnlag =
                    listOf(
                        ForventetInntektDto(
                            id = avkortingsgrunnlagId,
                            fom = dato,
                            tom = dato,
                            inntektTom = 100000,
                            fratrekkInnAar = 10000,
                            spesifikasjon = "Spesifikasjon",
                            inntektUtlandTom = 0,
                            fratrekkInnAarUtland = 0,
                            innvilgaMaaneder = 12,
                            inntektInnvilgetPeriode = 90000,
                            kilde =
                                AvkortingGrunnlagKildeDto(
                                    tidspunkt = tidspunkt.toString(),
                                    ident = "Saksbehandler01",
                                ),
                        ),
                    ),
                avkortetYtelse =
                    listOf(
                        AvkortetYtelseDto(
                            id = avkortetYtelseId,
                            type = AvkortetYtelseType.AARSOPPGJOER.name,
                            fom = dato,
                            tom = dato,
                            ytelseFoerAvkorting = 300,
                            avkortingsbeloep = 200,
                            ytelseEtterAvkorting = 50,
                            restanse = 50,
                            sanksjon = null,
                        ),
                    ),
                tidligereAvkortetYtelse =
                    listOf(
                        AvkortetYtelseDto(
                            id = avkortetYtelseId,
                            type = AvkortetYtelseType.AARSOPPGJOER.name,
                            fom = dato,
                            tom = dato,
                            ytelseFoerAvkorting = 300,
                            avkortingsbeloep = 200,
                            ytelseEtterAvkorting = 50,
                            restanse = 50,
                            sanksjon = null,
                        ),
                    ),
            )
        coEvery { avkortingService.beregnAvkortingMedNyttGrunnlag(any(), any(), any()) } returns avkorting

        testApplication {
            val response =
                client.post("/api/beregning/avkorting/$behandlingsId") {
                    val request =
                        with(avkorting.redigerbarForventetInntekt!!) {
                            AvkortingGrunnlagLagreDto(
                                id = id,
                                inntektTom = inntektTom,
                                fratrekkInnAar = fratrekkInnAar,
                                inntektUtlandTom = inntektUtlandTom,
                                fratrekkInnAarUtland = fratrekkInnAarUtland,
                                spesifikasjon = spesifikasjon,
                                fom = dato,
                            )
                        }
                    setBody(request.toJson())
                    header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    header(HttpHeaders.Authorization, "Bearer ${mockOAuth2Server.issueSaksbehandlerToken()}")
                }

            response.status shouldBe HttpStatusCode.OK
            val result = objectMapper.readValue(response.bodyAsText(), AvkortingFrontendGammelDto::class.java)
            result shouldBe avkorting
            coVerify {
                avkortingService.beregnAvkortingMedNyttGrunnlag(
                    behandlingsId,
                    any(),
                    withArg {
                        it.inntektTom shouldBe avkortingsgrunnlag.inntektTom
                        it.fratrekkInnAar shouldBe avkortingsgrunnlag.fratrekkInnAar
                        it.spesifikasjon shouldBe avkortingsgrunnlag.spesifikasjon
                    },
                )
            }
        }
    }

    private fun testApplication(block: suspend ApplicationTestBuilder.() -> Unit) {
        io.ktor.server.testing.testApplication {
            runServer(mockOAuth2Server) {
                avkorting(
                    avkortingService,
                    behandlingKlient,
                    avkortingTidligAlderspensjonService,
                    aarligInntektsjusteringService,
                    mottattInntektsjusteringService,
                    etteroppgjoerService,
                )
            }
            block(this)
        }
    }
}
