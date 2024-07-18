
import com.github.benmanes.caffeine.cache.AsyncCache
import com.github.benmanes.caffeine.cache.Caffeine
import com.github.michaelbull.result.Ok
import com.typesafe.config.ConfigFactory
import io.kotest.matchers.shouldBe
import io.ktor.client.call.body
import io.ktor.client.statement.HttpResponse
import io.ktor.http.Parameters
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
import no.nav.etterlatte.ktor.token.simpleSaksbehandler
import no.nav.etterlatte.ktor.token.systembruker
import no.nav.etterlatte.libs.ktor.ktor.ktorobo.AccessToken
import no.nav.etterlatte.libs.ktor.ktor.ktorobo.AzureAdClient
import no.nav.etterlatte.libs.ktor.ktor.ktorobo.AzureAdOpenIdConfiguration
import no.nav.etterlatte.libs.ktor.ktor.ktorobo.ClientCredentialsTokenRequest
import no.nav.etterlatte.libs.ktor.ktor.ktorobo.IAzureAdHttpClient
import no.nav.etterlatte.libs.ktor.ktor.ktorobo.OboTokenRequest
import no.nav.etterlatte.libs.ktor.token.Claims
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

    @Test
    fun `henter open id configuration fra well known url i config ved oppstart`() {
        testApplication {
            AzureAdClient(config, FakeAzureAdHttpClient())
        }
    }

    @Test
    fun `henter OBO access token hvis det ikke finnes noe i cache`() {
        testApplication {
            val azureAdClient = AzureAdClient(config, FakeAzureAdHttpClient())
            val response = azureAdClient.getOnBehalfOfAccessTokenForResource(listOf(), "")
            response shouldBe Ok(AccessToken("token", 60, "testToken"))
        }
    }

    @Test
    fun `lagrer OBO access token i cache ved api-kall`() {
        val cache: AsyncCache<OboTokenRequest, AccessToken> =
            Caffeine
                .newBuilder()
                .expireAfterAccess(5, TimeUnit.SECONDS)
                .buildAsync()

        runBlocking {
            AzureAdClient(config, FakeAzureAdHttpClient(), cache).getOnBehalfOfAccessTokenForResource(
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

        val httpClient = FakeAzureAdHttpClient()
        runBlocking {
            AzureAdClient(config, httpClient, cache)
                .run {
                    getOnBehalfOfAccessTokenForResource(listOf("testScope"), "saksbehandlerToken")
                    getOnBehalfOfAccessTokenForResource(listOf("testScope"), "saksbehandlerToken")
                    getOnBehalfOfAccessTokenForResource(listOf("testScope"), "saksbehandlerToken")
                }
        }
        assertEquals(1, httpClient.called)
    }

    @Test
    fun `bruker OBO cachet access token ved parallele kall`() =
        runTest {
            val cache: AsyncCache<OboTokenRequest, AccessToken> =
                Caffeine
                    .newBuilder()
                    .expireAfterAccess(5, TimeUnit.SECONDS)
                    .buildAsync()

            val httpClient = FakeAzureAdHttpClient()
            val client = AzureAdClient(config, httpClient, cache)
            generateSequence {
                async { client.getOnBehalfOfAccessTokenForResource(listOf("testScope"), "saksbehandlerToken") }
            }.take(3).toList().awaitAll()

            assertEquals(1, httpClient.called)
        }

    @Test
    fun `henter client credentials access token hvis det ikke finnes noe i cache`() {
        testApplication {
            val azureAdClient = AzureAdClient(config, FakeAzureAdHttpClient())
            val response = azureAdClient.getAccessTokenForResource(listOf())
            response shouldBe Ok(AccessToken("token", 60, "testToken"))
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
            AzureAdClient(config, FakeAzureAdHttpClient(), clientCredentialsCache = cache).getAccessTokenForResource(
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

        val httpClient = FakeAzureAdHttpClient()
        runBlocking {
            AzureAdClient(config, httpClient, clientCredentialsCache = cache).run {
                getAccessTokenForResource(listOf("testScope"))
                getAccessTokenForResource(listOf("testScope"))
                getAccessTokenForResource(listOf("testScope"))
            }
        }
        assertEquals(1, httpClient.called)
    }

    @Test
    fun `bruker client credentials cachet access token ved parallele kall`() =
        runTest {
            val cache: AsyncCache<ClientCredentialsTokenRequest, AccessToken> =
                Caffeine
                    .newBuilder()
                    .expireAfterAccess(5, TimeUnit.SECONDS)
                    .buildAsync()

            val httpClient = FakeAzureAdHttpClient()
            val client = AzureAdClient(config, httpClient, clientCredentialsCache = cache)
            generateSequence {
                async { client.getAccessTokenForResource(listOf("testScope")) }
            }.take(3).toList().awaitAll()

            assertEquals(1, httpClient.called)
        }

    @Test
    fun `bruker client credentials viss JWT-claims sub og oid er like`() {
        val client =
            spyk(AzureAdClient(config, FakeAzureAdHttpClient())).also {
                coEvery { it.getAccessTokenForResource(any()) } returns Ok(mockk())
            }

        runBlocking {
            client.hentTokenFraAD(
                systembruker(claims = mapOf(Claims.idtyp to "app", Claims.azp_name to "cluster:appname:dev")),
                listOf(),
            )
        }
        coVerify { client.getAccessTokenForResource(any()) }
        coVerify(exactly = 0) { client.getOnBehalfOfAccessTokenForResource(any(), any()) }
    }

    @Test
    fun `bruker OBO viss JWT-claim idtype er tom eller ikke lik app`() {
        val client =
            spyk(AzureAdClient(config, FakeAzureAdHttpClient())).also {
                coEvery { it.getAccessTokenForResource(any()) } returns Ok(mockk())
            }
        every { runBlocking { client.getOnBehalfOfAccessTokenForResource(any(), any()) } } returns Ok(mockk())

        runBlocking {
            client.hentTokenFraAD(
                simpleSaksbehandler(ident = "s1"),
                listOf(),
            )
        }
        coVerify { client.getOnBehalfOfAccessTokenForResource(any(), "token") }
        coVerify(exactly = 0) { client.getAccessTokenForResource(any()) }
    }
}

private class FakeAzureAdHttpClient : IAzureAdHttpClient {
    var called = 0

    override suspend fun doGet(url: String): HttpResponse = adConfigResponse()

    override suspend fun submitForm(
        url: String,
        params: Parameters,
    ) = hentAccessToken().also { called++ }

    private fun hentAccessToken(): HttpResponse {
        val accessToken = AccessToken(accessToken = "token", expiresIn = 60, tokenType = "testToken")
        return mockk<HttpResponse>().also { coEvery { it.body<AccessToken>() } returns accessToken }
    }

    private fun adConfigResponse(): HttpResponse {
        val adConfig =
            AzureAdOpenIdConfiguration(
                jwksUri = "jwks_uri",
                issuer = "issuer",
                tokenEndpoint = "token_endpoint",
                authorizationEndpoint = "authorization_endpoint",
            )
        return mockk<HttpResponse>().also { coEvery { it.body<AzureAdOpenIdConfiguration>() } returns adConfig }
    }
}
