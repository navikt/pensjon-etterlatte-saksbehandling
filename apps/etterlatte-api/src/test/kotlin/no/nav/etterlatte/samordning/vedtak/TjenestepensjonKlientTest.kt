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
    fun `har ikke TP-ytelse`() {
        val httpClient =
            createHttpClient { request ->
                request.headers["fnr"] shouldBe "01018012345"

                when (request.url.fullPath) {
                    "/api/tjenestepensjon/tpNrWithYtelse?fomDate=$datoHarIkkeTpYtelse" -> {
                        respond("{\"tpNr\": []}", headers = headers)
                    }

                    else -> error(request.url.fullPath)
                }
            }

        val tpKlient = TjenestepensjonKlient(config, httpClient)

        runBlocking {
            tpKlient.harTpYtelseOnDate("01018012345", Tjenestepensjonnummer("3010"), datoHarIkkeTpYtelse)
        } shouldBe false
    }

    @Test
    fun `har TP-ytelse`() {
        val httpClient =
            createHttpClient { request ->
                request.headers["fnr"] shouldBe "01018012345"

                when (request.url.fullPath) {
                    "/api/tjenestepensjon/tpNrWithYtelse?fomDate=$datoHarTpYtelse" -> {
                        respond("{\"tpNr\": [ \"3010\", \"4100\" ]}", headers = headers)
                    }

                    else -> error(request.url.fullPath)
                }
            }

        val tpKlient = TjenestepensjonKlient(config, httpClient)

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
