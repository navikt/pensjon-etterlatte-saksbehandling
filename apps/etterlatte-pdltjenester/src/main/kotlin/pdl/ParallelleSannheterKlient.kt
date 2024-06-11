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
import no.nav.etterlatte.funksjonsbrytere.FeatureToggle
import no.nav.etterlatte.funksjonsbrytere.FeatureToggleService
import no.nav.etterlatte.libs.common.RetryResult
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.person.Folkeregisteridentifikator
import no.nav.etterlatte.libs.common.retry
import no.nav.etterlatte.libs.common.toJson
import no.nav.etterlatte.sikkerLogg
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class ParallelleSannheterException(
    override val message: String,
) : RuntimeException(message)

class ParallelleSannheterKlient(
    val httpClient: HttpClient,
    val apiUrl: String,
    val featureToggleService: FeatureToggleService,
) {
    suspend fun avklarNavn(pdlNavn: List<PdlNavn>): PdlNavn =
        if (featureToggleService.isEnabled(NavnFeatureToggles.AksepterManglendeNavn, false)) {
            avklarNullable(pdlNavn, Avklaring.NAVN) ?: fallbackperson()
        } else {
            avklar(pdlNavn, Avklaring.NAVN)
        }

    private fun fallbackperson() =
        PdlNavn(
            fornavn = "Navn",
            etternavn = "Navnesen",
            metadata =
                PdlMetadata(
                    endringer = listOf(),
                    historisk = false,
                    master = "Hardkoda data fra Gjenny",
                    opplysningsId = "-1",
                ),
        )

    suspend fun avklarFolkeregisteridentifikator(folkeregisteridentifikator: List<PdlFolkeregisteridentifikator>) =
        avklar(folkeregisteridentifikator, Avklaring.FOLKEREGISTERIDENTIFIKATOR)

    suspend fun avklarAdressebeskyttelse(adressebeskyttelse: List<PdlAdressebeskyttelse>) =
        avklarNullable(adressebeskyttelse, Avklaring.ADRESSEBESKYTTELSE)

    suspend fun avklarStatsborgerskap(pdlStatsborgerskap: List<PdlStatsborgerskap>) =
        avklarNullable(pdlStatsborgerskap, Avklaring.STATSBORGERSKAP)

    suspend fun avklarSivilstand(
        pdlSivilstand: List<PdlSivilstand>,
        foedselsnummer: Folkeregisteridentifikator,
    ): PdlSivilstand? {
        val aktiveSivilstander = pdlSivilstand.filterNot { it.metadata.historisk }

        if (aktiveSivilstander.size > 1) {
            logger.warn("Fant ${aktiveSivilstander.size} aktive sivilstander")
            if (aktiveSivilstander.all { it.type == aktiveSivilstander.first().type }) {
                logger.warn("Fant flere aktive sivilstander av samme type")
                return aktiveSivilstander.sortedByDescending { it.gyldigFraOgMed }.first()
            } else {
                val toSiste = aktiveSivilstander.sortedByDescending { it.gyldigFraOgMed }.takeLast(2)
                if (toSiste.first().gyldigFraOgMed == toSiste.last().gyldigFraOgMed) {
                    logger.warn("Fant flere aktive sivilstander av ulik type for $foedselsnummer. Se sikkerlogg for detaljer.")
                    sikkerLogg.info("Fant flere aktive sivilstander for ${foedselsnummer.value}: ${aktiveSivilstander.toJson()}")
                }
            }
        }

        return avklarNullable(
            list = aktiveSivilstander,
            avklaring = Avklaring.SIVILSTAND,
        )
    }

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

    private suspend inline fun <reified T> avklar(
        list: List<T>,
        avklaring: Avklaring,
    ): T =
        avklarNullable(list, avklaring)
            ?: throw ParallelleSannheterException("Forventet verdi for feltet ${avklaring.feltnavn}, men var null")

    private suspend inline fun <reified T> avklarNullable(
        list: List<T>,
        avklaring: Avklaring,
    ): T? {
        val listAsJsonNode = objectMapper.readValue(list.toJson(), JsonNode::class.java)
        val nodeWithFieldName: JsonNode = objectMapper.createObjectNode().set(avklaring.feltnavn, listAsJsonNode)
        return when (list.size) {
            0 -> null
            1 -> list.first()
            else -> {
                logger.info("Felt av typen ${avklaring.feltnavn} har ${list.size} elementer, sjekker mot PPS")
                val responseAsJsonNode =
                    retry {
                        httpClient
                            .post("$apiUrl/api/${avklaring.feltnavn}") {
                                accept(Json)
                                setBody(TextContent(nodeWithFieldName.toJson(), Json))
                            }.body<JsonNode>()
                    }.let {
                        when (it) {
                            is RetryResult.Success -> it.content
                            is RetryResult.Failure -> throw it.samlaExceptions()
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

    private enum class Avklaring(
        val feltnavn: String,
    ) {
        FOLKEREGISTERIDENTIFIKATOR("folkeregisteridentifikator"),
        NAVN("navn"),
        ADRESSEBESKYTTELSE("adressebeskyttelse"),
        STATSBORGERSKAP("statsborgerskap"),
        SIVILSTAND("sivilstand"),
        FOEDSEL("foedsel"),
        DOEDSFALL("doedsfall"),
        BOSTEDSADRESSE("bostedsadresse"),
        DELTBOSTEDSADRESSE("deltbostedsadresse"),
        KONTAKTADRESSE("kontaktadresse"),
        OPPHOLDSADRESSE("oppholdsadresse"),
    }

    companion object {
        val logger: Logger = LoggerFactory.getLogger(ParallelleSannheterKlient::class.java)
    }
}

enum class NavnFeatureToggles(
    private val key: String,
) : FeatureToggle {
    AksepterManglendeNavn("aksepter-manglende-navn"),
    ;

    override fun key() = key
}
