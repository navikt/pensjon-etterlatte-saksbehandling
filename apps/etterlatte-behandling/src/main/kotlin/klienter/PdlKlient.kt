package no.nav.etterlatte.klienter

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.libs.common.pdl.PersonDTO
import no.nav.etterlatte.libs.common.person.Foedselsnummer
import no.nav.etterlatte.libs.common.person.GeografiskTilknytning
import no.nav.etterlatte.libs.common.person.HentGeografiskTilknytningRequest
import no.nav.etterlatte.libs.common.person.HentPersonRequest
import no.nav.etterlatte.libs.common.person.PersonRolle
import no.nav.etterlatte.libs.common.person.Utland
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.LocalDate

interface PdlKlient {
    fun hentPdlModell(foedselsnummer: String, rolle: PersonRolle): PersonDTO

    fun hentGeografiskTilknytning(foedselsnummer: String): GeografiskTilknytning
}

class PdlKlientImpl(
    private val pdl_app: HttpClient,
    private val url: String
) : PdlKlient {

    companion object {
        val logger: Logger = LoggerFactory.getLogger(PdlKlientImpl::class.java)
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

    override fun hentGeografiskTilknytning(foedselsnummer: String): GeografiskTilknytning {
        val fnr = Foedselsnummer.of(foedselsnummer)
        logger.info("Henter geografisk tilknytning for $fnr")
        val request = HentGeografiskTilknytningRequest(fnr)
        val response = runBlocking {
            pdl_app.post("$url/geografisktilknytning") {
                contentType(ContentType.Application.Json)
                setBody(request)
            }.body<GeografiskTilknytning>()
        }
        return response
    }
}

fun PersonDTO.hentDoedsdato(): LocalDate? = this.doedsdato?.verdi

fun PersonDTO.hentAnsvarligeForeldre(): List<Foedselsnummer>? = this.familieRelasjon?.verdi?.ansvarligeForeldre

fun PersonDTO.hentBarn(): List<Foedselsnummer>? = this.familieRelasjon?.verdi?.barn

fun PersonDTO.hentUtland(): Utland? = this.utland?.verdi