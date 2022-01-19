package no.nav.etterlatte.pdl

import io.ktor.client.HttpClient
import io.ktor.client.request.accept
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.http.ContentType
import io.ktor.http.contentType
import no.nav.etterlatte.libs.common.pdl.AdressebeskyttelseResponse
import no.nav.etterlatte.libs.common.person.Foedselsnummer
import no.nav.etterlatte.libs.common.person.Person
import no.nav.etterlatte.libs.common.person.eyUtland
import org.slf4j.LoggerFactory

interface Pdl {
    suspend fun finnAdressebeskyttelseForFnr(fnrListe: List<Foedselsnummer>): AdressebeskyttelseResponse
    suspend fun hentPerson(fnr: Foedselsnummer): Person
    suspend fun hentUtland(fnr: Foedselsnummer): eyUtland
}

class PdlKlient(private val client: HttpClient, private val apiUrl: String) : Pdl {
    private val logger = LoggerFactory.getLogger(PdlKlient::class.java)

    /**
     * Henter personer fra PDL.
     *
     * @param fnr: fødselsnummer.
     *
     * @return [PersonResponse]: Responsobjekt fra PDL.
     */
    override suspend fun hentPerson(fnr: Foedselsnummer): Person {
        val response = client.post<Person>(apiUrl + "/hentperson") {
            header("Tema", "PEN")
            accept(ContentType.Application.Json)
            contentType(ContentType.Application.Json)
            body = fnr.value

        }

        //TODO ordne feilhåndtering
        // Logge feil dersom det finnes noen
        //response.errors?.forEach { error ->
         //   logger.error("Feil ved uthenting av adressebeskyttelse", error.toString())
       // }
        return response
    }
    override suspend fun hentUtland(fnr: Foedselsnummer): eyUtland {
        val response = client.post<eyUtland>(apiUrl + "/hentperson") {
            header("Tema", "PEN")
            accept(ContentType.Application.Json)
            contentType(ContentType.Application.Json)
            body = fnr.value

        }
        //TODO ordne feilhåndtering
        // Logge feil dersom det finnes noen
        //response.errors?.forEach { error ->
        //   logger.error("Feil ved uthenting av adressebeskyttelse", error.toString())
        // }
        return response
    }
    override suspend fun finnAdressebeskyttelseForFnr(fnrListe: List<Foedselsnummer>): AdressebeskyttelseResponse {

        val response = client.post<AdressebeskyttelseResponse>(apiUrl) {
            header("Tema", "PEN")
            accept(ContentType.Application.Json)
            contentType(ContentType.Application.Json)
            body = fnrListe
        }

        // Logge feil dersom det finnes noen
        response.errors?.forEach { error ->
            logger.error("Feil ved uthenting av adressebeskyttelse", error.toString())
        }

        return response
    }
}
