package no.nav.etterlatte.enhetsregister

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.features.json.JacksonSerializer
import io.ktor.client.features.json.JsonFeature
import io.ktor.client.request.HttpResponseData
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.fullPath
import io.ktor.http.headersOf
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

internal class EnhetsregKlientTest {

    private val mockEnhet = Enhet("123456789", "Test AS", Organisasjonsform("AS", "Aksjeselskap"))
    private val enhetJson = jacksonObjectMapper().writeValueAsString(mockEnhet)

    private val mockEnhetWrapper = ResponseWrapper(ResponseWrapper.Embedded(listOf(mockEnhet)))
    private val wrapperJson = jacksonObjectMapper().writeValueAsString(mockEnhetWrapper)

    private val defaultHeaders = headersOf("Content-Type" to listOf(ContentType.Application.Json.toString()))

    @Test
    fun hentEnhet() {
        val klient = mockHttpClient("/enhetsregisteret/api/enheter/123456789") {
            respond(
                enhetJson,
                HttpStatusCode.OK,
                defaultHeaders
            )
        }

        val enhet = runBlocking {
            klient.hentEnhet("123456789")
        }

        assertEquals(mockEnhet, enhet)
    }

    @Test
    fun hentEnhet_finnesIkke() {
        val orgnr = "123456789"

        val klient = mockHttpClient("/enhetsregisteret/api/enheter/$orgnr") {
            respond("", HttpStatusCode.NotFound )
        }

        val enhet = runBlocking {
            klient.hentEnhet(orgnr)
        }

        assertNull(enhet)
    }

    @Test
    fun hentEnheter() {
        val navn = "testfirma"

        val klient = mockHttpClient("/enhetsregisteret/api/enheter?navn=$navn&konkurs=false&underAvvikling=false") {
            respond(
                wrapperJson,
                HttpStatusCode.OK,
                defaultHeaders
            )
        }

        val enheter = runBlocking {
            klient.hentEnheter(navn)
        }

        assertEquals(1, enheter.size)
    }

    private fun mockHttpClient(path: String, respond: MockRequestHandleScope.() -> HttpResponseData): EnhetsregKlient {
        val httpClient = HttpClient(MockEngine) {
            engine {
                addHandler { request ->
                    when (request.url.fullPath) {
                        path -> respond()
                        else -> error("Unhandled ${request.url.fullPath}")
                    }
                }
            }
            install(JsonFeature) { serializer = JacksonSerializer() }
        }

        return EnhetsregKlient("", httpClient)
    }

}
