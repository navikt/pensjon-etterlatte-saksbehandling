package no.nav.etterlatte.beregningkafka

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.grunnbeloep.Grunnbeloep
import no.nav.etterlatte.libs.common.beregning.AarligInntektsjusteringAvkortingRequest
import no.nav.etterlatte.libs.common.feilhaandtering.checkInternFeil
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

    fun haandterTidligAlderpensjon(behandlingId: UUID): HttpResponse =
        runBlocking {
            beregningApp.post("$url/api/beregning/avkorting/$behandlingId/haandter-tidlig-alderspensjon")
        }

    fun omregnAvkorting(
        behandlingId: UUID,
        forrigeBehandlingId: UUID,
    ): HttpResponse =
        runBlocking {
            beregningApp.post("$url/api/beregning/avkorting/$behandlingId/med/$forrigeBehandlingId")
        }

    fun hentAvkorting(behandlingId: UUID) =
        runBlocking {
            beregningApp.get("$url/api/beregning/avkorting/$behandlingId/ferdig")
        }

    fun omregnAarligInntektsjustering(
        aar: Int,
        behandlingId: UUID,
        forrigeBehandlingId: UUID,
    ): HttpResponse =
        runBlocking {
            beregningApp.post("$url/api/beregning/avkorting/aarlig-inntektsjustering") {
                contentType(ContentType.Application.Json)
                setBody(
                    AarligInntektsjusteringAvkortingRequest(
                        aar = aar,
                        nyBehandling = behandlingId,
                        forrigeBehandling = forrigeBehandlingId,
                    ),
                )
            }
        }

    suspend fun hentGrunnbeloep(): Grunnbeloep =
        beregningApp
            .get("$url/api/beregning/grunnbeloep")
            .also { checkInternFeil(it.status.isSuccess()) { "Kunne ikke hente grunnbeloep" } }
            .body<Grunnbeloep>()
}
