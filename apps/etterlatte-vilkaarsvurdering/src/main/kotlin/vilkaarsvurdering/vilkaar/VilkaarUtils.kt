package no.nav.etterlatte.vilkaarsvurdering.vilkaar

import no.nav.etterlatte.libs.common.grunnlag.Opplysning
import no.nav.etterlatte.libs.common.vilkaarsvurdering.VilkaarOpplysningType
import no.nav.etterlatte.libs.common.vilkaarsvurdering.Vilkaarsgrunnlag

internal fun <T> Opplysning.Konstant<out T>.toVilkaarsgrunnlag(type: VilkaarOpplysningType) =
    Vilkaarsgrunnlag(
        id = id,
        opplysningsType = type,
        kilde = kilde,
        opplysning = verdi,
    )
