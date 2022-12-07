package no.nav.etterlatte.grunnbeloep

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.HttpResponseData
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.fullPath
import io.ktor.http.headersOf
import io.ktor.serialization.jackson.jackson
import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.brev.grunnbeloep.GrunnbeloepKlient
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test

internal class GrunnbeloepKlientTest {

    private val defaultHeaders = headersOf("Content-Type" to listOf(ContentType.Application.Json.toString()))

    @Test
    fun `Uthenting av grunnbeløp fungerer`() {
        val klient = mockHttpClient {
            respond(grunnbeloepJson, HttpStatusCode.OK, defaultHeaders)
        }

        val grunnbeloep = runBlocking {
            klient.hentGrunnbeloep()
        }

        assertNotNull(grunnbeloep)
        assertEquals("2022-05-01", grunnbeloep.dato)
        assertEquals(111477, grunnbeloep.grunnbeloep)
        assertEquals(9290, grunnbeloep.grunnbeloepPerMaaned)
        assertEquals(109784, grunnbeloep.gjennomsnittPerAar)
        assertEquals(1.047726, grunnbeloep.omregningsfaktor)
        assertEquals("2022-05-23", grunnbeloep.virkningstidspunktForMinsteinntekt)
    }

    private fun mockHttpClient(respond: MockRequestHandleScope.() -> HttpResponseData): GrunnbeloepKlient {
        val httpClient = HttpClient(MockEngine) {
            engine {
                addHandler { request ->
                    when (request.url.fullPath) {
                        "/api/v1/grunnbeloep" -> respond()
                        else -> error("Unhandled ${request.url.fullPath}")
                    }
                }
            }
            expectSuccess = true
            install(ContentNegotiation) { jackson { } }
        }

        return GrunnbeloepKlient(httpClient)
    }
}

private val grunnbeloepJson = """
    {
        "dato": "2022-05-01",
        "grunnbeloep": 111477,
        "grunnbeloepPerMaaned": 9290,
        "gjennomsnittPerAar": 109784,
        "omregningsfaktor": 1.047726,
        "virkningstidspunktForMinsteinntekt": "2022-05-23"
    }
""".trimIndent()