package no.nav.etterlatte.brreg.enhetsregister

import io.ktor.client.HttpClient
import io.ktor.client.features.json.JacksonSerializer
import io.ktor.client.features.json.JsonFeature
import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.libs.common.objectMapper
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.fail
import org.junit.jupiter.api.Test

internal class EnhetsregServiceIT {

    private val httpClient = HttpClient {
        install(JsonFeature) { serializer = JacksonSerializer(objectMapper) }
    }.also {
        Runtime.getRuntime().addShutdownHook(Thread { it.close() })
    }

    private val klient = EnhetsregKlient("https://data.brreg.no", httpClient)

    private val service = EnhetsregService(klient)

    @Test
    fun hentEnhet() {
        val enhet = runBlocking {
            service.hentEnhet("922121893")
        }

        assertNotNull(enhet)
    }

    @Test
    fun hentEnhet_medUgyldigOrgnr() {
        try {
            runBlocking { service.hentEnhet("1234") }
            fail("Skal kaste feil ved feilmelding fra brreg")
        } catch (e: Exception) {
            assertNotNull(e)
        }
    }
}