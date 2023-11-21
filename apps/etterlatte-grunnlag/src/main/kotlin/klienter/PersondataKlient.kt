package no.nav.etterlatte.grunnlag.klienter

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
import no.nav.etterlatte.libs.common.person.Folkeregisteridentifikator
import no.nav.etterlatte.libs.common.retry
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
    fun hentAdresseForVerge(folkeregisteridentifikator: Folkeregisteridentifikator): PersondataAdresse? {
        val request = ""
        try {
            return runBlocking {
                retry<PersondataAdresse>(times = 3) {
                    httpClient.get("$apiUrl/api/adresse/kontaktadresse") {
                        parameter("checkForVerge", true)
                        header("pid", folkeregisteridentifikator.value)
                        accept(Json)
                        contentType(Json)
                        setBody(request)
                    }.body()
                }.let {
                    when (it) {
                        is RetryResult.Success -> it.content
                        is RetryResult.Failure -> throw it.samlaExceptions()
                    }
                }
            }
        } catch (e: ClientRequestException) {
            when (e.response.status) {
                HttpStatusCode.NotFound -> return null
            }
            logger.error("Feil i henting av vergeadresser", e)
            return null
        } catch (e: Exception) {
            logger.error("Feil i henting av vergeadresser", e)
            return null
        }
    }
}
