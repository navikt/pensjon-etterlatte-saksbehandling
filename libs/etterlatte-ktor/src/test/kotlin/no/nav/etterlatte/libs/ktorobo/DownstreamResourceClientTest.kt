package no.nav.etterlatte.libs.ktorobo

import io.ktor.http.ContentType
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class DownstreamResourceClientTest {
    private val azureAdClient = mockk<AzureAdClient>()
    private val resource = Resource("clientId1", "")
    private val client =
        DownstreamResourceClient(
            azureAdClient = azureAdClient,
        )

    @Test
    fun `egen sjekk av content-type håndterer parsede parametere`() {
        val contenttypeMedCharset = ContentType.parse("application/json; charset=UTF-8")

        assertTrue(contentTypeErLik(contenttypeMedCharset, ContentType.Application.Json))
        assertTrue(contentTypeErLik(ContentType.Application.Json, contenttypeMedCharset))
    }
}
