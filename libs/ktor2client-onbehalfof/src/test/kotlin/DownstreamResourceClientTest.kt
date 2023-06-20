package no.nav.etterlatte.libs.ktorobo

import com.github.michaelbull.result.Ok
import io.ktor.http.ContentType
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.token.BrukerTokenInfo
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class DownstreamResourceClientTest {
    private val azureAdClient = mockk<AzureAdClient>()
    private val resource = Resource("clientId1", "")
    private val client = DownstreamResourceClient(
        azureAdClient = azureAdClient
    )

    @Test
    fun `egen sjekk av content-type håndterer parsede parametere`() {
        val contenttypeMedCharset = ContentType.parse("application/json; charset=UTF-8")

        assertTrue(contentTypeErLik(contenttypeMedCharset, ContentType.Application.Json))
        assertTrue(contentTypeErLik(ContentType.Application.Json, contenttypeMedCharset))
    }

    @Test
    fun `bruker client credentials viss JWT-claims sub og oid er like`() {
        every { runBlocking { azureAdClient.getAccessTokenForResource(any()) } } returns Ok(mockk())

        runBlocking {
            client.get(
                resource,
                BrukerTokenInfo.of(accessToken = "a", oid = "b", sub = "b", saksbehandler = null, claims = null)
            )
        }
        verify { runBlocking { azureAdClient.getAccessTokenForResource(any()) } }
        verify(exactly = 0) {
            runBlocking { azureAdClient.getOnBehalfOfAccessTokenForResource(any(), any()) }
        }
    }

    @Test
    fun `bruker OBO viss JWT-claims sub og oid er ulike`() {
        every { runBlocking { azureAdClient.getOnBehalfOfAccessTokenForResource(any(), any()) } } returns Ok(mockk())

        runBlocking {
            client.get(
                resource,
                BrukerTokenInfo.of(accessToken = "a", oid = "b", sub = "c", saksbehandler = "s1", claims = null)
            )
        }
        verify { runBlocking { azureAdClient.getOnBehalfOfAccessTokenForResource(any(), "a") } }
        verify(exactly = 0) { runBlocking { azureAdClient.getAccessTokenForResource(any()) } }
    }
}