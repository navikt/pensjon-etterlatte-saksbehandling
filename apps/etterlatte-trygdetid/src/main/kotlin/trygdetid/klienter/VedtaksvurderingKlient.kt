package no.nav.etterlatte.trygdetid.klienter

import no.nav.etterlatte.libs.common.sak.SakId
import no.nav.etterlatte.libs.common.vedtak.VedtakSammendragDto
import no.nav.etterlatte.libs.ktor.token.BrukerTokenInfo

interface VedtaksvurderingKlient {
    suspend fun hentIverksatteVedtak(
        sakId: SakId,
        brukerTokenInfo: BrukerTokenInfo,
    ): List<VedtakSammendragDto>
}

class VedtaksvurderingKlientException(
    override val message: String,
    override val cause: Throwable,
) : Exception(message, cause)
