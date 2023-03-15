package no.nav.etterlatte.adressebeskyttelse

import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.jackson.jackson
import io.ktor.server.testing.testApplication
import no.nav.etterlatte.BehandlingIntegrationTest
import no.nav.etterlatte.TILGANG_ROUTE_PATH
import no.nav.etterlatte.behandling.BehandlingsBehov
import no.nav.etterlatte.libs.common.behandling.Persongalleri
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.pdlhendelse.Adressebeskyttelse
import no.nav.etterlatte.libs.common.pdlhendelse.AdressebeskyttelseGradering
import no.nav.etterlatte.libs.common.pdlhendelse.Endringstype
import no.nav.etterlatte.libs.common.person.Foedselsnummer
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.tidspunkt.toLocalDatetimeUTC
import no.nav.etterlatte.module
import no.nav.etterlatte.sak.Sak
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.util.UUID

@TestInstance(TestInstance.Lifecycle.PER_METHOD)
class AdressebeskyttelseTest : BehandlingIntegrationTest() {

    @BeforeEach
    fun start() = startServer()

    @AfterEach
    fun AfterEach() {
        afterAll()
    }

    @Test
    fun `Skal kunne se på en vanlig behandling før adressebeskyttelse men ikke etter med mindre systembruker`() {
        val fnr = Foedselsnummer.of("08071272487").value

        testApplication {
            environment {
                config = hoconApplicationConfig
            }
            val client = createClient {
                install(ContentNegotiation) {
                    jackson { registerModule(JavaTimeModule()) }
                }
            }
            application {
                module(beanFactory)
            }

            val sak: Sak = client.get("personer/$fnr/saker/${SakType.BARNEPENSJON}") {
                addAuthToken(tokenSaksbehandler)
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            }.let {
                Assertions.assertEquals(HttpStatusCode.OK, it.status)
                it.body()
            }
            Assertions.assertNotNull(sak.id)

            val behandlingId = client.post("/behandlinger/foerstegangsbehandling") {
                addAuthToken(tokenSaksbehandler)
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                setBody(
                    BehandlingsBehov(
                        sak.id,
                        Persongalleri(fnr, "innsender", emptyList(), emptyList(), emptyList()),
                        Tidspunkt.now().toLocalDatetimeUTC().toString()
                    )
                )
            }.let {
                Assertions.assertEquals(HttpStatusCode.OK, it.status)
                UUID.fromString(it.body())
            }

            client.get("/behandlinger/$behandlingId") {
                addAuthToken(systemBruker)
            }.let {
                Assertions.assertEquals(HttpStatusCode.OK, it.status)
            }

            client.get("/behandlinger/$behandlingId") {
                addAuthToken(tokenSaksbehandler)
            }.let {
                Assertions.assertEquals(HttpStatusCode.OK, it.status)
            }

            client.post("/grunnlagsendringshendelse/adressebeskyttelse") {
                addAuthToken(tokenServiceUser)
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                setBody(
                    Adressebeskyttelse(
                        fnr = fnr,
                        adressebeskyttelseGradering = AdressebeskyttelseGradering.STRENGT_FORTROLIG,
                        endringstype = Endringstype.OPPRETTET
                    )
                )
            }

            client.get("/behandlinger/$behandlingId") {
                addAuthToken(tokenSaksbehandler)
            }.let {
                Assertions.assertEquals(HttpStatusCode.NotFound, it.status)
            }
            // smoketest
            client.get("/fakeurlwithbehandlingsid/$behandlingId") {
                addAuthToken(tokenSaksbehandler)
            }.let {
                Assertions.assertEquals(HttpStatusCode.NotFound, it.status)
            }
            // systemBruker skal alltid ha tilgang
            client.get("/behandlinger/$behandlingId") {
                addAuthToken(systemBruker)
            }.let {
                Assertions.assertEquals(HttpStatusCode.OK, it.status)
            }
        }
    }

    @Test
    fun `Skal kunne hente behandlinger på fnr før adressebeskyttelse`() {
        val fnr = Foedselsnummer.of("08071272487").value

        testApplication {
            environment {
                config = hoconApplicationConfig
            }
            val client = createClient {
                install(ContentNegotiation) {
                    jackson { registerModule(JavaTimeModule()) }
                }
            }
            application {
                module(beanFactory)
            }

            val sak: Sak = client.get("personer/$fnr/saker/${SakType.BARNEPENSJON}") {
                addAuthToken(tokenSaksbehandler)
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            }.let {
                Assertions.assertEquals(HttpStatusCode.OK, it.status)
                it.body()
            }
            Assertions.assertNotNull(sak.id)

            val behandlingId = client.post("/behandlinger/foerstegangsbehandling") {
                addAuthToken(tokenSaksbehandler)
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                setBody(
                    BehandlingsBehov(
                        sak.id,
                        Persongalleri(fnr, "innsender", emptyList(), emptyList(), emptyList()),
                        Tidspunkt.now().toLocalDatetimeUTC().toString()
                    )
                )
            }.let {
                Assertions.assertEquals(HttpStatusCode.OK, it.status)
                UUID.fromString(it.body())
            }

            client.get("/behandlinger/$behandlingId") {
                addAuthToken(tokenSaksbehandler)
            }.let {
                Assertions.assertEquals(HttpStatusCode.OK, it.status)
            }

            client.get("api/personer/$fnr/behandlinger") {
                addAuthToken(tokenSaksbehandler)
            }.let {
                Assertions.assertEquals(HttpStatusCode.OK, it.status)
            }

            client.post("/grunnlagsendringshendelse/adressebeskyttelse") {
                addAuthToken(tokenServiceUser)
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                setBody(
                    Adressebeskyttelse(
                        fnr = fnr,
                        adressebeskyttelseGradering = AdressebeskyttelseGradering.STRENGT_FORTROLIG,
                        endringstype = Endringstype.OPPRETTET
                    )
                )
            }

            client.get("api/personer/$fnr/behandlinger") {
                addAuthToken(tokenSaksbehandler)
            }.let {
                Assertions.assertEquals(HttpStatusCode.NotFound, it.status)
            }

            client.get("api/personer/$fnr/behandlinger") {
                addAuthToken(systemBruker)
            }.let {
                Assertions.assertEquals(HttpStatusCode.OK, it.status)
            }
        }
    }

    @Test
    fun `skal kunne aksessere tilgangsroutes også etter adressebeskyttelse`() {
        val fnr = Foedselsnummer.of("08071272487").value

        testApplication {
            environment {
                config = hoconApplicationConfig
            }
            val client = createClient {
                install(ContentNegotiation) {
                    jackson { registerModule(JavaTimeModule()) }
                }
            }
            application {
                module(beanFactory)
            }

            val sak: Sak = client.get("personer/$fnr/saker/${SakType.BARNEPENSJON}") {
                addAuthToken(tokenSaksbehandler)
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            }.let {
                Assertions.assertEquals(HttpStatusCode.OK, it.status)
                it.body()
            }
            Assertions.assertNotNull(sak.id)

            val behandlingId = client.post("/behandlinger/foerstegangsbehandling") {
                addAuthToken(tokenSaksbehandler)
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                setBody(
                    BehandlingsBehov(
                        sak.id,
                        Persongalleri(fnr, "innsender", emptyList(), emptyList(), emptyList()),
                        Tidspunkt.now().toLocalDatetimeUTC().toString()
                    )
                )
            }.let {
                Assertions.assertEquals(HttpStatusCode.OK, it.status)
                UUID.fromString(it.body())
            }

            client.get("/behandlinger/$behandlingId") {
                addAuthToken(systemBruker)
            }.let {
                Assertions.assertEquals(HttpStatusCode.OK, it.status)
            }

            val harTilgang: Boolean = client.get("/$TILGANG_ROUTE_PATH/behandling/$behandlingId") {
                addAuthToken(tokenSaksbehandler)
            }.let {
                Assertions.assertEquals(HttpStatusCode.OK, it.status)
                it.body()
            }
            Assertions.assertTrue(harTilgang)

            client.post("/grunnlagsendringshendelse/adressebeskyttelse") {
                addAuthToken(tokenServiceUser)
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                setBody(
                    Adressebeskyttelse(
                        fnr = fnr,
                        adressebeskyttelseGradering = AdressebeskyttelseGradering.STRENGT_FORTROLIG,
                        endringstype = Endringstype.OPPRETTET
                    )
                )
            }

            val harIkkeTilgang: Boolean = client.get("/$TILGANG_ROUTE_PATH/behandling/$behandlingId") {
                addAuthToken(tokenSaksbehandler)
            }.let {
                Assertions.assertEquals(HttpStatusCode.OK, it.status)
                it.body()
            }
            Assertions.assertFalse(harIkkeTilgang)
        }
    }
}