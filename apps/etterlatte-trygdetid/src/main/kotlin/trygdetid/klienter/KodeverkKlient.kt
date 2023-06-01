package no.nav.etterlatte.trygdetid.klienter

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.typesafe.config.Config
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.accept
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.http.ContentType
import org.slf4j.LoggerFactory
import java.util.*

class KodeverkKlient(config: Config, private val httpKlient: HttpClient) {
    private val logger = LoggerFactory.getLogger(KodeverkKlient::class.java)
    private val url = config.getString("kodeverk.resource.url")

    suspend fun hentLandkoder(): KodeverkResponse = try {
        logger.info("Henter alle landkoder fra Kodeverk")

        httpKlient.get("$url/Landkoder/koder/betydninger?ekskluderUgyldige=false&spraak=nb") {
            accept(ContentType.Application.Json)
            header("Nav-Consumer-Id", "etterlatte-trygdetid")
            header("Nav-Call-Id", UUID.randomUUID())
        }.body()
    } catch (e: Exception) {
        logger.error("Henting av landkoder feilet", e)
        throw e
    }
}

@JsonIgnoreProperties(ignoreUnknown = true)
data class KodeverkResponse(val betydninger: Map<String, List<Betydning>>)

data class Beskrivelse(val term: String, val tekst: String)
data class Betydning(val gyldigFra: String, val gyldigTil: String, val beskrivelser: Map<String, Beskrivelse>)

data class BetydningMedIsoKode(
    val gyldigFra: String,
    val gyldigTil: String,
    val beskrivelser: Map<String, Beskrivelse>,
    val isolandkode: String
)