package migrering.pen

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.http.isSuccess
import org.slf4j.LoggerFactory

class PenKlient(
    private val pen: HttpClient,
    private val resourceUrl: String
) {
    private val logger = LoggerFactory.getLogger(this::class.java)
    suspend fun hentSak(
        sakid: Long
    ): PenDTO {
        logger.info("Henter sak $sakid fra PEN")
        val sak = pen.get("$resourceUrl/barnepensjon-migrering/grunnlag/$sakid")
        return if (sak.status.isSuccess()) {
            sak.body<PenDTO>()
        } else {
            throw RuntimeException("Kunne ikke hente sak $sakid fra PEN")
        }
    }
}

data class PenDTO(val sakid: Long)