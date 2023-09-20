package no.nav.etterlatte.pdl

import com.fasterxml.jackson.databind.JsonNode
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.ContentType
import io.ktor.http.fullPath
import io.ktor.http.headersOf
import io.ktor.serialization.jackson.JacksonConverter
import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.toJson
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test

internal class ParallelleSannheterKlientTest {
    private val parallelleSannheterKlient =
        ParallelleSannheterKlient(
            httpClient = setupHttpClient(),
            apiUrl = "url",
        )

    private fun setupHttpClient() =
        HttpClient(MockEngine) {
            expectSuccess = true
            engine {
                addHandler { request ->
                    val headers = headersOf("Content-Type" to listOf(ContentType.Application.Json.toString()))

                    when (request.url.fullPath) {
                        "/url/api/navn" -> {
                            respond(mockResponse("navn", mockNavn(FREG)), headers = headers)
                        }
                        else -> error(request.url.fullPath)
                    }
                }
            }
            install(ContentNegotiation) { register(ContentType.Application.Json, JacksonConverter(objectMapper)) }
        }

    @Test
    fun `skal returnere kun ett navn fra parallelle sannheter`() {
        val navn = listOf(mockNavn(FREG), mockNavn(PDL))

        val avklartNavn =
            runBlocking {
                parallelleSannheterKlient.avklarNavn(navn)
            }

        assertNotNull(avklartNavn)
        assertEquals(FREG, avklartNavn.metadata.master)
    }

    private fun <T> mockResponse(
        feltnavn: String,
        verdi: T,
    ): String {
        return objectMapper.createObjectNode()
            .set<JsonNode?>(feltnavn, objectMapper.readValue(listOf(verdi).toJson(), JsonNode::class.java))
            .toJson()
    }

    private fun mockNavn(master: String): PdlNavn {
        return PdlNavn(
            fornavn = "Ola",
            etternavn = "Nordmann",
            metadata =
                PdlMetadata(
                    endringer = emptyList(),
                    historisk = false,
                    master = master,
                    opplysningsId = "1",
                ),
        )
    }

    companion object {
        const val PDL = "PDL"
        const val FREG = "FREG"
    }
}
