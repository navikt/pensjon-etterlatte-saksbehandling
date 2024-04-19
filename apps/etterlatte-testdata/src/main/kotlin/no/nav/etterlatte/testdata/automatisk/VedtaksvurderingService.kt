package no.nav.etterlatte.no.nav.etterlatte.testdata.automatisk

import io.ktor.client.HttpClient
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import no.nav.etterlatte.rapidsandrivers.migrering.MigreringKjoringVariant
import java.util.UUID

class VedtaksvurderingService(private val klient: HttpClient, private val url: String) {
    suspend fun fattVedtak(
        sakId: Long,
        behandlingId: UUID,
    ) = klient.post("$url/api/vedtak/$sakId/$behandlingId/automatisk/stegvis") {
        contentType(ContentType.Application.Json)
        setBody(MigreringKjoringVariant.MED_PAUSE)
    }

    suspend fun attesterOgIverksettVedtak(
        sakId: Long,
        behandlingId: UUID,
    ) = klient.post("$url/api/vedtak/$sakId/$behandlingId/automatisk/stegvis") {
        contentType(ContentType.Application.Json)
        setBody(MigreringKjoringVariant.FORTSETT_ETTER_PAUSE)
    }
}
