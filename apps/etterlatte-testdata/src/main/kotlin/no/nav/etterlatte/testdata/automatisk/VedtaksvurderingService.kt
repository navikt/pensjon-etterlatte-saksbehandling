package no.nav.etterlatte.no.nav.etterlatte.testdata.automatisk

import io.ktor.client.HttpClient
import java.util.UUID

class VedtaksvurderingService(private val klient: HttpClient, private val url: String) {
    suspend fun fattAttesterOgIverksettVedtak(behandlingId: UUID) {
        TODO("Not yet implemented")
    }

    fun attesterOgIverksett(behandlingId: UUID) {
        TODO("Not yet implemented")
    }
}
