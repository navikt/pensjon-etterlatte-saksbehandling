package no.nav.etterlatte.brev.hentinformasjon.beregning

import no.nav.etterlatte.brev.hentinformasjon.BeregningKlient
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.token.BrukerTokenInfo
import java.util.UUID

class BeregningService(private val beregningKlient: BeregningKlient) {
    suspend fun hentGrunnbeloep(bruker: BrukerTokenInfo) = beregningKlient.hentGrunnbeloep(bruker)

    suspend fun hentBeregning(
        behandlingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
    ) = beregningKlient.hentBeregning(behandlingId, brukerTokenInfo)

    suspend fun hentBeregningsGrunnlag(
        behandlingId: UUID,
        sakType: SakType,
        brukerTokenInfo: BrukerTokenInfo,
    ) = beregningKlient.hentBeregningsGrunnlag(behandlingId, sakType, brukerTokenInfo)

    suspend fun hentYtelseMedGrunnlag(
        behandlingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
    ) = beregningKlient.hentYtelseMedGrunnlag(behandlingId, brukerTokenInfo)
}
