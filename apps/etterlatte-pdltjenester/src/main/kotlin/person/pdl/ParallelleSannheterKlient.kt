package no.nav.etterlatte.person.pdl

import com.fasterxml.jackson.databind.JsonNode
import io.ktor.client.HttpClient
import io.ktor.client.request.accept
import io.ktor.client.request.post
import io.ktor.http.ContentType.Application.Json
import io.ktor.http.content.TextContent
import no.nav.etterlatte.common.mapJsonToAny
import no.nav.etterlatte.common.toJson
import no.nav.etterlatte.libs.common.objectMapper
import org.slf4j.Logger
import org.slf4j.LoggerFactory


class ParallelleSannheterException(override val message: String) : RuntimeException(message)

class ParallelleSannheterKlient(val httpClient: HttpClient, val apiUrl: String) {

    companion object {
        val logger: Logger = LoggerFactory.getLogger(ParallelleSannheterKlient::class.java)
    }

    suspend fun avklarNavn(navn: List<Navn>) = avklar(navn, Avklaring.NAVN)
    suspend fun avklarAdressebeskyttelse(adressebeskyttelse: List<PdlAdressebeskyttelse>) =
        avklarNullable(adressebeskyttelse, Avklaring.ADRESSEBESKYTTELSE)
    suspend fun avklarStatsborgerskap(statsborgerskap: List<Statsborgerskap>) =
        avklarNullable(statsborgerskap, Avklaring.STATSBORGERSKAP)
    suspend fun avklarSivilstand(sivilstand: List<Sivilstand>) = avklarNullable(sivilstand, Avklaring.SIVILSTAND)
    suspend fun avklarFoedsel(foedsel: List<Foedsel>) = avklarNullable(foedsel, Avklaring.FOEDSEL)
    suspend fun avklarDoedsfall(doedsfall: List<Doedsfall>) = avklarNullable(doedsfall, Avklaring.DOEDSFALL)
    suspend fun avklarBostedsadresse(bostedsadresse: List<Bostedsadresse>) = avklarNullable(bostedsadresse, Avklaring.BOSTEDSADRESSE)
    suspend fun avklarKontaktadresse(kontaktadresse: List<Kontaktadresse>) = avklarNullable(kontaktadresse, Avklaring.KONTAKTADRESSE)
    suspend fun avklarOppholdsadresse(oppholdsadresse: List<Oppholdsadresse>) = avklarNullable(oppholdsadresse, Avklaring.OPPHOLDSADRESSE)

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
                val responseAsJsonNode = httpClient.post<JsonNode>("$apiUrl/api/${avklaring.feltnavn}") {
                    accept(Json)
                    body = TextContent(nodeWithFieldName.toJson(), Json)
                }

                // Svar fra parallelle sannheter skal kun inneholde ett element
                if (responseAsJsonNode.size() != 1)
                    logger.warn("Parallelle sannheter returnerte mer enn et element for felt ${avklaring.feltnavn}")

                mapJsonToAny(responseAsJsonNode.get(avklaring.feltnavn).first().toJson())
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
        KONTAKTADRESSE("kontaktadresse"),
        OPPHOLDSADRESSE("oppholdsadresse")
    }

}