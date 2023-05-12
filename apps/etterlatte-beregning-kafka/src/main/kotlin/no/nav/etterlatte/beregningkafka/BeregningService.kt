package no.nav.etterlatte.beregningkafka

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.libs.common.beregning.BeregningDTO
import java.util.*

class BeregningService(
    private val beregningApp: HttpClient,
    private val url: String
) {

    fun opprettBeregningsGrunnlag(omregningsId: UUID, forrigeBehandlingsId: UUID): HttpResponse = runBlocking {
        beregningApp.post("$url/api/beregning/beregningsgrunnlag/$omregningsId/fra/$forrigeBehandlingsId")
    }

    fun beregn(behandlingId: UUID): BeregningDTO = runBlocking {
        beregningApp.post("$url/api/beregning/$behandlingId") {
            contentType(ContentType.Application.Json)
        }.body()
    }
}