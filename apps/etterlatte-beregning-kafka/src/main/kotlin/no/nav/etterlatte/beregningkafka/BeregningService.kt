package no.nav.etterlatte.beregningkafka

import io.ktor.client.HttpClient
import io.ktor.client.request.post
import io.ktor.client.statement.HttpResponse
import kotlinx.coroutines.runBlocking
import java.util.*

class BeregningService(
    private val beregningApp: HttpClient,
    private val url: String
) {
    fun beregn(behandlingId: UUID): HttpResponse = runBlocking {
        beregningApp.post("$url/api/beregning/$behandlingId")
    }

    fun opprettBeregningsGrunnlag(omregningsId: UUID, forrigeBehandlingsId: UUID): HttpResponse = runBlocking {
        beregningApp.post("$url/api/beregning/beregningsgrunnlag/$omregningsId/fra/$forrigeBehandlingsId") {}
    }

    fun regulerAvkorting(behandlingId: UUID, forrigeBehandlingId: UUID): HttpResponse = runBlocking {
        beregningApp.post("$url/api/beregning/avkorting/$behandlingId/med/$forrigeBehandlingId") {}
    }
}