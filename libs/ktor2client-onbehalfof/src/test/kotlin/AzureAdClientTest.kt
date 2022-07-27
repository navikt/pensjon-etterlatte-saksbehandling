import WireMockBase.Companion.mockServer
import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import com.github.michaelbull.result.Ok
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor
import com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor
import com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
import com.typesafe.config.ConfigFactory
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.libs.ktorobo.AccessToken
import no.nav.etterlatte.libs.ktorobo.AzureAdClient
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.concurrent.TimeUnit

internal class AzureAdClientTest {
    private val configMap = mapOf(
        "azure.app.well.known.url" to "wellKnownUrl",
        "azure.app.client.id" to "clientId",
        "azure.app.client.secret" to "secret"
    )
    private val config = ConfigFactory.parseMap(configMap)
    private val mockHttpClient = WireMockBase.mockHttpClient

    @BeforeEach
    fun initMockServer() {
        mockServer.resetAll()
        mockServer.stubFor(
            WireMock.get(WireMock.urlPathMatching("/wellKnownUrl"))
                .willReturn(WireMock.okJson(openIdConfigurationMockResponse()))
        )
        mockServer.stubFor(
            WireMock.post(WireMock.urlPathMatching("/token_endpoint"))
                .willReturn(WireMock.okJson(accessTokenMockResponse()))
        )
    }


    @Test
    fun `henter open id configuration fra well known url i config ved oppstart`() {
        AzureAdClient(config, mockHttpClient)
        mockServer.verify(getRequestedFor(urlEqualTo("/wellKnownUrl")))
    }

    @Test
    fun `henter access token hvis det ikke finnes noe i cache`() {
        val response = runBlocking {
            AzureAdClient(config, mockHttpClient).getOnBehalfOfAccessTokenForResource(listOf(), "")
        }

        response shouldBe Ok(AccessToken("token", 60, "testToken"))
        mockServer.verify(postRequestedFor(urlEqualTo("/token_endpoint")))
    }

    @Test
    fun `lagrer access token i cache ved api-kall`() {
        val cache: Cache<String, AccessToken> = Caffeine
            .newBuilder()
            .expireAfterAccess(5, TimeUnit.SECONDS)
            .build()

        runBlocking {
            AzureAdClient(config, mockHttpClient, cache).getOnBehalfOfAccessTokenForResource(listOf(), "saksbehandlerToken")
        }

        cache.estimatedSize() shouldBe 1
        cache.getIfPresent("saksbehandlerToken") shouldNotBe null
    }
    @Test
    fun `bruker cachet access token ved påfølgende kall`() {
        val cache: Cache<String, AccessToken> = Caffeine
            .newBuilder()
            .expireAfterAccess(5, TimeUnit.SECONDS)
            .build()

        runBlocking {
            AzureAdClient(config, mockHttpClient, cache).run {
                getOnBehalfOfAccessTokenForResource(emptyList(), "saksbehandlerToken")
                getOnBehalfOfAccessTokenForResource(emptyList(), "saksbehandlerToken")
                getOnBehalfOfAccessTokenForResource(emptyList(), "saksbehandlerToken")
            }
        }

        mockServer.verify(1, postRequestedFor(urlEqualTo("/token_endpoint")))
    }
}

private fun openIdConfigurationMockResponse() = """
    {
        "jwks_uri": "jwks_uri",
        "issuer": "issuer",
        "token_endpoint": "token_endpoint",
        "authorization_endpoint": "authorization_endpoint"
    }
    """.trimIndent()

private fun accessTokenMockResponse() = """
       {
        "access_token": "token",
        "expires_in": "60",
        "token_type": "testToken"
    } 
""".trimIndent()