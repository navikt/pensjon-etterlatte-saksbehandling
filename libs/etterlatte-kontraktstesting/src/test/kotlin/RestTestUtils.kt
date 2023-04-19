
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.isSuccess
import io.ktor.server.testing.ApplicationTestBuilder
import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.libs.common.toJson
import org.junit.jupiter.api.Assertions

fun ApplicationTestBuilder.verifiserAtRutaMatcherDetKlientenKallerForPost(
    sti: String,
    request: Any,
    token: String
) {
    runBlocking {
        client.post(sti) {
            setBody(request.toJson())
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            header(HttpHeaders.Authorization, "Bearer $token")
        }.also { Assertions.assertTrue { it.status.isSuccess() } }
    }
}