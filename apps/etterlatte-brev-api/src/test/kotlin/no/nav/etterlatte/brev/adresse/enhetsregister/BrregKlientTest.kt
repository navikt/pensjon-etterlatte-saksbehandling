package no.nav.etterlatte.brev.adresse.enhetsregister

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.ResponseException
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.HttpResponseData
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.fullPath
import io.ktor.http.headersOf
import io.ktor.serialization.jackson.jackson
import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.libs.common.toJson
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assertions.fail
import org.junit.jupiter.api.Test

internal class BrregKlientTest {
    @Test
    fun `Henting av enheter fra brreg`() {
        val httpClient = createClient {
            respond(
                enheterResponse.toJson(),
                HttpStatusCode.OK,
                defaultHeaders
            )
        }

        val brregKlient = BrregKlient(httpClient, "")

        val resultat = runBlocking { brregKlient.hentEnheter() }

        assertEquals(4, resultat.size)
        assertEquals(enheterResponse._embedded!!.enheter, resultat)
    }

    @Test
    fun `Feil i respons fra brreg`() {
        val httpClient = createClient {
            respond(Feilmelding(400, "feilmelding", emptyList()).toJson(), HttpStatusCode.BadRequest, defaultHeaders)
        }

        val brregKlient = BrregKlient(httpClient, "")

        try {
            runBlocking { brregKlient.hentEnheter() }
            fail()
        } catch (e: Exception) {
            assertTrue(e is ResponseException)
        }
    }

    private fun createClient(respond: MockRequestHandleScope.() -> HttpResponseData) = HttpClient(MockEngine) {
        engine {
            addHandler { request ->
                if ("/enhetsregisteret/api/enheter" in request.url.fullPath) {
                    respond()
                } else {
                    error("Unhandled ${request.url.fullPath}")
                }
            }
        }
//        expectSuccess = true
        install(ContentNegotiation) { jackson() }
    }

    private val enheterResponse = ResponseWrapper(
        ResponseWrapper.Embedded(
            listOf(
                Enhet("921627009", "STATSFORVALTERENS FELLESTJENESTER"),
                Enhet("974762994", "STATSFORVALTEREN I AGDER"),
                Enhet("974761645", "STATSFORVALTEREN I INNLANDET"),
                Enhet("974764687", "STATSFORVALTEREN I NORDLAND")
            )
        )
    )

    private val defaultHeaders = headersOf("Content-Type" to listOf(ContentType.Application.Json.toString()))
}