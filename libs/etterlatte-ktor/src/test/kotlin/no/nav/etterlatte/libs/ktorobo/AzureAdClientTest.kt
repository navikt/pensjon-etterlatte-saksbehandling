
import com.github.benmanes.caffeine.cache.AsyncCache
import com.github.benmanes.caffeine.cache.Caffeine
import com.github.michaelbull.result.Ok
import com.typesafe.config.ConfigFactory
import io.kotest.matchers.shouldBe
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.statement.HttpResponse
import io.ktor.http.Parameters
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
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.ktor.httpClient
import no.nav.etterlatte.libs.ktor.ktor.ktorobo.AccessToken
import no.nav.etterlatte.libs.ktor.ktor.ktorobo.AzureAdClient
import no.nav.etterlatte.libs.ktor.ktor.ktorobo.AzureAdOpenIdConfiguration
import no.nav.etterlatte.libs.ktor.ktor.ktorobo.ClientCredentialsTokenRequest
import no.nav.etterlatte.libs.ktor.ktor.ktorobo.OboTokenRequest
import no.nav.etterlatte.libs.ktor.token.BrukerTokenInfo
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.util.concurrent.TimeUnit

internal class AzureAdClientTest {
    private val config =
        ConfigFactory.parseMap(
            mapOf(
                "azure.app.well.known.url" to "wellKnownUrl",
                "azure.app.client.id" to "clientId",
                "azure.app.client.secret" to "secret",
            ),
        )

    @AfterEach
    fun afterEach() {
        clearAllMocks()
    }

    private fun ApplicationTestBuilder.httpClient(): HttpClient = createClient { }

    @Test
    fun `henter open id configuration fra well known url i config ved oppstart`() {
        testApplication {
            AzureAdClient(config, httpClient(), httpGetter = { adConfigResponse() })
        }
    }

    @Test
    fun `henter OBO access token hvis det ikke finnes noe i cache`() {
        testApplication {
            val azureAdClient =
                AzureAdClient(
                    config,
                    httpClient(),
                    httpGetter = { adConfigResponse() },
                    httpSubmitForm = { _, _ -> hentAccessToken() },
                )
            val resp = azureAdClient.getOnBehalfOfAccessTokenForResource(listOf(), "")
            resp shouldBe Ok(AccessToken("token", 60, "testToken"))
        }
    }

    private fun hentAccessToken(): HttpResponse {
        val accessToken = objectMapper.readValue(accessTokenMockResponse(), AccessToken::class.java)
        return mockk<HttpResponse>().also { coEvery { it.body<AccessToken>() } returns accessToken }
    }

    private fun adConfigResponse(): HttpResponse {
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
            AzureAdClient(
                config,
                httpClient(),
                cache,
                httpGetter = { adConfigResponse() },
                httpSubmitForm =
                    { _, _ -> hentAccessToken() },
            ).getOnBehalfOfAccessTokenForResource(
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

        var called = 0
        val httpSubmitForm: suspend HttpClient.(url: String, params: Parameters) -> HttpResponse =
            { _, _ -> hentAccessToken().also { called++ } }
        runBlocking {
            AzureAdClient(
                config,
                httpClient(),
                cache,
                httpGetter = { adConfigResponse() },
                httpSubmitForm = httpSubmitForm,
            )
                .run {
                    getOnBehalfOfAccessTokenForResource(listOf("testScope"), "saksbehandlerToken")
                    getOnBehalfOfAccessTokenForResource(listOf("testScope"), "saksbehandlerToken")
                    getOnBehalfOfAccessTokenForResource(listOf("testScope"), "saksbehandlerToken")
                }
        }
        assertEquals(1, called)
    }

    @Test
    fun `bruker OBO cachet access token ved parallele kall`() {
        return runTest {
            val cache: AsyncCache<OboTokenRequest, AccessToken> =
                Caffeine
                    .newBuilder()
                    .expireAfterAccess(5, TimeUnit.SECONDS)
                    .buildAsync()

            var called = 0
            val httpSubmitForm: suspend HttpClient.(url: String, params: Parameters) -> HttpResponse =
                { _, _ -> hentAccessToken().also { called++ } }
            val client =
                AzureAdClient(
                    config,
                    httpClient(),
                    cache,
                    httpGetter = { adConfigResponse() },
                    httpSubmitForm = httpSubmitForm,
                )
            generateSequence {
                async { client.getOnBehalfOfAccessTokenForResource(listOf("testScope"), "saksbehandlerToken") }
            }.take(3).toList().awaitAll()

            assertEquals(1, called)
        }
    }

    @Test
    fun `henter client credentials access token hvis det ikke finnes noe i cache`() {
        testApplication {
            val azureAdClient =
                AzureAdClient(
                    config,
                    httpClient(),
                    httpGetter = { adConfigResponse() },
                    httpSubmitForm = { _, _ -> hentAccessToken() },
                )
            val resp = azureAdClient.getAccessTokenForResource(listOf())
            resp shouldBe Ok(AccessToken("token", 60, "testToken"))
        }
    }

    @Test
    fun `lagrer client credentials access token i cache ved api-kall`() {
        val cache: AsyncCache<ClientCredentialsTokenRequest, AccessToken> =
            Caffeine
                .newBuilder()
                .expireAfterAccess(5, TimeUnit.SECONDS)
                .buildAsync()

        runBlocking {
            AzureAdClient(
                config,
                httpClient(),
                clientCredentialsCache = cache,
                httpGetter = { adConfigResponse() },
                httpSubmitForm =
                    { _, _ -> hentAccessToken() },
            ).getAccessTokenForResource(
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

        var called = 0
        val httpSubmitForm: suspend HttpClient.(url: String, params: Parameters) -> HttpResponse =
            { _, _ -> hentAccessToken().also { called++ } }
        runBlocking {
            AzureAdClient(
                config,
                httpClient(),
                clientCredentialsCache = cache,
                httpGetter = { adConfigResponse() },
                httpSubmitForm = httpSubmitForm,
            )
                .run {
                    getAccessTokenForResource(listOf("testScope"))
                    getAccessTokenForResource(listOf("testScope"))
                    getAccessTokenForResource(listOf("testScope"))
                }
        }
        assertEquals(1, called)
    }

    @Test
    fun `bruker client credentials cachet access token ved parallele kall`() {
        return runTest {
            val cache: AsyncCache<ClientCredentialsTokenRequest, AccessToken> =
                Caffeine
                    .newBuilder()
                    .expireAfterAccess(5, TimeUnit.SECONDS)
                    .buildAsync()

            var called = 0
            val httpSubmitForm: suspend HttpClient.(url: String, params: Parameters) -> HttpResponse =
                { _, _ -> hentAccessToken().also { called++ } }
            val client =
                AzureAdClient(
                    config,
                    httpClient(),
                    clientCredentialsCache = cache,
                    httpGetter = { adConfigResponse() },
                    httpSubmitForm = httpSubmitForm,
                )
            generateSequence {
                async { client.getAccessTokenForResource(listOf("testScope")) }
            }.take(3).toList().awaitAll()

            assertEquals(1, called)
        }
    }

    @Test
    fun `bruker client credentials viss JWT-claims sub og oid er like`() {
        val client =
            spyk(AzureAdClient(config, httpClient(), httpGetter = { adConfigResponse() })).also {
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
            spyk(AzureAdClient(config, httpClient(), httpGetter = { adConfigResponse() })).also {
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
