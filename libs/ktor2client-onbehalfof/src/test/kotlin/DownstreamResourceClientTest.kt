import io.ktor.http.ContentType
import no.nav.etterlatte.libs.ktorobo.contentTypeErLik
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class DownstreamResourceClientTest {

    @Test
    fun `egen sjekk av content-type håndterer parsede parametere`() {
        val contenttypeMedCharset = ContentType.parse("application/json; charset=UTF-8")

        assertTrue(contentTypeErLik(contenttypeMedCharset, ContentType.Application.Json))
        assertTrue(contentTypeErLik(ContentType.Application.Json, contenttypeMedCharset))
    }
}