package no.nav.etterlatte.adresse

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import no.nav.etterlatte.libs.common.logging.getXCorrelationId
import no.nav.etterlatte.brev.model.RegoppslagResponseDTO
import org.slf4j.LoggerFactory

class AdresseKlient(
    private val client: HttpClient,
    private val url: String,
) : AdresseService {
    private val logger = LoggerFactory.getLogger(AdresseService::class.java)

    override suspend fun hentMottakerAdresse(ident: String): RegoppslagResponseDTO = try {
        logger.info("Henter mottakere fra regoppslag")
        client.get("$url/regoppslag/${ident}") {
            header("x_correlation_id", getXCorrelationId())
            header("Nav_Call_Id", getXCorrelationId())
        }.body()

    } catch (exception: Exception) {
        throw AdresseException("Feil i kall mot Regoppslag", exception)
    }

}

open class AdresseException(msg: String, cause: Throwable) : Exception(msg, cause)
