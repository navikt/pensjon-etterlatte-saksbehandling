package no.nav.etterlatte.brev.adresse

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.github.benmanes.caffeine.cache.Caffeine
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import no.nav.etterlatte.libs.common.logging.getXCorrelationId
import org.slf4j.LoggerFactory
import java.time.Duration

class RegoppslagKlient(
    private val client: HttpClient,
    private val url: String,
) {
    private val logger = LoggerFactory.getLogger(RegoppslagKlient::class.java)

    private val cache = Caffeine.newBuilder()
        .expireAfterWrite(Duration.ofMinutes(15))
        .build<String, RegoppslagResponseDTO>()

    suspend fun hentMottakerAdresse(ident: String): RegoppslagResponseDTO = try {
        val regoppslagCache = cache.getIfPresent(ident)

        if (regoppslagCache != null) {
            logger.info("Fant cachet mottakeradresse")
            regoppslagCache
        } else {
            logger.info("Ingen cachet mottakeradresse funnet. Henter fra regoppslag")

            client.get("$url/regoppslag/${ident}") {
                header("x_correlation_id", getXCorrelationId())
                header("Nav_Call_Id", getXCorrelationId())
            }.body()
        }
    } catch (exception: Exception) {
        throw AdresseException("Feil i kall mot Regoppslag", exception)
    }
}

@JsonIgnoreProperties(ignoreUnknown = true)
data class RegoppslagResponseDTO(
    val navn: String,
    val adresse: Adresse
) {
    data class Adresse(
        val type: AdresseType,
        val adresseKilde: AdresseKilde?,
        val adresselinje1: String,
        val adresselinje2: String?,
        val adresselinje3: String?,
        val postnummer: String?,
        val poststed: String?,
        val landkode: String,
        val land: String
    )

    enum class AdresseType {
        NORSKPOSTADRESSE, UTENLANDSKPOSTADRESSE
    }

    enum class AdresseKilde {
        BOSTEDSADRESSE, OPPHOLDSADRESSE, KONTAKTADRESSE, DELTBOSTED, KONTAKTINFORMASJONFORDØDSBO, ENHETPOSTADRESSE, ENHETFORRETNINGSADRESSE
    }
}

open class AdresseException(msg: String, cause: Throwable) : Exception(msg, cause)
