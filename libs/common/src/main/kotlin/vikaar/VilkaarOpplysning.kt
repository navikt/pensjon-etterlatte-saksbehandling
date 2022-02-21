package no.nav.etterlatte.libs.common.vikaar

import no.nav.etterlatte.libs.common.behandling.Behandlingsopplysning

open class VilkaarOpplysning<T>(
    val opplysningsType: String,
    val kilde: Behandlingsopplysning.Kilde,
    val opplysning: T
)

