package no.nav.etterlatte.utbetaling.simulering

import com.typesafe.config.ConfigFactory
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.ContentType
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.fullPath
import io.ktor.http.headersOf
import io.ktor.serialization.jackson3.JacksonConverter
import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.system.os.tjenester.simulerfpservice.simulerfpserviceservicetypes.SimulerBeregningRequest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class SimuleringOsKlientObjectMapperTest {
    @Test
    fun `skal trimme whitespace i respons felter ved deserialisering`() {
        val klient = simuleringKlientMedRespons(simuleringResponsMedWhitespace())

        val response = runBlocking { klient.simuler(SimulerBeregningRequest()) }

        assertEquals("12345678901", response.simulering.gjelderId)
        assertEquals("2024-05-02", response.simulering.datoBeregnet)
        assertEquals("Simulering OK", response.infomelding.beskrMelding)
    }

    @Test
    fun `skal returnere tom respons hvis wrapper response mangler`() {
        val klient = simuleringKlientMedRespons("""{"response":null}""")

        val response = runBlocking { klient.simuler(SimulerBeregningRequest()) }

        assertNull(response.simulering)
        assertNull(response.infomelding)
    }

    private fun simuleringKlientMedRespons(responseJson: String) =
        SimuleringOsKlient(
            config = ConfigFactory.parseMap(mapOf("etterlatteproxy.url" to "http://test")),
            client = mockedHttpClient(responseJson),
            objectMapper = simuleringObjectMapper(),
        )

    private fun mockedHttpClient(responseJson: String) =
        HttpClient(MockEngine) {
            install(ContentNegotiation) {
                register(ContentType.Application.Json, JacksonConverter(objectMapper))
            }
            engine {
                addHandler { request ->
                    if (request.method == HttpMethod.Post && request.url.fullPath == "/simuleringoppdrag/simulerberegning") {
                        respond(
                            content = responseJson,
                            status = HttpStatusCode.OK,
                            headers = headersOf("Content-Type", ContentType.Application.Json.toString()),
                        )
                    } else {
                        error("Unhandled ${request.method.value} ${request.url.fullPath}")
                    }
                }
            }
        }

    private fun simuleringResponsMedWhitespace() =
        """{
            "response": {
              "simulering": {
                "gjelderId": " 12345678901 ",
                "datoBeregnet": " 2024-05-02 ",
                "ukjentFelt": " ignoreres "
              },
              "infomelding": {
                "beskrMelding": " Simulering OK ",
                "ukjentFelt": " ignoreres "
              },
              "ukjentWrapperFelt": " ignoreres "
            }
        }"""
}
