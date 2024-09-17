package no.nav.etterlatte.egenansatt

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
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.spyk
import no.nav.etterlatte.BehandlingIntegrationTest
import no.nav.etterlatte.PdltjenesterKlientTest
import no.nav.etterlatte.behandling.domain.ArbeidsFordelingEnhet
import no.nav.etterlatte.behandling.klienter.Norg2Klient
import no.nav.etterlatte.common.Enhet
import no.nav.etterlatte.ktor.runServerWithModule
import no.nav.etterlatte.libs.common.behandling.BehandlingsBehov
import no.nav.etterlatte.libs.common.behandling.Persongalleri
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.pdlhendelse.Adressebeskyttelse
import no.nav.etterlatte.libs.common.pdlhendelse.Endringstype
import no.nav.etterlatte.libs.common.person.AdressebeskyttelseGradering
import no.nav.etterlatte.libs.common.person.GeografiskTilknytning
import no.nav.etterlatte.libs.common.person.HentAdressebeskyttelseRequest
import no.nav.etterlatte.libs.common.person.PersonIdent
import no.nav.etterlatte.libs.common.sak.Sak
import no.nav.etterlatte.libs.common.skjermet.EgenAnsattSkjermet
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
class EgenAnsattRouteTest : BehandlingIntegrationTest() {
    private val norg2Klient = mockk<Norg2Klient>()
    private val pdltjenesterKlient = spyk<PdltjenesterKlientTest>()

    @BeforeEach
    fun start() =
        startServer(norg2Klient = norg2Klient, pdlTjenesterKlient = pdltjenesterKlient)
            .also { resetDatabase() }

    @AfterEach
    fun afterEach() {
        afterAll()
    }

    @Test
    fun `Skal kunne sette på skjerming og få satt ny enhet`() {
        val fnr = AVDOED_FOEDSELSNUMMER.value

        testApplication {
            val client =
                runServerWithModule(mockOAuth2Server) {
                    module(applicationContext)
                }
            coEvery {
                norg2Klient.hentArbeidsfordelingForOmraadeOgTema(any())
            } returns listOf(ArbeidsFordelingEnhet(Enhet.PORSGRUNN.navn, Enhet.PORSGRUNN))

            coEvery {
                pdltjenesterKlient.hentAdressebeskyttelseForPerson(HentAdressebeskyttelseRequest(PersonIdent(fnr), SakType.BARNEPENSJON))
            } returns AdressebeskyttelseGradering.UGRADERT
            coEvery {
                pdltjenesterKlient.hentGeografiskTilknytning(
                    fnr,
                    SakType.BARNEPENSJON,
                )
            } returns GeografiskTilknytning(kommune = "0301")

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
            Assertions.assertEquals(Enhet.PORSGRUNN, sak.enhet)

            client
                .post("egenansatt") {
                    addAuthToken(this@EgenAnsattRouteTest.systemBruker)
                    contentType(ContentType.Application.Json)
                    setBody(
                        EgenAnsattSkjermet(
                            fnr = fnr,
                            inntruffet = Tidspunkt.now(),
                            skjermet = true,
                        ),
                    )
                }.let {
                    Assertions.assertEquals(HttpStatusCode.OK, it.status)
                }

            val saketterSkjerming: Sak =
                client
                    .post("personer/saker/${SakType.BARNEPENSJON}") {
                        addAuthToken(this@EgenAnsattRouteTest.systemBruker)
                        contentType(ContentType.Application.Json)
                        setBody(FoedselsnummerDTO(fnr))
                        header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    }.let {
                        Assertions.assertEquals(HttpStatusCode.OK, it.status)
                        it.body()
                    }

            Assertions.assertEquals(sak.id, saketterSkjerming.id)
            // Denne skal alltid settes hvis noen blir skjermet hvis de ikke er adressebeskyttet(se test under)
            Assertions.assertEquals(Enhet.EGNE_ANSATTE, saketterSkjerming.enhet)

            val steinkjer = ArbeidsFordelingEnhet(Enhet.STEINKJER.navn, Enhet.STEINKJER)
            coEvery { norg2Klient.hentArbeidsfordelingForOmraadeOgTema(any()) } returns listOf(steinkjer)
            client
                .post("egenansatt") {
                    addAuthToken(this@EgenAnsattRouteTest.systemBruker)
                    contentType(ContentType.Application.Json)
                    setBody(
                        EgenAnsattSkjermet(
                            fnr = fnr,
                            inntruffet = Tidspunkt.now(),
                            skjermet = false,
                        ),
                    )
                }.let {
                    Assertions.assertEquals(HttpStatusCode.OK, it.status)
                }

            val sakUtenSkjermingIgjen: Sak =
                client
                    .post("personer/saker/${SakType.BARNEPENSJON}") {
                        addAuthToken(this@EgenAnsattRouteTest.systemBruker)
                        contentType(ContentType.Application.Json)
                        setBody(FoedselsnummerDTO(fnr))
                        header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    }.let {
                        Assertions.assertEquals(HttpStatusCode.OK, it.status)
                        it.body()
                    }

            Assertions.assertEquals(sak.id, sakUtenSkjermingIgjen.id)
            Assertions.assertEquals(steinkjer.enhetNr, sakUtenSkjermingIgjen.enhet)
        }
    }

    @Test
    fun `Skal ikke sette skjerming hvis adressebeskyttet, og da beholde 2103 som enhet`() {
        val fnr = AVDOED_FOEDSELSNUMMER.value

        testApplication {
            val client =
                runServerWithModule(mockOAuth2Server) {
                    module(applicationContext)
                }

            coEvery {
                norg2Klient.hentArbeidsfordelingForOmraadeOgTema(any())
            } returns listOf(ArbeidsFordelingEnhet(Enhet.PORSGRUNN.navn, Enhet.PORSGRUNN))

            coEvery {
                pdltjenesterKlient.hentAdressebeskyttelseForPerson(HentAdressebeskyttelseRequest(PersonIdent(fnr), SakType.BARNEPENSJON))
            } returns AdressebeskyttelseGradering.UGRADERT
            coEvery {
                pdltjenesterKlient.hentGeografiskTilknytning(
                    fnr,
                    SakType.BARNEPENSJON,
                )
            } returns GeografiskTilknytning(kommune = "0301")

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
            Assertions.assertEquals(Enhet.PORSGRUNN, sak.enhet)

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

            client.post("/grunnlagsendringshendelse/adressebeskyttelse") {
                addAuthToken(this@EgenAnsattRouteTest.systemBruker)
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

            client
                .get("/behandlinger/$behandlingId") {
                    addAuthToken(this@EgenAnsattRouteTest.systemBruker)
                }.let {
                    Assertions.assertEquals(HttpStatusCode.OK, it.status)
                }

            client
                .post("egenansatt") {
                    addAuthToken(this@EgenAnsattRouteTest.systemBruker)
                    contentType(ContentType.Application.Json)
                    setBody(
                        EgenAnsattSkjermet(
                            fnr = fnr,
                            inntruffet = Tidspunkt.now(),
                            skjermet = true,
                        ),
                    )
                }.let {
                    Assertions.assertEquals(HttpStatusCode.OK, it.status)
                }

            val adressebeskyttetUtenSkjerming: Sak =
                client
                    .post("personer/saker/${SakType.BARNEPENSJON}") {
                        addAuthToken(this@EgenAnsattRouteTest.systemBruker)
                        contentType(ContentType.Application.Json)
                        setBody(FoedselsnummerDTO(fnr))
                        header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    }.let {
                        Assertions.assertEquals(HttpStatusCode.OK, it.status)
                        it.body()
                    }

            Assertions.assertNotNull(adressebeskyttetUtenSkjerming.id)
            Assertions.assertEquals(Enhet.STRENGT_FORTROLIG, adressebeskyttetUtenSkjerming.enhet)
        }
    }
}
