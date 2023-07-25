package no.nav.etterlatte.common.klienter

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.accept
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import no.nav.etterlatte.libs.ktor.PingResult
import no.nav.etterlatte.libs.ktor.PingResultDown
import no.nav.etterlatte.libs.ktor.PingResultUp
import no.nav.etterlatte.libs.ktor.Pingable
import org.slf4j.LoggerFactory

class SkjermingKlient(
    private val httpClient: HttpClient,
    private val url: String
) : Pingable {
    val logger = LoggerFactory.getLogger(this::class.java)

    suspend fun personErSkjermet(fnr: String): Boolean {
        return httpClient.post("$url/skjermet") {
            accept(ContentType.Application.Json)
            contentType(ContentType.Application.Json)
            setBody(SkjermetDataRequestDTO(personident = fnr))
        }.body()
    }

    override suspend fun ping(): PingResult {
        try {
            val skjermetFalse: Boolean = httpClient.post("$url/skjermet") {
                accept(ContentType.Application.Json)
                contentType(ContentType.Application.Json)
                setBody(SkjermetDataRequestDTO(personident = "dummy")) // Det er meningen å sende inn "dummy"
            }.body()
        } catch (e: Exception) {
            return PingResultDown(serviceName, endpoint = endpoint, errorMessage = e.message, beskrivelse = beskrivelse)
                .also {
                    logger.warn("Skjermingstjeneste svarer IKKE ok. ${it.toStringServiceDown()}")
                }
        }
        logger.info("Skjermingstjeneste svarer OK")
        return PingResultUp(serviceName, endpoint = endpoint, beskrivelse = beskrivelse)
    }

    override val serviceName: String
        get() = "SkjermingKlient"
    override val beskrivelse: String
        get() = "Sjekker om en person er skjermet"
    override val endpoint: String
        get() = this.url
}

data class SkjermetDataRequestDTO(
    val personident: String
)