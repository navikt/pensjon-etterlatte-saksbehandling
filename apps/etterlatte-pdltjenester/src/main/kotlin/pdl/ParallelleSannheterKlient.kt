package no.nav.etterlatte.pdl

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.readValue
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.accept
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType.Application.Json
import io.ktor.http.content.TextContent
import no.nav.etterlatte.libs.common.RetryResult
import no.nav.etterlatte.libs.common.logging.samleExceptions
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.retry
import no.nav.etterlatte.libs.common.toJson
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class ParallelleSannheterException(override val message: String) : RuntimeException(message)

class ParallelleSannheterKlient(val httpClient: HttpClient, val apiUrl: String) {

    suspend fun avklarNavn(pdlNavn: List<PdlNavn>) = avklar(pdlNavn, Avklaring.NAVN)

    suspend fun avklarAdressebeskyttelse(adressebeskyttelse: List<PdlAdressebeskyttelse>) =
        avklarNullable(adressebeskyttelse, Avklaring.ADRESSEBESKYTTELSE)

    suspend fun avklarStatsborgerskap(pdlStatsborgerskap: List<PdlStatsborgerskap>) =
        avklarNullable(pdlStatsborgerskap, Avklaring.STATSBORGERSKAP)

    suspend fun avklarSivilstand(pdlSivilstand: List<PdlSivilstand>) =
        avklarNullable(pdlSivilstand, Avklaring.SIVILSTAND)

    suspend fun avklarFoedsel(pdlFoedsel: List<PdlFoedsel>) = avklar(pdlFoedsel, Avklaring.FOEDSEL)

    suspend fun avklarDoedsfall(pdlDoedsfall: List<PdlDoedsfall>) = avklarNullable(pdlDoedsfall, Avklaring.DOEDSFALL)

    suspend fun avklarBostedsadresse(pdlBostedsadresse: List<PdlBostedsadresse>) =
        avklarNullable(pdlBostedsadresse, Avklaring.BOSTEDSADRESSE)

    // TODO PPS skal implementere delt bostedsadresse - bruker samme endepunkt som bostedsadresse inntil videre
    suspend fun avklarDeltBostedsadresse(pdlDeltBostedsadresse: List<PdlDeltBostedsadresse>) =
        avklarNullable(pdlDeltBostedsadresse, Avklaring.BOSTEDSADRESSE)

    suspend fun avklarKontaktadresse(pdlKontaktadresse: List<PdlKontaktadresse>) =
        avklarNullable(pdlKontaktadresse, Avklaring.KONTAKTADRESSE)

    suspend fun avklarOppholdsadresse(pdlOppholdsadresse: List<PdlOppholdsadresse>) =
        avklarNullable(pdlOppholdsadresse, Avklaring.OPPHOLDSADRESSE)

    private suspend inline fun <reified T> avklar(list: List<T>, avklaring: Avklaring): T {
        return avklarNullable(list, avklaring)
            ?: throw ParallelleSannheterException("Forventet verdi for feltet ${avklaring.feltnavn}, men var null")
    }

    private suspend inline fun <reified T> avklarNullable(list: List<T>, avklaring: Avklaring): T? {
        val listAsJsonNode = objectMapper.readValue(list.toJson(), JsonNode::class.java)
        val nodeWithFieldName: JsonNode = objectMapper.createObjectNode().set(avklaring.feltnavn, listAsJsonNode)
        return when (list.size) {
            0 -> null
            1 -> list.first()
            else -> {
                logger.info("Felt av typen ${avklaring.feltnavn} har ${list.size} elementer, sjekker mot PPS")
                val responseAsJsonNode = retry {
                    httpClient.post("$apiUrl/api/${avklaring.feltnavn}") {
                        accept(Json)
                        setBody(TextContent(nodeWithFieldName.toJson(), Json))
                    }.body<JsonNode>()
                }.let {
                    when (it) {
                        is RetryResult.Success -> it.content
                        is RetryResult.Failure -> throw samleExceptions(it.exceptions)
                    }
                }

                // Svar fra parallelle sannheter skal kun inneholde ett element
                if (responseAsJsonNode.size() != 1) {
                    logger.warn("Parallelle sannheter returnerte mer enn et element for felt ${avklaring.feltnavn}")
                }

                objectMapper.readValue(responseAsJsonNode.get(avklaring.feltnavn).first().toJson())
            }
        }
    }

    private enum class Avklaring(val feltnavn: String) {
        NAVN("navn"),
        ADRESSEBESKYTTELSE("adressebeskyttelse"),
        STATSBORGERSKAP("statsborgerskap"),
        SIVILSTAND("sivilstand"),
        FOEDSEL("foedsel"),
        DOEDSFALL("doedsfall"),
        BOSTEDSADRESSE("bostedsadresse"),
        DELTBOSTEDSADRESSE("deltbostedsadresse"),
        KONTAKTADRESSE("kontaktadresse"),
        OPPHOLDSADRESSE("oppholdsadresse")
    }

    companion object {
        val logger: Logger = LoggerFactory.getLogger(ParallelleSannheterKlient::class.java)
    }
}