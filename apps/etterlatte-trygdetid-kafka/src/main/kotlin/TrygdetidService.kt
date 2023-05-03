package no.nav.etterlatte.trygdetid.kafka

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.libs.common.trygdetid.TrygdetidDto
import java.util.*

class TrygdetidService(
    private val trygdetidApp: HttpClient,
    private val url: String
) {

    fun beregnTrygdetid(behandlingId: UUID): TrygdetidDto = runBlocking {
        trygdetidApp.post("$url/api/trygdetid/$behandlingId") {
            contentType(ContentType.Application.Json)
        }.body()
    }
}