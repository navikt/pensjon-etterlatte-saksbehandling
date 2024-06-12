package no.nav.etterlatte.brev.hentinformasjon

import no.nav.etterlatte.brev.behandlingklient.BehandlingKlient
import no.nav.etterlatte.libs.ktor.token.BrukerTokenInfo

class SakService(
    private val behandlingKlient: BehandlingKlient,
) {
    suspend fun hentSak(
        sakId: Long,
        bruker: BrukerTokenInfo,
    ) = behandlingKlient.hentSak(sakId, bruker)
}
