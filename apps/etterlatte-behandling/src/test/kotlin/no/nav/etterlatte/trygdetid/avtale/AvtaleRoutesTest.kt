package no.nav.etterlatte.trygdetid.avtale

import io.kotest.matchers.shouldBe
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import no.nav.etterlatte.behandling.sakId1
import no.nav.etterlatte.common.Enheter
import no.nav.etterlatte.config.ApplicationContext
import no.nav.etterlatte.ktor.runServerWithModule
import no.nav.etterlatte.ktor.token.issueSaksbehandlerToken
import no.nav.etterlatte.libs.common.sak.SakMedGraderingOgSkjermet
import no.nav.etterlatte.libs.common.trygdetid.avtale.Trygdeavtale
import no.nav.etterlatte.module
import no.nav.etterlatte.saksbehandler.SaksbehandlerEnhet
import no.nav.etterlatte.trygdetid.trygdeavtale
import no.nav.security.mock.oauth2.MockOAuth2Server
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.util.UUID.randomUUID

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class AvtaleRoutesTest {
    private val applicationContext: ApplicationContext = mockk(relaxed = true)
    private val server = MockOAuth2Server()

    @BeforeAll
    fun before() {
        server.start()
        every { applicationContext.internTrygdetidAktivert } returns true
        every { applicationContext.tilgangService } returns
            mockk {
                every { harTilgangTilBehandling(any(), any()) } returns true
            }
        every {
            applicationContext.saksbehandlerService.hentEnheterForSaksbehandlerIdentWrapper(any())
        } returns listOf(SaksbehandlerEnhet(enhetsNummer = Enheter.defaultEnhet.enhetNr, navn = Enheter.defaultEnhet.navn))
        every {
            applicationContext.sakTilgangDao.hentSakMedGraderingOgSkjermingPaaBehandling(any())
        } returns
            SakMedGraderingOgSkjermet(
                id = sakId1,
                adressebeskyttelseGradering = null,
                erSkjermet = null,
                enhetNr = Enheter.defaultEnhet.enhetNr,
            )
    }

    @AfterAll
    fun after() {
        applicationContext.close()
        server.shutdown()
    }

    @Test
    fun `skal levere informasjon om avtaler`() {
        every { applicationContext.avtaleService.hentAvtaler() } returns listOf(mockk(relaxed = true))

        testApplication {
            val client = runServerWithModule(server) { module(applicationContext) }

            val response =
                client.get("/api/trygdetid/avtaler") {
                    header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    header(HttpHeaders.Authorization, "Bearer $token")
                }

            response.status shouldBe HttpStatusCode.OK
        }
    }

    @Test
    fun `skal levere informasjon om avtale kriterier`() {
        every { applicationContext.avtaleService.hentAvtaleKriterier() } returns listOf(mockk(relaxed = true))

        testApplication {
            val client = runServerWithModule(server) { module(applicationContext) }

            val response =
                client.get("/api/trygdetid/avtaler/kriteria") {
                    header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    header(HttpHeaders.Authorization, "Bearer $token")
                }

            response.status shouldBe HttpStatusCode.OK
        }
    }

    @Test
    fun `skal returnere 204 naar avtale ikke finnes for behandling`() {
        every { applicationContext.avtaleService.hentAvtaleForBehandling(any()) } returns null

        testApplication {
            val client = runServerWithModule(server) { module(applicationContext) }

            val response =
                client.get("/api/trygdetid/avtaler/${randomUUID()}") {
                    header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    header(HttpHeaders.Authorization, "Bearer $token")
                }

            response.status shouldBe HttpStatusCode.NoContent
        }
    }

    @Test
    fun `skal levere avtale for behandling`() {
        val behandlingId = randomUUID()
        every {
            applicationContext.avtaleService.hentAvtaleForBehandling(behandlingId)
        } returns trygdeavtale(behandlingId = behandlingId, avtaleKode = "TEST")

        testApplication {
            val client = runServerWithModule(server) { module(applicationContext) }

            val response =
                client.get("/api/trygdetid/avtaler/$behandlingId") {
                    header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    header(HttpHeaders.Authorization, "Bearer $token")
                }

            response.status shouldBe HttpStatusCode.OK
            response.body<Trygdeavtale>().avtaleKode shouldBe "TEST"
        }
    }

    @Test
    fun `skal opprette avtale for behandling`() {
        val behandlingId = randomUUID()
        every { applicationContext.avtaleService.opprettAvtale(any()) } just runs

        testApplication {
            val client = runServerWithModule(server) { module(applicationContext) }

            val response =
                client.post("/api/trygdetid/avtaler/$behandlingId") {
                    header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    header(HttpHeaders.Authorization, "Bearer $token")
                    setBody(
                        TrygdeavtaleRequest(
                            id = null,
                            avtaleKode = "TEST",
                            avtaleDatoKode = "TESTDATO",
                            avtaleKriteriaKode = "TESTKRITERIA",
                            personKrets = null,
                            arbInntekt1G = null,
                            arbInntekt1GKommentar = null,
                            beregArt50 = null,
                            beregArt50Kommentar = null,
                            nordiskTrygdeAvtale = null,
                            nordiskTrygdeAvtaleKommentar = null,
                        ),
                    )
                }

            response.status shouldBe HttpStatusCode.OK
            val avtale = response.body<Trygdeavtale>()
            avtale.avtaleKode shouldBe "TEST"
            avtale.avtaleDatoKode shouldBe "TESTDATO"
            avtale.behandlingId shouldBe behandlingId
        }
    }

    @Test
    fun `skal oppdatere avtale for behandling`() {
        val behandlingId = randomUUID()
        val id = randomUUID()
        every { applicationContext.avtaleService.lagreAvtale(any()) } just runs

        testApplication {
            val client = runServerWithModule(server) { module(applicationContext) }

            val response =
                client.post("/api/trygdetid/avtaler/$behandlingId") {
                    header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    header(HttpHeaders.Authorization, "Bearer $token")
                    setBody(
                        TrygdeavtaleRequest(
                            id = id,
                            avtaleKode = "TEST",
                            avtaleDatoKode = "TESTDATO",
                            avtaleKriteriaKode = "TESTKRITERIA",
                            personKrets = null,
                            arbInntekt1G = null,
                            arbInntekt1GKommentar = null,
                            beregArt50 = null,
                            beregArt50Kommentar = null,
                            nordiskTrygdeAvtale = null,
                            nordiskTrygdeAvtaleKommentar = null,
                        ),
                    )
                }

            response.status shouldBe HttpStatusCode.OK
            val avtale = response.body<Trygdeavtale>()
            avtale.id shouldBe id
            avtale.avtaleKode shouldBe "TEST"
            avtale.behandlingId shouldBe behandlingId
        }
    }

    private val token: String by lazy { server.issueSaksbehandlerToken() }
}
