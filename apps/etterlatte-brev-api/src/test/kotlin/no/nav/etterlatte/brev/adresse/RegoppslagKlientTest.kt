package no.nav.etterlatte.brev.adresse

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.HttpResponseData
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.jackson3.jackson
import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.feilhaandtering.ForespoerselException
import no.nav.etterlatte.libs.testdata.grunnlag.SOEKER_FOEDSELSNUMMER
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

internal class RegoppslagKlientTest {
    private val ident = SOEKER_FOEDSELSNUMMER.value

    @Test
    fun `409 med reason code falsk_identitet gir null`() {
        val klient =
            klient {
                respond(
                    "",
                    HttpStatusCode.Conflict,
                    headersOf("Nav-Reason-Code", "falsk_identitet"),
                )
            }

        runBlocking { klient.hentMottakerAdresse(SakType.BARNEPENSJON, ident) } shouldBe null
    }

    @Test
    fun `409 med reason code falsk_identitet caches ikke`() {
        var antallKall = 0
        val klient =
            klient {
                antallKall++
                respond("", HttpStatusCode.Conflict, headersOf("Nav-Reason-Code", "falsk_identitet"))
            }

        runBlocking {
            klient.hentMottakerAdresse(SakType.BARNEPENSJON, ident)
            klient.hentMottakerAdresse(SakType.BARNEPENSJON, ident)
        }

        antallKall shouldBe 2
    }

    @Test
    fun `409 uten reason code falsk_identitet kaster feil`() {
        val klient = klient { respond("", HttpStatusCode.Conflict) }

        val feil =
            assertThrows<ForespoerselException> {
                runBlocking { klient.hentMottakerAdresse(SakType.BARNEPENSJON, ident) }
            }

        feil.code shouldBe "UKJENT_FEIL_REGOPPSLAG"
    }

    @Test
    fun `404 gir null`() {
        val klient = klient { respond("", HttpStatusCode.NotFound) }

        runBlocking { klient.hentMottakerAdresse(SakType.OMSTILLINGSSTOENAD, ident) } shouldBe null
    }

    @Test
    fun `200 gir adresse`() {
        val klient =
            klient {
                respond(
                    """
                    {
                      "navn": "Test Testesen",
                      "adresse": {
                        "type": "NORSKPOSTADRESSE",
                        "adresseKilde": "BOSTEDSADRESSE",
                        "adresselinje1": "Testveien 1",
                        "postnummer": "0150",
                        "poststed": "OSLO",
                        "landkode": "NO",
                        "land": "NORGE"
                      }
                    }
                    """.trimIndent(),
                    HttpStatusCode.OK,
                    headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
                )
            }

        val respons = runBlocking { klient.hentMottakerAdresse(SakType.BARNEPENSJON, ident) }

        respons shouldNotBe null
        respons!!.navn shouldBe "Test Testesen"
    }

    private fun klient(handler: suspend MockRequestHandleScope.() -> HttpResponseData): RegoppslagKlient {
        val httpClient =
            HttpClient(MockEngine) {
                engine { addHandler { handler() } }
                expectSuccess = true
                install(ContentNegotiation) { jackson { } }
            }
        return RegoppslagKlient(httpClient, "http://regoppslag")
    }
}
