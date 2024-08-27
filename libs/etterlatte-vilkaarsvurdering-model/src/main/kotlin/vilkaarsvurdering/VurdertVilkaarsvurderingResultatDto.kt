package no.nav.etterlatte.libs.vilkaarsvurdering

import no.nav.etterlatte.libs.common.vilkaarsvurdering.Vilkaarsvurdering
import no.nav.etterlatte.libs.common.vilkaarsvurdering.VilkaarsvurderingResultat
import no.nav.etterlatte.libs.common.vilkaarsvurdering.VilkaarsvurderingUtfall
import java.time.LocalDate

data class VurdertVilkaarsvurderingResultatDto(
    val resultat: VilkaarsvurderingUtfall,
    val kommentar: String?,
)

class VurdertVilkaarsvurderingDto(
    val virkningstidspunkt: LocalDate,
    val resultat: VilkaarsvurderingResultat,
    val vilkaarsvurdering: Vilkaarsvurdering,
)
