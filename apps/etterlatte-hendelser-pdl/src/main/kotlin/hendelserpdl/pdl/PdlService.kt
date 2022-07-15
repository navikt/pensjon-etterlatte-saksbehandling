package no.nav.etterlatte.hendelserpdl.pdl

import io.ktor.client.HttpClient
import io.ktor.client.request.post
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.libs.common.person.FolkeregisterIdent
import no.nav.etterlatte.libs.common.person.HentFolkeregisterIdentRequest

interface Pdl {
    suspend fun hentFolkeregisterIdentifikator(ident: String): FolkeregisterIdent
}

class PdlService(
    private val pdl_app: HttpClient,
    private val url: String
) : Pdl {

    override suspend fun hentFolkeregisterIdentifikator(ident: String) =
        runBlocking {
            pdl_app.post<FolkeregisterIdent>("$url/personident") {
                contentType(ContentType.Application.Json)
                body = HentFolkeregisterIdentRequest(ident)
            }
        }
}