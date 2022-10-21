package no.nav.etterlatte.vilkaarsvurdering

import no.nav.etterlatte.libs.common.vilkaarsvurdering.Vilkaar
import no.nav.etterlatte.libs.common.vilkaarsvurdering.VilkaarsvurderingResultat
import java.time.LocalDate
import java.util.*

data class VilkaarsvurderingIntern(
    val behandlingId: UUID,
    val payload: String,
    val vilkaar: List<Vilkaar>,
    val virkningstidspunkt: LocalDate,
    val resultat: VilkaarsvurderingResultat? = null
)