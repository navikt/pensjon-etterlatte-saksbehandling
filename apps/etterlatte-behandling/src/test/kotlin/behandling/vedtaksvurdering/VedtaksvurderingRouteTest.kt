package no.nav.etterlatte.behandling.vedtaksvurdering

import com.fasterxml.jackson.module.kotlin.readValue
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.ktor.client.HttpClient
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
import kotlinx.coroutines.asContextElement
import no.nav.etterlatte.Context
import no.nav.etterlatte.DatabaseExtension
import no.nav.etterlatte.Kontekst
import no.nav.etterlatte.SaksbehandlerMedEnheterOgRoller
import no.nav.etterlatte.behandling.sakId1
import no.nav.etterlatte.behandling.vedtaksvurdering.routes.UnderkjennVedtakDto
import no.nav.etterlatte.behandling.vedtaksvurdering.routes.klagevedtakRoute
import no.nav.etterlatte.behandling.vedtaksvurdering.routes.vedtaksvurderingRoute
import no.nav.etterlatte.behandling.vedtaksvurdering.service.VedtakBehandlingService
import no.nav.etterlatte.behandling.vedtaksvurdering.service.VedtakKlageService
import no.nav.etterlatte.behandling.vedtaksvurdering.service.VedtaksvurderingRapidService
import no.nav.etterlatte.behandling.vedtaksvurdering.service.VedtaksvurderingService
import no.nav.etterlatte.common.Enheter
import no.nav.etterlatte.ktor.runServer
import no.nav.etterlatte.ktor.startRandomPort
import no.nav.etterlatte.ktor.token.issueSaksbehandlerToken
import no.nav.etterlatte.libs.common.behandling.Klage
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.deserialize
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.person.AdressebeskyttelseGradering
import no.nav.etterlatte.libs.common.sak.Sak
import no.nav.etterlatte.libs.common.sak.SakId
import no.nav.etterlatte.libs.common.sak.SakMedGraderingOgSkjermet
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
import no.nav.etterlatte.nyKontekstMedBrukerOgDatabase
import no.nav.etterlatte.vedtaksvurdering.VedtakOgRapid
import no.nav.security.mock.oauth2.MockOAuth2Server
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import java.math.BigDecimal
import java.time.LocalDate
import java.time.format.DateTimeFormatter.ISO_OFFSET_DATE_TIME
import java.util.UUID
import javax.sql.DataSource

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(DatabaseExtension::class)
internal class VedtaksvurderingRouteTest(
    val dataSource: DataSource,
) {
    private val vedtaksvurderingService: VedtaksvurderingService = mockk()
    private val vedtakBehandlingService: VedtakBehandlingService = mockk()
    private val vedtakKlageService: VedtakKlageService = mockk()
    private val rapidService: VedtaksvurderingRapidService = mockk()
    private val mockOAuth2Server = MockOAuth2Server()

    private val user: SaksbehandlerMedEnheterOgRoller = mockk()
    private lateinit var context: Context

    @BeforeAll
    fun before() {
        mockOAuth2Server.startRandomPort()
    }

    @AfterEach
    fun afterEach() {
        confirmVerified(vedtaksvurderingService, vedtakBehandlingService, vedtakKlageService, rapidService)
        clearAllMocks()
    }

    @BeforeEach
    fun beforeEach() {
        clearAllMocks()
        every { user.enheterMedSkrivetilgang() } returns listOf(Enheter.defaultEnhet.enhetNr)
        context =
            nyKontekstMedBrukerOgDatabase(
                user,
                dataSource,
            )
        every { context.sakTilgangDao.hentSakMedGraderingOgSkjermingPaaBehandling(any()) } returns
            SakMedGraderingOgSkjermet(
                id = SakId(2),
                adressebeskyttelseGradering = AdressebeskyttelseGradering.UGRADERT,
                erSkjermet = false,
                enhetNr = Enheter.defaultEnhet.enhetNr,
            )
    }

    @AfterAll
    fun after() {
        mockOAuth2Server.shutdown()
    }

    @Test
    fun `skal returnere 401 naar token mangler`() {
        withTestApplication(context) { client ->
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

        withTestApplication(context) { client ->

            val response =
                client.get("/api/vedtak/${UUID.randomUUID()}") {
                    header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    header(HttpHeaders.Authorization, "Bearer $token")
                }

            response.status shouldBe HttpStatusCode.InternalServerError
        }

        coVerify(exactly = 1) {
            vedtaksvurderingService.hentVedtakMedBehandlingId(any<UUID>())
        }
    }

    @Test
    fun `skal returnere 404 naar vedtaksvurdering ikke finnes`() {
        every { vedtaksvurderingService.hentVedtakMedBehandlingId(any<UUID>()) } returns null

        withTestApplication(context) { client ->

            val response =
                client.get("/api/vedtak/${UUID.randomUUID()}") {
                    header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    header(HttpHeaders.Authorization, "Bearer $token")
                }

            response.status shouldBe HttpStatusCode.NotFound
        }

        coVerify(exactly = 1) {
            vedtaksvurderingService.hentVedtakMedBehandlingId(any<UUID>())
        }
    }

    @Test
    fun `skal returnere eksisterende vedtaksvurdering`() {
        val opprettetVedtak = vedtak()
        every { vedtaksvurderingService.hentVedtakMedBehandlingId(any<UUID>()) } returns opprettetVedtak

        withTestApplication(context) { client ->

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
                vedtaksvurderingService.hentVedtakMedBehandlingId(any<UUID>())
            }
        }
    }

    @Test
    fun `skal returnere eksisterende vedtaksvurdering for en behandling`() {
        val opprettetVedtak = vedtak()
        every { vedtaksvurderingService.hentVedtakMedBehandlingId(any<UUID>()) } returns opprettetVedtak

        withTestApplication(context) { client ->

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
                vedtaksvurderingService.hentVedtakMedBehandlingId(any<UUID>())
            }
        }
    }

    @Test
    fun `skal returnere eksisterende vedtaksvurdering for en tilbakekreving`() {
        val opprettetVedtak = vedtakTilbakekreving()
        every { vedtaksvurderingService.hentVedtakMedBehandlingId(any<UUID>()) } returns opprettetVedtak

        withTestApplication(context) { client ->

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

        withTestApplication(context) { client ->

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
                vedtaksvurderingService.hentVedtakMedBehandlingId(any<UUID>())
            }
        }
    }

    @Test
    fun `skal sjekke om sak har loepende vedtak`() {
        val sakId = sakId1
        val loependeYtelse = LoependeYtelse(erLoepende = true, underSamordning = false, LocalDate.now(), UUID.randomUUID())

        every { vedtakBehandlingService.sjekkOmVedtakErLoependePaaDato(any(), any()) } returns loependeYtelse

        withTestApplication(context) { client ->

            val hentetLoependeYtelse =
                client
                    .get("/api/vedtak/loepende/${sakId.sakId}?dato=${loependeYtelse.dato}") {
                        header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                        header(HttpHeaders.Authorization, "Bearer $token")
                    }.let {
                        it.status shouldBe HttpStatusCode.OK
                        deserialize<LoependeYtelseDTO>(it.bodyAsText())
                    }

            hentetLoependeYtelse.erLoepende shouldBe loependeYtelse.erLoepende
            hentetLoependeYtelse.dato shouldBe loependeYtelse.dato

            coVerify(exactly = 1) {
                vedtakBehandlingService.sjekkOmVedtakErLoependePaaDato(any(), any())
            }
        }
    }

    @Test
    fun `skal opprette eller oppdatere vedtak`() {
        val opprettetVedtak = vedtak()
        coEvery { vedtakBehandlingService.opprettEllerOppdaterVedtak(any(), any()) } returns opprettetVedtak

        withTestApplication(context) { client ->

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

        withTestApplication(context) { client ->

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

        withTestApplication(context) { client ->

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

        withTestApplication(context) { client ->

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

        withTestApplication(context) { client ->

            client
                .patch("/api/vedtak/${UUID.randomUUID()}/tilbakestill") {
                    header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    header(HttpHeaders.Authorization, "Bearer $token")
                }.let {
                    it.status shouldBe HttpStatusCode.OK
                }

            coVerify(exactly = 1) {
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
        every { context.sakTilgangDao.hentSakMedGraderingOgSkjermingPaaKlage(klage.id) } returns
            SakMedGraderingOgSkjermet(
                id = SakId(2),
                adressebeskyttelseGradering = AdressebeskyttelseGradering.UGRADERT,
                erSkjermet = false,
                enhetNr = Enheter.defaultEnhet.enhetNr,
            )

        withTestApplication(context) { client ->
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
                vedtakKlageService.opprettEllerOppdaterVedtakOmAvvisning(
                    withArg { it.id shouldBe klage.id },
                )
            }
        }
    }

    private val token: String by lazy { mockOAuth2Server.issueSaksbehandlerToken(navIdent = SAKSBEHANDLER_1) }

    private fun klage(): Klage =
        Klage.ny(
            sak = Sak("ident", SakType.BARNEPENSJON, sakId1, ENHET_1, null, null),
            innkommendeDokument = null,
        )

    private fun withTestApplication(
        context: Context,
        block: suspend (client: HttpClient) -> Unit,
    ) {
        testApplication(Kontekst.asContextElement(context)) {
            runServer(mockOAuth2Server) {
                vedtaksvurderingRoute(
                    vedtakService = vedtaksvurderingService,
                    vedtakBehandlingService = vedtakBehandlingService,
                    rapidService = rapidService,
                )
                klagevedtakRoute(
                    vedtakKlageService = vedtakKlageService,
                )
            }
            block(client)
        }
    }
}
