package no.nav.etterlatte.beregningkafka

import io.ktor.client.HttpClient
import io.ktor.client.request.post
import io.ktor.client.statement.HttpResponse
import kotlinx.coroutines.runBlocking
import java.util.UUID

class BeregningService(
    private val behandlingApp: HttpClient,
    private val url: String
) {
    fun opprettOmregning(omregningsId: UUID): HttpResponse = runBlocking {
        behandlingApp.post("$url/api/beregning/$omregningsId") {}
    }

    fun opprettBeregningsGrunnlag(omregningsId: UUID, forrigeBehandlingsId: UUID): HttpResponse = runBlocking {
        behandlingApp.post("$url/api/beregning/beregningsgrunnlag/$omregningsId/fra/$forrigeBehandlingsId") {}
    }

    fun regulerAvkorting(behandlingId: UUID, forrigeBehandlingId: UUID): HttpResponse = runBlocking {
        behandlingApp.post("$url/api/beregning/avkorting/$behandlingId/med/$forrigeBehandlingId") {}
    }
}