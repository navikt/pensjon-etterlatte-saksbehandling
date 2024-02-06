package no.nav.etterlatte.tidshendelser.klient

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.accept
import io.ktor.client.request.get
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.coroutines.runBlocking
import java.time.YearMonth

class GrunnlagKlient(
    private val grunnlagHttpClient: HttpClient,
    private val grunnlagUrl: String,
) {
    fun hentSaker(foedselsmaaned: YearMonth): List<Long> {
        return runBlocking {
            grunnlagHttpClient.get("$grunnlagUrl/grunnlag/aldersovergang/$foedselsmaaned") {
                accept(ContentType.Application.Json)
                contentType(ContentType.Application.Json)
            }.body()
        }
    }
}
