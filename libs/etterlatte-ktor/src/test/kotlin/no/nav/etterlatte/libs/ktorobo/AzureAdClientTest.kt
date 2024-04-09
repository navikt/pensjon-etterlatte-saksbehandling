
import WireMockBase.Companion.mockServer
import com.github.benmanes.caffeine.cache.AsyncCache
import com.github.benmanes.caffeine.cache.Caffeine
import com.github.michaelbull.result.Ok
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor
import com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
import com.typesafe.config.ConfigFactory
import io.kotest.matchers.shouldBe
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.statement.HttpResponse
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import no.nav.etterlatte.ktor.runServer
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.ktor.ktor.ktorobo.AccessToken
import no.nav.etterlatte.libs.ktor.ktor.ktorobo.AzureAdClient
import no.nav.etterlatte.libs.ktor.ktor.ktorobo.AzureAdOpenIdConfiguration
import no.nav.etterlatte.libs.ktor.ktor.ktorobo.ClientCredentialsTokenRequest
import no.nav.etterlatte.libs.ktor.ktor.ktorobo.OboTokenRequest
import no.nav.etterlatte.libs.ktor.token.BrukerTokenInfo
import no.nav.security.mock.oauth2.MockOAuth2Server
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.util.concurrent.TimeUnit

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
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

    @BeforeAll
    fun before() {
        mockOAuth2Server.start()
    }

    @AfterEach
    fun afterEach() {
        clearAllMocks()
    }

    @AfterAll
    fun after() {
        mockOAuth2Server.shutdown()
    }

    private val mockOAuth2Server = MockOAuth2Server()

    private fun ApplicationTestBuilder.httpClient(): HttpClient =
        runServer(mockOAuth2Server, "") {
        }

    @Test
    fun `henter open id configuration fra well known url i config ved oppstart`() {
        testApplication {
            val adConfigResponse = httpResponse()
            val client = httpClient()
            AzureAdClient(config, client, httpGetter = { adConfigResponse })
        }
    }

    @Test
    fun `henter OBO access token hvis det ikke finnes noe i cache`() {
        testApplication {
            val adConfigResponse = httpResponse()
            val accessToken = objectMapper.readValue(accessTokenMockResponse(), AccessToken::class.java)
            val tokenResponse = mockk<HttpResponse>().also { coEvery { it.body<AccessToken>() } returns accessToken }

            val client = httpClient()
            val azureAdClient =
                AzureAdClient(
                    config,
                    client,
                    httpGetter = { adConfigResponse },
                    httpSubmitForm =
                        { _, _ -> tokenResponse },
                )
            val resp = azureAdClient.getOnBehalfOfAccessTokenForResource(listOf(), "")
            resp shouldBe Ok(AccessToken("token", 60, "testToken"))
        }
    }

    private fun httpResponse(): HttpResponse {
        val adConfig = objectMapper.readValue(openIdConfigurationMockResponse(), AzureAdOpenIdConfiguration::class.java)
        return mockk<HttpResponse>().also { coEvery { it.body<AzureAdOpenIdConfiguration>() } returns adConfig }
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

    @Test
    fun `bruker client credentials viss JWT-claims sub og oid er like`() {
        val client =
            spyk(AzureAdClient(config, mockHttpClient)).also {
                coEvery { it.getAccessTokenForResource(any()) } returns Ok(mockk())
            }
        runBlocking {
            client.hentTokenFraAD(
                BrukerTokenInfo.of(accessToken = "a", oid = "b", sub = "b", saksbehandler = null, claims = null),
                listOf(),
            )
        }
        coVerify { client.getAccessTokenForResource(any()) }
        coVerify(exactly = 0) { client.getOnBehalfOfAccessTokenForResource(any(), any()) }
    }

    @Test
    fun `bruker OBO viss JWT-claims sub og oid er ulike`() {
        val client =
            spyk(AzureAdClient(config, mockHttpClient)).also {
                coEvery { it.getAccessTokenForResource(any()) } returns Ok(mockk())
            }
        every { runBlocking { client.getOnBehalfOfAccessTokenForResource(any(), any()) } } returns Ok(mockk())

        runBlocking {
            client.hentTokenFraAD(
                BrukerTokenInfo.of(accessToken = "a", oid = "b", sub = "c", saksbehandler = "s1", claims = null),
                listOf(),
            )
        }
        coVerify { client.getOnBehalfOfAccessTokenForResource(any(), "a") }
        coVerify(exactly = 0) { client.getAccessTokenForResource(any()) }
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
