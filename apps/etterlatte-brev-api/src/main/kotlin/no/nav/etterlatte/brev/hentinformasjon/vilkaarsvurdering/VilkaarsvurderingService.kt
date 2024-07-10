package no.nav.etterlatte.brev.hentinformasjon.vilkaarsvurdering

import no.nav.etterlatte.libs.ktor.token.BrukerTokenInfo
import java.util.UUID

class VilkaarsvurderingService(
    private val klient: VilkaarsvurderingKlient,
) {
    suspend fun hentVilkaarsvurdering(
        behandlingId: UUID,
        bruker: BrukerTokenInfo,
    ) = klient.hentVilkaarsvurdering(behandlingId, bruker)
}
