package no.nav.etterlatte.pdl

import io.ktor.client.HttpClient
import io.ktor.client.request.accept
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.utils.EmptyContent.contentType
import io.ktor.http.ContentType
import io.ktor.http.ContentType.Application.Json
import io.ktor.http.contentType
import no.nav.etterlatte.libs.common.logging.X_CORRELATION_ID
import no.nav.etterlatte.libs.common.logging.getCorrelationId
import no.nav.etterlatte.libs.common.person.Foedselsnummer
import no.nav.etterlatte.libs.common.person.HentPersonRequest
import no.nav.etterlatte.libs.common.person.Person
import org.slf4j.LoggerFactory

interface Pdl {
    suspend fun hentPerson(hentPersonRequest: HentPersonRequest): Person
}

class PdlKlient(private val client: HttpClient, private val apiUrl: String) : Pdl {

    private val logger = LoggerFactory.getLogger(PdlKlient::class.java)

    override suspend fun hentPerson(hentPersonRequest: HentPersonRequest): Person {
        val response = client.post<Person>(apiUrl) {
            accept(Json)
            contentType(Json)
            body = hentPersonRequest
        }
        //TODO ordne feilhåndtering

        return response
    }
}
