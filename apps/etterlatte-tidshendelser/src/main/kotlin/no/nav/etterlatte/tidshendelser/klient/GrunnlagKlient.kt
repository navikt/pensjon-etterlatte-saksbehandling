package no.nav.etterlatte.tidshendelser.klient

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.accept
import io.ktor.client.request.get
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.libs.common.sak.SakId
import java.time.YearMonth

class GrunnlagKlient(
    private val grunnlagHttpClient: HttpClient,
    private val grunnlagUrl: String,
) {
    fun hentSaker(foedselsmaaned: YearMonth): List<SakId> =
        runBlocking {
            grunnlagHttpClient
                .get("$grunnlagUrl/api/grunnlag/aldersovergang/$foedselsmaaned") {
                    accept(ContentType.Application.Json)
                    contentType(ContentType.Application.Json)
                }.body()
        }

    fun hentSakerForDoedsfall(doedsfallsmaaned: YearMonth): List<SakId> =
        runBlocking {
            grunnlagHttpClient
                .get("$grunnlagUrl/api/grunnlag/doedsdato/$doedsfallsmaaned") {
                    accept(ContentType.Application.Json)
                    contentType(ContentType.Application.Json)
                }.body()
        }
}
