package no.nav.etterlatte.pdl

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.libs.common.pdl.PersonDTO
import no.nav.etterlatte.libs.common.person.Foedselsnummer
import no.nav.etterlatte.libs.common.person.HentPersonRequest
import no.nav.etterlatte.libs.common.person.PersonRolle
import no.nav.etterlatte.libs.common.person.Utland
import org.slf4j.LoggerFactory
import java.time.LocalDate

interface Pdl {
    fun hentPdlModell(foedselsnummer: String, rolle: PersonRolle): PersonDTO
}

class PdlService(
    private val pdl_app: HttpClient,
    private val url: String
) : Pdl {

    companion object {
        val logger = LoggerFactory.getLogger(PdlService::class.java)
    }

    override fun hentPdlModell(foedselsnummer: String, rolle: PersonRolle): PersonDTO {
        logger.info("Henter Pdl-modell for ${rolle.name}")
        val personRequest = HentPersonRequest(Foedselsnummer.of(foedselsnummer), rolle)
        val response = runBlocking {
            pdl_app.post("$url/person/v2") {
                contentType(ContentType.Application.Json)
                setBody(personRequest)
            }.body<PersonDTO>()
        }
        return response
    }

    fun hentDoedsdato(fnr: String, rolle: PersonRolle): LocalDate? {
        return hentPdlModell(
            foedselsnummer = fnr,
            rolle = rolle
        ).doedsdato?.verdi
    }

    fun hentAnsvarligeForeldre(fnr: String, rolle: PersonRolle): List<Foedselsnummer>? {
        return hentPdlModell(
            foedselsnummer = fnr,
            rolle = rolle
        ).familieRelasjon?.verdi?.ansvarligeForeldre
    }

    fun hentBarn(fnr: String, rolle: PersonRolle): List<Foedselsnummer>? {
        return hentPdlModell(
            foedselsnummer = fnr,
            rolle = rolle
        ).familieRelasjon?.verdi?.barn
    }

    fun hentUtland(fnr: String, rolle: PersonRolle): Utland? {
        return hentPdlModell(
            foedselsnummer = fnr,
            rolle = rolle
        ).utland?.verdi
    }
}