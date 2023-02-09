package no.nav.etterlatte.statistikk.clients

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import no.nav.etterlatte.libs.common.beregning.BeregningDTO
import no.nav.etterlatte.statistikk.domain.Beregning
import java.util.*

interface BeregningClient {
    suspend fun hentBeregningForVedtak(behandlingId: UUID): Beregning
}

class BeregningClientImpl(private val beregningHttpClient: HttpClient) : BeregningClient {
    override suspend fun hentBeregningForVedtak(behandlingId: UUID): Beregning {
        return try {
            beregningHttpClient.get("api/beregning/$behandlingId") // TODO fix url her
                .body<BeregningDTO>()
                .let { Beregning.fraBeregningDTO(it) }
        } catch (e: Exception) {
            throw KunneIkkeHenteFraBeregning(
                "Kunne ikke hente beregningen for behandlingen med id=$behandlingId " +
                    "fra beregning",
                e
            )
        }
    }
}

class KunneIkkeHenteFraBeregning(override val message: String, override val cause: Throwable) :
    Exception(message, cause)