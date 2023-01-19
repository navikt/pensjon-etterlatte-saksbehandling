package no.nav.etterlatte.fordeler.digdirkrr

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.accept
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import no.nav.etterlatte.libs.common.logging.getCorrelationId
import no.nav.etterlatte.libs.common.person.Foedselsnummer
import org.slf4j.LoggerFactory

class KontaktinfoKlient(private val client: HttpClient, private val apiUrl: String) {
    private val logger = LoggerFactory.getLogger(this.javaClass.name)

    suspend fun hentSpraak(foedselsnummer: Foedselsnummer): KontaktInfo {
        val httpResponse = client.get("$apiUrl/rest/v1/person") {
            accept(ContentType.Application.Json)
            contentType(ContentType.Application.Json)
            header("Nav-Call-Id", getCorrelationId())
            header("Nav-Personident", foedselsnummer.value)
        }
        val httpCode = httpResponse.status
        if (httpCode.value in 200..299) {
            return httpResponse.body()
        }
        if (httpCode == HttpStatusCode.NotFound) {
            logger.error("Person $foedselsnummer ble ikke funnet i PDL")
            throw PersonFinnesIkkeIPdlException("Person $foedselsnummer ble ikke funnet i PDL")
        }
        if (httpCode == HttpStatusCode.Forbidden) {
            logger.error("Systemet har ikke tilgang til person $foedselsnummer")
            throw IkkeTilgangTilPersonException("Systemet har ikke tilgang til person $foedselsnummer")
        }
        logger.error("Ukjent feil i fordeler ved oppslag mot kontaktinfo")
        throw RuntimeException("Ukjent feil i oppslag mot kontaktinfo for $foedselsnummer")
    }
}

class PersonFinnesIkkeIPdlException(msg: String) : RuntimeException(msg)
class IkkeTilgangTilPersonException(msg: String) : RuntimeException(msg)

@JsonIgnoreProperties(ignoreUnknown = true)
data class KontaktInfo(
    @JsonProperty("spraak")
    val spraak: String?
)