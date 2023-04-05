package no.nav.etterlatte.brev.adresse

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.github.benmanes.caffeine.cache.Caffeine
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.http.HttpStatusCode
import io.ktor.http.isSuccess
import no.nav.etterlatte.libs.common.logging.X_CORRELATION_ID
import no.nav.etterlatte.libs.common.logging.getXCorrelationId
import org.slf4j.LoggerFactory
import java.time.Duration

class Norg2Klient(
    private val apiUrl: String,
    private val klient: HttpClient
) {

    private val logger = LoggerFactory.getLogger(Norg2Klient::class.java)

    private val cache = Caffeine.newBuilder()
        .expireAfterWrite(Duration.ofDays(1))
        .build<String, Norg2Enhet>()

    suspend fun hentEnhet(enhet: String): Norg2Enhet {
        return try {
            val enhetCache = cache.getIfPresent(enhet)

            if (enhetCache != null) {
                logger.info("Fant cachet enhet for enhet $enhet")
                return enhetCache
            }

            val response = klient.get("$apiUrl/enhet/$enhet") {
                header(X_CORRELATION_ID, getXCorrelationId())
            }

            if (response.status.isSuccess()) {
                logger.info("Hentet enhet fra Norg2 for enhet $enhet")

                response.body<Norg2Enhet>()
                    .apply { kontaktinfo = hentKontaktinformasjon(enhet) }
                    .also { cache.put(enhet, it) }
            } else {
                val err = response.body<Norg2Error>()

                throw Exception(err.message)
            }
        } catch (e: Exception) {
            throw Exception("Feil ved uthenting av enhet $enhet", e)
        }
    }

    private suspend fun hentKontaktinformasjon(enhet: String): Norg2Kontaktinfo {
        val response = klient.get("$apiUrl/enhet/$enhet/kontaktinformasjon")

        return if (response.status == HttpStatusCode.OK) {
            response.body()
        } else {
            val err = response.body<Norg2Error>()

            throw Exception(err.message)
        }
    }
}

@JsonIgnoreProperties(ignoreUnknown = true)
data class Norg2Enhet(
    val navn: String? = null,
    val enhetNr: String? = null,
    val status: String? = null,
    val type: String? = null,
    var kontaktinfo: Norg2Kontaktinfo? = null
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class Norg2Kontaktinfo(
    val telefonnummer: String? = null,
    val telefonnummerKommentar: String? = null,
    val faksnummer: String? = null,
    val epost: String? = null,
    val postadresse: Postadresse? = null
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class Postadresse(
    val type: String,
    val postnummer: String? = null,
    val poststed: String? = null,
    val postboksnummer: String? = null,
    val postboksanlegg: String? = null,
    val gatenavn: String? = null,
    val husnummer: String? = null,
    val husbokstav: String? = null,
    val adresseTilleggsnavn: String? = null
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class Norg2Error(
    val field: String? = null,
    val message: String? = null
)