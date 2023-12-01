package no.nav.etterlatte.grunnlag.klienter

import com.fasterxml.jackson.module.kotlin.readValue
import grunnlag.VurdertBostedsland
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.request.accept
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.request.setBody
import io.ktor.http.ContentType.Application.Json
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.grunnlag.adresse.PersondataAdresse
import no.nav.etterlatte.grunnlag.adresse.RegoppslagResponseDTO
import no.nav.etterlatte.libs.common.RetryResult
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.person.Folkeregisteridentifikator
import no.nav.etterlatte.libs.common.retry
import no.nav.etterlatte.libs.common.retryOgPakkUt
import org.slf4j.LoggerFactory

class PersondataKlient(private val httpClient: HttpClient, private val apiUrl: String) {
    private val logger = LoggerFactory.getLogger(this::class.java)

    /**
     * Fra Pensjondata dokumentasjon: Hvis checkForVerge er true gjøres det først et oppslag mot pensjon-fullmakt
     * for å se om bruker har verge.
     * Hvis< det foreligger en verge som er samhandler vil det slås opp i TSS for å hente vergens adresse som
     * returneres. Hvis det foreligger en verge som er privatperson slås det opp i PDL og denne personens adresse
     * returneres i stedet. Hvis det ikke identifiseres noen verge gås det videre til neste steg.

     * @see <a href="https://pensjon-dokumentasjon.intern.dev.nav.no/pensjon-persondata/main/index.html#_adresse_api">
     * Pensjondata dokumentasjon</a>. */
    fun hentAdresseForVerge(vergehaverFnr: String): PersondataAdresse? {
        try {
            val kontaktadresse = hentKontaktadresse(vergehaverFnr, true)

            return if (erVergesAdresse(kontaktadresse)) {
                kontaktadresse
            } else {
                null
            }
        } catch (e: ClientRequestException) {
            if (e.response.status == HttpStatusCode.NotFound) {
                return null
            }
            logger.error("Feil i henting av vergeadresse", e)
            return null
        } catch (e: Exception) {
            logger.error("Feil i henting av vergeadresse", e)
            return null
        }
    }

    fun hentAdresse(foedselsnummer: String): PersondataAdresse? {
        try {
            return hentKontaktadresse(foedselsnummer, false)
        } catch (e: ClientRequestException) {
            if (e.response.status == HttpStatusCode.NotFound) {
                return null
            }
            logger.error("Feil i henting av adresse", e)
            return null
        } catch (e: Exception) {
            logger.error("Feil i henting av adresse", e)
            return null
        }
    }

    private fun hentKontaktadresse(
        vergehaverFnr: String,
        checkForVerge: Boolean,
    ): PersondataAdresse {
        return runBlocking {
            retry(times = 3) {
                fetchAndMapAdresse(checkForVerge, vergehaverFnr)
            }.let {
                when (it) {
                    is RetryResult.Success -> it.content
                    is RetryResult.Failure -> throw it.samlaExceptions()
                }
            }
        }
    }

    private suspend fun fetchAndMapAdresse(
        checkForVerge: Boolean,
        vergehaverFnr: String,
    ): PersondataAdresse {
        val jsonResponse: String =
            httpClient.get("$apiUrl/api/adresse/kontaktadresse") {
                parameter("checkForVerge", checkForVerge)
                header("pid", vergehaverFnr)
                accept(Json)
                contentType(Json)
                setBody("")
            }.body()

        val persondataAdresse: PersondataAdresse = objectMapper.readValue(jsonResponse)

        return when (persondataAdresse.type) {
            "REGOPPSLAG_ADRESSE" -> toPersondataAdresse(objectMapper.readValue<RegoppslagResponseDTO>(jsonResponse))
            else -> persondataAdresse
        }
    }

    private fun toPersondataAdresse(regoppslagAdresse: RegoppslagResponseDTO): PersondataAdresse {
        return PersondataAdresse(
            adresselinjer =
                listOfNotNull(
                    regoppslagAdresse.adresse.adresselinje1,
                    regoppslagAdresse.adresse.adresselinje2,
                    regoppslagAdresse.adresse.adresselinje3,
                ),
            type = "REGOPPSLAG_ADRESSE",
            land = regoppslagAdresse.adresse.land,
            landkode = regoppslagAdresse.adresse.landkode,
            navn = regoppslagAdresse.navn,
            postnr = regoppslagAdresse.adresse.postnummer,
            postnummer = regoppslagAdresse.adresse.postnummer,
            poststed = regoppslagAdresse.adresse.poststed,
            vergePid = null,
        )
    }

    private fun erVergesAdresse(adresse: PersondataAdresse) =
        listOf("VERGE_PERSON_POSTADRESSE", "VERGE_SAMHANDLER_POSTADRESSE").contains(adresse.type)

    suspend fun hentBostedsland(person: Folkeregisteridentifikator) =
        retryOgPakkUt<VurdertBostedsland> {
            httpClient.get("$apiUrl/api/bostedsland") {
                header("pid", person.value)
                accept(Json)
                contentType(Json)
            }.body()
        }
}
