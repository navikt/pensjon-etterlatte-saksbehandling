package no.nav.etterlatte.beregningkafka

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpStatusCode
import io.mockk.every
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import io.mockk.verify
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
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.NullSource
import org.junit.jupiter.params.provider.ValueSource
import java.util.UUID

internal class AppBuilderTest {
    @AfterEach
    fun ryddOpp() {
        unmockkStatic(::httpClientClientCredentials)
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(booleans = [false, true])
    fun `alle rivers bruker valgt backend og tilhoerende scope`(aktivert: Boolean?) {
        val brukBehandling = aktivert == true
        val app = if (brukBehandling) "behandling" else "beregning"
        val scope = "api://$app/.default"
        val engine = MockEngine { respond("", HttpStatusCode.NoContent) }
        HttpClient(engine).use { httpClient ->
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
            val service = AppBuilder(env).createBeregningService()
            val behandlingId = UUID.randomUUID()
            val forrigeId = UUID.randomUUID()

            service.hentOverstyrt(behandlingId)
            service.hentBeregning(behandlingId)
            service.beregn(behandlingId)
            service.opprettBeregningsgrunnlagFraForrigeBehandling(behandlingId, forrigeId)
            service.tilpassOverstyrtBeregningsgrunnlagForRegulering(behandlingId)
            service.haandterTidligAlderpensjon(behandlingId)
            service.omregnAvkorting(behandlingId, forrigeId)
            service.hentAvkorting(behandlingId)

            assertEquals(
                listOf(
                    "GET /api/beregning/$behandlingId/overstyrt",
                    "GET /api/beregning/$behandlingId",
                    "POST /api/beregning/$behandlingId",
                    "POST /api/beregning/beregningsgrunnlag/$behandlingId/fra/$forrigeId",
                    "PUT /api/beregning/beregningsgrunnlag/$behandlingId/overstyr/regulering",
                    "POST /api/beregning/avkorting/$behandlingId/haandter-tidlig-alderspensjon",
                    "POST /api/beregning/avkorting/$behandlingId/med/$forrigeId",
                    "GET /api/beregning/avkorting/$behandlingId/ferdig",
                ),
                engine.requestHistory.map { "${it.method.value} ${it.url.encodedPath}" },
            )
            assertEquals(setOf(app), engine.requestHistory.map { it.url.host }.toSet())
            verify(exactly = 1) { httpClientClientCredentials("client", "jwk", "well-known", scope, true) }
        }
    }

    @Test
    fun `ugyldig bryterverdi stopper oppstart`() {
        assertThrows<IllegalArgumentException> {
            AppBuilder(Miljoevariabler.httpClient(mapOf(BRUK_INTERN_BEREGNING to "tru")))
        }
    }
}
