package no.nav.etterlatte.brev.norg2

import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.jackson.jackson
import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.brev.adresse.Norg2Klient
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow

@Disabled
internal class Norg2KlientIT {
    private val apiUrl = "https://norg2.dev-fss-pub.nais.io/norg2/api/v1"

    private val httpClient = HttpClient(OkHttp) {
        install(ContentNegotiation) {
            jackson()
        }
    }

    private val klient = Norg2Klient(apiUrl, httpClient)

    @Test
    fun `test diverse`() {
        assertDoesNotThrow {
            val kontaktinfo = runBlocking {
                klient.hentEnhet("4817")
            }

            assertNotNull(kontaktinfo)
        }
    }
}