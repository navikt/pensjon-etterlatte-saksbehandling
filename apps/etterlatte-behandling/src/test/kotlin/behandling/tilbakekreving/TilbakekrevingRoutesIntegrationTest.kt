package no.nav.etterlatte.behandling.tilbakekreving

import behandling.tilbakekreving.kravgrunnlag
import behandling.tilbakekreving.tilbakekrevingVurdering
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
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
import no.nav.etterlatte.inTransaction
import no.nav.etterlatte.ktor.runServerWithModule
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.feilhaandtering.ExceptionResponse
import no.nav.etterlatte.libs.common.oppgave.Status
import no.nav.etterlatte.libs.common.sak.Sak
import no.nav.etterlatte.libs.common.tilbakekreving.TilbakekrevingBehandling
import no.nav.etterlatte.libs.common.tilbakekreving.TilbakekrevingResultat
import no.nav.etterlatte.libs.common.tilbakekreving.TilbakekrevingSkyld
import no.nav.etterlatte.libs.common.tilbakekreving.TilbakekrevingStatus
import no.nav.etterlatte.libs.ktor.route.FoedselsnummerDTO
import no.nav.etterlatte.libs.testdata.grunnlag.SOEKER_FOEDSELSNUMMER
import no.nav.etterlatte.mockSaksbehandler
import no.nav.etterlatte.module
import no.nav.etterlatte.nyKontekstMedBrukerOgDatabase
import no.nav.etterlatte.oppgave.OppgaveService
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class TilbakekrevingRoutesIntegrationTest : BehandlingIntegrationTest() {
    private lateinit var tilbakekrevingService: TilbakekrevingService
    private lateinit var oppgaveService: OppgaveService

    @BeforeAll
    fun start() {
        startServer()
        tilbakekrevingService = applicationContext.tilbakekrevingService
        oppgaveService = applicationContext.oppgaveService

        nyKontekstMedBrukerOgDatabase(mockSaksbehandler("Z123456"), applicationContext.dataSource)
    }

    @BeforeEach
    fun beforeEach() {
        resetDatabase()
    }

    @AfterAll
    fun afterAllTests() {
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
    fun `skal kunne oppdatere vurdering paa tilbakekreving`() {
        withTestApplication { client ->
            val tilbakekreving = opprettTilbakekrevingOgTildelOppgave(client)

            client.putAndAssertOk(
                "/api/tilbakekreving/${tilbakekreving.id}/vurdering",
                tokenSaksbehandler,
                tilbakekrevingVurdering("en ny beskrivelse"),
            )

            val oppdatertTilbakekreving: TilbakekrevingBehandling =
                client
                    .getAndAssertOk(
                        "/api/tilbakekreving/${tilbakekreving.id}",
                        tokenSaksbehandler,
                    ).body()

            oppdatertTilbakekreving.tilbakekreving.vurdering?.beskrivelse shouldBe "en ny beskrivelse"
        }
    }

    @Test
    fun `skal kunne oppdatere perioder paa tilbakekreving`() {
        withTestApplication { client ->
            val tilbakekreving = opprettTilbakekrevingOgTildelOppgave(client)

            val oppdatertPeriode =
                tilbakekreving.tilbakekreving.perioder.first().let {
                    it.copy(ytelse = it.ytelse.copy(nettoTilbakekreving = 100))
                }

            client.putAndAssertOk(
                "/api/tilbakekreving/${tilbakekreving.id}/perioder",
                tokenSaksbehandler,
                TilbakekrevingPerioderRequest(listOf(oppdatertPeriode)),
            )

            val oppdatertTilbakekreving: TilbakekrevingBehandling =
                client
                    .getAndAssertOk(
                        "/api/tilbakekreving/${tilbakekreving.id}",
                        tokenSaksbehandler,
                    ).body()

            oppdatertTilbakekreving.tilbakekreving.perioder
                .first()
                .ytelse.nettoTilbakekreving shouldBe 100
        }
    }

    @Test
    fun `skal feile dersom ikke paakrevde felter er fylt ut`() {
        withTestApplication { client ->
            val tilbakekreving = opprettTilbakekrevingOgTildelOppgave(client)

            client.putAndAssertOk(
                "/api/tilbakekreving/${tilbakekreving.id}/vurdering",
                tokenSaksbehandler,
                tilbakekrevingVurdering(),
            )

            val respons =
                client.postAndAssertOk(
                    "/api/tilbakekreving/${tilbakekreving.id}/valider",
                    tokenSaksbehandler,
                )

            val exceptionRespons: ExceptionResponse = respons.body()

            respons.status shouldBe HttpStatusCode.BadRequest
            exceptionRespons.code shouldBe "UGYLDIGE_FELTER"
        }
    }

    @Test
    fun `skal validere tilbakekreving og sette status til validert`() {
        withTestApplication { client ->
            val tilbakekreving = opprettTilbakekrevingOgTildelOppgave(client)

            client.putAndAssertOk(
                "/api/tilbakekreving/${tilbakekreving.id}/vurdering",
                tokenSaksbehandler,
                tilbakekrevingVurdering(),
            )

            val oppdatertPeriode =
                tilbakekreving.tilbakekreving.perioder.first().let {
                    it.copy(
                        ytelse =
                            it.ytelse.copy(
                                beregnetFeilutbetaling = 100,
                                bruttoTilbakekreving = 100,
                                nettoTilbakekreving = 100,
                                skatt = 10,
                                skyld = TilbakekrevingSkyld.BRUKER,
                                resultat = TilbakekrevingResultat.FULL_TILBAKEKREV,
                                tilbakekrevingsprosent = 100,
                                rentetillegg = 10,
                            ),
                    )
                }

            client.putAndAssertOk(
                "/api/tilbakekreving/${tilbakekreving.id}/perioder",
                tokenSaksbehandler,
                TilbakekrevingPerioderRequest(listOf(oppdatertPeriode)),
            )

            val respons =
                client.postAndAssertOk(
                    "/api/tilbakekreving/${tilbakekreving.id}/valider",
                    tokenSaksbehandler,
                )

            val validertTilbakekreving: TilbakekrevingBehandling = respons.body()

            validertTilbakekreving.status shouldBe TilbakekrevingStatus.VALIDERT
        }
    }

    @Test
    fun `skal endre sende brev`() {
        withTestApplication { client ->
            val tilbakekreving = opprettTilbakekrevingOgTildelOppgave(client)

            client.putAndAssertOk(
                "/api/tilbakekreving/${tilbakekreving.id}/skal-sende-brev",
                tokenSaksbehandler,
                TilbakekrevingSendeBrevRequest(false),
            )

            val oppdatertTilbakekreving: TilbakekrevingBehandling =
                client
                    .getAndAssertOk(
                        "/api/tilbakekreving/${tilbakekreving.id}",
                        tokenSaksbehandler,
                    ).body()

            oppdatertTilbakekreving.sendeBrev shouldBe false
        }
    }

    @Test
    fun `skal hente liste med tilbakekrevinger for sak`() {
        withTestApplication { client ->
            val sak: Sak = opprettSak(client)

            val tilbakekreving = opprettTilbakekreving(sak, client)

            val tilbakekrevinger: List<TilbakekrevingBehandling> =
                client
                    .getAndAssertOk(
                        "/api/tilbakekreving/sak/${tilbakekreving.sak.id}",
                        tokenSaksbehandler,
                    ).body()

            tilbakekrevinger.size shouldBe 1
        }
    }

    @Test
    fun `skal sette tilbakekreving paa vent og saa tilbake av vent`() {
        withTestApplication { client ->
            val tilbakekreving = opprettTilbakekrevingOgTildelOppgave(client)

            client.putAndAssertOk(
                "/tilbakekreving/${tilbakekreving.sak.id}/oppgave-status",
                systemBruker,
                OppgaveStatusRequest(paaVent = true),
            )

            inTransaction {
                val oppgave = oppgaveService.hentOppgaverForReferanse(tilbakekreving.id.toString()).first()
                oppgave.status shouldBe Status.PAA_VENT
                oppgave.merknad shouldBe "Kravgrunnlag er sperret"
            }

            client.putAndAssertOk(
                "/tilbakekreving/${tilbakekreving.sak.id}/oppgave-status",
                systemBruker,
                OppgaveStatusRequest(paaVent = false),
            )

            inTransaction {
                val oppgave = oppgaveService.hentOppgaverForReferanse(tilbakekreving.id.toString()).first()
                oppgave.status shouldBe Status.UNDER_BEHANDLING
                oppgave.merknad shouldBe "Sperre på kravgrunnlag opphevet"
            }
        }
    }

    @Test
    fun `skal avbryte en tilbakekreving`() {
        withTestApplication { client ->
            val tilbakekreving = opprettTilbakekrevingOgTildelOppgave(client)

            client.putAndAssertOk(
                "/tilbakekreving/${tilbakekreving.sak.id}/avbryt",
                systemBruker,
            )

            inTransaction {
                val oppgave = oppgaveService.hentOppgaverForReferanse(tilbakekreving.id.toString()).first()
                oppgave.status shouldBe Status.AVBRUTT
            }

            val avbruttTilbakekreving = tilbakekrevingService.hentTilbakekreving(tilbakekreving.id)
            avbruttTilbakekreving.status shouldBe TilbakekrevingStatus.AVBRUTT
        }
    }

    private suspend fun opprettTilbakekrevingOgTildelOppgave(client: HttpClient): TilbakekrevingBehandling {
        val sak: Sak = opprettSak(client)
        val tilbakekreving = opprettTilbakekreving(sak, client)
        inTransaction { tildelOppgaveTilSaksbehandler(tilbakekreving, saksbehandlerIdent) }
        return tilbakekreving
    }

    private fun tildelOppgaveTilSaksbehandler(
        tilbakekreving: TilbakekrevingBehandling,
        saksbehandlerIdent: String,
    ) {
        oppgaveService.hentOppgaverForReferanse(tilbakekreving.id.toString()).first().let {
            oppgaveService.tildelSaksbehandler(it.id, saksbehandlerIdent)
        }
    }

    private suspend fun opprettTilbakekreving(
        sak: Sak,
        client: HttpClient,
    ): TilbakekrevingBehandling {
        val tilbakekreving: TilbakekrevingBehandling =
            client
                .post("/tilbakekreving/${sak.id}") {
                    addAuthToken(systemBruker)
                    contentType(ContentType.Application.Json)
                    setBody(kravgrunnlag(sak))
                }.body()
        return tilbakekreving
    }

    private suspend fun opprettSak(client: HttpClient): Sak {
        val fnr = SOEKER_FOEDSELSNUMMER.value
        val sak: Sak =
            client
                .post("/personer/saker/${SakType.BARNEPENSJON}") {
                    addAuthToken(tokenSaksbehandler)
                    contentType(ContentType.Application.Json)
                    setBody(FoedselsnummerDTO(fnr))
                }.apply {
                    assertEquals(HttpStatusCode.OK, status)
                }.body()
        return sak
    }

    private fun HttpClient.getAndAssertOk(
        url: String,
        token: String,
        block: ((HttpResponse) -> Unit)? = null,
    ): HttpResponse =
        get(url, token, block)
            .also { assertEquals(HttpStatusCode.OK, it.status) }

    private fun HttpClient.get(
        url: String,
        token: String,
        block: ((HttpResponse) -> Unit)? = null,
    ): HttpResponse =
        runBlocking {
            val response =
                get(url) {
                    addAuthToken(token)
                }
            block?.invoke(response)
            response
        }

    private suspend fun HttpClient.postAndAssertOk(
        s: String,
        token: String,
        body: Any? = null,
    ): HttpResponse =
        post(s) {
            contentType(ContentType.Application.Json)
            setBody(body)
            addAuthToken(token)
        }

    private suspend fun HttpClient.putAndAssertOk(
        url: String,
        token: String,
        body: Any? = null,
    ): HttpResponse =
        put(url) {
            contentType(ContentType.Application.Json)
            setBody(body)
            addAuthToken(token)
        }.also { assertEquals(HttpStatusCode.OK, it.status) }

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
