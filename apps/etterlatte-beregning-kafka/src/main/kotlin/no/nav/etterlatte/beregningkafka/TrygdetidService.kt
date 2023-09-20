package no.nav.etterlatte.beregningkafka

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
        trygdetidApp.post("$url/api/trygdetid/$behandlingId/kopier/$forrigeBehandlingId") {
            contentType(ContentType.Application.Json)
        }
    }
}
