package no.nav.etterlatte.brev.dokarkiv

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.ResponseException
import io.ktor.client.request.accept
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import no.nav.etterlatte.brev.journalpost.Bruker
import no.nav.etterlatte.brev.journalpost.JournalpostRequest
import no.nav.etterlatte.brev.journalpost.JournalpostResponse
import no.nav.etterlatte.brev.journalpost.JournalpostSak
import org.slf4j.LoggerFactory

class DokarkivKlient(private val client: HttpClient, private val url: String) {
    private val logger = LoggerFactory.getLogger(DokarkivKlient::class.java)

    internal suspend fun opprettJournalpost(
        request: JournalpostRequest,
        ferdigstill: Boolean,
    ): JournalpostResponse =
        try {
            client.post("$url?forsoekFerdigstill=$ferdigstill") {
                accept(ContentType.Application.Json)
                contentType(ContentType.Application.Json)
                setBody(request)
            }.body()
        } catch (responseException: ResponseException) {
            throw when (responseException.response.status.value) {
                HttpStatusCode.Conflict.value -> DuplikatJournalpostException("Duplikat journalpost", responseException)
                else -> JournalpostException("Feil i kall mot Dokarkiv", responseException)
            }
        } catch (exception: Exception) {
            logger.error("Feil i kall mot Dokarkiv: ", exception)
            throw JournalpostException("Feil i kall mot Dokarkiv", exception)
        }

    internal suspend fun ferdigstillJournalpost(
        journalpostId: String,
        journalfoerendeEnhet: String,
    ): String =
        client.patch("$url/$journalpostId/ferdigstill") {
            contentType(ContentType.Application.Json)
            setBody(FerdigstillJournalpostRequest(journalfoerendeEnhet))
        }.body()

    internal suspend fun oppdaterFagsak(
        journalpostId: String,
        request: OppdaterJournalpostSakRequest,
    ): String =
        client.put("$url/$journalpostId") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()

    internal suspend fun endreTema(
        journalpostId: String,
        nyttTema: String,
    ): String =
        client.put("$url/$journalpostId") {
            contentType(ContentType.Application.Json)
            setBody(OppdaterJournalpostTemaRequest(nyttTema))
        }.body()
}

data class OppdaterJournalpostTemaRequest(val tema: String)

data class FerdigstillJournalpostRequest(val journalfoerendeEnhet: String)

data class OppdaterJournalpostSakRequest(val bruker: Bruker, val sak: JournalpostSak)

open class JournalpostException(msg: String, cause: Throwable) : Exception(msg, cause)

class DuplikatJournalpostException(msg: String, cause: Throwable) : JournalpostException(msg, cause)
