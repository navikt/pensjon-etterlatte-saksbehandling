package regulering

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.ContentType
import io.ktor.http.Url
import io.ktor.http.headersOf
import io.ktor.serialization.jackson.JacksonConverter
import no.nav.etterlatte.VedtakServiceImpl
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.toJson
import no.nav.etterlatte.libs.common.vedtak.LoependeYtelseDTO
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.time.LocalDate

internal class VedtakServiceImplTest {
    @Test
    fun `kaller paa vedtakklient med riktig formatert url`() {
        lateinit var request: Url
        val mockEngine = MockEngine { req ->
            request = req.url
            val headers = headersOf("Content-Type" to listOf(ContentType.Application.Json.toString()))
            respond(
                LoependeYtelseDTO(true, LocalDate.of(2023, 5, 1)).toJson(),
                headers = headers
            )
        }

        val httpClientMock = HttpClient(mockEngine) {
            install(ContentNegotiation) {
                register(
                    ContentType.Application.Json,
                    JacksonConverter(objectMapper)
                )
            }
        }
        val vedtakService = VedtakServiceImpl(httpClientMock, "http://test")
        val dato = LocalDate.of(2023, 1, 1)
        vedtakService.harLoependeYtelserFra(1, dato)

        Assertions.assertEquals("/api/vedtak/loepende/1?dato=2023-01-01", request.encodedPathAndQuery)
    }
}