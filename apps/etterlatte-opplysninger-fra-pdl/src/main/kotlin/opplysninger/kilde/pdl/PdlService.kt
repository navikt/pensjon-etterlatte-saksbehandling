package no.nav.etterlatte.opplysninger.kilde.pdl

import io.ktor.client.HttpClient
import io.ktor.client.request.post
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.common.objectMapper
import no.nav.etterlatte.common.toJson
import no.nav.etterlatte.libs.common.person.Person

class PdlService(private val pdl: HttpClient, private val url: String) : Pdl {
    override fun hentPdlModell(): Person {
        val response = runBlocking {
            pdl.post<String>("$url/person/hentperson") {
                contentType(ContentType.Application.Json)
            }
        }
        return objectMapper.readValue(response.toJson(), Person::class.java)
    }
}