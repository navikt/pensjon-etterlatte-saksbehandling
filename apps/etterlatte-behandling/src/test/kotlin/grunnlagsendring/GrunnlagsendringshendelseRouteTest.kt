package no.nav.etterlatte.grunnlagsendring

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
import no.nav.etterlatte.BehandlingIntegrationTest
import no.nav.etterlatte.ktor.runServerWithModule
import no.nav.etterlatte.libs.common.behandling.BehandlingsBehov
import no.nav.etterlatte.libs.common.behandling.Persongalleri
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.pdlhendelse.Adressebeskyttelse
import no.nav.etterlatte.libs.common.pdlhendelse.Endringstype
import no.nav.etterlatte.libs.common.person.AdressebeskyttelseGradering
import no.nav.etterlatte.libs.common.sak.Sak
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.tidspunkt.toLocalDatetimeUTC
import no.nav.etterlatte.libs.ktor.route.FoedselsnummerDTO
import no.nav.etterlatte.libs.testdata.grunnlag.AVDOED_FOEDSELSNUMMER
import no.nav.etterlatte.module
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.util.UUID

@TestInstance(TestInstance.Lifecycle.PER_METHOD)
class GrunnlagsendringshendelseRouteTest : BehandlingIntegrationTest() {
    @BeforeEach
    fun start() = startServer()

    @AfterEach
    fun afterEach() {
        afterAll()
    }

    @Test
    fun `Skal kunne håndtere en adressebeskyttelsesendring`() {
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
                    addAuthToken(this@GrunnlagsendringshendelseRouteTest.systemBruker)
                }.let {
                    Assertions.assertEquals(HttpStatusCode.OK, it.status)
                }

            client
                .post("/grunnlagsendringshendelse/adressebeskyttelse") {
                    addAuthToken(this@GrunnlagsendringshendelseRouteTest.systemBruker)
                    header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    setBody(
                        Adressebeskyttelse(
                            hendelseId = "1",
                            fnr = fnr,
                            adressebeskyttelseGradering = AdressebeskyttelseGradering.STRENGT_FORTROLIG,
                            endringstype = Endringstype.OPPRETTET,
                        ),
                    )
                }.also {
                    Assertions.assertEquals(HttpStatusCode.OK, it.status)
                }

            client
                .post("/grunnlagsendringshendelse/adressebeskyttelse") {
                    addAuthToken(this@GrunnlagsendringshendelseRouteTest.systemBruker)
                    header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    setBody(
                        Adressebeskyttelse(
                            hendelseId = "1",
                            fnr = fnr,
                            adressebeskyttelseGradering = AdressebeskyttelseGradering.FORTROLIG,
                            endringstype = Endringstype.OPPRETTET,
                        ),
                    )
                }.also {
                    Assertions.assertEquals(HttpStatusCode.OK, it.status)
                }

            client
                .post("/grunnlagsendringshendelse/adressebeskyttelse") {
                    addAuthToken(this@GrunnlagsendringshendelseRouteTest.systemBruker)
                    header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    setBody(
                        Adressebeskyttelse(
                            hendelseId = "1",
                            fnr = fnr,
                            adressebeskyttelseGradering = AdressebeskyttelseGradering.UGRADERT,
                            endringstype = Endringstype.OPPRETTET,
                        ),
                    )
                }.also {
                    Assertions.assertEquals(HttpStatusCode.OK, it.status)
                }
        }
    }
}
