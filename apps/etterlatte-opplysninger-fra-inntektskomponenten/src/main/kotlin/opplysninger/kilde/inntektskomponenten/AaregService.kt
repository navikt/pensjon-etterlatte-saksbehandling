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
import org.slf4j.LoggerFactory

class AaregService(private val aaregClient: HttpClient, private val url: String) : Aareg {
    private val logger = LoggerFactory.getLogger(AaregService::class.java)
    override fun hentArbeidsforhold(fnr: Foedselsnummer): List<AaregResponse> = runBlocking {
        try {
            aaregClient.post(url) {
                contentType(ContentType.Application.Json)
                header("Nav-Personident", fnr.value)
            }.body()
        } catch (e: Exception) {
            logger.error("Klarte ikke å hente ut arbeidsforhold", e)
            return@runBlocking emptyList()
        }
    }
}