package no.nav.etterlatte.libs.common.vikaar

import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstype
import java.util.*

open class VilkaarOpplysning<T>(
    val id: UUID,
    val opplysningType: Opplysningstype,
    val kilde: Grunnlagsopplysning.Kilde,
    val opplysning: T
)