package no.nav.etterlatte.statistikk.clients

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import no.nav.etterlatte.libs.common.beregning.AvkortingDto
import no.nav.etterlatte.libs.common.beregning.BeregningDTO
import no.nav.etterlatte.statistikk.domain.Avkorting
import no.nav.etterlatte.statistikk.domain.Beregning
import org.slf4j.LoggerFactory
import java.util.*

interface BeregningKlient {
    suspend fun hentBeregningForBehandling(behandlingId: UUID): Beregning?
    suspend fun hentAvkortingForBehandling(behandlingId: UUID): Avkorting?
}

class BeregningKlientImpl(
    private val beregningHttpClient: HttpClient,
    private val beregningUrl: String
) : BeregningKlient {

    val logger = LoggerFactory.getLogger(this.javaClass)
    override suspend fun hentBeregningForBehandling(behandlingId: UUID): Beregning? {
        return try {
            beregningHttpClient.get("$beregningUrl/api/beregning/$behandlingId")
                .body<BeregningDTO>()
                .let { Beregning.fraBeregningDTO(it) }
        } catch (e: Exception) {
            logger.warn(
                "Kunne ikke hente beregningen for behandlingen med id=$behandlingId " +
                    "fra beregning"
            )
            null
        }
    }

    override suspend fun hentAvkortingForBehandling(behandlingId: UUID): Avkorting? {
        return try {
            beregningHttpClient.get("$beregningUrl/api/beregning/avkorting/$behandlingId")
                .body<AvkortingDto>()
                .let { Avkorting.fraDTO(it) }
        } catch (e: Exception) {
            logger.warn(
                "Kunne ikke hente avkorting for behandlingen med id=$behandlingId " +
                        "fra beregning"
            )
            null
        }
    }
}

class KunneIkkeHenteFraBeregning(override val message: String, override val cause: Throwable) :
    Exception(message, cause)