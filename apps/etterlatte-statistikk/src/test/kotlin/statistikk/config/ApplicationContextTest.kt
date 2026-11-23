package no.nav.etterlatte.statistikk.config

import io.kotest.matchers.shouldBe
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpStatusCode
import io.mockk.every
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.EnvKey.BEHANDLING_AZURE_SCOPE
import no.nav.etterlatte.EnvKey.BEHANDLING_URL
import no.nav.etterlatte.EnvKey.BEREGNING_AZURE_SCOPE
import no.nav.etterlatte.EnvKey.BEREGNING_URL
import no.nav.etterlatte.EnvKey.BRUK_INTERN_BEREGNING
import no.nav.etterlatte.libs.common.EnvEnum
import no.nav.etterlatte.libs.common.Miljoevariabler
import no.nav.etterlatte.libs.ktor.AzureEnums.AZURE_APP_CLIENT_ID
import no.nav.etterlatte.libs.ktor.AzureEnums.AZURE_APP_JWK
import no.nav.etterlatte.libs.ktor.AzureEnums.AZURE_APP_WELL_KNOWN_URL
import no.nav.etterlatte.libs.ktor.httpClientClientCredentials
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.NullSource
import org.junit.jupiter.params.provider.ValueSource
import java.util.UUID

internal class ApplicationContextTest {
    @AfterEach
    fun ryddOpp() {
        unmockkStatic(::httpClientClientCredentials)
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(booleans = [false, true])
    fun `beregning og avkorting bruker valgt backend og scope uten fallback`(aktivert: Boolean?) {
        val brukBehandling = aktivert == true
        val app = if (brukBehandling) "behandling" else "beregning"
        val scope = "api://$app/.default"
        val engine = MockEngine { respond("", HttpStatusCode.NotFound) }
        HttpClient(engine) { expectSuccess = true }.use { httpClient ->
            mockkStatic(::httpClientClientCredentials)
            every { httpClientClientCredentials("client", "jwk", "well-known", scope, true) } returns httpClient
            val env =
                Miljoevariabler.httpClient(
                    buildMap<EnvEnum, String> {
                        put(AZURE_APP_CLIENT_ID, "client")
                        put(AZURE_APP_JWK, "jwk")
                        put(AZURE_APP_WELL_KNOWN_URL, "well-known")
                        put(if (brukBehandling) BEHANDLING_AZURE_SCOPE else BEREGNING_AZURE_SCOPE, scope)
                        put(if (brukBehandling) BEHANDLING_URL else BEREGNING_URL, "http://$app")
                        aktivert?.let { put(BRUK_INTERN_BEREGNING, it.toString()) }
                    },
                )
            val klient = ApplicationContext(env).beregningKlient
            val behandlingId = UUID.randomUUID()

            runBlocking {
                klient.hentBeregningForBehandling(behandlingId) shouldBe null
                klient.hentAvkortingForBehandling(behandlingId) shouldBe null
            }

            engine.requestHistory.map { it.url.toString() } shouldBe
                listOf(
                    "http://$app/api/beregning/$behandlingId",
                    "http://$app/api/beregning/avkorting/$behandlingId/ferdig",
                )
            verify(exactly = 1) { httpClientClientCredentials("client", "jwk", "well-known", scope, true) }
        }
    }

    @Test
    fun `ugyldig bryterverdi stopper oppstart`() {
        assertThrows<IllegalArgumentException> {
            ApplicationContext(Miljoevariabler.httpClient(mapOf(BRUK_INTERN_BEREGNING to "tru")))
        }
    }
}
