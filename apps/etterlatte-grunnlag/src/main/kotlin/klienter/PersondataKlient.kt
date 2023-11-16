package no.nav.etterlatte.klienter

import grunnlag.adresse.PersondataAdresse
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.accept
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.request.setBody
import io.ktor.http.ContentType.Application.Json
import io.ktor.http.contentType
import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.libs.common.RetryResult
import no.nav.etterlatte.libs.common.person.Folkeregisteridentifikator
import no.nav.etterlatte.libs.common.retry

class PersondataKlient(private val httpClient: HttpClient, private val apiUrl: String) {
    fun hentAdresseForVerge(folkeregisteridentifikator: Folkeregisteridentifikator): PersondataAdresse {
        val request = ""

        return runBlocking {
            retry<PersondataAdresse>(times = 3) {
                httpClient.get("$apiUrl/api/adresse/kontaktadresse") {
                    parameter("checkForVerge", true)
                    header("pid", folkeregisteridentifikator.value)
                    accept(Json)
                    contentType(Json)
                    setBody(request)
                }.body()
            }.let {
                when (it) {
                    is RetryResult.Success -> it.content
                    is RetryResult.Failure -> throw it.samlaExceptions()
                }
            }
        }
    }
}
