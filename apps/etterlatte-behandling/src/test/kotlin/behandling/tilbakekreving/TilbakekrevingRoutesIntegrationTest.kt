package no.nav.etterlatte.behandling.klage

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.testing.testApplication
import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.BehandlingIntegrationTest
import no.nav.etterlatte.behandling.tilbakekreving.TilbakekrevingSendeBrevRequest
import no.nav.etterlatte.behandling.tilbakekreving.TilbakekrevingService
import no.nav.etterlatte.ktor.runServerWithModule
import no.nav.etterlatte.libs.common.behandling.Kabalrespons
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.sak.Sak
import no.nav.etterlatte.libs.common.tilbakekreving.Grunnlagsbeloep
import no.nav.etterlatte.libs.common.tilbakekreving.KlasseKode
import no.nav.etterlatte.libs.common.tilbakekreving.KlasseType
import no.nav.etterlatte.libs.common.tilbakekreving.Kontrollfelt
import no.nav.etterlatte.libs.common.tilbakekreving.Kravgrunnlag
import no.nav.etterlatte.libs.common.tilbakekreving.KravgrunnlagId
import no.nav.etterlatte.libs.common.tilbakekreving.KravgrunnlagPeriode
import no.nav.etterlatte.libs.common.tilbakekreving.KravgrunnlagStatus
import no.nav.etterlatte.libs.common.tilbakekreving.NavIdent
import no.nav.etterlatte.libs.common.tilbakekreving.Periode
import no.nav.etterlatte.libs.common.tilbakekreving.SakId
import no.nav.etterlatte.libs.common.tilbakekreving.TilbakekrevingBehandling
import no.nav.etterlatte.libs.common.tilbakekreving.TilbakekrevingStatus
import no.nav.etterlatte.libs.common.tilbakekreving.UUID30
import no.nav.etterlatte.libs.common.tilbakekreving.VedtakId
import no.nav.etterlatte.libs.ktor.route.FoedselsnummerDTO
import no.nav.etterlatte.libs.testdata.grunnlag.SOEKER_FOEDSELSNUMMER
import no.nav.etterlatte.module
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.math.BigDecimal
import java.time.YearMonth

@TestInstance(TestInstance.Lifecycle.PER_METHOD)
class TilbakekrevingRoutesIntegrationTest : BehandlingIntegrationTest() {
    private lateinit var tilbakekrevingService: TilbakekrevingService

    @BeforeEach
    fun start() =
        startServer().also {
            tilbakekrevingService = applicationContext.tilbakekrevingService
            resetDatabase()
        }

    @AfterEach
    fun afterEach() {
        afterAll()
    }

    @Test
    fun `skal opprette tilbakekreving fra kravgrunnlag`() {
        withTestApplication { client ->
            val sak: Sak = opprettSak(client)

            val tilbakekreving = opprettTilbakekreving(sak, client)

            tilbakekreving shouldNotBe null
            tilbakekreving.status shouldBe TilbakekrevingStatus.OPPRETTET
        }
    }

    @Test
    fun `skal endre sende brev til false`() {
        withTestApplication { client ->
            val sak: Sak = opprettSak(client)

            val tilbakekreving = opprettTilbakekreving(sak, client)

            client.putAndAssertOk(
                "/api/tilbakekreving/${tilbakekreving.id}/skal-sende-brev",
                tokenSaksbehandler,
                TilbakekrevingSendeBrevRequest(false),
            )

            val oppdatertTilbakekreving: TilbakekrevingBehandling =
                client.getAndAssertOk(
                    "/api/tilbakekreving/${tilbakekreving.id}",
                    tokenSaksbehandler,
                ).body()

            oppdatertTilbakekreving.sendeBrev shouldBe false
        }
    }

    private suspend fun opprettTilbakekreving(
        sak: Sak,
        client: HttpClient,
    ): TilbakekrevingBehandling {
        val tilbakekreving: TilbakekrevingBehandling =
            client.post("/tilbakekreving") {
                addAuthToken(fagsystemTokenEY)
                contentType(ContentType.Application.Json)
                setBody(kravgrunnlag(sak))
            }.body()
        return tilbakekreving
    }

    private suspend fun opprettSak(client: HttpClient): Sak {
        val fnr = SOEKER_FOEDSELSNUMMER.value
        val sak: Sak =
            client.post("/personer/saker/${SakType.BARNEPENSJON}") {
                addAuthToken(tokenSaksbehandler)
                contentType(ContentType.Application.Json)
                setBody(FoedselsnummerDTO(fnr))
            }.apply {
                assertEquals(HttpStatusCode.OK, status)
            }.body()
        return sak
    }

    private fun kravgrunnlag(sak: Sak) =
        Kravgrunnlag(
            kravgrunnlagId = KravgrunnlagId(123L),
            sakId = SakId(sak.id),
            vedtakId = VedtakId(2L),
            kontrollFelt = Kontrollfelt(""),
            status = KravgrunnlagStatus.ANNU,
            saksbehandler = NavIdent(""),
            sisteUtbetalingslinjeId = UUID30(""),
            perioder =
                listOf(
                    KravgrunnlagPeriode(
                        periode =
                            Periode(
                                fraOgMed = YearMonth.of(2023, 1),
                                tilOgMed = YearMonth.of(2023, 2),
                            ),
                        skatt = BigDecimal(200),
                        grunnlagsbeloep =
                            listOf(
                                Grunnlagsbeloep(
                                    klasseKode = KlasseKode(""),
                                    klasseType = KlasseType.YTEL,
                                    bruttoUtbetaling = BigDecimal(1000),
                                    nyBruttoUtbetaling = BigDecimal(1200),
                                    bruttoTilbakekreving = BigDecimal(200),
                                    beloepSkalIkkeTilbakekreves = BigDecimal(200),
                                    skatteProsent = BigDecimal(20),
                                    resultat = null,
                                    skyld = null,
                                    aarsak = null,
                                ),
                                Grunnlagsbeloep(
                                    klasseKode = KlasseKode(""),
                                    klasseType = KlasseType.FEIL,
                                    bruttoUtbetaling = BigDecimal(0),
                                    nyBruttoUtbetaling = BigDecimal(0),
                                    bruttoTilbakekreving = BigDecimal(0),
                                    beloepSkalIkkeTilbakekreves = BigDecimal(0),
                                    skatteProsent = BigDecimal(0),
                                    resultat = null,
                                    skyld = null,
                                    aarsak = null,
                                ),
                            ),
                    ),
                ),
        )

    private fun HttpClient.getAndAssertOk(
        url: String,
        token: String,
        block: ((HttpResponse) -> Unit)? = null,
    ): HttpResponse {
        return get(url, token, block)
            .also { assertEquals(HttpStatusCode.OK, it.status) }
    }

    private fun HttpClient.get(
        url: String,
        token: String,
        block: ((HttpResponse) -> Unit)? = null,
    ): HttpResponse {
        return runBlocking {
            val response =
                get(url) {
                    addAuthToken(token)
                }
            block?.invoke(response)
            response
        }
    }

    private suspend fun HttpClient.patchAndAssertOk(
        url: String,
        token: String,
        body: Kabalrespons,
    ) {
        val response =
            patch(url) {
                addAuthToken(token)
                contentType(ContentType.Application.Json)
                setBody(body)
            }
        assertEquals(HttpStatusCode.OK, response.status)
    }

    private suspend fun HttpClient.putAndAssertOk(
        url: String,
        token: String,
        body: Any? = null,
    ): HttpResponse {
        return put(url) {
            contentType(ContentType.Application.Json)
            setBody(body)
            addAuthToken(token)
        }.also { assertEquals(HttpStatusCode.OK, it.status) }
    }

    private fun withTestApplication(block: suspend (client: HttpClient) -> Unit) {
        testApplication {
            val client =
                runServerWithModule(server) {
                    module(applicationContext)
                }
            block(client)
        }
    }
}
