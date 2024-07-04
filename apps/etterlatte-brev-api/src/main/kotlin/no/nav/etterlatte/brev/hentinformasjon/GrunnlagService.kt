package no.nav.etterlatte.brev.hentinformasjon

import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import no.nav.etterlatte.libs.common.sak.Sak
import no.nav.etterlatte.libs.common.vedtak.VedtakType
import no.nav.etterlatte.libs.ktor.token.BrukerTokenInfo
import java.util.UUID

class GrunnlagService(
    private val klient: GrunnlagKlient,
) {
    suspend fun hentGrunnlag(
        vedtakType: VedtakType?,
        sakId: Long,
        bruker: BrukerTokenInfo,
        behandlingId: UUID?,
    ) = coroutineScope {
        when (vedtakType) {
            VedtakType.TILBAKEKREVING,
            VedtakType.AVVIST_KLAGE,
            -> async { hentGrunnlagForSak(sakId, bruker) }.await()

            null -> async { hentGrunnlagForSak(sakId, bruker) }.await()
            else -> async { klient.hentGrunnlag(behandlingId!!, bruker) }.await()
        }
    }

    suspend fun hentGrunnlagForSak(
        sakId: Long,
        bruker: BrukerTokenInfo,
    ) = klient.hentGrunnlagForSak(sakId, bruker)

    suspend fun hentGrunnlag(
        behandlingId: UUID,
        bruker: BrukerTokenInfo,
    ) = klient.hentGrunnlag(behandlingId, bruker)

    suspend fun oppdaterGrunnlagForSak(
        sak: Sak,
        bruker: BrukerTokenInfo,
    ) = klient.oppdaterGrunnlagForSak(sak, bruker)
}
