package no.nav.etterlatte.statistikk.clients

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import no.nav.etterlatte.libs.common.beregning.BeregningDTO
import no.nav.etterlatte.statistikk.domain.Beregning
import java.util.*

interface BeregningKlient {
    suspend fun hentBeregningForBehandling(behandlingId: UUID): Beregning
}

class BeregningKlientImpl(
    private val beregningHttpClient: HttpClient,
    private val beregningUrl: String
) : BeregningKlient {
    override suspend fun hentBeregningForBehandling(behandlingId: UUID): Beregning {
        return try {
            beregningHttpClient.get("$beregningUrl/api/beregning/$behandlingId")
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