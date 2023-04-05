package no.nav.etterlatte.brev.adresse

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.github.benmanes.caffeine.cache.Caffeine
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.ResponseException
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.http.isSuccess
import no.nav.etterlatte.libs.common.logging.X_CORRELATION_ID
import no.nav.etterlatte.libs.common.logging.getXCorrelationId
import no.nav.etterlatte.sikkerLogg
import org.slf4j.LoggerFactory
import java.time.Duration

class RegoppslagKlient(
    private val client: HttpClient,
    private val url: String
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

            val response = client.get("$url/regoppslag/$ident") {
                header(X_CORRELATION_ID, getXCorrelationId())
                header("Nav_Call_Id", getXCorrelationId())
            }

            if (response.status.isSuccess()) {
                response.body<RegoppslagResponseDTO>()
                    .also {
                        sikkerLogg.info("Respons fra regoppslag: $it")
                        cache.put(ident, it)
                    }
            } else {
                throw ResponseException(response, "Ukjent feil fra navansatt api")
            }
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
        BOSTEDSADRESSE, OPPHOLDSADRESSE, KONTAKTADRESSE, DELTBOSTED,
        KONTAKTINFORMASJONFORDØDSBO, ENHETPOSTADRESSE, ENHETFORRETNINGSADRESSE
    }
}

open class AdresseException(msg: String, cause: Throwable) : Exception(msg, cause)