package no.nav.etterlatte.adresse

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import no.nav.etterlatte.libs.common.logging.getXCorrelationId
import no.nav.etterlatte.brev.model.RegoppslagResponseDTO
import no.nav.etterlatte.libs.common.person.Foedselsnummer
import org.slf4j.LoggerFactory

class AdresseKlient(
    private val client: HttpClient,
    private val url: String,
) : AdresseService {
    private val logger = LoggerFactory.getLogger(AdresseService::class.java)

    // api://dev-fss.teamdokumenthandtering.regoppslag/.default

    override suspend fun hentMottakerAdresse(id: Foedselsnummer): RegoppslagResponseDTO = try {
        logger.info("Henter mottakere fra regoppslag: ${id.toString()}")
        client.get("$url/regoppslag/${id.value}") {
            header("x_correlation_id", getXCorrelationId())
            // setBody(TextContent(AdresseRequest(id).toJson(), ContentType.Application.Json))
        }.body()

    } catch (exception: Exception) {
        logger.error("Feil i kall mot Regoppslag: ", exception)
        throw AdresseException("Feil i kall mot Regoppslag", exception)
    }

}

open class AdresseException(msg: String, cause: Throwable) : Exception(msg, cause)
