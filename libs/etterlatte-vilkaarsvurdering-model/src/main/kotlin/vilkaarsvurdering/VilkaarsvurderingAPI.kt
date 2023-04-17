package vilkaarsvurdering

import no.nav.etterlatte.libs.common.Route
import no.nav.etterlatte.vilkaarsvurdering.OpprettVilkaarsvurderingFraBehandling

object VilkaarsvurderingAPI {
    const val basisrute = "api/vilkaarsvurdering"
    fun kopier(behandlingsId: String) =
        Route("/$behandlingsId/kopier", basisrute, OpprettVilkaarsvurderingFraBehandling::class.java)

    fun opprett(behandlingsId: String) = Route("/$behandlingsId/opprett", basisrute, Unit::class.java)
}