import WireMockBase.Companion.mockServer
import com.github.benmanes.caffeine.cache.AsyncCache
import com.github.benmanes.caffeine.cache.Caffeine
import com.github.michaelbull.result.Ok
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor
import com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor
import com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
import com.typesafe.config.ConfigFactory
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import no.nav.etterlatte.libs.ktorobo.AccessToken
import no.nav.etterlatte.libs.ktorobo.AzureAdClient
import no.nav.etterlatte.libs.ktorobo.ClientCredentialsTokenRequest
import no.nav.etterlatte.libs.ktorobo.OboTokenRequest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalCoroutinesApi::class)
internal class AzureAdClientTest {
    private val configMap =
        mapOf(
            "azure.app.well.known.url" to "wellKnownUrl",
            "azure.app.client.id" to "clientId",
            "azure.app.client.secret" to "secret",
        )
    private val config = ConfigFactory.parseMap(configMap)
    private val mockHttpClient = WireMockBase.mockHttpClient

    @BeforeEach
    fun initMockServer() {
        mockServer.resetAll()
        mockServer.stubFor(
            WireMock.get(WireMock.urlPathMatching("/wellKnownUrl"))
                .willReturn(WireMock.okJson(openIdConfigurationMockResponse())),
        )
        mockServer.stubFor(
            WireMock.post(WireMock.urlPathMatching("/token_endpoint"))
                .willReturn(WireMock.okJson(accessTokenMockResponse())),
        )
    }

    @Test
    fun `henter open id configuration fra well known url i config ved oppstart`() {
        AzureAdClient(config, mockHttpClient)
        mockServer.verify(getRequestedFor(urlEqualTo("/wellKnownUrl")))
    }

    @Test
    fun `henter OBO access token hvis det ikke finnes noe i cache`() {
        val response =
            runBlocking {
                AzureAdClient(config, mockHttpClient).getOnBehalfOfAccessTokenForResource(listOf(), "")
            }

        response shouldBe Ok(AccessToken("token", 60, "testToken"))
        mockServer.verify(postRequestedFor(urlEqualTo("/token_endpoint")))
    }

    @Test
    fun `lagrer OBO access token i cache ved api-kall`() {
        val cache: AsyncCache<OboTokenRequest, AccessToken> =
            Caffeine
                .newBuilder()
                .expireAfterAccess(5, TimeUnit.SECONDS)
                .buildAsync()

        runBlocking {
            AzureAdClient(config, mockHttpClient, cache).getOnBehalfOfAccessTokenForResource(
                listOf("testScope"),
                "saksbehandlerToken",
            )
        }

        val cachedValue = cache.getIfPresent(OboTokenRequest(listOf("testScope"), "saksbehandlerToken"))!!.get()

        cachedValue.accessToken shouldBe "token"
        cachedValue.expiresIn shouldBe 60
        cachedValue.tokenType shouldBe "testToken"
    }

    @Test
    fun `bruker OBO cachet access token ved påfølgende kall`() {
        val cache: AsyncCache<OboTokenRequest, AccessToken> =
            Caffeine
                .newBuilder()
                .expireAfterAccess(5, TimeUnit.SECONDS)
                .buildAsync()

        runBlocking {
            AzureAdClient(config, mockHttpClient, cache).run {
                getOnBehalfOfAccessTokenForResource(listOf("testScope"), "saksbehandlerToken")
                getOnBehalfOfAccessTokenForResource(listOf("testScope"), "saksbehandlerToken")
                getOnBehalfOfAccessTokenForResource(listOf("testScope"), "saksbehandlerToken")
            }
        }

        mockServer.verify(1, postRequestedFor(urlEqualTo("/token_endpoint")))
    }

    @Test
    fun `bruker OBO cachet access token ved parallele kall`() {
        return runTest {
            val cache: AsyncCache<OboTokenRequest, AccessToken> =
                Caffeine
                    .newBuilder()
                    .expireAfterAccess(5, TimeUnit.SECONDS)
                    .buildAsync()

            val client = AzureAdClient(config, mockHttpClient, cache)
            generateSequence {
                async { client.getOnBehalfOfAccessTokenForResource(listOf("testScope"), "saksbehandlerToken") }
            }.take(3).toList().awaitAll()

            mockServer.verify(1, postRequestedFor(urlEqualTo("/token_endpoint")))
        }
    }

    @Test
    fun `henter client credentials access token hvis det ikke finnes noe i cache`() {
        val response =
            runBlocking {
                AzureAdClient(config, mockHttpClient).getAccessTokenForResource(listOf())
            }

        response shouldBe Ok(AccessToken("token", 60, "testToken"))
        mockServer.verify(postRequestedFor(urlEqualTo("/token_endpoint")))
    }

    @Test
    fun `lagrer client credentials access token i cache ved api-kall`() {
        val cache: AsyncCache<ClientCredentialsTokenRequest, AccessToken> =
            Caffeine
                .newBuilder()
                .expireAfterAccess(5, TimeUnit.SECONDS)
                .buildAsync()

        runBlocking {
            AzureAdClient(config, mockHttpClient, clientCredentialsCache = cache).getAccessTokenForResource(
                listOf("testScope"),
            )
        }

        val cachedValue = cache.getIfPresent(ClientCredentialsTokenRequest(listOf("testScope")))!!.get()

        cachedValue.accessToken shouldBe "token"
        cachedValue.expiresIn shouldBe 60
        cachedValue.tokenType shouldBe "testToken"
    }

    @Test
    fun `bruker client credentials cachet access token ved påfølgende kall`() {
        val cache: AsyncCache<ClientCredentialsTokenRequest, AccessToken> =
            Caffeine
                .newBuilder()
                .expireAfterAccess(5, TimeUnit.SECONDS)
                .buildAsync()

        runBlocking {
            AzureAdClient(config, mockHttpClient, clientCredentialsCache = cache).run {
                getAccessTokenForResource(listOf("testScope"))
                getAccessTokenForResource(listOf("testScope"))
                getAccessTokenForResource(listOf("testScope"))
            }
        }

        mockServer.verify(1, postRequestedFor(urlEqualTo("/token_endpoint")))
    }

    @Test
    fun `bruker client credentials cachet access token ved parallele kall`() {
        return runTest {
            val cache: AsyncCache<ClientCredentialsTokenRequest, AccessToken> =
                Caffeine
                    .newBuilder()
                    .expireAfterAccess(5, TimeUnit.SECONDS)
                    .buildAsync()

            val client = AzureAdClient(config, mockHttpClient, clientCredentialsCache = cache)
            generateSequence {
                async { client.getAccessTokenForResource(listOf("testScope")) }
            }.take(3).toList().awaitAll()

            mockServer.verify(1, postRequestedFor(urlEqualTo("/token_endpoint")))
        }
    }
}

private fun openIdConfigurationMockResponse() =
    """
    {
        "jwks_uri": "jwks_uri",
        "issuer": "issuer",
        "token_endpoint": "token_endpoint",
        "authorization_endpoint": "authorization_endpoint"
    }
    """.trimIndent()

private fun accessTokenMockResponse() =
    """
       {
        "access_token": "token",
        "expires_in": "60",
        "token_type": "testToken"
    } 
    """.trimIndent()
