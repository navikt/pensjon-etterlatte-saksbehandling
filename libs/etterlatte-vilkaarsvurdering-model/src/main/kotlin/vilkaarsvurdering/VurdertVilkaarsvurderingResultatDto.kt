package no.nav.etterlatte.libs.vilkaarsvurdering

import no.nav.etterlatte.libs.common.vilkaarsvurdering.VilkaarsvurderingUtfall

data class VurdertVilkaarsvurderingResultatDto(
    val resultat: VilkaarsvurderingUtfall,
    val kommentar: String?
)