package no.nav.etterlatte.common.klienter

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.pdl.PersonDTO
import no.nav.etterlatte.libs.common.person.Folkeregisteridentifikator
import no.nav.etterlatte.libs.common.person.GeografiskTilknytning
import no.nav.etterlatte.libs.common.person.HentGeografiskTilknytningRequest
import no.nav.etterlatte.libs.common.person.HentPersonRequest
import no.nav.etterlatte.libs.common.person.PersonRolle
import no.nav.etterlatte.libs.common.person.Sivilstand
import no.nav.etterlatte.libs.common.person.Utland
import no.nav.etterlatte.libs.common.person.VergemaalEllerFremtidsfullmakt
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.LocalDate

interface PdlKlient {
    fun hentPdlModell(foedselsnummer: String, rolle: PersonRolle, saktype: SakType): PersonDTO
    fun hentGeografiskTilknytning(foedselsnummer: String, saktype: SakType): GeografiskTilknytning
}

class PdlKlientImpl(
    private val pdl_app: HttpClient,
    private val url: String
) : PdlKlient {

    companion object {
        val logger: Logger = LoggerFactory.getLogger(PdlKlientImpl::class.java)
    }

    override fun hentPdlModell(foedselsnummer: String, rolle: PersonRolle, saktype: SakType): PersonDTO {
        logger.info("Henter Pdl-modell for ${rolle.name}")
        val personRequest = HentPersonRequest(Folkeregisteridentifikator.of(foedselsnummer), rolle, saktype)
        val response = runBlocking {
            pdl_app.post("$url/person/v2") {
                contentType(ContentType.Application.Json)
                setBody(personRequest)
            }.body<PersonDTO>()
        }
        return response
    }

    override fun hentGeografiskTilknytning(foedselsnummer: String, saktype: SakType): GeografiskTilknytning {
        val request = HentGeografiskTilknytningRequest(Folkeregisteridentifikator.of(foedselsnummer), saktype)
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

fun PersonDTO.hentAnsvarligeForeldre(): List<Folkeregisteridentifikator>? =
    this.familieRelasjon?.verdi?.ansvarligeForeldre

fun PersonDTO.hentBarn(): List<Folkeregisteridentifikator>? = this.familieRelasjon?.verdi?.barn

fun PersonDTO.hentVergemaal(): List<VergemaalEllerFremtidsfullmakt>? = this.vergemaalEllerFremtidsfullmakt?.map {
    it.verdi
}

fun PersonDTO.hentSivilstand(): List<Sivilstand>? = this.sivilstand?.map {
    it.verdi
}

fun PersonDTO.hentUtland(): Utland? = this.utland?.verdi