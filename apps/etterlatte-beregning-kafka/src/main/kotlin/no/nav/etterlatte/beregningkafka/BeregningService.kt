package no.nav.etterlatte.beregningkafka

import io.ktor.client.HttpClient
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.beregning.grunnlag.LagreBeregningsGrunnlag
import java.util.UUID

class BeregningService(
    private val beregningApp: HttpClient,
    private val url: String,
) {
    fun beregn(behandlingId: UUID): HttpResponse =
        runBlocking {
            beregningApp.post("$url/api/beregning/$behandlingId")
        }

    fun opprettBeregningsgrunnlagFraForrigeBehandling(
        omregningsId: UUID,
        forrigeBehandlingId: UUID,
    ): HttpResponse =
        runBlocking {
            beregningApp.post("$url/api/beregning/beregningsgrunnlag/$omregningsId/fra/$forrigeBehandlingId")
        }

    fun opprettBeregningsgrunnlag(
        behandlingId: UUID,
        request: LagreBeregningsGrunnlag,
    ) = runBlocking {
        beregningApp.post("$url/api/beregning/beregningsgrunnlag/$behandlingId/barnepensjon") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }
    }

    fun regulerAvkorting(
        behandlingId: UUID,
        forrigeBehandlingId: UUID,
    ): HttpResponse =
        runBlocking {
            beregningApp.post("$url/api/beregning/avkorting/$behandlingId/med/$forrigeBehandlingId")
        }
}
