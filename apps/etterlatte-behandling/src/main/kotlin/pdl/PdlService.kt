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
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.LocalDate

interface PdlService {
    fun hentPdlModell(foedselsnummer: String, rolle: PersonRolle): PersonDTO
}

class PdlServiceImpl(
    private val pdl_app: HttpClient,
    private val url: String
) : PdlService {

    companion object {
        val logger: Logger = LoggerFactory.getLogger(PdlServiceImpl::class.java)
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
}

fun PersonDTO.hentDoedsdato(): LocalDate? = this.doedsdato?.verdi

fun PersonDTO.hentAnsvarligeForeldre(): List<Foedselsnummer>? = this.familieRelasjon?.verdi?.ansvarligeForeldre

fun PersonDTO.hentBarn(): List<Foedselsnummer>? = this.familieRelasjon?.verdi?.barn

fun PersonDTO.hentUtland(): Utland? = this.utland?.verdi