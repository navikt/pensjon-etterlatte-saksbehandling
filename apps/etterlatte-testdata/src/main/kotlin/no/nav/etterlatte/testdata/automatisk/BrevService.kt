package no.nav.etterlatte.no.nav.etterlatte.testdata.automatisk

import io.ktor.client.HttpClient
import java.util.UUID

class BrevService(private val klient: HttpClient, private val url: String) {
    suspend fun opprettOgDistribuerVedtaksbrev(behandlingId: UUID) {}
}
