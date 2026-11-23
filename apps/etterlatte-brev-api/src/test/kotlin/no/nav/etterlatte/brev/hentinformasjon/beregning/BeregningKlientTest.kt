package no.nav.etterlatte.brev.hentinformasjon.beregning

import com.github.michaelbull.result.Ok
import com.typesafe.config.ConfigFactory
import io.kotest.matchers.shouldBe
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpStatusCode
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockkConstructor
import io.mockk.unmockkConstructor
import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.EnvKey.BRUK_INTERN_BEREGNING
import no.nav.etterlatte.libs.common.EnvEnum
import no.nav.etterlatte.libs.common.Miljoevariabler
import no.nav.etterlatte.libs.common.feilhaandtering.ForespoerselException
import no.nav.etterlatte.libs.ktor.ktor.ktorobo.AccessToken
import no.nav.etterlatte.libs.ktor.ktor.ktorobo.AzureAdClient
import no.nav.etterlatte.libs.ktor.token.HardkodaSystembruker
import no.nav.security.mock.oauth2.MockOAuth2Server
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.NullSource
import org.junit.jupiter.params.provider.ValueSource
import java.util.UUID

internal class BeregningKlientTest {
    private val server = MockOAuth2Server()

    @BeforeEach
    fun setup() {
        server.start()
        mockkConstructor(AzureAdClient::class)
        coEvery { anyConstructed<AzureAdClient>().hentTokenFraAD(any(), any()) } returns
            Ok(AccessToken("test-token", 3600, "Bearer"))
    }

    @AfterEach
    fun teardown() {
        unmockkConstructor(AzureAdClient::class)
        server.shutdown()
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(booleans = [false, true])
    fun `brev bruker valgt backend og client-id uten aa kreve gammel konfigurasjon`(aktivert: Boolean?) {
        val app = if (aktivert == true) "behandling" else "beregning"
        val config =
            ConfigFactory.parseMap(
                mapOf(
                    "azure.app.well.known.url" to server.wellKnownUrl("azure").toString(),
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
        val engine = MockEngine { respond("", HttpStatusCode.NotFound) }
        HttpClient(engine) { expectSuccess = true }.use { httpClient ->
            val klient = BeregningKlient(config, httpClient, env)
            val behandlingId = UUID.randomUUID()

            runBlocking {
                klient.hentBeregning(behandlingId, HardkodaSystembruker.testdata) shouldBe null
                klient.hentBeregningsGrunnlag(behandlingId, HardkodaSystembruker.testdata) shouldBe null
            }
            assertThrows<ForespoerselException> {
                runBlocking { klient.hentBeregningOgAvkorting(behandlingId, HardkodaSystembruker.testdata) }
            }
            assertThrows<ForespoerselException> {
                runBlocking { klient.hentGrunnbeloep(HardkodaSystembruker.testdata) }
            }

            engine.requestHistory.map { it.url.toString() } shouldBe
                listOf(
                    "http://$app/api/beregning/$behandlingId",
                    "http://$app/api/beregning/beregningsgrunnlag/$behandlingId",
                    "http://$app/api/beregning/ytelse-med-grunnlag/$behandlingId",
                    "http://$app/api/beregning/grunnbeloep",
                )
            coVerify(exactly = 4) {
                anyConstructed<AzureAdClient>().hentTokenFraAD(
                    HardkodaSystembruker.testdata,
                    listOf("api://$app-client/.default"),
                )
            }
        }
    }

    @Test
    fun `ugyldig bryterverdi stopper oppstart`() {
        HttpClient(MockEngine { respond("", HttpStatusCode.NoContent) }).use { httpClient ->
            assertThrows<IllegalArgumentException> {
                BeregningKlient(
                    ConfigFactory.empty(),
                    httpClient,
                    Miljoevariabler.httpClient(mapOf(BRUK_INTERN_BEREGNING to "tru")),
                )
            }
        }
    }
}
