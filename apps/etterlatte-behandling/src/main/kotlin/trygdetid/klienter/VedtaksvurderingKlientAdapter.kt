package no.nav.etterlatte.trygdetid.klienter

import no.nav.etterlatte.behandling.vedtaksvurdering.service.VedtaksvurderingService
import no.nav.etterlatte.behandling.vedtaksvurdering.toVedtakSammendragDto
import no.nav.etterlatte.libs.common.sak.SakId
import no.nav.etterlatte.libs.common.vedtak.VedtakSammendragDto
import no.nav.etterlatte.libs.ktor.token.BrukerTokenInfo

class VedtaksvurderingKlientAdapter(
    private val vedtaksvurderingService: VedtaksvurderingService,
) : VedtaksvurderingKlient {
    override suspend fun hentIverksatteVedtak(
        sakId: SakId,
        brukerTokenInfo: BrukerTokenInfo,
    ): List<VedtakSammendragDto> = vedtaksvurderingService.hentIverksatteVedtakISak(sakId).map { it.toVedtakSammendragDto() }
}
