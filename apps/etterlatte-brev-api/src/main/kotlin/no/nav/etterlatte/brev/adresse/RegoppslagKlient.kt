package no.nav.etterlatte.brev.adresse

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.github.benmanes.caffeine.cache.Caffeine
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.ResponseException
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.feilhaandtering.ForespoerselException
import no.nav.etterlatte.libs.ktor.behandlingsnummer
import no.nav.etterlatte.sikkerLogg
import org.slf4j.LoggerFactory
import java.time.Duration

class RegoppslagKlient(
    private val client: HttpClient,
    private val url: String,
) {
    private val logger = LoggerFactory.getLogger(RegoppslagKlient::class.java)

    private val cache =
        Caffeine
            .newBuilder()
            .expireAfterWrite(Duration.ofMinutes(15))
            .build<String, RegoppslagResponseDTO>()

    suspend fun hentMottakerAdresse(
        sakType: SakType,
        ident: String,
    ): RegoppslagResponseDTO? =
        try {
            val regoppslagCache = cache.getIfPresent(ident)

            if (regoppslagCache != null) {
                logger.info("Fant cachet mottakeradresse")
                regoppslagCache
            } else {
                logger.info("Ingen cachet mottakeradresse funnet. Henter fra regoppslag")

                client
                    .post("$url/rest/postadresse") {
                        behandlingsnummer(sakType)
                        contentType(ContentType.Application.Json)
                        setBody(RegoppslagRequest(ident))
                    }.body<RegoppslagResponseDTO>()
                    .also {
                        sikkerLogg.info("Respons fra regoppslag: $it")
                        cache.put(ident, it)
                    }
            }
        } catch (re: ResponseException) {
            if (re.response.status == HttpStatusCode.NotFound) {
                null
            } else if (re.response.status == HttpStatusCode.Gone) {
                logger.warn(re.response.bodyAsText())
                null
            } else {
                logger.error("Uhåndtert feil fra regoppslag: ${re.response.bodyAsText()}")

                throw ForespoerselException(
                    status = re.response.status.value,
                    code = "UKJENT_FEIL_REGOPPSLAG",
                    detail = "Ukjent feil oppsto ved uthenting av mottakers adresse fra regoppslag",
                )
            }
        }
}

data class RegoppslagRequest(
    val ident: String,
    // Todo: mulig bytte tema til et av de nye
    val tema: String = "PEN",
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class RegoppslagResponseDTO(
    val navn: String,
    val adresse: Adresse,
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
        val land: String,
    )

    enum class AdresseType {
        NORSKPOSTADRESSE,
        UTENLANDSKPOSTADRESSE,
    }

    enum class AdresseKilde {
        BOSTEDSADRESSE,
        OPPHOLDSADRESSE,
        KONTAKTADRESSE,
        DELTBOSTED,
        KONTAKTINFORMASJONFORDØDSBO,
        ENHETPOSTADRESSE,
        ENHETFORRETNINGSADRESSE,
    }
}
