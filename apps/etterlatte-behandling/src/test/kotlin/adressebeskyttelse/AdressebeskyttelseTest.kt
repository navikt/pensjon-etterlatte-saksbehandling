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
import io.ktor.http.contentType
import io.ktor.serialization.jackson.jackson
import io.ktor.server.testing.testApplication
import no.nav.etterlatte.BehandlingIntegrationTest
import no.nav.etterlatte.behandling.BehandlingsBehov
import no.nav.etterlatte.libs.common.FoedselsNummerMedGraderingDTO
import no.nav.etterlatte.libs.common.FoedselsnummerDTO
import no.nav.etterlatte.libs.common.behandling.Persongalleri
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.pdlhendelse.Adressebeskyttelse
import no.nav.etterlatte.libs.common.pdlhendelse.Endringstype
import no.nav.etterlatte.libs.common.person.AdressebeskyttelseGradering
import no.nav.etterlatte.libs.common.person.Folkeregisteridentifikator
import no.nav.etterlatte.libs.common.sak.Sak
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.tidspunkt.toLocalDatetimeUTC
import no.nav.etterlatte.module
import no.nav.etterlatte.tilgangsstyring.TILGANG_ROUTE_PATH
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.util.*

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
        val fnr = Folkeregisteridentifikator.of("08071272487").value

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
                module(applicationContext)
            }

            val sak: Sak = client.post("personer/saker/${SakType.BARNEPENSJON}") {
                addAuthToken(tokenSaksbehandler)
                contentType(ContentType.Application.Json)
                setBody(FoedselsnummerDTO(fnr))
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            }.let {
                Assertions.assertEquals(HttpStatusCode.OK, it.status)
                it.body()
            }
            Assertions.assertNotNull(sak.id)

            val behandlingId = client.post("/behandlinger/opprettbehandling") {
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
                addAuthToken(systemBruker)
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                setBody(
                    Adressebeskyttelse(
                        hendelseId = "1",
                        endringstype = Endringstype.OPPRETTET,
                        fnr = fnr,
                        adressebeskyttelseGradering = AdressebeskyttelseGradering.STRENGT_FORTROLIG
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
        val fnr = Folkeregisteridentifikator.of("08071272487").value

        testApplication {
            environment {
                config = hoconApplicationConfig
            }
            val httpClient = createClient {
                install(ContentNegotiation) {
                    jackson { registerModule(JavaTimeModule()) }
                }
            }
            application {
                module(applicationContext)
            }

            val sak: Sak = httpClient.post("personer/saker/${SakType.BARNEPENSJON}") {
                addAuthToken(tokenSaksbehandler)
                contentType(ContentType.Application.Json)
                setBody(FoedselsnummerDTO(fnr))
            }.let {
                Assertions.assertEquals(HttpStatusCode.OK, it.status)
                it.body()
            }
            Assertions.assertNotNull(sak.id)

            val behandlingId = httpClient.post("/behandlinger/opprettbehandling") {
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

            httpClient.get("/behandlinger/$behandlingId") {
                addAuthToken(tokenSaksbehandler)
            }.let {
                Assertions.assertEquals(HttpStatusCode.OK, it.status)
            }

            httpClient.post("api/personer/behandlinger") {
                addAuthToken(tokenSaksbehandler)
                contentType(ContentType.Application.Json)
                setBody(FoedselsnummerDTO(fnr))
            }.let {
                Assertions.assertEquals(HttpStatusCode.OK, it.status)
            }

            httpClient.post("/grunnlagsendringshendelse/adressebeskyttelse") {
                addAuthToken(systemBruker)
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                setBody(
                    Adressebeskyttelse(
                        hendelseId = "1",
                        fnr = fnr,
                        adressebeskyttelseGradering = AdressebeskyttelseGradering.STRENGT_FORTROLIG,
                        endringstype = Endringstype.OPPRETTET
                    )
                )
            }

            httpClient.post("api/personer/behandlinger") {
                addAuthToken(tokenSaksbehandler)
                contentType(ContentType.Application.Json)
                setBody(FoedselsnummerDTO(fnr))
            }.let {
                Assertions.assertEquals(HttpStatusCode.NotFound, it.status)
            }

            httpClient.post("api/personer/behandlinger") {
                addAuthToken(systemBruker)
                contentType(ContentType.Application.Json)
                setBody(FoedselsnummerDTO(fnr))
            }.let {
                Assertions.assertEquals(HttpStatusCode.OK, it.status)
            }
        }
    }

    @Test
    fun `skal kunne aksessere tilgangsroutes også etter adressebeskyttelse`() {
        val fnr = Folkeregisteridentifikator.of("08071272487").value

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
                module(applicationContext)
            }

            val sak: Sak = client.post("personer/saker/${SakType.BARNEPENSJON}") {
                addAuthToken(tokenSaksbehandler)
                contentType(ContentType.Application.Json)
                setBody(FoedselsnummerDTO(fnr))
            }.let {
                Assertions.assertEquals(HttpStatusCode.OK, it.status)
                it.body()
            }
            Assertions.assertNotNull(sak.id)

            val behandlingId = client.post("/behandlinger/opprettbehandling") {
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
                addAuthToken(systemBruker)
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                setBody(
                    Adressebeskyttelse(
                        hendelseId = "1",
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

    @Test
    fun `Skal kunne hente saker på fnr før adressebeskyttelse`() {
        val fnr = Folkeregisteridentifikator.of("08071272487").value
        testApplication {
            environment {
                config = hoconApplicationConfig
            }
            val httpClient = createClient {
                install(ContentNegotiation) {
                    jackson { registerModule(JavaTimeModule()) }
                }
            }
            application {
                module(applicationContext)
            }

            httpClient.post("/personer/saker/${SakType.BARNEPENSJON}") {
                addAuthToken(tokenSaksbehandler)
                contentType(ContentType.Application.Json)
                setBody(FoedselsnummerDTO(fnr))
            }.apply {
                Assertions.assertEquals(HttpStatusCode.OK, status)
            }.body<Sak>()

            httpClient.post("/grunnlagsendringshendelse/adressebeskyttelse") {
                addAuthToken(systemBruker)
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                setBody(
                    Adressebeskyttelse(
                        hendelseId = "1",
                        fnr = fnr,
                        adressebeskyttelseGradering = AdressebeskyttelseGradering.STRENGT_FORTROLIG,
                        endringstype = Endringstype.OPPRETTET
                    )
                )
            }

            httpClient.post("/personer/saker/${SakType.BARNEPENSJON}") {
                addAuthToken(tokenSaksbehandler)
                contentType(ContentType.Application.Json)
                setBody(FoedselsnummerDTO(fnr))
            }.apply {
                Assertions.assertEquals(HttpStatusCode.NotFound, status)
            }
        }
    }

    @Test
    fun `Skal kunne sende med gradering ved opprettelse av sak`() {
        val fnr = Folkeregisteridentifikator.of("08071272487").value
        testApplication {
            environment {
                config = hoconApplicationConfig
            }
            val httpClient = createClient {
                install(ContentNegotiation) {
                    jackson { registerModule(JavaTimeModule()) }
                }
            }
            application {
                module(applicationContext)
            }

            httpClient.post("/personer/saker/${SakType.BARNEPENSJON}") {
                addAuthToken(tokenSaksbehandler)
                contentType(ContentType.Application.Json)
                setBody(FoedselsNummerMedGraderingDTO(fnr, AdressebeskyttelseGradering.STRENGT_FORTROLIG))
            }.apply {
                Assertions.assertEquals(HttpStatusCode.OK, status)
            }.body<Sak>()

            httpClient.post("/personer/saker/${SakType.BARNEPENSJON}") {
                addAuthToken(tokenSaksbehandler)
                contentType(ContentType.Application.Json)
                setBody(FoedselsNummerMedGraderingDTO(fnr))
            }.apply {
                Assertions.assertEquals(HttpStatusCode.NotFound, status)
            }

            httpClient.post("/personer/saker/${SakType.BARNEPENSJON}") {
                addAuthToken(tokenSaksbehandlerMedStrengtFortrolig)
                contentType(ContentType.Application.Json)
                setBody(FoedselsNummerMedGraderingDTO(fnr))
            }.apply {
                Assertions.assertEquals(HttpStatusCode.OK, status)
            }

            httpClient.post("/personer/saker/${SakType.BARNEPENSJON}") {
                addAuthToken(systemBruker)
                contentType(ContentType.Application.Json)
                setBody(FoedselsNummerMedGraderingDTO(fnr))
            }.apply {
                Assertions.assertEquals(HttpStatusCode.OK, status)
            }
        }
    }
}