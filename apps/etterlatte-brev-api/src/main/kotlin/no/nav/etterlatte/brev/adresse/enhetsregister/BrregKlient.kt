package no.nav.etterlatte.brev.adresse.enhetsregister

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.ResponseException
import io.ktor.client.request.accept
import io.ktor.client.request.get
import io.ktor.http.ContentType
import io.ktor.http.isSuccess
import org.slf4j.LoggerFactory

class BrregKlient(private val httpClient: HttpClient, private val host: String) {
    private val logger = LoggerFactory.getLogger(BrregService::class.java)

    suspend fun hentEnheter(): List<Enhet> {
        try {
            val response = httpClient.get("$host/enhetsregisteret/api/enheter") {
                accept(ContentType.Application.Json)
                url {
                    parameters.append("navn", "statsforvalteren")
                    // kode 6100 = "Statsforvaltningen"
                    parameters.append("institusjonellSektorkode", "6100")
                }
            }

            return if (response.status.isSuccess()) {
                val wrapper = response.body<ResponseWrapper>()
                wrapper._embedded?.enheter ?: emptyList()
            } else {
                val feilmelding = response.body<Feilmelding>()
                throw ResponseException(response, feilmelding.feilmelding ?: "Feil ved uthenting av statsforvaltere.")
            }
        } catch (e: Exception) {
            logger.error("Ukjent feil ved uthenting av statsforvaltere:", e)

            throw e
        }
    }
}

class ResponseWrapper(val _embedded: Embedded?) {
    class Embedded(val enheter: List<Enhet>)
}

@JsonIgnoreProperties(ignoreUnknown = true)
data class Enhet(
    val organisasjonsnummer: String,
    val navn: String
)

data class Feilmelding(
    val status: Int,
    val feilmelding: String?,
    val valideringsfeil: List<Feil>
) {
    data class Feil(
        val feilmelding: String?,
        val parametere: List<String>,
        val feilaktigVerdi: String?
    )
}