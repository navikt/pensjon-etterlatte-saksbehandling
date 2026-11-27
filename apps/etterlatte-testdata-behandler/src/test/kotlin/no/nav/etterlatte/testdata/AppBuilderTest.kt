package no.nav.etterlatte.testdata

import com.github.michaelbull.result.Ok
import com.typesafe.config.ConfigFactory
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.EnvKey.BRUK_INTERN_BEREGNING
import no.nav.etterlatte.libs.common.EnvEnum
import no.nav.etterlatte.libs.common.Miljoevariabler
import no.nav.etterlatte.libs.ktor.ktor.ktorobo.DownstreamResourceClient
import no.nav.etterlatte.libs.ktor.ktor.ktorobo.Resource
import no.nav.etterlatte.libs.ktor.token.HardkodaSystembruker
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.NullSource
import org.junit.jupiter.params.provider.ValueSource
import java.time.YearMonth
import java.util.UUID

internal class AppBuilderTest {
    @ParameterizedTest
    @NullSource
    @ValueSource(booleans = [false, true])
    fun `beregning og avkorting bruker samme backend og client-id`(aktivert: Boolean?) {
        val app = if (aktivert == true) "behandling" else "beregning"
        val config =
            ConfigFactory.parseMap(
                mapOf(
                    "$app.client.id" to "$app-client",
                    "$app.resource.url" to "http://$app",
                ),
            )
        val env =
            Miljoevariabler.httpClient(
                buildMap<EnvEnum, String> {
                    aktivert?.let { put(BRUK_INTERN_BEREGNING, it.toString()) }
                },
            )
        val builder = AppBuilder(config, env)
        val httpClient = mockk<DownstreamResourceClient>()
        coEvery { httpClient.post(any(), any(), any(), any()) } answers { Ok(firstArg<Resource>()) }
        val behandlingId = UUID.randomUUID()
        val bruker = HardkodaSystembruker.testdata

        runBlocking {
            builder.lagBeregningService(httpClient).beregn(behandlingId, bruker)
            builder.lagBeregningService(httpClient).lagreBeregningsgrunnlag(behandlingId, bruker)
            builder.lagAvkortingService(httpClient).avkort(behandlingId, YearMonth.of(2026, 1), bruker)
        }

        listOf(
            "/api/beregning/$behandlingId",
            "/api/beregning/beregningsgrunnlag/$behandlingId",
            "/api/beregning/avkorting/$behandlingId/liste",
        ).forEach { sti ->
            coVerify(exactly = 1) {
                httpClient.post(Resource("$app-client", "http://$app$sti"), bruker, any(), any())
            }
        }
    }

    @Test
    fun `ugyldig bryterverdi stopper oppstart`() {
        assertThrows<IllegalArgumentException> {
            AppBuilder(
                ConfigFactory.empty(),
                Miljoevariabler.httpClient(mapOf(BRUK_INTERN_BEREGNING to "tru")),
            )
        }
    }
}
