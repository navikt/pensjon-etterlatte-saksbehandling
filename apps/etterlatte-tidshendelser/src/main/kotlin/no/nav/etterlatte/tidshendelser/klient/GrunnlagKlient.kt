package no.nav.etterlatte.tidshendelser.klient

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.accept
import io.ktor.client.request.get
import io.ktor.http.ContentType
import io.ktor.http.contentType
import java.time.YearMonth

class GrunnlagKlient(
    private val grunnlagHttpClient: HttpClient,
    private val grunnlagUrl: String,
) {
    suspend fun hentSakerForBrukereFoedtIMaaned(maaned: YearMonth): List<Long> {
        return grunnlagHttpClient.get("$grunnlagUrl/grunnlag/aldersovergang/$maaned") {
            accept(ContentType.Application.Json)
            contentType(ContentType.Application.Json)
        }.body()
    }
}
