package no.nav.etterlatte.beregningkafka

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.statement.HttpResponse
import io.ktor.http.isSuccess
import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.grunnbeloep.Grunnbeloep
import java.util.UUID

class BeregningService(
    private val beregningApp: HttpClient,
    private val url: String,
) {
    fun hentOverstyrt(behandlingId: UUID): HttpResponse =
        runBlocking {
            beregningApp.get("$url/api/beregning/$behandlingId/overstyrt")
        }

    fun beregn(behandlingId: UUID): HttpResponse =
        runBlocking {
            beregningApp.post("$url/api/beregning/$behandlingId")
        }

    fun hentBeregning(behandlingId: UUID): HttpResponse =
        runBlocking {
            beregningApp.get("$url/api/beregning/$behandlingId")
        }

    fun opprettBeregningsgrunnlagFraForrigeBehandling(
        omregningsId: UUID,
        forrigeBehandlingId: UUID,
    ): HttpResponse =
        runBlocking {
            beregningApp.post("$url/api/beregning/beregningsgrunnlag/$omregningsId/fra/$forrigeBehandlingId")
        }

    fun tilpassOverstyrtBeregningsgrunnlagForRegulering(omregningsId: UUID): HttpResponse =
        runBlocking {
            beregningApp.put("$url/api/beregning/beregningsgrunnlag/$omregningsId/overstyr/regulering")
        }

    fun regulerAvkorting(
        behandlingId: UUID,
        forrigeBehandlingId: UUID,
    ): HttpResponse =
        runBlocking {
            beregningApp.post("$url/api/beregning/avkorting/$behandlingId/med/$forrigeBehandlingId")
        }

    suspend fun hentGrunnbeloep(): Grunnbeloep =
        beregningApp
            .get("$url/api/beregning/grunnbeloep")
            .also { require(it.status.isSuccess()) }
            .body<Grunnbeloep>()
}
