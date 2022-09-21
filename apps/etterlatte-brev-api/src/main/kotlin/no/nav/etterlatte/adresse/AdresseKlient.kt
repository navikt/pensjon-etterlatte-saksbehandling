package no.nav.etterlatte.adresse

import com.github.michaelbull.result.get
import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.accept
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import no.nav.etterlatte.StsClient
import no.nav.etterlatte.libs.common.logging.getXCorrelationId
import no.nav.etterlatte.brev.model.Mottaker
import no.nav.etterlatte.libs.ktorobo.AzureAdClient
import org.slf4j.LoggerFactory

class AdresseKlient(
    private val client: HttpClient,
    private val url: String,
    private val stsClient: StsClient
) : AdresseService {
    private val logger = LoggerFactory.getLogger(AdresseService::class.java)
    val configLocation: String? = null
    private val config: Config = configLocation?.let { ConfigFactory.load(it) } ?: ConfigFactory.load()
    val azureAdClient = AzureAdClient(config, client)

    // api://dev-fss.teamdokumenthandtering.regoppslag/.default

    override suspend fun hentMottakerAdresse(id: String): Mottaker = try {
        logger.info("Henter mottakere fra regoppslag")
        client.post("$url/rest/postadresse") {
            accept(ContentType.Application.Json)
            contentType(ContentType.Application.Json)
            header("Nav-Callid", getXCorrelationId())
            header("Authorization", "Bearer ${stsClient.getToken()}")
            setBody(AdresseRequest(id, "PEN"))
        }.body()

    } catch (exception: Exception) {
        logger.error("Feil i kall mot Regoppslag: ", exception)
        throw AdresseException("Feil i kall mot Regoppslag", exception)
    }

    private suspend fun getToken(accessToken: String): String {
        val token = azureAdClient.getOnBehalfOfAccessTokenForResource(
            listOf("api://dev-fss.teamdokumenthandtering.regoppslag/.default"),
            accessToken
        )
        return token.get()?.accessToken ?: ""
    }
}

open class AdresseException(msg: String, cause: Throwable) : Exception(msg, cause)

data class AdresseRequest(
    val ident: String,
    val tema: String
)
