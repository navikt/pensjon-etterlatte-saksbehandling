package no.nav.etterlatte.testdata.automatisk

import com.github.michaelbull.result.mapBoth
import no.nav.etterlatte.libs.ktor.ktor.ktorobo.DownstreamResourceClient
import no.nav.etterlatte.libs.ktor.ktor.ktorobo.Resource
import no.nav.etterlatte.libs.ktor.token.Systembruker
import no.nav.etterlatte.rapidsandrivers.migrering.MigreringKjoringVariant
import java.util.UUID

class VedtaksvurderingService(private val klient: DownstreamResourceClient, private val url: String, private val clientId: String) {
    suspend fun fattVedtak(
        sakId: Long,
        behandlingId: UUID,
    ) = klient.post(
        Resource(clientId, "$url/api/vedtak/$sakId/$behandlingId/automatisk/stegvis"),
        Systembruker.testdata,
        MigreringKjoringVariant.MED_PAUSE,
    ).mapBoth(
        success = {},
        failure = { throw it },
    )

    suspend fun attesterOgIverksettVedtak(
        sakId: Long,
        behandlingId: UUID,
    ) = klient.post(
        Resource(clientId, "$url/api/vedtak/$sakId/$behandlingId/automatisk/stegvis"),
        Systembruker.testdata,
        MigreringKjoringVariant.FORTSETT_ETTER_PAUSE,
    ).mapBoth(
        success = {},
        failure = { throw it },
    )
}
