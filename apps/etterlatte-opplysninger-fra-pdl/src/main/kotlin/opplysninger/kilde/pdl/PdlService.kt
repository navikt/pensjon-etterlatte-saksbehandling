package no.nav.etterlatte.opplysninger.kilde.pdl

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
import no.nav.etterlatte.libs.common.person.HentPersonRequest
import no.nav.etterlatte.libs.common.person.Person
import no.nav.etterlatte.libs.common.person.PersonRolle

interface PdlServiceInterface {
    fun hentPerson(foedselsnummer: String, rolle: PersonRolle, sakType: SakType): Person
    fun hentOpplysningsperson(foedselsnummer: String, rolle: PersonRolle, sakType: SakType): PersonDTO
}

class PdlService(private val pdl: HttpClient, private val url: String) : PdlServiceInterface {
    override fun hentPerson(foedselsnummer: String, rolle: PersonRolle, sakType: SakType): Person {
        val personRequest = HentPersonRequest(Folkeregisteridentifikator.of(foedselsnummer), rolle, sakType)
        val response = runBlocking {
            pdl.post("$url/person") {
                contentType(ContentType.Application.Json)
                setBody(personRequest)
            }.body<Person>()
        }
        return response
    }

    override fun hentOpplysningsperson(foedselsnummer: String, rolle: PersonRolle, sakType: SakType): PersonDTO {
        val personRequest = HentPersonRequest(Folkeregisteridentifikator.of(foedselsnummer), rolle, sakType)
        val response = runBlocking {
            pdl.post("$url/person/v2") {
                contentType(ContentType.Application.Json)
                setBody(personRequest)
            }.body<PersonDTO>()
        }
        return response
    }
}