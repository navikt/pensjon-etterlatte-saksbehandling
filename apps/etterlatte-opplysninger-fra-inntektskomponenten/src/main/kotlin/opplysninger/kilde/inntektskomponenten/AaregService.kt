package no.nav.etterlatte.opplysninger.kilde.inntektskomponenten

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.Aareg
import no.nav.etterlatte.libs.common.arbeidsforhold.AaregResponse
import no.nav.etterlatte.libs.common.person.Foedselsnummer

class AaregService(private val aaregClient: HttpClient, private val url: String) : Aareg {
    override fun hentArbeidsforhold(fnr: Foedselsnummer): List<AaregResponse> {
        val result = runBlocking {
            try {
                aaregClient.post(url) {
                    contentType(ContentType.Application.Json)
                    header("Nav-Personident", fnr.value)
                }.body<List<AaregResponse>>()
            } catch (e: Exception) {
                print(e.message)
                return@runBlocking emptyList()
            }
        }
        return result
    }
}