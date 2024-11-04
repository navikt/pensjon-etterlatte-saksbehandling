package no.nav.etterlatte.adressebeskyttelse

import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.testing.testApplication
import io.mockk.spyk
import no.nav.etterlatte.BehandlingIntegrationTest
import no.nav.etterlatte.PdltjenesterKlientTest
import no.nav.etterlatte.behandling.tilgang.SKRIVETILGANG_CALL_QUERYPARAMETER
import no.nav.etterlatte.ktor.runServerWithModule
import no.nav.etterlatte.libs.common.behandling.BehandlingsBehov
import no.nav.etterlatte.libs.common.behandling.Persongalleri
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.dbutils.Tidspunkt
import no.nav.etterlatte.libs.common.dbutils.toLocalDatetimeUTC
import no.nav.etterlatte.libs.common.pdlhendelse.Adressebeskyttelse
import no.nav.etterlatte.libs.common.pdlhendelse.Endringstype
import no.nav.etterlatte.libs.common.person.AdressebeskyttelseGradering
import no.nav.etterlatte.libs.common.sak.Sak
import no.nav.etterlatte.libs.ktor.route.FoedselsnummerDTO
import no.nav.etterlatte.libs.testdata.grunnlag.AVDOED_FOEDSELSNUMMER
import no.nav.etterlatte.module
import no.nav.etterlatte.tilgangsstyring.TILGANG_ROUTE_PATH
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.util.UUID

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AdressebeskyttelseTest : BehandlingIntegrationTest() {
    private val pdltjenesterKlient = spyk<PdltjenesterKlientTest>()

    @BeforeAll
    fun start() {
        startServer(pdlTjenesterKlient = pdltjenesterKlient)
    }

    @AfterEach
    fun afterEach() {
        resetDatabase()
    }

    @AfterAll
    fun afterAllTests() {
        afterAll()
    }

    @Test
    fun `Skal kunne se på en vanlig behandling før adressebeskyttelse men ikke etter med mindre systembruker`() {
        val fnr = AVDOED_FOEDSELSNUMMER.value

        testApplication {
            val client =
                runServerWithModule(mockOAuth2Server) {
                    module(applicationContext)
                }

            val sak: Sak =
                client
                    .post("personer/saker/${SakType.BARNEPENSJON}") {
                        addAuthToken(tokenSaksbehandler)
                        contentType(ContentType.Application.Json)
                        setBody(FoedselsnummerDTO(fnr))
                        header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    }.let {
                        Assertions.assertEquals(HttpStatusCode.OK, it.status)
                        it.body()
                    }
            Assertions.assertNotNull(sak.id)

            val behandlingId =
                client
                    .post("/behandlinger/opprettbehandling") {
                        addAuthToken(tokenSaksbehandler)
                        header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                        setBody(
                            BehandlingsBehov(
                                sak.id,
                                Persongalleri(fnr, "innsender", emptyList(), emptyList(), emptyList()),
                                Tidspunkt.now().toLocalDatetimeUTC().toString(),
                            ),
                        )
                    }.let {
                        Assertions.assertEquals(HttpStatusCode.OK, it.status)
                        UUID.fromString(it.body())
                    }

            client
                .get("/behandlinger/$behandlingId") {
                    addAuthToken(this@AdressebeskyttelseTest.systemBruker)
                }.let {
                    Assertions.assertEquals(HttpStatusCode.OK, it.status)
                }

            client
                .get("/behandlinger/$behandlingId") {
                    addAuthToken(tokenSaksbehandler)
                }.let {
                    Assertions.assertEquals(HttpStatusCode.OK, it.status)
                }

            client.post("/grunnlagsendringshendelse/adressebeskyttelse") {
                addAuthToken(this@AdressebeskyttelseTest.systemBruker)
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                setBody(
                    Adressebeskyttelse(
                        hendelseId = "1",
                        endringstype = Endringstype.OPPRETTET,
                        fnr = fnr,
                        adressebeskyttelseGradering = AdressebeskyttelseGradering.STRENGT_FORTROLIG,
                    ),
                )
            }

            client
                .get("/behandlinger/$behandlingId") {
                    addAuthToken(tokenSaksbehandler)
                }.let {
                    Assertions.assertEquals(HttpStatusCode.NotFound, it.status)
                }
            // smoketest
            client
                .get("/fakeurlwithbehandlingsid/$behandlingId") {
                    addAuthToken(tokenSaksbehandler)
                }.let {
                    Assertions.assertEquals(HttpStatusCode.NotFound, it.status)
                }
            // systemBruker skal alltid ha tilgang
            client
                .get("/behandlinger/$behandlingId") {
                    addAuthToken(this@AdressebeskyttelseTest.systemBruker)
                }.let {
                    Assertions.assertEquals(HttpStatusCode.OK, it.status)
                }
        }
    }

    @Test
    fun `Skal kunne hente behandlinger på fnr før adressebeskyttelse`() {
        val fnr = AVDOED_FOEDSELSNUMMER.value

        testApplication {
            val httpClient =
                runServerWithModule(mockOAuth2Server) {
                    module(applicationContext)
                }

            val sak: Sak =
                httpClient
                    .post("personer/saker/${SakType.BARNEPENSJON}") {
                        addAuthToken(tokenSaksbehandler)
                        contentType(ContentType.Application.Json)
                        setBody(FoedselsnummerDTO(fnr))
                    }.let {
                        Assertions.assertEquals(HttpStatusCode.OK, it.status)
                        it.body()
                    }
            Assertions.assertNotNull(sak.id)

            val behandlingId =
                httpClient
                    .post("/behandlinger/opprettbehandling") {
                        addAuthToken(tokenSaksbehandler)
                        header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                        setBody(
                            BehandlingsBehov(
                                sak.id,
                                Persongalleri(fnr, "innsender", emptyList(), emptyList(), emptyList()),
                                Tidspunkt.now().toLocalDatetimeUTC().toString(),
                            ),
                        )
                    }.let {
                        Assertions.assertEquals(HttpStatusCode.OK, it.status)
                        UUID.fromString(it.body())
                    }

            httpClient
                .get("/behandlinger/$behandlingId") {
                    addAuthToken(tokenSaksbehandler)
                }.let {
                    Assertions.assertEquals(HttpStatusCode.OK, it.status)
                }

            httpClient.post("/grunnlagsendringshendelse/adressebeskyttelse") {
                addAuthToken(this@AdressebeskyttelseTest.systemBruker)
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                setBody(
                    Adressebeskyttelse(
                        hendelseId = "1",
                        fnr = fnr,
                        adressebeskyttelseGradering = AdressebeskyttelseGradering.STRENGT_FORTROLIG,
                        endringstype = Endringstype.OPPRETTET,
                    ),
                )
            }
        }
    }

    @Test
    fun `skal kunne aksessere tilgangsroutes også etter adressebeskyttelse`() {
        val fnr = AVDOED_FOEDSELSNUMMER.value

        testApplication {
            val client =
                runServerWithModule(mockOAuth2Server) {
                    module(applicationContext)
                }

            val sak: Sak =
                client
                    .post("personer/saker/${SakType.BARNEPENSJON}") {
                        addAuthToken(tokenSaksbehandler)
                        contentType(ContentType.Application.Json)
                        setBody(FoedselsnummerDTO(fnr))
                    }.let {
                        Assertions.assertEquals(HttpStatusCode.OK, it.status)
                        it.body()
                    }
            Assertions.assertNotNull(sak.id)

            val behandlingId =
                client
                    .post("/behandlinger/opprettbehandling") {
                        addAuthToken(tokenSaksbehandler)
                        header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                        setBody(
                            BehandlingsBehov(
                                sak.id,
                                Persongalleri(fnr, "innsender", emptyList(), emptyList(), emptyList()),
                                Tidspunkt.now().toLocalDatetimeUTC().toString(),
                            ),
                        )
                    }.let {
                        Assertions.assertEquals(HttpStatusCode.OK, it.status)
                        UUID.fromString(it.body())
                    }

            client
                .get("/behandlinger/$behandlingId") {
                    addAuthToken(this@AdressebeskyttelseTest.systemBruker)
                }.let {
                    Assertions.assertEquals(HttpStatusCode.OK, it.status)
                }

            val harTilgang: Boolean =
                client
                    .get("/$TILGANG_ROUTE_PATH/behandling/$behandlingId?$SKRIVETILGANG_CALL_QUERYPARAMETER=true") {
                        addAuthToken(tokenSaksbehandler)
                    }.let {
                        Assertions.assertEquals(HttpStatusCode.OK, it.status)
                        it.body()
                    }
            Assertions.assertTrue(harTilgang)

            client.post("/grunnlagsendringshendelse/adressebeskyttelse") {
                addAuthToken(this@AdressebeskyttelseTest.systemBruker)
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                setBody(
                    Adressebeskyttelse(
                        hendelseId = "1",
                        fnr = fnr,
                        adressebeskyttelseGradering = AdressebeskyttelseGradering.STRENGT_FORTROLIG,
                        endringstype = Endringstype.OPPRETTET,
                    ),
                )
            }

            val harIkkeTilgang: Boolean =
                client
                    .get("/$TILGANG_ROUTE_PATH/behandling/$behandlingId?$SKRIVETILGANG_CALL_QUERYPARAMETER=true") {
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
        val fnr = AVDOED_FOEDSELSNUMMER.value
        testApplication {
            val httpClient =
                runServerWithModule(mockOAuth2Server) {
                    module(applicationContext)
                }

            httpClient
                .post("/personer/saker/${SakType.BARNEPENSJON}") {
                    addAuthToken(tokenSaksbehandler)
                    contentType(ContentType.Application.Json)
                    setBody(FoedselsnummerDTO(fnr))
                }.apply {
                    Assertions.assertEquals(HttpStatusCode.OK, status)
                }.body<Sak>()

            httpClient.post("/grunnlagsendringshendelse/adressebeskyttelse") {
                addAuthToken(this@AdressebeskyttelseTest.systemBruker)
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                setBody(
                    Adressebeskyttelse(
                        hendelseId = "1",
                        fnr = fnr,
                        adressebeskyttelseGradering = AdressebeskyttelseGradering.STRENGT_FORTROLIG,
                        endringstype = Endringstype.OPPRETTET,
                    ),
                )
            }

            httpClient
                .post("/personer/saker/${SakType.BARNEPENSJON}") {
                    addAuthToken(tokenSaksbehandler)
                    contentType(ContentType.Application.Json)
                    setBody(FoedselsnummerDTO(fnr))
                }.apply {
                    Assertions.assertEquals(HttpStatusCode.NotFound, status)
                }
        }
    }
}
