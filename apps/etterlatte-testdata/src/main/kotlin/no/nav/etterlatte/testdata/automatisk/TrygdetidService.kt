package no.nav.etterlatte.no.nav.etterlatte.testdata.automatisk

import io.ktor.client.HttpClient
import io.ktor.client.request.post
import io.ktor.http.ContentType
import io.ktor.http.contentType
import java.util.UUID

class TrygdetidService(private val klient: HttpClient, private val url: String) {
    suspend fun beregnTrygdetid(behandlingId: UUID) =
        klient.post("$url/api/trygdetid/$behandlingId") {
            contentType(ContentType.Application.Json)
        }
}
