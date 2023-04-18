import io.ktor.client.HttpClient
import io.ktor.server.testing.ApplicationTestBuilder
import no.nav.etterlatte.libs.common.toJson

private suspend fun ApplicationTestBuilder.verifyKlientOgRouteHarLikSignaturForPost(
    klient: HttpClient,
    request: Any
) {
    val sti = slot<String>()
    val jsonHeader = header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
    val body = request.toJson()
    verify {
        runBlocking {
            klient.post(capture(sti)) {
                jsonHeader
                body
            }
        }
    }

    client.post(sti.captured) {
        setBody(body)
        jsonHeader
        header(HttpHeaders.Authorization, "Bearer $token")
    }.also { verify { it.status.isSuccess() } }
}