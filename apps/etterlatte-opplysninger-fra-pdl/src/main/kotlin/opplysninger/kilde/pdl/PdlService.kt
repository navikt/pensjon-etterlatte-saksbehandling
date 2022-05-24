package no.nav.etterlatte.opplysninger.kilde.pdl

import io.ktor.client.HttpClient
import io.ktor.client.request.post
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.libs.common.person.Foedselsnummer
import no.nav.etterlatte.libs.common.person.HentPersonRequest
import no.nav.etterlatte.libs.common.person.Person
import no.nav.etterlatte.libs.common.person.PersonRolle


class PdlService(private val pdl: HttpClient, private val url: String) : Pdl {
    override fun hentPdlModell(foedselsnummer: String, rolle: PersonRolle): Person {
        val personRequest = HentPersonRequest(Foedselsnummer.of(foedselsnummer), rolle)
        val response = runBlocking {
            pdl.post<Person>("$url/person") {
                contentType(ContentType.Application.Json)
                body = personRequest
            }
        }
        return response
    }
}