package no.nav.etterlatte.libs.common.vikaar

import no.nav.etterlatte.libs.common.behandling.Behandlingsopplysning
import no.nav.etterlatte.libs.common.behandling.opplysningstyper.Opplysningstyper

open class VilkaarOpplysning<T>(
    val opplysningType: Opplysningstyper,
    val kilde: Behandlingsopplysning.Kilde,
    val opplysning: T
)

