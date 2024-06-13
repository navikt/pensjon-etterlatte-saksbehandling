package no.nav.etterlatte.brev.hentinformasjon

import no.nav.etterlatte.libs.common.vedtak.VedtakStatus
import no.nav.etterlatte.libs.ktor.token.BrukerTokenInfo
import java.util.UUID

class VedtaksvurderingService(
    private val vedtaksvurderingKlient: VedtaksvurderingKlient,
) {
    suspend fun hentVedtakSaksbehandlerOgStatus(
        behandlingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
    ): Pair<String, VedtakStatus> {
        val vedtakDto = requireNotNull(vedtaksvurderingKlient.hentVedtak(behandlingId, brukerTokenInfo))
        val saksbehandlerIdent = vedtakDto.vedtakFattet?.ansvarligSaksbehandler ?: brukerTokenInfo.ident()

        return Pair(saksbehandlerIdent, vedtakDto.status)
    }

    suspend fun hentVedtak(
        behandlingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
    ) = vedtaksvurderingKlient.hentVedtak(behandlingId, brukerTokenInfo)
}
