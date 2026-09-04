package no.nav.etterlatte.behandling.revurdering

import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import io.ktor.client.HttpClient
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
import io.mockk.verify
import no.nav.etterlatte.behandling.sakId1
import no.nav.etterlatte.common.Enheter
import no.nav.etterlatte.config.ApplicationContext
import no.nav.etterlatte.ktor.runServer
import no.nav.etterlatte.ktor.token.issueSaksbehandlerToken
import no.nav.etterlatte.libs.common.behandling.Revurderingaarsak
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.sak.SakMedGraderingOgSkjermet
import no.nav.etterlatte.saksbehandler.SaksbehandlerEnhet
import no.nav.etterlatte.settOppApplikasjonen
import no.nav.security.mock.oauth2.MockOAuth2Server
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.util.UUID

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class RevurderingRoutesTest {
    private val applicationContext: ApplicationContext = mockk(relaxed = true)
    private val mockOAuth2Server: MockOAuth2Server = MockOAuth2Server()

    @BeforeAll
    fun before() {
        mockOAuth2Server.start()
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
        every { applicationContext.sakService.finnSak(any()) } returns
            mockk {
                every { sakType } returns SakType.BARNEPENSJON
            }
    }

    @AfterAll
    fun after() {
        applicationContext.close()
        mockOAuth2Server.shutdown()
    }

    @Test
    fun `kan opprette manuell revurdering inntektsjustering`() {
        withTestApplication { client ->
            val response =
                client.post("api/revurdering/1/manuell-inntektsjustering") {
                    header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    header(HttpHeaders.Authorization, "Bearer $token")
                    setBody(OpprettManuellInntektsjustering(oppgaveId = UUID.randomUUID()))
                }

            assertEquals(HttpStatusCode.OK, response.status)
        }
    }

    @Test
    fun `kan opprette en revurdering`() {
        withTestApplication { client ->
            val response =
                client.post("api/revurdering/1") {
                    header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    header(HttpHeaders.Authorization, "Bearer $token")
                    setBody(OpprettRevurderingRequest(aarsak = Revurderingaarsak.REGULERING))
                }

            assertEquals(HttpStatusCode.OK, response.status)
            verify {
                applicationContext.manuellRevurderingService.opprettManuellRevurderingWrapper(
                    any(),
                    any(),
                    any(),
                    any(),
                    any(),
                    any(),
                    any(),
                    any(),
                    any(),
                )
            }
        }
    }

    @Test
    fun `returnerer bad request hvis payloaden er ugyldig for opprettelse av en revurdering`() {
        withTestApplication { client ->
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
        withTestApplication { client ->
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
        withTestApplication { client ->
            val response =
                client.get("api/stoettederevurderinger/${SakType.OMSTILLINGSSTOENAD.name}") {
                    header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    header(HttpHeaders.Authorization, "Bearer $token")
                }

            val aarsaker: List<Revurderingaarsak> = response.body()

            response.status shouldBe HttpStatusCode.OK
            aarsaker shouldContainExactlyInAnyOrder
                Revurderingaarsak.entries
                    .filter { it.gyldigForSakType(SakType.OMSTILLINGSSTOENAD) }
                    .filter {
                        it.name !in
                            listOf(
                                Revurderingaarsak.AARLIG_INNTEKTSJUSTERING.toString(),
                            )
                    }
        }
    }

    @Test
    fun `returnerer bad request hvis saktype ikke er angitt ved uthenting av gyldig revurderingstyper`() {
        withTestApplication { client ->
            val response =
                client.get("api/stoettederevurderinger/ugyldigtype") {
                    header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    header(HttpHeaders.Authorization, "Bearer $token")
                }

            assertEquals(HttpStatusCode.InternalServerError, response.status)
        }
    }

    private val token: String by lazy { mockOAuth2Server.issueSaksbehandlerToken() }

    private fun withTestApplication(block: suspend (client: HttpClient) -> Unit) {
        testApplication {
            val client =
                runServer(mockOAuth2Server, withMetrics = true) {
                    settOppApplikasjonen(applicationContext)
                }
            block(client)
        }
    }
}
