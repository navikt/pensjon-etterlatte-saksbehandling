package no.nav.etterlatte

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.ContentType
import io.ktor.http.Url
import io.ktor.http.headersOf
import io.ktor.serialization.jackson.JacksonConverter
import no.nav.etterlatte.behandling.sakId1
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.toJson
import no.nav.etterlatte.libs.common.vedtak.LoependeYtelseDTO
import no.nav.etterlatte.vedtaksvurdering.VedtakServiceImpl
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.time.LocalDate

internal class VedtakServiceImplTest {
    @Test
    fun `henter loepende vedtak med riktig formatert url`() {
        lateinit var request: Url
        val mockEngine =
            MockEngine { req ->
                request = req.url
                val headers = headersOf("Content-Type" to listOf(ContentType.Application.Json.toString()))
                respond(
                    LoependeYtelseDTO(true, false, LocalDate.of(2023, 5, 1)).toJson(),
                    headers = headers,
                )
            }

        val httpClientMock =
            HttpClient(mockEngine) {
                install(ContentNegotiation) {
                    register(
                        ContentType.Application.Json,
                        JacksonConverter(objectMapper),
                    )
                }
            }
        val vedtakService = VedtakServiceImpl(httpClientMock, "http://test")
        val dato = LocalDate.of(2023, 1, 1)
        vedtakService.harLoependeYtelserFra(sakId1, dato, sjekkNullBeloep = false)

        Assertions.assertEquals("/api/vedtak/loepende/1?dato=2023-01-01&sjekkNullBeloep=false", request.encodedPathAndQuery)
    }
}
