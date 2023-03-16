package vedtaksvurdering

import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.log
import io.ktor.server.config.HoconApplicationConfig
import io.ktor.server.testing.testApplication
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.mockk
import no.nav.etterlatte.libs.common.behandling.VedtakStatus
import no.nav.etterlatte.libs.common.deserialize
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.toJson
import no.nav.etterlatte.libs.common.vedtak.Attestasjon
import no.nav.etterlatte.libs.common.vedtak.LoependeYtelseDTO
import no.nav.etterlatte.libs.common.vedtak.Periode
import no.nav.etterlatte.libs.common.vedtak.UtbetalingsperiodeType
import no.nav.etterlatte.libs.common.vedtak.VedtakDto
import no.nav.etterlatte.libs.common.vedtak.VedtakFattet
import no.nav.etterlatte.libs.ktor.AZURE_ISSUER
import no.nav.etterlatte.libs.ktor.restModule
import no.nav.etterlatte.vedtaksvurdering.LoependeYtelse
import no.nav.etterlatte.vedtaksvurdering.UnderkjennVedtakDto
import no.nav.etterlatte.vedtaksvurdering.VedtakSammendragDto
import no.nav.etterlatte.vedtaksvurdering.VedtaksvurderingService
import no.nav.etterlatte.vedtaksvurdering.klienter.BehandlingKlient
import no.nav.etterlatte.vedtaksvurdering.vedtaksvurderingRoute
import no.nav.security.mock.oauth2.MockOAuth2Server
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import testsupport.buildTestApplicationConfigurationForOauth
import java.math.BigDecimal
import java.time.LocalDate
import java.util.*

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class VedtaksvurderingRouteTest {

    private val server = MockOAuth2Server()
    private lateinit var applicationConfig: HoconApplicationConfig
    private val behandlingKlient = mockk<BehandlingKlient>()
    private val vedtaksvurderingService: VedtaksvurderingService = mockk()

    @BeforeAll
    fun before() {
        server.start()

        applicationConfig =
            buildTestApplicationConfigurationForOauth(server.config.httpServer.port(), AZURE_ISSUER, CLIENT_ID)
    }

    @BeforeEach
    fun beforeEach() {
        clearAllMocks()
        coEvery { behandlingKlient.harTilgangTilBehandling(any(), any()) } returns true
    }

    @AfterAll
    fun after() {
        server.shutdown()
    }

    @Test
    fun `skal returnere 401 naar token mangler`() {
        testApplication {
            environment { config = applicationConfig }
            application { restModule(log) { vedtaksvurderingRoute(vedtaksvurderingService, behandlingKlient) } }

            val response = client.get("/api/vedtak/${UUID.randomUUID()}") {
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            }

            response.status shouldBe HttpStatusCode.Unauthorized
        }
    }

    @Test
    fun `skal returnere 500 ved ukjent feil og returnere generell feilmelding`() {
        every { vedtaksvurderingService.hentVedtak(any()) } throws Exception("ukjent feil")

        testApplication {
            environment { config = applicationConfig }
            application { restModule(log) { vedtaksvurderingRoute(vedtaksvurderingService, behandlingKlient) } }

            val response = client.get("/api/vedtak/${UUID.randomUUID()}") {
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                header(HttpHeaders.Authorization, "Bearer $token")
            }

            response.status shouldBe HttpStatusCode.InternalServerError
            response.bodyAsText() shouldBe "En intern feil har oppstått"
        }

        coVerify(exactly = 1) {
            behandlingKlient.harTilgangTilBehandling(any(), any())
            vedtaksvurderingService.hentVedtak(any())
        }

        confirmVerified()
    }

    @Test
    fun `skal returnere 404 naar vedtaksvurdering ikke finnes`() {
        every { vedtaksvurderingService.hentVedtak(any()) } returns null

        testApplication {
            environment { config = applicationConfig }
            application { restModule(log) { vedtaksvurderingRoute(vedtaksvurderingService, behandlingKlient) } }

            val response = client.get("/api/vedtak/${UUID.randomUUID()}") {
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                header(HttpHeaders.Authorization, "Bearer $token")
            }

            response.status shouldBe HttpStatusCode.NotFound
        }

        coVerify(exactly = 1) {
            behandlingKlient.harTilgangTilBehandling(any(), any())
            vedtaksvurderingService.hentVedtak(any())
        }

        confirmVerified()
    }

    @Test
    fun `skal returnere eksisterende vedtaksvurdering`() {
        val opprettetVedtak = vedtak()
        every { vedtaksvurderingService.hentVedtak(any()) } returns opprettetVedtak

        testApplication {
            environment { config = applicationConfig }
            application { restModule(log) { vedtaksvurderingRoute(vedtaksvurderingService, behandlingKlient) } }

            val vedtak = client.get("/api/vedtak/${UUID.randomUUID()}") {
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                header(HttpHeaders.Authorization, "Bearer $token")
            }.let {
                it.status shouldBe HttpStatusCode.OK
                deserialize<VedtakDto>(it.bodyAsText())
            }

            with(vedtak) {
                vedtakId shouldBe opprettetVedtak.id
                status shouldBe opprettetVedtak.status
                virkningstidspunkt shouldBe opprettetVedtak.virkningstidspunkt
                behandling.id shouldBe opprettetVedtak.behandlingId
                behandling.type shouldBe opprettetVedtak.behandlingType
                sak.sakType shouldBe opprettetVedtak.sakType
                sak.id shouldBe opprettetVedtak.sakId
                type shouldBe opprettetVedtak.type
                vedtakFattet shouldBe null
                attestasjon shouldBe null
                utbetalingsperioder shouldHaveSize 1
                with(utbetalingsperioder.first()) {
                    id shouldBe 1L
                    periode shouldBe Periode(opprettetVedtak.virkningstidspunkt, null)
                    beloep shouldBe BigDecimal.valueOf(100)
                    type shouldBe UtbetalingsperiodeType.UTBETALING
                }
            }

            coVerify(exactly = 1) {
                behandlingKlient.harTilgangTilBehandling(any(), any())
                vedtaksvurderingService.hentVedtak(any())
            }

            confirmVerified()
        }
    }

    @Test
    fun `skal returnere vedtaksammendrag`() {
        val attestertVedtak = vedtak().copy(
            status = VedtakStatus.ATTESTERT,
            attestasjon = Attestasjon(SAKSBEHANDLER_2, ENHET_2, Tidspunkt.now())
        )
        every { vedtaksvurderingService.hentVedtak(any()) } returns attestertVedtak

        testApplication {
            environment { config = applicationConfig }
            application { restModule(log) { vedtaksvurderingRoute(vedtaksvurderingService, behandlingKlient) } }

            val vedtaksammendrag = client.get("/api/vedtak/${UUID.randomUUID()}/sammendrag") {
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                header(HttpHeaders.Authorization, "Bearer $token")
            }.let {
                it.status shouldBe HttpStatusCode.OK
                deserialize<VedtakSammendragDto>(it.bodyAsText())
            }

            vedtaksammendrag.id shouldBe attestertVedtak.id.toString()
            vedtaksammendrag.behandlingId shouldBe attestertVedtak.behandlingId
            vedtaksammendrag.datoAttestert shouldNotBe null
            // TODO datoAttestert leses inn som UTC selv om den er +01:00 - ville forventet med norsk tidssone?
            // vedtaksammendrag.datoAttestert shouldBe attestertVedtak.attestasjon?.tidspunkt?.toNorskTid()

            coVerify(exactly = 1) {
                behandlingKlient.harTilgangTilBehandling(any(), any())
                vedtaksvurderingService.hentVedtak(any())
            }

            confirmVerified()
        }
    }

    @Test
    fun `skal sjekke om sak har loepende vedtak`() {
        val sakId = 1L
        val loependeYtelse = LoependeYtelse(erLoepende = true, LocalDate.now())

        every { vedtaksvurderingService.sjekkOmVedtakErLoependePaaDato(any(), any()) } returns loependeYtelse
        coEvery { behandlingKlient.harTilgangTilSak(any(), any()) } returns true

        testApplication {
            environment { config = applicationConfig }
            application { restModule(log) { vedtaksvurderingRoute(vedtaksvurderingService, behandlingKlient) } }

            val hentetLoependeYtelse =
                client.get("/api/vedtak/loepende/$sakId?dato=${loependeYtelse.dato}") {
                    header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    header(HttpHeaders.Authorization, "Bearer $token")
                }.let {
                    it.status shouldBe HttpStatusCode.OK
                    deserialize<LoependeYtelseDTO>(it.bodyAsText())
                }

            hentetLoependeYtelse.erLoepende shouldBe loependeYtelse.erLoepende
            hentetLoependeYtelse.dato shouldBe loependeYtelse.dato

            coVerify(exactly = 1) {
                behandlingKlient.harTilgangTilSak(any(), any())
                vedtaksvurderingService.sjekkOmVedtakErLoependePaaDato(any(), any())
            }

            confirmVerified()
        }
    }

    @Test
    fun `skal opprette eller oppdatere vedtak`() {
        val opprettetVedtak = vedtak()
        coEvery { vedtaksvurderingService.opprettEllerOppdaterVedtak(any(), any()) } returns opprettetVedtak

        testApplication {
            environment { config = applicationConfig }
            application { restModule(log) { vedtaksvurderingRoute(vedtaksvurderingService, behandlingKlient) } }

            val vedtak = client.post("/api/vedtak/${UUID.randomUUID()}/upsert") {
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                header(HttpHeaders.Authorization, "Bearer $token")
            }.let {
                it.status shouldBe HttpStatusCode.OK
                deserialize<VedtakDto>(it.bodyAsText())
            }

            with(vedtak) {
                vedtakId shouldBe opprettetVedtak.id
                status shouldBe opprettetVedtak.status
                virkningstidspunkt shouldBe opprettetVedtak.virkningstidspunkt
                behandling.id shouldBe opprettetVedtak.behandlingId
                behandling.type shouldBe opprettetVedtak.behandlingType
                sak.sakType shouldBe opprettetVedtak.sakType
                sak.id shouldBe opprettetVedtak.sakId
                type shouldBe opprettetVedtak.type
                vedtakFattet shouldBe null
                attestasjon shouldBe null
                utbetalingsperioder shouldHaveSize 1
                with(utbetalingsperioder.first()) {
                    id shouldBe 1L
                    periode shouldBe Periode(opprettetVedtak.virkningstidspunkt, null)
                    beloep shouldBe BigDecimal.valueOf(100)
                    type shouldBe UtbetalingsperiodeType.UTBETALING
                }
            }

            coVerify(exactly = 1) {
                behandlingKlient.harTilgangTilBehandling(any(), any())
                vedtaksvurderingService.opprettEllerOppdaterVedtak(any(), match { it.ident() == SAKSBEHANDLER_1 })
            }

            confirmVerified()
        }
    }

    @Test
    fun `skal fatte vedtak`() {
        val fattetVedtak = vedtak().copy(
            status = VedtakStatus.FATTET_VEDTAK,
            vedtakFattet = VedtakFattet(SAKSBEHANDLER_1, ENHET_1, Tidspunkt.now())
        )
        coEvery { vedtaksvurderingService.fattVedtak(any(), any()) } returns fattetVedtak

        testApplication {
            environment { config = applicationConfig }
            application { restModule(log) { vedtaksvurderingRoute(vedtaksvurderingService, behandlingKlient) } }

            val vedtak = client.post("/api/vedtak/${UUID.randomUUID()}/fattvedtak") {
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                header(HttpHeaders.Authorization, "Bearer $token")
            }.let {
                it.status shouldBe HttpStatusCode.OK
                deserialize<VedtakDto>(it.bodyAsText())
            }

            with(vedtak) {
                vedtakId shouldBe fattetVedtak.id
                status shouldBe fattetVedtak.status
                virkningstidspunkt shouldBe fattetVedtak.virkningstidspunkt
                behandling.id shouldBe fattetVedtak.behandlingId
                behandling.type shouldBe fattetVedtak.behandlingType
                sak.sakType shouldBe fattetVedtak.sakType
                sak.id shouldBe fattetVedtak.sakId
                type shouldBe fattetVedtak.type
                vedtakFattet shouldBe fattetVedtak.vedtakFattet
                attestasjon shouldBe null
                utbetalingsperioder shouldHaveSize 1
                with(utbetalingsperioder.first()) {
                    id shouldBe 1L
                    periode shouldBe Periode(fattetVedtak.virkningstidspunkt, null)
                    beloep shouldBe BigDecimal.valueOf(100)
                    type shouldBe UtbetalingsperiodeType.UTBETALING
                }
            }

            coVerify(exactly = 1) {
                behandlingKlient.harTilgangTilBehandling(any(), any())
                vedtaksvurderingService.fattVedtak(any(), match { it.ident() == SAKSBEHANDLER_1 })
            }

            confirmVerified()
        }
    }

    @Test
    fun `skal attestere vedtak`() {
        val attestertVedtak = vedtak().copy(
            status = VedtakStatus.ATTESTERT,
            vedtakFattet = VedtakFattet(SAKSBEHANDLER_1, ENHET_1, Tidspunkt.now()),
            attestasjon = Attestasjon(SAKSBEHANDLER_2, ENHET_2, Tidspunkt.now())
        )
        coEvery { vedtaksvurderingService.attesterVedtak(any(), any()) } returns attestertVedtak

        testApplication {
            environment { config = applicationConfig }
            application { restModule(log) { vedtaksvurderingRoute(vedtaksvurderingService, behandlingKlient) } }

            val vedtak = client.post("/api/vedtak/${UUID.randomUUID()}/attester") {
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                header(HttpHeaders.Authorization, "Bearer $token")
            }.let {
                it.status shouldBe HttpStatusCode.OK
                deserialize<VedtakDto>(it.bodyAsText())
            }

            with(vedtak) {
                vedtakId shouldBe attestertVedtak.id
                status shouldBe attestertVedtak.status
                virkningstidspunkt shouldBe attestertVedtak.virkningstidspunkt
                behandling.id shouldBe attestertVedtak.behandlingId
                behandling.type shouldBe attestertVedtak.behandlingType
                sak.sakType shouldBe attestertVedtak.sakType
                sak.id shouldBe attestertVedtak.sakId
                type shouldBe attestertVedtak.type
                vedtakFattet shouldBe attestertVedtak.vedtakFattet
                attestasjon shouldBe attestertVedtak.attestasjon
                utbetalingsperioder shouldHaveSize 1
                with(utbetalingsperioder.first()) {
                    id shouldBe 1L
                    periode shouldBe Periode(attestertVedtak.virkningstidspunkt, null)
                    beloep shouldBe BigDecimal.valueOf(100)
                    type shouldBe UtbetalingsperiodeType.UTBETALING
                }
            }

            coVerify(exactly = 1) {
                behandlingKlient.harTilgangTilBehandling(any(), any())
                vedtaksvurderingService.attesterVedtak(any(), match { it.ident() == SAKSBEHANDLER_1 })
            }

            confirmVerified()
        }
    }

    @Test
    fun `skal underkjenne vedtak`() {
        val underkjentVedtak = vedtak().copy(
            status = VedtakStatus.RETURNERT
        )
        val begrunnelse = UnderkjennVedtakDto("Ikke bra nok begrunnet", "Annet")
        coEvery { vedtaksvurderingService.underkjennVedtak(any(), any(), any()) } returns underkjentVedtak

        testApplication {
            environment { config = applicationConfig }
            application { restModule(log) { vedtaksvurderingRoute(vedtaksvurderingService, behandlingKlient) } }

            val vedtak = client.post("/api/vedtak/${UUID.randomUUID()}/underkjenn") {
                setBody(begrunnelse.toJson())
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                header(HttpHeaders.Authorization, "Bearer $token")
            }.let {
                it.status shouldBe HttpStatusCode.OK
                deserialize<VedtakDto>(it.bodyAsText())
            }

            with(vedtak) {
                vedtakId shouldBe underkjentVedtak.id
                status shouldBe underkjentVedtak.status
                virkningstidspunkt shouldBe underkjentVedtak.virkningstidspunkt
                behandling.id shouldBe underkjentVedtak.behandlingId
                behandling.type shouldBe underkjentVedtak.behandlingType
                sak.sakType shouldBe underkjentVedtak.sakType
                sak.id shouldBe underkjentVedtak.sakId
                type shouldBe underkjentVedtak.type
                vedtakFattet shouldBe null
                attestasjon shouldBe null
                utbetalingsperioder shouldHaveSize 1
                with(utbetalingsperioder.first()) {
                    id shouldBe 1L
                    periode shouldBe Periode(underkjentVedtak.virkningstidspunkt, null)
                    beloep shouldBe BigDecimal.valueOf(100)
                    type shouldBe UtbetalingsperiodeType.UTBETALING
                }
            }

            coVerify(exactly = 1) {
                behandlingKlient.harTilgangTilBehandling(any(), any())
                vedtaksvurderingService.underkjennVedtak(
                    any(),
                    match { it.ident() == SAKSBEHANDLER_1 },
                    match { it == begrunnelse }
                )
            }

            confirmVerified()
        }
    }

    private val token: String by lazy {
        server.issueToken(
            issuerId = AZURE_ISSUER,
            audience = CLIENT_ID,
            claims = mapOf("navn" to "John Doe", "NAVident" to SAKSBEHANDLER_1)
        ).serialize()
    }

    private companion object {
        const val CLIENT_ID = "azure-id for saksbehandler"
    }
}