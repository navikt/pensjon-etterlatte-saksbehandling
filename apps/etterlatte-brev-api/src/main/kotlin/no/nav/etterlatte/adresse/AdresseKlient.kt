package no.nav.etterlatte.adresse

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.accept
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import no.nav.etterlatte.libs.common.logging.getXCorrelationId
import no.nav.etterlatte.brev.model.Mottaker
import org.slf4j.LoggerFactory

class AdresseKlient(
    private val client: HttpClient,
    private val url: String,
) : AdresseService {
    private val logger = LoggerFactory.getLogger(AdresseService::class.java)

    // api://dev-fss.teamdokumenthandtering.regoppslag/.default

    override suspend fun hentMottakerAdresse(id: String): Mottaker = try {
        logger.info("Henter mottakere fra regoppslag")
        client.post("$url/rest/postadresse") {
            accept(ContentType.Application.Json)
            contentType(ContentType.Application.Json)
            header("Nav-Callid", getXCorrelationId())
            setBody(AdresseRequest(id, "PEN"))
        }.body()

    } catch (exception: Exception) {
        logger.error("Feil i kall mot Regoppslag: ", exception)
        throw AdresseException("Feil i kall mot Regoppslag", exception)
    }
}


open class AdresseException(msg: String, cause: Throwable) : Exception(msg, cause)

data class AdresseRequest(
    val ident: String,
    val tema: String
)
