package no.nav.etterlatte.beregningasynk

import io.ktor.client.HttpClient
import io.ktor.client.request.post
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.coroutines.runBlocking

class BeregningService(
    private val behandling_app: HttpClient,
    private val url: String
) {
    fun opprettOmberegning(omberegningsid: Long): HttpResponse = runBlocking {
        behandling_app.post("$url/api/beregning/$omberegningsid") {
            contentType(ContentType.Application.Json)
        }
    }
}