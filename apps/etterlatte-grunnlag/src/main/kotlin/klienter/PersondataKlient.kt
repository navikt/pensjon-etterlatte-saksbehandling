package no.nav.etterlatte.grunnlag.klienter

import com.fasterxml.jackson.module.kotlin.readValue
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
import no.nav.etterlatte.libs.common.RetryResult
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.retry
import org.slf4j.LoggerFactory

class PersondataKlient(
    private val httpClient: HttpClient,
    private val apiUrl: String,
) {
    private val logger = LoggerFactory.getLogger(this::class.java)

    fun hentAdresseGittFnr(foedselsnummer: String): PersondataAdresse? {
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

    /**
     * Fra Pensjondata dokumentasjon: Hvis checkForVerge er true gjøres det først et oppslag mot pensjon-fullmakt
     * for å se om bruker har verge.
     * Hvis det foreligger en verge som er samhandler vil det slås opp i TSS for å hente vergens adresse som
     * returneres. Hvis det foreligger en verge som er privatperson slås det opp i PDL og denne personens adresse
     * returneres i stedet. Hvis det ikke identifiseres noen verge gås det videre til neste steg.

     * @see <a href="https://pensjon-dokumentasjon.intern.dev.nav.no/pensjon-persondata/main/index.html#_adresse_api">
     * Pensjondata dokumentasjon</a>. */
    private fun hentKontaktadresse(
        foedselsnummer: String,
        seEtterVerge: Boolean,
    ): PersondataAdresse =
        runBlocking {
            retry(times = 3) {
                val jsonResponse: String =
                    httpClient
                        .get("$apiUrl/api/adresse/kontaktadresse") {
                            parameter("checkForVerge", seEtterVerge)
                            header("pid", foedselsnummer)
                            accept(Json)
                            contentType(Json)
                            setBody("")
                        }.body<String>()
                objectMapper.readValue<PersondataAdresse>(jsonResponse)
            }.let {
                when (it) {
                    is RetryResult.Success -> it.content
                    is RetryResult.Failure -> throw it.samlaExceptions()
                }
            }
        }

    private fun erVergesAdresse(adresse: PersondataAdresse) =
        listOf("VERGE_PERSON_POSTADRESSE", "VERGE_SAMHANDLER_POSTADRESSE").contains(adresse.type)
}
