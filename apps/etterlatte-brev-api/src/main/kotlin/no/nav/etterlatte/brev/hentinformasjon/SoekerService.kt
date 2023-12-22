package no.nav.etterlatte.brev.hentinformasjon

import no.nav.etterlatte.brev.behandling.mapSoeker
import no.nav.etterlatte.libs.common.behandling.BrevutfallDto
import no.nav.etterlatte.token.BrukerTokenInfo

class SoekerService(private val grunnlagKlient: GrunnlagKlient) {
    suspend fun hentSoeker(
        sakId: Long,
        bruker: BrukerTokenInfo,
        brevutfall: BrevutfallDto?,
    ) = grunnlagKlient.hentGrunnlagForSak(sakId, bruker).mapSoeker(brevutfall)
}
