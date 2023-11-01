package no.nav.etterlatte.samordning.vedtak

import com.typesafe.config.Config
import io.kotest.matchers.shouldBe
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandler
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.ContentType
import io.ktor.http.fullPath
import io.ktor.http.headersOf
import io.ktor.serialization.jackson.JacksonConverter
import io.mockk.clearAllMocks
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.toJson
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Month
import java.time.YearMonth

class TjenestepensjonKlientTest {
    private val config: Config = mockk(relaxed = true)
    private val headers = headersOf("Content-Type" to listOf(ContentType.Application.Json.toString()))
    private val datoHarTpYtelse = YearMonth.of(2024, Month.MARCH).atStartOfMonth()
    private val datoHarIkkeTpYtelse = YearMonth.of(2024, Month.FEBRUARY).atStartOfMonth()

    @BeforeEach
    fun setupDefaults() {
        clearAllMocks()
    }

    @Test
    fun `har TP-forhold`() {
        val httpClient =
            createHttpClient { request ->
                request.headers["fnr"] shouldBe "17057554321"
                request.headers["tpnr"] shouldBe "3010"

                when (request.url.fullPath) {
                    "/api/tjenestepensjon/finnForholdForBruker?datoFom=2024-03-01" -> {
                        respond(
                            SamhandlerPersonDto(
                                fnr = "17057554321",
                                forhold =
                                    listOf(
                                        TjenestepensjonForhold(
                                            tpNr = "3010",
                                            kilde = "PP01",
                                        ),
                                    ),
                            ).toJson(),
                            headers = headers,
                        )
                    }
                    else -> error(request.url.fullPath)
                }
            }

        val tpKlient = TjenestepensjonKlient(config, httpClient)

        runBlocking {
            tpKlient.harTpForholdByDate("17057554321", tpnr = Tjenestepensjonnummer("3010"), datoHarTpYtelse)
        } shouldBe true
    }

    @Test
    fun `har ikke TP-forhold`() {
        val httpClient =
            createHttpClient { request ->
                request.headers["fnr"] shouldBe "17057554321"
                request.headers["tpnr"] shouldBe "4100"

                when (request.url.fullPath) {
                    "/api/tjenestepensjon/finnForholdForBruker?datoFom=2024-02-01" -> {
                        respond(
                            SamhandlerPersonDto(
                                fnr = "17057554321",
                                forhold = emptyList(),
                            ).toJson(),
                            headers = headers,
                        )
                    }

                    else -> error(request.url.fullPath)
                }
            }

        val tpKlient = TjenestepensjonKlient(config, httpClient)

        runBlocking {
            tpKlient.harTpForholdByDate("17057554321", Tjenestepensjonnummer("4100"), datoHarIkkeTpYtelse)
        } shouldBe false
    }

    @Test
    fun `har ikke TP-ytelse - verifisere api path, param og headers`() {
        val httpClient =
            createHttpClient { request ->
                request.headers["fnr"] shouldBe "01018012345"

                when (request.url.fullPath) {
                    "/api/tjenestepensjon/tpNrWithYtelse?fomDate=2024-03-01" -> {
                        respond("{\"tpNr\": [ \"3010\", \"4100\" ]}", headers = headers)
                    }

                    "/api/tjenestepensjon/tpNrWithYtelse?fomDate=2024-02-01" -> {
                        respond("{\"tpNr\": []}", headers = headers)
                    }

                    else -> error(request.url.fullPath)
                }
            }

        val tpKlient = TjenestepensjonKlient(config, httpClient)

        runBlocking {
            tpKlient.harTpYtelseOnDate("01018012345", Tjenestepensjonnummer("3010"), datoHarIkkeTpYtelse)
        } shouldBe false

        runBlocking {
            tpKlient.harTpYtelseOnDate("01018012345", Tjenestepensjonnummer("3010"), datoHarTpYtelse)
        } shouldBe true
    }

    private fun createHttpClient(handler: MockRequestHandler): HttpClient {
        val httpClient =
            HttpClient(MockEngine) {
                expectSuccess = true
                engine {
                    addHandler(handler)
                }
                install(ContentNegotiation) { register(ContentType.Application.Json, JacksonConverter(objectMapper)) }
            }
        return httpClient
    }
}
