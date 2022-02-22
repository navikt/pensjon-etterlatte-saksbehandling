package no.nav.etterlatte.person.pdl

import com.fasterxml.jackson.databind.JsonNode
import io.ktor.client.HttpClient
import io.ktor.client.request.accept
import io.ktor.client.request.post
import io.ktor.http.ContentType
import io.ktor.http.content.TextContent
import no.nav.etterlatte.common.mapJsonToAny
import no.nav.etterlatte.common.toJson
import no.nav.etterlatte.libs.common.objectMapper
import org.slf4j.Logger
import org.slf4j.LoggerFactory

enum class Avklaring(val type: String) {
    NAVN("navn"),
    ADRESSEBESKYTTELSE("adressebeskyttelse"),
    STATSBORGERSKAP("statsborgerskap"),
    SIVILSTAND("sivilstand"),
    FOEDSEL("foedsel"),
    DOEDSFALL("doedsfall"),
}


class ParallelleSannheterKlient(val httpClient: HttpClient, val apiUrl: String) {

    companion object {
        val logger: Logger = LoggerFactory.getLogger(ParallelleSannheterKlient::class.java)
    }

    suspend fun avklarNavn(hentPerson: HentPerson) = avklar<Navn>(hentPerson, Avklaring.NAVN)
    suspend fun avklarAdressebeskyttelse(hentPerson: HentPerson) = avklarNullable<PdlAdressebeskyttelse>(hentPerson, Avklaring.ADRESSEBESKYTTELSE)
    suspend fun avklarStatsborgerskap(hentPerson: HentPerson) = avklarNullable<Statsborgerskap>(hentPerson, Avklaring.STATSBORGERSKAP)
    suspend fun avklarSivilstand(hentPerson: HentPerson) = avklarNullable<Sivilstand>(hentPerson, Avklaring.SIVILSTAND)
    suspend fun avklarFoedsel(hentPerson: HentPerson) = avklarNullable<Foedsel>(hentPerson, Avklaring.FOEDSEL)
    suspend fun avklarDoedsfall(hentPerson: HentPerson) = avklarNullable<Doedsfall>(hentPerson, Avklaring.DOEDSFALL)

    private suspend inline fun <reified T> avklar(hentPerson: HentPerson, avklaring: Avklaring): T {
        return avklarNullable(hentPerson, avklaring)
            ?: throw RuntimeException("Forventet verdi for ${avklaring.type}, men var null")
    }

    private suspend inline fun <reified T> avklarNullable(hentPerson: HentPerson, avklaring: Avklaring): T? {
        val hentPersonJsonNode: JsonNode = objectMapper.readValue(hentPerson.toJson(), JsonNode::class.java)
        return when (hentPersonJsonNode[avklaring.type].size()) {
            0 -> null
            1 -> mapJsonToAny(hentPersonJsonNode[avklaring.type].first().toJson())
            else -> {
                logger.info("Felt av typen ${avklaring.type} har flere elementer, sjekker mot PPS")
                val jsonNode = httpClient.post<JsonNode>("$apiUrl/api/${avklaring.type}") {
                    accept(ContentType.Application.Json)
                    body = TextContent(hentPerson.toJson(), ContentType.Application.Json)
                }

                mapJsonToAny(jsonNode.get(avklaring.type).first().toJson())
            }
        }
    }

}