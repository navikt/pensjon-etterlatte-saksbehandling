package no.nav.etterlatte.pdl

import io.ktor.client.HttpClient
import io.ktor.client.request.accept
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.http.ContentType
import io.ktor.http.contentType
import no.nav.etterlatte.common.toJson
import no.nav.etterlatte.libs.common.pdl.AdressebeskyttelseResponse
import no.nav.etterlatte.libs.common.pdl.GraphqlRequest
import no.nav.etterlatte.libs.common.pdl.Variables
import no.nav.etterlatte.libs.common.person.Foedselsnummer
import no.nav.etterlatte.prosess.pdl.PersonResponse
import org.slf4j.LoggerFactory

interface Pdl {
    suspend fun finnAdressebeskyttelseForFnr(fnrListe: List<Foedselsnummer>): AdressebeskyttelseResponse
    suspend fun hentPerson(fnr: Foedselsnummer): PersonResponse
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
    override suspend fun hentPerson(fnr: Foedselsnummer): PersonResponse {
        val response = client.post<PersonResponse>(apiUrl + "/hentperson") {
            header("Tema", "PEN")
            accept(ContentType.Application.Json)
            contentType(ContentType.Application.Json)
            body = fnr.value

        }

        // Logge feil dersom det finnes noen
        response.errors?.forEach { error ->
            logger.error("Feil ved uthenting av adressebeskyttelse", error.toString())
        }
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
