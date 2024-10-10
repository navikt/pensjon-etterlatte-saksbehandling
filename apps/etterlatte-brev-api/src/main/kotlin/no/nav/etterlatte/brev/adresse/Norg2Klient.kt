package no.nav.etterlatte.brev.adresse

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.github.benmanes.caffeine.cache.Caffeine
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode
import io.ktor.http.isSuccess
import no.nav.etterlatte.libs.common.Enhetsnummer
import org.slf4j.LoggerFactory
import java.time.Duration

class Norg2Klient(
    val apiUrl: String,
    val klient: HttpClient,
) {
    val logger = LoggerFactory.getLogger(Norg2Klient::class.java)

    val cache =
        Caffeine
            .newBuilder()
            .expireAfterWrite(Duration.ofDays(1))
            .build<Enhetsnummer, Norg2Enhet>()

    suspend fun hentEnhet(enhet: Enhetsnummer): Norg2Enhet {
        return try {
            val enhetCache = cache.getIfPresent(enhet)

            if (enhetCache != null) {
                logger.info("Fant cachet enhet for enhet $enhet")
                return enhetCache
            }

            val response = klient.get("$apiUrl/enhet/${enhet.enhetNr}")

            if (response.status.isSuccess()) {
                logger.info("Hentet enhet fra Norg2 for enhet $enhet")

                response
                    .body<Norg2Enhet>()
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

    private suspend fun hentKontaktinformasjon(enhet: Enhetsnummer): Norg2Kontaktinfo {
        val response = klient.get("$apiUrl/enhet/${enhet.enhetNr}/kontaktinformasjon")

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
    val enhetNr: Enhetsnummer? = null,
    val status: String? = null,
    val type: String? = null,
    var kontaktinfo: Norg2Kontaktinfo? = null,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class Norg2Kontaktinfo(
    val telefonnummer: String? = null,
    val telefonnummerKommentar: String? = null,
    val faksnummer: String? = null,
    val epost: Norg2Epost? = null,
    val postadresse: Postadresse? = null,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class Norg2Epost(
    val adresse: String? = null,
    val kommentar: String? = null,
    val kunIntern: Boolean = false,
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
    val adresseTilleggsnavn: String? = null,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class Norg2Error(
    val field: String? = null,
    val message: String? = null,
)
