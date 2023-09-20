package no.nav.etterlatte.behandling.klage

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.serialization.jackson.JacksonConverter
import io.ktor.server.testing.testApplication
import io.mockk.every
import io.mockk.mockk
import no.nav.etterlatte.BehandlingIntegrationTest
import no.nav.etterlatte.behandling.objectMapper
import no.nav.etterlatte.common.Enheter
import no.nav.etterlatte.libs.common.FoedselsnummerDTO
import no.nav.etterlatte.libs.common.behandling.Klage
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.pdlhendelse.Adressebeskyttelse
import no.nav.etterlatte.libs.common.pdlhendelse.Endringstype
import no.nav.etterlatte.libs.common.person.AdressebeskyttelseGradering
import no.nav.etterlatte.libs.common.person.Folkeregisteridentifikator
import no.nav.etterlatte.libs.common.sak.Sak
import no.nav.etterlatte.libs.common.skjermet.EgenAnsattSkjermet
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.module
import no.nav.etterlatte.sak.SakServiceFeatureToggle
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_METHOD)
class KlageRoutesIntegrationTest : BehandlingIntegrationTest() {
    @BeforeEach
    fun start() =
        startServer(
            featureToggleService =
                mockk {
                    every { isEnabled(KlageFeatureToggle.KanBrukeKlageToggle, any()) } returns true
                    every { isEnabled(SakServiceFeatureToggle.FiltrerMedEnhetId, any()) } returns true
                },
        )

    @AfterEach
    fun afterEach() {
        afterAll()
    }

    @Test
    fun `opprettelse av klage går bra og henting gir 404 etter at saken blir skjermet`() {
        withTestApplication { client ->
            val fnr = Folkeregisteridentifikator.of("08071272487").value
            val sak: Sak =
                client.post("/personer/saker/${SakType.BARNEPENSJON}") {
                    addAuthToken(tokenSaksbehandler)
                    contentType(ContentType.Application.Json)
                    setBody(FoedselsnummerDTO(fnr))
                }.apply {
                    assertEquals(HttpStatusCode.OK, status)
                }.body()

            val klage: Klage =
                client.post("/api/klage/opprett/${sak.id}") {
                    addAuthToken(tokenSaksbehandler)
                }.body()
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
                setBody(EgenAnsattSkjermet(fnr = fnr, inntruffet = Tidspunkt.now(), skjermet = true))
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
    fun `opprettelse av klage går bra og henting gjør tilgangskontroll når saken får adressebeskyttelse`() {
        withTestApplication { client ->
            val fnr = Folkeregisteridentifikator.of("08071272487").value
            val sak: Sak =
                client.post("/personer/saker/${SakType.BARNEPENSJON}") {
                    addAuthToken(tokenSaksbehandler)
                    contentType(ContentType.Application.Json)
                    setBody(FoedselsnummerDTO(fnr))
                }.apply {
                    assertEquals(HttpStatusCode.OK, status)
                }.body()
            val klage: Klage =
                client.post("/api/klage/opprett/${sak.id}") {
                    addAuthToken(tokenSaksbehandler)
                }.body()
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
                        fnr = fnr,
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

    private fun withTestApplication(block: suspend (client: HttpClient) -> Unit) {
        testApplication {
            environment {
                config = hoconApplicationConfig
            }
            application {
                module(applicationContext)
            }
            val client =
                createClient {
                    install(ContentNegotiation) {
                        register(ContentType.Application.Json, JacksonConverter(objectMapper = objectMapper))
                    }
                }

            block(client)
        }
    }
}
