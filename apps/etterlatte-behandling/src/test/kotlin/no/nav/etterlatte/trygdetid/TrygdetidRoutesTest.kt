package no.nav.etterlatte.trygdetid

import io.kotest.matchers.shouldBe
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import no.nav.etterlatte.behandling.sakId1
import no.nav.etterlatte.common.Enheter
import no.nav.etterlatte.config.ApplicationContext
import no.nav.etterlatte.ktor.runServerWithModule
import no.nav.etterlatte.ktor.token.issueSaksbehandlerToken
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.sak.SakMedGraderingOgSkjermet
import no.nav.etterlatte.libs.common.trygdetid.StatusOppdatertDto
import no.nav.etterlatte.libs.common.trygdetid.TrygdetidDto
import no.nav.etterlatte.module
import no.nav.etterlatte.saksbehandler.SaksbehandlerEnhet
import no.nav.security.mock.oauth2.MockOAuth2Server
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.util.UUID.randomUUID

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class TrygdetidRoutesTest {
    private val applicationContext: ApplicationContext = mockk(relaxed = true)
    private val server = MockOAuth2Server()

    @BeforeAll
    fun before() {
        server.start()
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
    fun `skal returnere 204 naar trygdetid ikke finnes`() {
        coEvery { applicationContext.trygdetidService.hentTrygdetiderIBehandling(any(), any()) } returns emptyList()

        testApplication {
            val client = runServerWithModule(server) { module(applicationContext) }

            val response =
                client.get("/api/trygdetid_v2/${randomUUID()}") {
                    header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    header(HttpHeaders.Authorization, "Bearer $token")
                }

            response.status shouldBe HttpStatusCode.NoContent
        }
    }

    @Test
    fun `skal returnere 200 og status ved kall mot oppdater-status`() {
        coEvery {
            applicationContext.trygdetidService.sjekkGyldighetOgOppdaterBehandlingStatus(any(), any())
        } returns true

        testApplication {
            val client = runServerWithModule(server) { module(applicationContext) }

            val response =
                client.post("/api/trygdetid_v2/${randomUUID()}/oppdater-status") {
                    header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    header(HttpHeaders.Authorization, "Bearer $token")
                }

            val dto = objectMapper.readValue(response.bodyAsText(), StatusOppdatertDto::class.java)
            response.status shouldBe HttpStatusCode.OK
            dto.statusOppdatert shouldBe true
        }
    }

    @Test
    fun `skal returnere 200 ved kall til kopier-og-overskriv`() {
        val behandlingId = randomUUID()
        val kildeBehandlingId = randomUUID()
        val etterKopiering = listOf(trygdetid(behandlingId = behandlingId)).map(Trygdetid::toDto)

        coEvery {
            applicationContext.trygdetidService.kopierOgOverskrivTrygdetid(
                behandlingId = behandlingId,
                kildeBehandlingId = kildeBehandlingId,
                brukerTokenInfo = any(),
            )
        } returns etterKopiering

        testApplication {
            val client = runServerWithModule(server) { module(applicationContext) }

            val response =
                client.post("/api/trygdetid_v2/$behandlingId/kopier-og-overskriv/$kildeBehandlingId") {
                    header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    header(HttpHeaders.Authorization, "Bearer $token")
                }

            val responseBody =
                objectMapper.readValue(
                    response.bodyAsText(),
                    objectMapper.typeFactory.constructCollectionType(List::class.java, TrygdetidDto::class.java),
                ) as List<*>
            response.status shouldBe HttpStatusCode.OK
            responseBody.size shouldBe 1
        }
    }

    @Test
    fun `skal returnere behandling-id for annen behandling med trygdetid for samme avdoede`() {
        val behandlingId = randomUUID()
        val kildeBehandlingId = randomUUID()

        coEvery {
            applicationContext.trygdetidService.finnBehandlingMedTrygdetidForSammeAvdoede(behandlingId, any())
        } returns kildeBehandlingId

        testApplication {
            val client = runServerWithModule(server) { module(applicationContext) }

            val response =
                client.get("/api/trygdetid_v2/$behandlingId/behandling-med-trygdetid-for-avdoede") {
                    header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    header(HttpHeaders.Authorization, "Bearer $token")
                }

            response.status shouldBe HttpStatusCode.OK
            response.bodyAsText() shouldBe kildeBehandlingId.toString()
        }
    }

    private val token: String by lazy { server.issueSaksbehandlerToken() }
}
