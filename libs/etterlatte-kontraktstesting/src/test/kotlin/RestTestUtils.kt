import io.ktor.client.HttpClient
import io.ktor.server.testing.ApplicationTestBuilder
import no.nav.etterlatte.libs.common.toJson

private suspend fun ApplicationTestBuilder.verifyKlientOgRouteHarLikSignatur(
    klient: HttpClient,
    request: Any
) {
    val sti = slot<String>()
    verify {
        runBlocking {
            klient.post(capture(sti)) {
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                request.toJson()
            }
        }
    }

    client.post(sti.captured) {
        setBody(request.toJson())
        header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
        header(HttpHeaders.Authorization, "Bearer $token")
    }.also { verify { it.status.isSuccess() } }
}