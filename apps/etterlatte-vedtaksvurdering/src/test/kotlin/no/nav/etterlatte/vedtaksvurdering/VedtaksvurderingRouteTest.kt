package no.nav.etterlatte.vedtaksvurdering

import com.fasterxml.jackson.module.kotlin.readValue
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import no.nav.etterlatte.ktor.runServer
import no.nav.etterlatte.ktor.startRandomPort
import no.nav.etterlatte.ktor.token.issueSaksbehandlerToken
import no.nav.etterlatte.libs.common.behandling.Klage
import no.nav.etterlatte.libs.common.behandling.KlageStatus
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.deserialize
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.sak.Sak
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.tidspunkt.toNorskTid
import no.nav.etterlatte.libs.common.toJson
import no.nav.etterlatte.libs.common.vedtak.Attestasjon
import no.nav.etterlatte.libs.common.vedtak.AttesterVedtakDto
import no.nav.etterlatte.libs.common.vedtak.LoependeYtelseDTO
import no.nav.etterlatte.libs.common.vedtak.Periode
import no.nav.etterlatte.libs.common.vedtak.UtbetalingsperiodeType
import no.nav.etterlatte.libs.common.vedtak.VedtakDto
import no.nav.etterlatte.libs.common.vedtak.VedtakFattet
import no.nav.etterlatte.libs.common.vedtak.VedtakInnholdDto
import no.nav.etterlatte.libs.common.vedtak.VedtakSammendragDto
import no.nav.etterlatte.libs.common.vedtak.VedtakStatus
import no.nav.etterlatte.no.nav.etterlatte.vedtaksvurdering.VedtakKlageService
import no.nav.etterlatte.vedtaksvurdering.klienter.BehandlingKlient
import no.nav.security.mock.oauth2.MockOAuth2Server
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.math.BigDecimal
import java.time.LocalDate
import java.time.format.DateTimeFormatter.ISO_OFFSET_DATE_TIME
import java.util.UUID

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class VedtaksvurderingRouteTest {
    private val mockOAuth2Server = MockOAuth2Server()
    private val behandlingKlient = mockk<BehandlingKlient>()
    private val vedtaksvurderingService: VedtaksvurderingService = mockk()
    private val vedtakBehandlingService: VedtakBehandlingService = mockk()
    private val vedtakKlageService: VedtakKlageService = mockk()
    private val rapidService: VedtaksvurderingRapidService = mockk()

    @BeforeAll
    fun before() {
        mockOAuth2Server.startRandomPort()
    }

    @AfterEach
    fun afterEach() {
        confirmVerified()
        clearAllMocks()
    }

    @BeforeEach
    fun beforeEach() {
        clearAllMocks()
        coEvery { behandlingKlient.harTilgangTilBehandling(any(), any(), any()) } returns true
    }

    @AfterAll
    fun after() {
        mockOAuth2Server.shutdown()
    }

    @Test
    fun `skal returnere 401 naar token mangler`() {
        testApplication {
            runServer(mockOAuth2Server) {
                vedtaksvurderingRoute(
                    vedtaksvurderingService,
                    vedtakBehandlingService,
                    rapidService,
                    behandlingKlient,
                )
            }

            val response =
                client.get("/api/vedtak/${UUID.randomUUID()}") {
                    header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                }

            response.status shouldBe HttpStatusCode.Unauthorized
        }
    }

    @Test
    fun `skal returnere 500 ved ukjent feil og returnere generell feilmelding`() {
        every { vedtaksvurderingService.hentVedtakMedBehandlingId(any<UUID>()) } throws Exception("ukjent feil")

        testApplication {
            runServer(mockOAuth2Server) {
                vedtaksvurderingRoute(
                    vedtaksvurderingService,
                    vedtakBehandlingService,
                    rapidService,
                    behandlingKlient,
                )
            }

            val response =
                client.get("/api/vedtak/${UUID.randomUUID()}") {
                    header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    header(HttpHeaders.Authorization, "Bearer $token")
                }

            response.status shouldBe HttpStatusCode.InternalServerError
        }

        coVerify(exactly = 1) {
            behandlingKlient.harTilgangTilBehandling(any(), any(), any())
            vedtaksvurderingService.hentVedtakMedBehandlingId(any<UUID>())
        }
    }

    @Test
    fun `skal returnere 404 naar vedtaksvurdering ikke finnes`() {
        every { vedtaksvurderingService.hentVedtakMedBehandlingId(any<UUID>()) } returns null

        testApplication {
            runServer(mockOAuth2Server) {
                vedtaksvurderingRoute(
                    vedtaksvurderingService,
                    vedtakBehandlingService,
                    rapidService,
                    behandlingKlient,
                )
            }

            val response =
                client.get("/api/vedtak/${UUID.randomUUID()}") {
                    header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    header(HttpHeaders.Authorization, "Bearer $token")
                }

            response.status shouldBe HttpStatusCode.NotFound
        }

        coVerify(exactly = 1) {
            behandlingKlient.harTilgangTilBehandling(any(), any(), any())
            vedtaksvurderingService.hentVedtakMedBehandlingId(any<UUID>())
        }
    }

    @Test
    fun `skal returnere eksisterende vedtaksvurdering`() {
        val opprettetVedtak = vedtak()
        every { vedtaksvurderingService.hentVedtakMedBehandlingId(any<UUID>()) } returns opprettetVedtak

        testApplication {
            runServer(mockOAuth2Server) {
                vedtaksvurderingRoute(
                    vedtaksvurderingService,
                    vedtakBehandlingService,
                    rapidService,
                    behandlingKlient,
                )
            }

            val vedtak =
                client
                    .get("/api/vedtak/${UUID.randomUUID()}") {
                        header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                        header(HttpHeaders.Authorization, "Bearer $token")
                    }.let {
                        it.status shouldBe HttpStatusCode.OK
                        deserialize<VedtakDto>(it.bodyAsText())
                    }

            with(vedtak) {
                id shouldBe opprettetVedtak.id
                status shouldBe opprettetVedtak.status
                behandlingId shouldBe opprettetVedtak.behandlingId
                sak.sakType shouldBe opprettetVedtak.sakType
                sak.id shouldBe opprettetVedtak.sakId
                type shouldBe opprettetVedtak.type
                vedtakFattet shouldBe null
                attestasjon shouldBe null
                with(innhold as VedtakInnholdDto.VedtakBehandlingDto) {
                    val opprettetVedtakInnhold = opprettetVedtak.innhold as VedtakInnhold.Behandling
                    virkningstidspunkt shouldBe opprettetVedtakInnhold.virkningstidspunkt
                    behandling.type shouldBe opprettetVedtakInnhold.behandlingType
                    utbetalingsperioder shouldHaveSize 1
                    with(utbetalingsperioder.first()) {
                        id shouldBe 1L
                        periode shouldBe Periode(opprettetVedtakInnhold.virkningstidspunkt, null)
                        beloep shouldBe BigDecimal.valueOf(100)
                        type shouldBe UtbetalingsperiodeType.UTBETALING
                    }
                }
            }

            coVerify(exactly = 1) {
                behandlingKlient.harTilgangTilBehandling(any(), any(), any())
                vedtaksvurderingService.hentVedtakMedBehandlingId(any<UUID>())
            }
        }
    }

    @Test
    fun `skal returnere eksisterende vedtaksvurdering for en behandling`() {
        val opprettetVedtak = vedtak()
        every { vedtaksvurderingService.hentVedtakMedBehandlingId(any<UUID>()) } returns opprettetVedtak

        testApplication {
            runServer(mockOAuth2Server) {
                vedtaksvurderingRoute(
                    vedtaksvurderingService,
                    vedtakBehandlingService,
                    rapidService,
                    behandlingKlient,
                )
            }

            val vedtak =
                client
                    .get("/api/vedtak/${UUID.randomUUID()}") {
                        header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                        header(HttpHeaders.Authorization, "Bearer $token")
                    }.let {
                        it.status shouldBe HttpStatusCode.OK
                        deserialize<VedtakDto>(it.bodyAsText())
                    }

            with(vedtak) {
                id shouldBe opprettetVedtak.id
                status shouldBe opprettetVedtak.status
                behandlingId shouldBe opprettetVedtak.behandlingId
                sak.sakType shouldBe opprettetVedtak.sakType
                sak.id shouldBe opprettetVedtak.sakId
                type shouldBe opprettetVedtak.type
                vedtakFattet shouldBe null
                attestasjon shouldBe null
                (innhold as VedtakInnholdDto.VedtakBehandlingDto).let {
                    val opprettVedtakInnhold = opprettetVedtak.innhold as VedtakInnhold.Behandling
                    it.virkningstidspunkt shouldBe opprettVedtakInnhold.virkningstidspunkt
                    it.behandling.type shouldBe opprettVedtakInnhold.behandlingType
                    it.utbetalingsperioder shouldHaveSize 1
                    with(it.utbetalingsperioder.first()) {
                        id shouldBe 1L
                        periode shouldBe Periode(it.virkningstidspunkt, null)
                        beloep shouldBe BigDecimal.valueOf(100)
                        type shouldBe UtbetalingsperiodeType.UTBETALING
                    }
                }
            }

            coVerify(exactly = 1) {
                behandlingKlient.harTilgangTilBehandling(any(), any(), any())
                vedtaksvurderingService.hentVedtakMedBehandlingId(any<UUID>())
            }
        }
    }

    @Test
    fun `skal returnere eksisterende vedtaksvurdering for en tilbakekreving`() {
        val opprettetVedtak = vedtakTilbakekreving()
        every { vedtaksvurderingService.hentVedtakMedBehandlingId(any<UUID>()) } returns opprettetVedtak

        testApplication {
            runServer(mockOAuth2Server) {
                vedtaksvurderingRoute(
                    vedtaksvurderingService,
                    vedtakBehandlingService,
                    rapidService,
                    behandlingKlient,
                )
            }

            val vedtak =
                client
                    .get("/api/vedtak/${UUID.randomUUID()}") {
                        header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                        header(HttpHeaders.Authorization, "Bearer $token")
                    }.let {
                        it.status shouldBe HttpStatusCode.OK
                        deserialize<VedtakDto>(it.bodyAsText())
                    }

            with(vedtak) {
                id shouldBe opprettetVedtak.id
                status shouldBe opprettetVedtak.status
                behandlingId shouldBe opprettetVedtak.behandlingId
                sak.sakType shouldBe opprettetVedtak.sakType
                sak.id shouldBe opprettetVedtak.sakId
                type shouldBe opprettetVedtak.type
                vedtakFattet shouldBe null
                attestasjon shouldBe null
                (innhold as VedtakInnholdDto.VedtakTilbakekrevingDto).tilbakekreving shouldBe
                    (opprettetVedtak.innhold as VedtakInnhold.Tilbakekreving).tilbakekreving
            }
        }

        coVerify(exactly = 1) {
            behandlingKlient.harTilgangTilBehandling(any(), any(), any())
            vedtaksvurderingService.hentVedtakMedBehandlingId(any<UUID>())
        }
    }

    @Test
    fun `skal returnere vedtaksammendrag`() {
        val attestertVedtak =
            vedtak().copy(
                status = VedtakStatus.ATTESTERT,
                attestasjon = Attestasjon(SAKSBEHANDLER_2, ENHET_2, Tidspunkt.now()),
            )
        every { vedtaksvurderingService.hentVedtakMedBehandlingId(any<UUID>()) } returns attestertVedtak

        testApplication {
            runServer(mockOAuth2Server) {
                vedtaksvurderingRoute(
                    vedtaksvurderingService,
                    vedtakBehandlingService,
                    rapidService,
                    behandlingKlient,
                )
            }

            val vedtaksammendrag =
                client
                    .get("/api/vedtak/${UUID.randomUUID()}/sammendrag") {
                        header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                        header(HttpHeaders.Authorization, "Bearer $token")
                    }.let {
                        it.status shouldBe HttpStatusCode.OK
                        objectMapper.readValue<VedtakSammendragDto>(it.bodyAsText())
                    }

            vedtaksammendrag.id shouldBe attestertVedtak.id.toString()
            vedtaksammendrag.behandlingId shouldBe attestertVedtak.behandlingId
            vedtaksammendrag.datoAttestert shouldNotBe null
            /*
            ISO_OFFSET_DATE_TIME brukes internt i jackson for https://github.com/FasterXML/jackson-modules-java8/tree/master/datetime
            mens toNorskTid "bruker" ISO_ZONED_DATE_TIME
             */
            vedtaksammendrag.datoAttestert?.format(ISO_OFFSET_DATE_TIME) shouldBe
                attestertVedtak.attestasjon
                    ?.tidspunkt
                    ?.toNorskTid()
                    ?.format(ISO_OFFSET_DATE_TIME)

            coVerify(exactly = 1) {
                behandlingKlient.harTilgangTilBehandling(any(), any(), any())
                vedtaksvurderingService.hentVedtakMedBehandlingId(any<UUID>())
            }
        }
    }

    @Test
    fun `skal sjekke om sak har loepende vedtak`() {
        val sakId = 1L
        val loependeYtelse = LoependeYtelse(erLoepende = true, underSamordning = false, LocalDate.now(), UUID.randomUUID())

        every { vedtakBehandlingService.sjekkOmVedtakErLoependePaaDato(any(), any()) } returns loependeYtelse
        coEvery { behandlingKlient.harTilgangTilSak(any(), any(), any()) } returns true

        testApplication {
            runServer(mockOAuth2Server) {
                vedtaksvurderingRoute(
                    vedtaksvurderingService,
                    vedtakBehandlingService,
                    rapidService,
                    behandlingKlient,
                )
            }

            val hentetLoependeYtelse =
                client
                    .get("/api/vedtak/loepende/$sakId?dato=${loependeYtelse.dato}") {
                        header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                        header(HttpHeaders.Authorization, "Bearer $token")
                    }.let {
                        it.status shouldBe HttpStatusCode.OK
                        deserialize<LoependeYtelseDTO>(it.bodyAsText())
                    }

            hentetLoependeYtelse.erLoepende shouldBe loependeYtelse.erLoepende
            hentetLoependeYtelse.dato shouldBe loependeYtelse.dato

            coVerify(exactly = 1) {
                behandlingKlient.harTilgangTilSak(any(), any(), any())
                vedtakBehandlingService.sjekkOmVedtakErLoependePaaDato(any(), any())
            }
        }
    }

    @Test
    fun `skal opprette eller oppdatere vedtak`() {
        val opprettetVedtak = vedtak()
        coEvery { vedtakBehandlingService.opprettEllerOppdaterVedtak(any(), any()) } returns opprettetVedtak

        testApplication {
            runServer(mockOAuth2Server) {
                vedtaksvurderingRoute(
                    vedtaksvurderingService,
                    vedtakBehandlingService,
                    rapidService,
                    behandlingKlient,
                )
            }

            val vedtak =
                client
                    .post("/api/vedtak/${UUID.randomUUID()}/upsert") {
                        header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                        header(HttpHeaders.Authorization, "Bearer $token")
                    }.let {
                        it.status shouldBe HttpStatusCode.OK
                        deserialize<VedtakDto>(it.bodyAsText())
                    }

            with(vedtak) {
                id shouldBe opprettetVedtak.id
                status shouldBe opprettetVedtak.status
                behandlingId shouldBe opprettetVedtak.behandlingId
                sak.sakType shouldBe opprettetVedtak.sakType
                sak.id shouldBe opprettetVedtak.sakId
                type shouldBe opprettetVedtak.type
                vedtakFattet shouldBe null
                attestasjon shouldBe null
                val opprettetVedtakInnhold = opprettetVedtak.innhold as VedtakInnhold.Behandling
                with(innhold as VedtakInnholdDto.VedtakBehandlingDto) {
                    virkningstidspunkt shouldBe opprettetVedtakInnhold.virkningstidspunkt
                    behandling.type shouldBe opprettetVedtakInnhold.behandlingType
                    utbetalingsperioder shouldHaveSize 1
                    with(utbetalingsperioder.first()) {
                        id shouldBe 1L
                        periode shouldBe Periode(opprettetVedtakInnhold.virkningstidspunkt, null)
                        beloep shouldBe BigDecimal.valueOf(100)
                        type shouldBe UtbetalingsperiodeType.UTBETALING
                    }
                }
            }

            coVerify(exactly = 1) {
                behandlingKlient.harTilgangTilBehandling(any(), any(), any())
                vedtakBehandlingService.opprettEllerOppdaterVedtak(any(), match { it.ident() == SAKSBEHANDLER_1 })
            }
        }
    }

    @Test
    fun `skal fatte vedtak`() {
        val fattetVedtak =
            vedtak().copy(
                status = VedtakStatus.FATTET_VEDTAK,
                vedtakFattet = VedtakFattet(SAKSBEHANDLER_1, ENHET_1, Tidspunkt.now()),
            )
        coEvery { vedtakBehandlingService.fattVedtak(any(), any(), any()) } returns
            VedtakOgRapid(
                fattetVedtak.toDto(),
                mockk(),
            )
        coEvery { rapidService.sendToRapid(any()) } just runs

        testApplication {
            runServer(mockOAuth2Server) {
                vedtaksvurderingRoute(
                    vedtaksvurderingService,
                    vedtakBehandlingService,
                    rapidService,
                    behandlingKlient,
                )
            }

            val vedtak =
                client
                    .post("/api/vedtak/${UUID.randomUUID()}/fattvedtak") {
                        header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                        header(HttpHeaders.Authorization, "Bearer $token")
                    }.let {
                        it.status shouldBe HttpStatusCode.OK
                        deserialize<VedtakDto>(it.bodyAsText())
                    }

            with(vedtak) {
                id shouldBe fattetVedtak.id
                status shouldBe fattetVedtak.status
                behandlingId shouldBe fattetVedtak.behandlingId
                sak.sakType shouldBe fattetVedtak.sakType
                sak.id shouldBe fattetVedtak.sakId
                type shouldBe fattetVedtak.type
                vedtakFattet shouldBe fattetVedtak.vedtakFattet
                attestasjon shouldBe null
                with(innhold as VedtakInnholdDto.VedtakBehandlingDto) {
                    val fattetVedtakInnhold = fattetVedtak.innhold as VedtakInnhold.Behandling
                    virkningstidspunkt shouldBe fattetVedtakInnhold.virkningstidspunkt
                    behandling.type shouldBe fattetVedtakInnhold.behandlingType
                    utbetalingsperioder shouldHaveSize 1
                    with(utbetalingsperioder.first()) {
                        id shouldBe 1L
                        periode shouldBe Periode(fattetVedtakInnhold.virkningstidspunkt, null)
                        beloep shouldBe BigDecimal.valueOf(100)
                        type shouldBe UtbetalingsperiodeType.UTBETALING
                    }
                }
            }

            coVerify(exactly = 1) {
                behandlingKlient.harTilgangTilBehandling(any(), any(), any())
                vedtakBehandlingService.fattVedtak(any(), match { it.ident() == SAKSBEHANDLER_1 }, any())
                rapidService.sendToRapid(any())
            }
        }
    }

    @Test
    fun `skal attestere vedtak`() {
        val attestertVedtakKommentar = AttesterVedtakDto("Alt ser fint ut")
        val attestertVedtak =
            vedtak().copy(
                status = VedtakStatus.ATTESTERT,
                vedtakFattet = VedtakFattet(SAKSBEHANDLER_1, ENHET_1, Tidspunkt.now()),
                attestasjon = Attestasjon(SAKSBEHANDLER_2, ENHET_2, Tidspunkt.now()),
            )
        coEvery { vedtakBehandlingService.attesterVedtak(any(), any(), any(), any()) } returns
            VedtakOgRapid(
                attestertVedtak.toDto(),
                mockk(),
            )
        coEvery { rapidService.sendToRapid(any()) } just runs

        testApplication {
            runServer(mockOAuth2Server) {
                vedtaksvurderingRoute(
                    vedtaksvurderingService,
                    vedtakBehandlingService,
                    rapidService,
                    behandlingKlient,
                )
            }

            val vedtakDto =
                client
                    .post("/api/vedtak/${UUID.randomUUID()}/attester") {
                        setBody(attestertVedtakKommentar.toJson())
                        header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                        header(HttpHeaders.Authorization, "Bearer $token")
                    }.let {
                        it.status shouldBe HttpStatusCode.OK
                        deserialize<VedtakDto>(it.bodyAsText())
                    }

            with(vedtakDto) {
                id shouldBe attestertVedtak.id
                status shouldBe attestertVedtak.status
                behandlingId shouldBe attestertVedtak.behandlingId
                sak.sakType shouldBe attestertVedtak.sakType
                sak.id shouldBe attestertVedtak.sakId
                type shouldBe attestertVedtak.type
                vedtakFattet shouldBe attestertVedtak.vedtakFattet
                attestasjon shouldBe attestertVedtak.attestasjon
                with(vedtakDto.innhold as VedtakInnholdDto.VedtakBehandlingDto) {
                    val attestertVedtakInnhold = attestertVedtak.innhold as VedtakInnhold.Behandling
                    virkningstidspunkt shouldBe attestertVedtakInnhold.virkningstidspunkt
                    behandling.type shouldBe attestertVedtakInnhold.behandlingType
                    utbetalingsperioder shouldHaveSize 1
                    with(utbetalingsperioder.first()) {
                        id shouldBe 1L
                        periode shouldBe Periode(attestertVedtakInnhold.virkningstidspunkt, null)
                        beloep shouldBe BigDecimal.valueOf(100)
                        type shouldBe UtbetalingsperiodeType.UTBETALING
                    }
                }
            }

            coVerify(exactly = 1) {
                behandlingKlient.harTilgangTilBehandling(any(), any(), any())
                vedtakBehandlingService.attesterVedtak(
                    any(),
                    match { it == attestertVedtakKommentar.kommentar },
                    match { it.ident() == SAKSBEHANDLER_1 },
                    any(),
                )
                rapidService.sendToRapid(any())
            }
        }
    }

    @Test
    fun `skal underkjenne vedtak`() {
        val underkjentVedtak =
            vedtak().copy(
                status = VedtakStatus.RETURNERT,
            )
        val begrunnelse = UnderkjennVedtakDto("Ikke bra nok begrunnet", "Annet")
        coEvery { vedtakBehandlingService.underkjennVedtak(any(), any(), any()) } returns
            VedtakOgRapid(underkjentVedtak.toDto(), mockk())
        coEvery { rapidService.sendToRapid(any()) } just runs

        testApplication {
            runServer(mockOAuth2Server) {
                vedtaksvurderingRoute(
                    vedtaksvurderingService,
                    vedtakBehandlingService,
                    rapidService,
                    behandlingKlient,
                )
            }

            val vedtak =
                client
                    .post("/api/vedtak/${UUID.randomUUID()}/underkjenn") {
                        setBody(begrunnelse.toJson())
                        header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                        header(HttpHeaders.Authorization, "Bearer $token")
                    }.let {
                        it.status shouldBe HttpStatusCode.OK
                        deserialize<VedtakDto>(it.bodyAsText())
                    }

            with(vedtak) {
                id shouldBe underkjentVedtak.id
                status shouldBe underkjentVedtak.status
                behandlingId shouldBe underkjentVedtak.behandlingId
                sak.sakType shouldBe underkjentVedtak.sakType
                sak.id shouldBe underkjentVedtak.sakId
                type shouldBe underkjentVedtak.type
                vedtakFattet shouldBe null
                attestasjon shouldBe null
                with(innhold as VedtakInnholdDto.VedtakBehandlingDto) {
                    val underkjentVedtakInnhold = underkjentVedtak.innhold as VedtakInnhold.Behandling
                    virkningstidspunkt shouldBe underkjentVedtakInnhold.virkningstidspunkt
                    behandling.type shouldBe underkjentVedtakInnhold.behandlingType
                    utbetalingsperioder shouldHaveSize 1
                    with(utbetalingsperioder.first()) {
                        id shouldBe 1L
                        periode shouldBe Periode(underkjentVedtakInnhold.virkningstidspunkt, null)
                        beloep shouldBe BigDecimal.valueOf(100)
                        type shouldBe UtbetalingsperiodeType.UTBETALING
                    }
                }
            }

            coVerify(exactly = 1) {
                behandlingKlient.harTilgangTilBehandling(any(), any(), any())
                vedtakBehandlingService.underkjennVedtak(
                    any(),
                    match { it.ident() == SAKSBEHANDLER_1 },
                    match { it == begrunnelse },
                )
                rapidService.sendToRapid(any())
            }
        }
    }

    @Test
    fun `skal tilbakestille vedtak`() {
        val tilbakestiltVedtak =
            vedtak().copy(
                status = VedtakStatus.RETURNERT,
            )
        coEvery { vedtakBehandlingService.tilbakestillIkkeIverksatteVedtak(any()) } returns tilbakestiltVedtak

        testApplication {
            runServer(mockOAuth2Server) {
                vedtaksvurderingRoute(
                    vedtaksvurderingService,
                    vedtakBehandlingService,
                    rapidService,
                    behandlingKlient,
                )
            }

            client
                .patch("/api/vedtak/${UUID.randomUUID()}/tilbakestill") {
                    header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    header(HttpHeaders.Authorization, "Bearer $token")
                }.let {
                    it.status shouldBe HttpStatusCode.OK
                }

            coVerify(exactly = 1) {
                behandlingKlient.harTilgangTilBehandling(any(), any(), any())
                vedtakBehandlingService.tilbakestillIkkeIverksatteVedtak(any())
            }
        }
    }

    @Test
    fun `skal opprette vedtak for avvist klage`() {
        val vedtakKlage = vedtakKlage()
        val klage = klage()
        coEvery { vedtakKlageService.opprettEllerOppdaterVedtakOmAvvisning(any()) } returns vedtakKlage
        every { vedtaksvurderingService.hentVedtakMedBehandlingId(any<UUID>()) } returns vedtakKlage

        testApplication {
            runServer(mockOAuth2Server) {
                vedtaksvurderingRoute(
                    vedtaksvurderingService,
                    vedtakBehandlingService,
                    rapidService,
                    behandlingKlient,
                )
                klagevedtakRoute(
                    vedtakKlageService,
                    behandlingKlient,
                )
            }
            val vedtakDto: VedtakDto =
                client
                    .post("/vedtak/klage/${klage.id}/upsert") {
                        header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                        header(HttpHeaders.Authorization, "Bearer $token")
                        setBody(klage.toJson())
                    }.let {
                        it.status shouldBe HttpStatusCode.OK
                        deserialize<VedtakDto>(it.bodyAsText())
                    }
            vedtakDto.id shouldBe vedtakKlage.id

            coVerify(exactly = 1) {
                behandlingKlient.harTilgangTilBehandling(any(), any(), any())
                vedtakKlageService.opprettEllerOppdaterVedtakOmAvvisning(
                    withArg { it.id shouldBe klage.id },
                )
            }
        }
    }

    private val token: String by lazy { mockOAuth2Server.issueSaksbehandlerToken(navIdent = SAKSBEHANDLER_1) }

    private fun klage(): Klage =
        Klage(
            UUID.randomUUID(),
            Sak("ident", SakType.BARNEPENSJON, 1L, "einheit"),
            Tidspunkt.now(),
            KlageStatus.OPPRETTET,
            kabalResultat = null,
            kabalStatus = null,
            formkrav = null,
            innkommendeDokument = null,
            resultat = null,
            utfall = null,
            aarsakTilAvbrytelse = null,
            initieltUtfall = null,
        )
}
