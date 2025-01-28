package no.nav.etterlatte.testdata.automatisk

import com.github.michaelbull.result.mapBoth
import no.nav.etterlatte.libs.common.retryOgPakkUt
import no.nav.etterlatte.libs.common.sak.SakId
import no.nav.etterlatte.libs.ktor.ktor.ktorobo.DownstreamResourceClient
import no.nav.etterlatte.libs.ktor.ktor.ktorobo.Resource
import no.nav.etterlatte.libs.ktor.token.BrukerTokenInfo
import no.nav.etterlatte.rapidsandrivers.migrering.MigreringKjoringVariant
import no.nav.etterlatte.readValue
import no.nav.etterlatte.vedtaksvurdering.VedtakOgRapid
import java.util.UUID

class VedtaksvurderingService(
    private val klient: DownstreamResourceClient,
    private val url: String,
    private val clientId: String,
) {
    suspend fun fattVedtak(
        sakId: SakId,
        behandlingId: UUID,
        bruker: BrukerTokenInfo,
    ): VedtakOgRapid =
        retryOgPakkUt {
            klient
                .post(
                    Resource(clientId, "$url/api/vedtak/${sakId.value}/$behandlingId/automatisk/stegvis"),
                    bruker,
                    MigreringKjoringVariant.MED_PAUSE,
                ).mapBoth(
                    success = { readValue(it) },
                    failure = { throw it },
                )
        }

    suspend fun attesterOgIverksettVedtak(
        sakId: SakId,
        behandlingId: UUID,
        bruker: BrukerTokenInfo,
    ): VedtakOgRapid =
        retryOgPakkUt {
            klient
                .post(
                    Resource(clientId, "$url/api/vedtak/${sakId.value}/$behandlingId/automatisk/stegvis"),
                    bruker,
                    MigreringKjoringVariant.FORTSETT_ETTER_PAUSE,
                ).mapBoth(
                    success = { readValue(it) },
                    failure = { throw it },
                )
        }
}
