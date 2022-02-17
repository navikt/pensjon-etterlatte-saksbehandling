package no.nav.etterlatte.pdl

import io.ktor.client.HttpClient
import io.ktor.client.request.*
import io.ktor.http.ContentType
import io.ktor.http.contentType
import no.nav.etterlatte.libs.common.person.*

import org.slf4j.LoggerFactory

interface Pdl {
    suspend fun hentPerson(
        fnr: Foedselsnummer,
        historikk: Boolean = false,
        adresse: Boolean = false,
        utland: Boolean = false,
        familieRelasjon: Boolean = false
    ): Person
}

class PdlKlient(private val client: HttpClient, private val apiUrl: String) : Pdl {

    private val logger = LoggerFactory.getLogger(PdlKlient::class.java)

    override suspend fun hentPerson(
        fnr: Foedselsnummer,
        historikk: Boolean,
        adresse: Boolean,
        utland: Boolean,
        familieRelasjon: Boolean,
    ): Person {
        val response = client.get<Person>(
            "$apiUrl/utvidetperson?historikk=$historikk&adresse=$adresse&utland=$utland&familieRelasjon=$familieRelasjon") {
            header("foedselsnummer", fnr.value)
            accept(ContentType.Application.Json)
            contentType(ContentType.Application.Json)
        }
        //TODO ordne feilhåndtering

        return response
    }
}
