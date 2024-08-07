package no.nav.etterlatte.trygdetid.kafka

import io.ktor.client.HttpClient
import io.ktor.client.request.post
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.coroutines.runBlocking
import java.util.UUID

class TrygdetidService(
    private val trygdetidApp: HttpClient,
    private val url: String,
) {
    fun kopierTrygdetidFraForrigeBehandling(
        behandlingId: UUID,
        forrigeBehandlingId: UUID,
    ) = runBlocking {
        trygdetidApp.post("$url/api/trygdetid_v2/$behandlingId/kopier/$forrigeBehandlingId") {
            contentType(ContentType.Application.Json)
        }
    }
}
