package no.nav.etterlatte.libs.common.vikaar

import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstyper

open class VilkaarOpplysning<T>(
    val opplysningType: Opplysningstyper,
    val kilde: Grunnlagsopplysning.Kilde,
    val opplysning: T
)

