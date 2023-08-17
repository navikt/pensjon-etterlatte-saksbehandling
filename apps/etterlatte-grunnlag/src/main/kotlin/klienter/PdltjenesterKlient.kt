package no.nav.etterlatte.klienter

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
import no.nav.etterlatte.pdl.HistorikkForeldreansvar

interface PdlTjenesterKlient {
    fun hentPerson(foedselsnummer: String, rolle: PersonRolle, sakType: SakType): Person
    fun hentOpplysningsperson(foedselsnummer: String, rolle: PersonRolle, sakType: SakType): PersonDTO
    fun hentHistoriskForeldreansvar(
        fnr: Folkeregisteridentifikator,
        rolle: PersonRolle,
        sakType: SakType
    ): HistorikkForeldreansvar
}

class PdlTjenesterKlientImpl(private val pdl: HttpClient, private val url: String) : PdlTjenesterKlient {
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

    override fun hentHistoriskForeldreansvar(
        fnr: Folkeregisteridentifikator,
        rolle: PersonRolle,
        sakType: SakType
    ): HistorikkForeldreansvar {
        val personRequest = HentPersonRequest(fnr, rolle, sakType)
        return runBlocking {
            pdl.post("$url/foreldreansvar") {
                contentType(ContentType.Application.Json)
                setBody(personRequest)
            }.body<HistorikkForeldreansvar>()
        }
    }
}