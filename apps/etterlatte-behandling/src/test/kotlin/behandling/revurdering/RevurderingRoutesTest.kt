package no.nav.etterlatte.behandling.revurdering

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
import io.mockk.mockk
import no.nav.etterlatte.behandling.sakId1
import no.nav.etterlatte.common.Enheter
import no.nav.etterlatte.config.ApplicationContext
import no.nav.etterlatte.ktor.runServerWithModule
import no.nav.etterlatte.ktor.token.issueSaksbehandlerToken
import no.nav.etterlatte.libs.common.behandling.Revurderingaarsak
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.sak.SakMedGraderingOgSkjermet
import no.nav.etterlatte.module
import no.nav.etterlatte.saksbehandler.SaksbehandlerEnhet
import no.nav.security.mock.oauth2.MockOAuth2Server
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class RevurderingRoutesTest {
    private val applicationContext: ApplicationContext = mockk(relaxed = true)
    private val server: MockOAuth2Server = MockOAuth2Server()

    @BeforeAll
    fun before() {
        server.start()
        every { applicationContext.tilgangService } returns
            mockk {
                every { harTilgangTilBehandling(any(), any()) } returns true
                every { harTilgangTilSak(any(), any()) } returns true
            }
        every { applicationContext.saksbehandlerService.hentEnheterForSaksbehandlerIdentWrapper(any()) } returns
            listOf(
                SaksbehandlerEnhet(Enheter.defaultEnhet.enhetNr, Enheter.defaultEnhet.name),
            )
        every {
            applicationContext.sakTilgangDao.hentSakMedGraderingOgSkjerming(any())
        } returns SakMedGraderingOgSkjermet(sakId1, null, null, Enheter.defaultEnhet.enhetNr)
    }

    @AfterAll
    fun after() {
        applicationContext.close()
        server.shutdown()
    }

    @Test
    fun `kan opprette en revurdering`() {
        testApplication {
            val client =
                runServerWithModule(server) {
                    module(applicationContext)
                }

            val response =
                client.post("api/revurdering/1") {
                    header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    header(HttpHeaders.Authorization, "Bearer $token")
                    setBody(OpprettRevurderingRequest(aarsak = Revurderingaarsak.REGULERING))
                }

            assertEquals(HttpStatusCode.OK, response.status)
        }
    }

    @Test
    fun `returnerer bad request hvis payloaden er ugyldig for opprettelse av en revurdering`() {
        testApplication {
            val client = runServerWithModule(server) { module(applicationContext) }

            val response =
                client.post("api/revurdering/1") {
                    header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    header(HttpHeaders.Authorization, "Bearer $token")
                    setBody("""{ "aarsak": "foo" }""")
                }

            assertEquals(HttpStatusCode.BadRequest, response.status)
        }
    }

    @Test
    fun `returnerer gyldig revurderingstyper for barnepensjon`() {
        testApplication {
            val client =
                runServerWithModule(server) {
                    module(applicationContext)
                }

            val response =
                client.get("api/stoettederevurderinger/${SakType.BARNEPENSJON.name}") {
                    header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    header(HttpHeaders.Authorization, "Bearer $token")
                }

            val revurderingAarsak: List<Revurderingaarsak> = response.body()
            assertEquals(HttpStatusCode.OK, response.status)
            val revurderingsaarsakerForBarnepensjon =
                Revurderingaarsak.entries.filter { it.erStoettaRevurdering(SakType.BARNEPENSJON) }
            assertEquals(revurderingsaarsakerForBarnepensjon.size, revurderingAarsak.size)
            assertTrue(
                revurderingAarsak.containsAll<Any>(
                    revurderingsaarsakerForBarnepensjon,
                ),
            )
        }
    }

    @Test
    fun `returnerer gyldig revurderingstyper for omstillingsstoenad`() {
        testApplication {
            val client =
                runServerWithModule(server) {
                    module(applicationContext)
                }

            val response =
                client.get("api/stoettederevurderinger/${SakType.OMSTILLINGSSTOENAD.name}") {
                    header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    header(HttpHeaders.Authorization, "Bearer $token")
                }

            val revurderingAarsak: List<Revurderingaarsak> = response.body()
            assertEquals(HttpStatusCode.OK, response.status)
            assertTrue(
                revurderingAarsak.containsAll(
                    Revurderingaarsak.entries
                        .filter { it.gyldigForSakType(SakType.OMSTILLINGSSTOENAD) }
                        .filter { it.name !== Revurderingaarsak.NY_SOEKNAD.toString() },
                ),
            )
        }
    }

    @Test
    fun `returnerer bad request hvis saktype ikke er angitt ved uthenting av gyldig revurderingstyper`() {
        testApplication {
            val client =
                runServerWithModule(server) {
                    module(applicationContext)
                }

            val response =
                client.get("api/stoettederevurderinger/ugyldigtype") {
                    header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    header(HttpHeaders.Authorization, "Bearer $token")
                }

            assertEquals(HttpStatusCode.InternalServerError, response.status)
        }
    }

    private val token: String by lazy { server.issueSaksbehandlerToken() }
}
