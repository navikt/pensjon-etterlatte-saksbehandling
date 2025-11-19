package no.nav.etterlatte.behandling.klage

import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.equals.shouldBeEqual
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.testing.testApplication
import io.mockk.coVerifyAll
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.BehandlingIntegrationTest
import no.nav.etterlatte.behandling.hendelse.LagretHendelse
import no.nav.etterlatte.funksjonsbrytere.DummyFeatureToggleService
import no.nav.etterlatte.ktor.runServerWithModule
import no.nav.etterlatte.libs.common.behandling.BehandlingResultat
import no.nav.etterlatte.libs.common.behandling.Formkrav
import no.nav.etterlatte.libs.common.behandling.InitieltUtfallMedBegrunnelseDto
import no.nav.etterlatte.libs.common.behandling.JaNei
import no.nav.etterlatte.libs.common.behandling.KabalStatus
import no.nav.etterlatte.libs.common.behandling.Kabalrespons
import no.nav.etterlatte.libs.common.behandling.Klage
import no.nav.etterlatte.libs.common.behandling.KlageStatus
import no.nav.etterlatte.libs.common.behandling.KlageUtfall
import no.nav.etterlatte.libs.common.behandling.KlageUtfallMedData
import no.nav.etterlatte.libs.common.behandling.KlageUtfallUtenBrev
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.behandling.VedtaketKlagenGjelder
import no.nav.etterlatte.libs.common.klage.AarsakTilAvbrytelse
import no.nav.etterlatte.libs.common.oppgave.OppgaveIntern
import no.nav.etterlatte.libs.common.oppgave.SaksbehandlerEndringDto
import no.nav.etterlatte.libs.common.sak.Sak
import no.nav.etterlatte.libs.common.toJson
import no.nav.etterlatte.libs.ktor.route.FoedselsnummerDTO
import no.nav.etterlatte.libs.testdata.grunnlag.SOEKER_FOEDSELSNUMMER
import no.nav.etterlatte.module
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.time.LocalDate
import java.time.Month
import java.time.OffsetDateTime
import java.time.ZonedDateTime
import java.util.UUID

@TestInstance(TestInstance.Lifecycle.PER_METHOD)
class KlageRoutesIntegrationTest : BehandlingIntegrationTest() {
    @BeforeEach
    fun start() =
        startServer(
            featureToggleService =
                DummyFeatureToggleService().apply {
                    settBryter(KlageFeatureToggle.KanOppretteVedtakAvvisningToggle, true)
                },
        ).also {
            resetDatabase()
        }

    @AfterEach
    fun afterEach() {
        afterAll()
    }

    @Test
    fun `dato sendt inn med norsk offset parses til riktig norsk localdate`() {
        val innkommendeKlageSommertid =
            InnkommendeKlageDto(
                // 17. april ved midnatt norsk sommertid
                mottattDato = "2024-04-16T22:00:00.000Z",
                journalpostId = "",
                innsender = "",
            )
        val forventetParsetDatoSommertid = LocalDate.of(2024, Month.APRIL, 17)
        assertEquals(forventetParsetDatoSommertid, innkommendeKlageSommertid.parseMottattDato())

        val innkommendeKlageVintertid =
            InnkommendeKlageDto(
                // 3. februar ved midnatt norsk vintertid
                mottattDato = "2024-02-02T23:00:00.000Z",
                journalpostId = "",
                innsender = "",
            )

        val forventetParsetDatoVintertid = LocalDate.of(2024, Month.FEBRUARY, 3)
        assertEquals(forventetParsetDatoVintertid, innkommendeKlageVintertid.parseMottattDato())
    }

    @Test
    fun `avbrytelse av klage gaar bra`() {
        withTestApplication { client ->
            val sak: Sak = opprettSak(client)

            val klage: Klage = opprettKlage(sak, client)
            tildelOppgavenForKlagen(client, klage, "Saksbehandler01")

            client.postAndAssertOk(
                "/api/klage/${klage.id}/avbryt",
                tokenSaksbehandler,
                AvbrytKlageDto(AarsakTilAvbrytelse.ANNET, "Bla bla"),
            )
            val avbruttKlage = hentKlage(client, klage.id)
            with(avbruttKlage) {
                id shouldBe klage.id
                status shouldBe KlageStatus.AVBRUTT
                aarsakTilAvbrytelse shouldBe AarsakTilAvbrytelse.ANNET
            }
        }
    }

    @Test
    fun `oppdatering av kabalstatus gaar bra`() {
        withTestApplication { client ->
            val sak: Sak = opprettSak(client)
            val klage: Klage = opprettKlage(sak, client)

            client.patchAndAssertOk(
                "/api/klage/${klage.id}/kabalstatus",
                systemBruker,
                Kabalrespons(KabalStatus.FERDIGSTILT, BehandlingResultat.IKKE_MEDHOLD),
            )

            hentKlage(client, klage.id)
                .let { oppdatertKlage ->
                    oppdatertKlage.id shouldBe klage.id
                    oppdatertKlage.kabalStatus shouldBe KabalStatus.FERDIGSTILT
                }
        }
    }

    @Test
    fun `lagring av initielt utfall`() {
        withTestApplication { client ->
            val sak: Sak = opprettSak(client)
            val klage: Klage =
                vurderFormkrav(opprettKlage(sak, client), client)

            val oppdatertKlage =
                client
                    .putAndAssertOk(
                        "/api/klage/${klage.id}/initieltutfall",
                        tokenSaksbehandler,
                        InitieltUtfallMedBegrunnelseDto(KlageUtfall.STADFESTE_VEDTAK, "Begrunnelse"),
                    ).body<Klage>()
            oppdatertKlage shouldBeEqual hentKlage(client, klage.id)

            with(oppdatertKlage.initieltUtfall!!) {
                utfallMedBegrunnelse.utfall shouldBe KlageUtfall.STADFESTE_VEDTAK
                utfallMedBegrunnelse.begrunnelse shouldBe "Begrunnelse"
                saksbehandler shouldBe "Saksbehandler01"
                tidspunkt shouldNotBe null
            }
            oppdatertKlage.status shouldBe KlageStatus.FORMKRAV_OPPFYLT
        }
    }

    @Test
    fun `lagring av utfall = avvist trigger vedtak og brev`() {
        withTestApplication { client ->
            val sak: Sak = opprettSak(client)
            val klage: Klage =
                vurderFormkrav(
                    klage = opprettKlage(sak = sak, client = client),
                    client = client,
                    erKlagenFramsattInnenFrist = JaNei.NEI,
                )
            val oppdatertKlage: Klage = vurderUtfall(klage, client, KlageUtfallUtenBrev.Avvist())

            coVerifyAll {
                applicationContext.vedtakKlient.lagreVedtakKlage(
                    klage,
                    withArg { it.ident() shouldBe "Saksbehandler01" },
                )
            }
            oppdatertKlage shouldBeEqual hentKlage(client, klage.id)
            oppdatertKlage.status shouldBe KlageStatus.UTFALL_VURDERT
            with(oppdatertKlage.utfall as KlageUtfallMedData.Avvist) {
                brev.brevId shouldNotBe null
                vedtak.vedtakId shouldNotBe null
            }
        }
    }

    @Test
    fun `attestering gaar bra`() {
        withTestApplication { client ->
            val vedtakKlient = applicationContext.vedtakKlient
            val sak: Sak = opprettSak(client)
            val klage = opprettKlageOgFattVedtakOmAvvisning(client, sak, saksbehandlerIdent)
            tildelOppgavenForKlagen(client, klage, attestantIdent)

            val attestert: Klage =
                client
                    .postAndAssertOk(
                        "/api/klage/${klage.id}/vedtak/attester",
                        tokenAttestant,
                        KlageAttesterRequest("Laissez un commentaire ici"),
                    ).body()

            coVerifyAll {
                vedtakKlient.lagreVedtakKlage(any(), any())
                vedtakKlient.fattVedtakKlage(any(), any())
                vedtakKlient.attesterVedtakKlage(
                    withArg {
                        it.id shouldBeEqual klage.id
                        it.status shouldBeEqual KlageStatus.FERDIGSTILT
                        it.utfall!! shouldBeEqual klage.utfall!!
                    },
                    withArg { it.ident() shouldBe attestantIdent },
                )
            }
            attestert shouldBeEqual hentKlage(client, klage.id)
            attestert.status shouldBe KlageStatus.FERDIGSTILT
            with(attestert.utfall as KlageUtfallMedData.Avvist) {
                brev.brevId shouldNotBe null
                vedtak.vedtakId shouldNotBe null
            }
        }
    }

    @Test
    fun `underkjenn gaar bra`() {
        withTestApplication { client ->
            val attestantIdent = "Saksbehandler02"
            val vedtakKlient = applicationContext.vedtakKlient
            val sak: Sak = opprettSak(client)
            val klage = opprettKlageOgFattVedtakOmAvvisning(client, sak, saksbehandlerIdent)
            tildelOppgavenForKlagen(client, klage, attestantIdent)

            val underkjent: Klage =
                client
                    .postAndAssertOk(
                        "/api/klage/${klage.id}/vedtak/underkjenn",
                        tokenAttestant,
                        KlageUnderkjennRequest("Laissez un commentaire ici, s'il te plait", "VALGT_BEGRUNNELSE"),
                    ).body()

            coVerifyAll {
                vedtakKlient.lagreVedtakKlage(any(), any())
                vedtakKlient.fattVedtakKlage(any(), any())
                vedtakKlient.underkjennVedtakKlage(klage.id, withArg { it.ident() shouldBe attestantIdent })
            }
            underkjent shouldBeEqual hentKlage(client, klage.id)
            underkjent.status shouldBe KlageStatus.RETURNERT
            with(underkjent.utfall as KlageUtfallMedData.Avvist) {
                brev.brevId shouldNotBe null
                vedtak.vedtakId shouldNotBe null
            }

            val hendelserIBehandling: List<LagretHendelse> =
                client
                    .getAndAssertOk("/api/sak/${sak.id}/hendelser", tokenAttestant)
                    .body<List<LagretHendelse>>()
                    .filter { it.behandlingId == klage.id }

            hendelserIBehandling.map { it.hendelse } shouldContainExactly
                listOf("KLAGE:OPPRETTET", "VEDTAK:FATTET", "VEDTAK:UNDERKJENT")
        }
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

    private suspend fun opprettKlage(
        sak: Sak,
        client: HttpClient,
        mottattDato: String = OffsetDateTime.now().toString(),
    ): Klage {
        val klage: Klage =
            client
                .post("/api/klage/opprett/${sak.id}") {
                    addAuthToken(tokenSaksbehandler)
                    contentType(ContentType.Application.Json)
                    setBody(
                        mapOf(
                            "mottattDato" to mottattDato,
                            "journalpostId" to "",
                            "innsender" to "En klager",
                        ).toJson(),
                    )
                }.body()
        return klage
    }

    private suspend fun vurderUtfall(
        klage: Klage,
        client: HttpClient,
        utfall: KlageUtfallUtenBrev,
    ): Klage {
        val response =
            client.put("/api/klage/${klage.id}/utfall") {
                addAuthToken(tokenSaksbehandler)
                contentType(ContentType.Application.Json)
                setBody(VurdertUtfallDto(utfall))
            }
        assertEquals(HttpStatusCode.OK, response.status)
        return response.body()
    }

    private suspend fun vurderInitieltUtfall(
        klage: Klage,
        client: HttpClient,
        klageUtfall: KlageUtfall,
    ): Klage {
        val response =
            client.put("/api/klage/${klage.id}/initieltutfall") {
                addAuthToken(tokenSaksbehandler)
                contentType(ContentType.Application.Json)
                setBody(InitieltUtfallMedBegrunnelseDto(klageUtfall, "Fordi"))
            }
        assertEquals(HttpStatusCode.OK, response.status)
        return response.body()
    }

    private suspend fun vurderFormkrav(
        klage: Klage,
        client: HttpClient,
        erKlagenFramsattInnenFrist: JaNei = JaNei.JA,
    ): Klage =
        client
            .put("/api/klage/${klage.id}/formkrav") {
                addAuthToken(tokenSaksbehandler)
                contentType(ContentType.Application.Json)
                setBody(
                    VurdereFormkravDto(
                        Formkrav(
                            begrunnelse = "Begrunnelse",
                            erFormkraveneOppfylt = JaNei.JA,
                            erKlagenSignert = JaNei.JA,
                            erKlagerPartISaken = JaNei.JA,
                            vedtaketKlagenGjelder =
                                VedtaketKlagenGjelder(
                                    "12",
                                    UUID.randomUUID().toString(),
                                    ZonedDateTime.now(),
                                    null,
                                ),
                            gjelderKlagenNoeKonkretIVedtaket = JaNei.JA,
                            erKlagenFramsattInnenFrist = erKlagenFramsattInnenFrist,
                        ),
                    ),
                )
            }.body()

    private suspend fun fattVedtak(
        klage: Klage,
        client: HttpClient,
    ): Klage =
        client
            .post("/api/klage/${klage.id}/vedtak/fatt") {
                addAuthToken(tokenSaksbehandler)
                contentType(ContentType.Application.Json)
            }.body()

    private suspend fun opprettKlageOgFattVedtakOmAvvisning(
        client: HttpClient,
        sak: Sak,
        saksbehandler: String,
    ): Klage {
        val klage = opprettKlage(sak, client)
        vurderFormkrav(klage, client)
        vurderInitieltUtfall(klage, client, KlageUtfall.AVVIST)
        vurderUtfall(klage, client, KlageUtfallUtenBrev.Avvist())
        tildelOppgavenForKlagen(client, klage, saksbehandler)
        return fattVedtak(klage, client)
    }

    private suspend fun tildelOppgavenForKlagen(
        client: HttpClient,
        klage: Klage,
        saksbehandler: String,
    ) {
        val oppgaver: List<OppgaveIntern> =
            client
                .get("/api/oppgaver") {
                    addAuthToken(tokenSaksbehandler)
                    header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                }.also {
                    assertEquals(HttpStatusCode.OK, it.status)
                }.body()

        val oppgave = oppgaver.single { it.referanse == klage.id.toString() }

        println("Tildeler oppgave ${oppgave.id} til saksbehandler $saksbehandler for klage ${klage.id}")
        client
            .post("/api/oppgaver/${oppgave.id}/tildel-saksbehandler") {
                val dto = SaksbehandlerEndringDto(saksbehandler)
                addAuthToken(tokenSaksbehandler)
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                setBody(dto)
            }.also {
                assertEquals(HttpStatusCode.OK, it.status)
            }
    }

    private suspend fun hentKlage(
        client: HttpClient,
        klageId: UUID,
        token: String = tokenSaksbehandler,
    ): Klage = client.getAndAssertOk("/api/klage/$klageId", token).body()

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
    ): HttpResponse =
        put(url) {
            contentType(ContentType.Application.Json)
            setBody(body)
            addAuthToken(token)
        }

    private fun withTestApplication(block: suspend (client: HttpClient) -> Unit) {
        testApplication {
            val client =
                runServerWithModule(mockOAuth2Server) {
                    module(applicationContext)
                }
            block(client)
        }
    }
}
