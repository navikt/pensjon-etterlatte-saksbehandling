package no.nav.etterlatte.libs.ktor

import com.github.michaelbull.result.get
import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpServer
import com.typesafe.config.ConfigFactory
import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.ktor.ktor.DefaultOAuth2HttpClient
import no.nav.etterlatte.libs.ktor.ktor.ktorobo.AccessToken
import no.nav.etterlatte.libs.ktor.ktor.ktorobo.AzureAdClient
import no.nav.security.token.support.client.core.http.OAuth2HttpHeaders
import no.nav.security.token.support.client.core.http.OAuth2HttpRequest
import no.nav.security.token.support.client.core.oauth2.OAuth2AccessTokenResponse
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import tools.jackson.module.kotlin.readValue
import java.net.InetSocketAddress
import java.net.URI

internal class Jackson3ObjectMapperKlientKontraktTest {
    @Test
    fun `azure ad klienten og felles objectMapper tolker access token likt`() {
        val tokenJson = """{"access_token":"token","expires_in":3600,"token_type":"Bearer","ukjent":"felt"}"""

        withLokalAuthServer(tokenJson) { baseUrl ->
            val azureAdClient =
                AzureAdClient(
                    ConfigFactory.parseMap(
                        mapOf(
                            "azure.app.well.known.url" to "$baseUrl/well-known",
                            "azure.app.client.id" to "client",
                            "azure.app.client.secret" to "secret",
                        ),
                    ),
                )

            val resultat = runBlocking { azureAdClient.getAccessTokenForResource(listOf("scope")) }
            assertTrue(resultat.isOk)

            val faktisk = resultat.get()
            val forventet = objectMapper.readValue<AccessToken>(tokenJson)
            assertEquals(forventet, faktisk)
        }
    }

    @Test
    fun `default oauth2 klienten og felles objectMapper tolker access token respons likt`() {
        val tokenJson = """{"access_token":"token","expires_in":1800,"ukjent":"felt"}"""

        withLokalAuthServer(tokenJson) { baseUrl ->
            val respons =
                DefaultOAuth2HttpClient().post(
                    OAuth2HttpRequest(
                        URI("$baseUrl/token"),
                        OAuth2HttpHeaders.NONE,
                        mapOf("grant_type" to "client_credentials"),
                    ),
                )
            val forventet = objectMapper.readValue<OAuth2AccessTokenResponse>(tokenJson)

            assertEquals(forventet.access_token, respons.access_token)
            assertEquals(forventet.expires_in, respons.expires_in)
            assertEquals(forventet.expires_at, respons.expires_at)
        }
    }

    private fun withLokalAuthServer(
        tokenJson: String,
        block: (baseUrl: String) -> Unit,
    ) {
        val server = HttpServer.create(InetSocketAddress(0), 0)
        val baseUrl = "http://127.0.0.1:${server.address.port}"

        server.createContext("/well-known") { exchange ->
            respondJson(
                exchange,
                """{"jwks_uri":"$baseUrl/jwks","issuer":"issuer","token_endpoint":"$baseUrl/token","authorization_endpoint":"$baseUrl/auth","ukjent":"felt"}""",
            )
        }
        server.createContext("/token") { exchange ->
            respondJson(exchange, tokenJson)
        }

        server.start()
        try {
            block(baseUrl)
        } finally {
            server.stop(0)
        }
    }

    private fun respondJson(
        exchange: HttpExchange,
        body: String,
    ) {
        val bytes = body.toByteArray()
        exchange.responseHeaders.add("Content-Type", "application/json")
        exchange.sendResponseHeaders(200, bytes.size.toLong())
        exchange.responseBody.use { it.write(bytes) }
    }
}
