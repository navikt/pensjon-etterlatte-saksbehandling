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
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import no.nav.etterlatte.BehandlingIntegrationTest
import no.nav.etterlatte.behandling.hendelse.LagretHendelse
import no.nav.etterlatte.common.Enheter
import no.nav.etterlatte.ktor.runServerWithModule
import no.nav.etterlatte.libs.common.FoedselsnummerDTO
import no.nav.etterlatte.libs.common.behandling.BehandlingResultat
import no.nav.etterlatte.libs.common.behandling.Formkrav
import no.nav.etterlatte.libs.common.behandling.InnkommendeKlage
import no.nav.etterlatte.libs.common.behandling.JaNei
import no.nav.etterlatte.libs.common.behandling.KabalStatus
import no.nav.etterlatte.libs.common.behandling.Kabalrespons
import no.nav.etterlatte.libs.common.behandling.Klage
import no.nav.etterlatte.libs.common.behandling.KlageStatus
import no.nav.etterlatte.libs.common.behandling.KlageUtfallMedData
import no.nav.etterlatte.libs.common.behandling.KlageUtfallUtenBrev
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.behandling.VedtaketKlagenGjelder
import no.nav.etterlatte.libs.common.klage.AarsakTilAvbrytelse
import no.nav.etterlatte.libs.common.oppgave.OppgaveListe
import no.nav.etterlatte.libs.common.oppgave.SaksbehandlerEndringDto
import no.nav.etterlatte.libs.common.pdlhendelse.Adressebeskyttelse
import no.nav.etterlatte.libs.common.pdlhendelse.Endringstype
import no.nav.etterlatte.libs.common.person.AdressebeskyttelseGradering
import no.nav.etterlatte.libs.common.sak.Sak
import no.nav.etterlatte.libs.common.skjermet.EgenAnsattSkjermet
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.testdata.grunnlag.SOEKER_FOEDSELSNUMMER
import no.nav.etterlatte.module
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.time.LocalDate
import java.util.UUID

@TestInstance(TestInstance.Lifecycle.PER_METHOD)
class KlageRoutesIntegrationTest : BehandlingIntegrationTest() {
    @BeforeEach
    fun start() =
        startServer(
            featureToggleService =
                mockk {
                    every { isEnabled(KlageFeatureToggle.KanBrukeKlageToggle, any()) } returns true
                    every { isEnabled(KlageFeatureToggle.KanOppretteVedtakAvvisningToggle, any()) } returns true
                    every { isEnabled(KlageFeatureToggle.KanFerdigstilleKlageToggle, any()) } returns true
                },
        ).also {
            resetDatabase()
        }

    @AfterEach
    fun afterEach() {
        afterAll()
    }

    @Test
    fun `opprettelse av klage gaar bra og henting gir 404 etter at saken blir skjermet`() {
        withTestApplication { client ->
            val sak: Sak = opprettSak(client)

            val klage: Klage = opprettKlage(sak, client)
            val response =
                client.get("/api/klage/${klage.id}") {
                    addAuthToken(tokenSaksbehandler)
                }
            assertEquals(HttpStatusCode.OK, response.status)
            val hentetKlage = response.body<Klage>()
            assertEquals(klage, hentetKlage)

            // setter skjerming for saken
            client.post("/egenansatt") {
                addAuthToken(fagsystemTokenEY)
                contentType(ContentType.Application.Json)
                setBody(EgenAnsattSkjermet(fnr = sak.ident, inntruffet = Tidspunkt.now(), skjermet = true))
            }

            // henter med saksbehandler som mangler tilgang
            val responseNotFound =
                client.get("/api/klage/${klage.id}") {
                    addAuthToken(tokenSaksbehandler)
                }
            assertEquals(HttpStatusCode.NotFound, responseNotFound.status)
            // henter med saksbehandler som har tilgang
            val responseSaksbehandlerMedTilgang =
                client.get("/api/klage/${klage.id}") {
                    addAuthToken(tokenSaksbehandlerMedEgenAnsattTilgang)
                }
            assertEquals(HttpStatusCode.OK, responseSaksbehandlerMedTilgang.status)
            assertEquals(klage.id, responseSaksbehandlerMedTilgang.body<Klage>().id)
        }
    }

    @Test
    fun `opprettelse av klage gaar bra og henting gjoer tilgangskontroll naar saken faar adressebeskyttelse`() {
        withTestApplication { client ->
            val sak: Sak = opprettSak(client)
            val klage: Klage =
                opprettKlage(sak, client)
            val response =
                client.get("/api/klage/${klage.id}") {
                    addAuthToken(tokenSaksbehandler)
                }
            assertEquals(HttpStatusCode.OK, response.status)
            val hentetKlage = response.body<Klage>()
            assertEquals(klage, hentetKlage)

            // Setter strengt fortrolig på saken klagen hører til
            client.post("/grunnlagsendringshendelse/adressebeskyttelse") {
                addAuthToken(fagsystemTokenEY)
                contentType(ContentType.Application.Json)
                setBody(
                    Adressebeskyttelse(
                        hendelseId = "1",
                        endringstype = Endringstype.OPPRETTET,
                        fnr = sak.ident,
                        adressebeskyttelseGradering = AdressebeskyttelseGradering.STRENGT_FORTROLIG,
                    ),
                )
            }

            val responseVanligSaksbehandler =
                client.get("/api/klage/${klage.id}") {
                    addAuthToken(tokenSaksbehandler)
                }
            assertEquals(HttpStatusCode.NotFound, responseVanligSaksbehandler.status)
            val responseSaksbehandlerMedTilgang =
                client.get("/api/klage/${klage.id}") {
                    addAuthToken(tokenSaksbehandlerMedStrengtFortrolig)
                }
            assertEquals(HttpStatusCode.OK, responseSaksbehandlerMedTilgang.status)
            assertEquals(
                klage.copy(sak = klage.sak.copy(enhet = Enheter.STRENGT_FORTROLIG.enhetNr)),
                responseSaksbehandlerMedTilgang.body<Klage>(),
            )
        }
    }

    @Test
    fun `avbrytelse av klage gaar bra`() {
        withTestApplication { client ->
            val sak: Sak = opprettSak(client)

            val klage: Klage =
                opprettKlage(sak, client)
            val oppgaver: OppgaveListe =
                client.get("/api/oppgaver/sak/${sak.id}/oppgaver") {
                    addAuthToken(systemBruker)
                }.body()
            val oppgaveForKlagen = oppgaver.oppgaver.first { it.referanse == klage.id.toString() }

            client.post("/api/oppgaver/${oppgaveForKlagen.id}/tildel-saksbehandler/") {
                addAuthToken(tokenSaksbehandler)
                contentType(ContentType.Application.Json)
                setBody(SaksbehandlerEndringDto("Saksbehandler01"))
            }

            val response =
                client.post("/api/klage/${klage.id}/avbryt") {
                    addAuthToken(tokenSaksbehandler)
                    contentType(ContentType.Application.Json)
                    setBody(AvbrytKlageDto(AarsakTilAvbrytelse.ANNET, "Bla bla"))
                }
            assertEquals(HttpStatusCode.OK, response.status)

            val avbruttKlageResponse =
                client.get("/api/klage/${klage.id}") {
                    addAuthToken(tokenSaksbehandler)
                }.also { assertEquals(HttpStatusCode.OK, it.status) }

            val avbruttKlage = avbruttKlageResponse.body<Klage>()
            assertEquals(klage.id, avbruttKlage.id)
            assertEquals(KlageStatus.AVBRUTT, avbruttKlage.status)
            assertEquals(AarsakTilAvbrytelse.ANNET, avbruttKlage.aarsakTilAvbrytelse)
        }
    }

    @Test
    fun `oppdatering av kabalstatus gaar bra`() {
        withTestApplication { client ->
            val sak: Sak = opprettSak(client)

            val klage: Klage = opprettKlage(sak, client)

            val response =
                client.patch("/api/klage/${klage.id}/kabalstatus") {
                    addAuthToken(fagsystemTokenEY)
                    contentType(ContentType.Application.Json)
                    setBody(Kabalrespons(KabalStatus.FERDIGSTILT, BehandlingResultat.IKKE_MEDHOLD))
                }
            assertEquals(HttpStatusCode.OK, response.status)

            client.get("/api/klage/${klage.id}") {
                addAuthToken(tokenSaksbehandler)
            }.also { assertEquals(HttpStatusCode.OK, it.status) }
                .also { respons ->
                    val oppdatert = respons.body<Klage>()
                    assertEquals(oppdatert.id, klage.id)
                    assertEquals(KabalStatus.FERDIGSTILT, oppdatert.kabalStatus)
                }
        }
    }

    @Test
    fun `lagring av utfall trigger vedtak og brev`() {
        withTestApplication { client ->
            val sak: Sak = opprettSak(client)
            val klage: Klage =
                vurderFormkrav(
                    opprettKlage(sak, client),
                    client,
                )
            val response = vurderUtfallTilAvvist(klage, client)
            assertEquals(HttpStatusCode.OK, response.status)

            coVerify {
                applicationContext.vedtakKlient.lagreVedtakKlage(
                    klage,
                    withArg { it.ident() shouldBe "Saksbehandler01" },
                )
            }

            val oppdatertKlage =
                client.get("/api/klage/${klage.id}") {
                    addAuthToken(tokenSaksbehandler)
                }.body<Klage>()

            oppdatertKlage.status shouldBe KlageStatus.UTFALL_VURDERT
            with(oppdatertKlage.utfall as KlageUtfallMedData.Avvist) {
                brev.brevId shouldNotBe null
                vedtak.vedtakId shouldNotBe null
            }
        }
    }

    private suspend fun vurderUtfallTilAvvist(
        klage: Klage,
        client: HttpClient,
    ): HttpResponse {
        val response =
            client.put("/api/klage/${klage.id}/utfall") {
                addAuthToken(tokenSaksbehandler)
                contentType(ContentType.Application.Json)
                setBody(VurdertUtfallDto(KlageUtfallUtenBrev.Avvist()))
            }
        return response
    }

    @Test
    fun `attestering gaar bra`() {
        withTestApplication { client ->
            val vedtakKlient = applicationContext.vedtakKlient
            val sak: Sak = opprettSak(client)
            val klage = opprettKlageOgFattVedtak(client, sak)

            val response =
                client.post("/api/klage/${klage.id}/vedtak/attester") {
                    addAuthToken(tokenSaksbehandler)
                    contentType(ContentType.Application.Json)
                    setBody(KlageAttesterRequest("Laissez un commentaire ici"))
                }
            assertEquals(HttpStatusCode.OK, response.status)

            coVerify {
                vedtakKlient.lagreVedtakKlage(any(), any())
                vedtakKlient.fattVedtakKlage(any(), any())
                vedtakKlient.attesterVedtakKlage(klage, withArg { it.ident() shouldBe "Saksbehandler01" })
            }

            val oppdatertKlage =
                client.get("/api/klage/${klage.id}") {
                    addAuthToken(tokenSaksbehandler)
                }.body<Klage>()

            oppdatertKlage.status shouldBe KlageStatus.ATTESTERT
            with(oppdatertKlage.utfall as KlageUtfallMedData.Avvist) {
                brev.brevId shouldNotBe null
                vedtak.vedtakId shouldNotBe null
            }
        }
    }

    @Test
    fun `underkjenn gaar bra`() {
        withTestApplication { client ->
            val vedtakKlient = applicationContext.vedtakKlient
            val sak: Sak = opprettSak(client)
            val klage = opprettKlageOgFattVedtak(client, sak)

            val response =
                client.post("/api/klage/${klage.id}/vedtak/underkjenn") {
                    addAuthToken(tokenSaksbehandler)
                    contentType(ContentType.Application.Json)
                    setBody(KlageUnderkjennRequest("Laissez un commentaire ici", "VALGT_BEGRUNNELSE"))
                }
            assertEquals(HttpStatusCode.OK, response.status)

            coVerify {
                vedtakKlient.lagreVedtakKlage(any(), any())
                vedtakKlient.fattVedtakKlage(any(), any())
                vedtakKlient.underkjennVedtakKlage(klage.id, withArg { it.ident() shouldBe "Saksbehandler01" })

                applicationContext.hendelseDao.finnHendelserIBehandling(klage.id) shouldNotBe emptyList<LagretHendelse>()
            }

            val oppdatertKlage =
                client.get("/api/klage/${klage.id}") {
                    addAuthToken(tokenSaksbehandler)
                }.body<Klage>()

            oppdatertKlage.status shouldBe KlageStatus.RETURNERT
            with(oppdatertKlage.utfall as KlageUtfallMedData.Avvist) {
                brev.brevId shouldNotBe null
                vedtak.vedtakId shouldNotBe null
            }
        }
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

    private suspend fun opprettKlage(
        sak: Sak,
        client: HttpClient,
    ): Klage {
        val klage: Klage =
            client.post("/api/klage/opprett/${sak.id}") {
                addAuthToken(tokenSaksbehandler)
                contentType(ContentType.Application.Json)
                setBody(
                    InnkommendeKlage(
                        mottattDato = LocalDate.now(),
                        journalpostId = "",
                        innsender = "En klager",
                    ),
                )
            }.body()
        return klage
    }

    private suspend fun vurderFormkrav(
        klage: Klage,
        client: HttpClient,
    ): Klage {
        return client.put("/api/klage/${klage.id}/formkrav") {
            addAuthToken(tokenSaksbehandler)
            contentType(ContentType.Application.Json)
            setBody(
                VurdereFormkravDto(
                    Formkrav(
                        begrunnelse = "Begrunnelse",
                        erFormkraveneOppfylt = JaNei.JA,
                        erKlagenSignert = JaNei.JA,
                        erKlagerPartISaken = JaNei.JA,
                        vedtaketKlagenGjelder = VedtaketKlagenGjelder("12", UUID.randomUUID().toString(), null, null),
                        gjelderKlagenNoeKonkretIVedtaket = JaNei.JA,
                        erKlagenFramsattInnenFrist = JaNei.JA,
                    ),
                ),
            )
        }.body()
    }

    private suspend fun fattVedtak(
        klage: Klage,
        client: HttpClient,
    ): Klage {
        return client.post("/api/klage/${klage.id}/vedtak/fatt") {
            addAuthToken(tokenSaksbehandler)
            contentType(ContentType.Application.Json)
        }.body()
    }

    private suspend fun opprettKlageOgFattVedtak(
        client: HttpClient,
        sak: Sak,
    ): Klage {
        val klage = opprettKlage(sak, client)
        vurderFormkrav(klage, client)
        vurderUtfallTilAvvist(klage, client)
        return fattVedtak(klage, client)
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
