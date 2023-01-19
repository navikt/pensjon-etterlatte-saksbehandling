package no.nav.etterlatte.fordeler.digdirkrr

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.accept
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.http.ContentType
import io.ktor.http.contentType
import no.nav.etterlatte.libs.common.logging.getCorrelationId
import no.nav.etterlatte.libs.common.person.Foedselsnummer
import org.slf4j.LoggerFactory

class KontaktinfoKlient(private val client: HttpClient, private val apiUrl: String) {
    private val logger = LoggerFactory.getLogger(this.javaClass.name)

    // TODO: må håndtere 404 hvis personen ikke finnes i PDL eller 403 om appen ikke har tilgang til vedkommende
    suspend fun hentSpraak(foedselsnummer: Foedselsnummer): KontaktInfo {
        return client.get("$apiUrl/rest/v1/person") {
            accept(ContentType.Application.Json)
            contentType(ContentType.Application.Json)
            header("Nav-Call-Id", getCorrelationId())
            header("Nav-Personident", foedselsnummer.value)
        }.body()
    }
}

@JsonIgnoreProperties(ignoreUnknown = true)
data class KontaktInfo(
    @JsonProperty("spraak")
    val spraak: String?
)