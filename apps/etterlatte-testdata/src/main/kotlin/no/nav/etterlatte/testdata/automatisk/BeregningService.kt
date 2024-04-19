package no.nav.etterlatte.no.nav.etterlatte.testdata.automatisk

import io.ktor.client.HttpClient
import io.ktor.client.request.post
import java.util.UUID

class BeregningService(private val klient: HttpClient, private val url: String) {
    suspend fun beregn(behandlingId: UUID) = klient.post("$url/api/beregning/$behandlingId")
}
