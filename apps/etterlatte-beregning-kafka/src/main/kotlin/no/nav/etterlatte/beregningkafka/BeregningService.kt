package no.nav.etterlatte.beregningkafka

import io.ktor.client.HttpClient
import io.ktor.client.request.post
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.coroutines.runBlocking
import java.util.*

class BeregningService(
    private val behandling_app: HttpClient,
    private val url: String
) {
    fun opprettOmregning(omregningsid: UUID): HttpResponse = runBlocking {
        behandling_app.post("$url/api/beregning/$omregningsid") {
            contentType(ContentType.Application.Json)
        }
    }
}