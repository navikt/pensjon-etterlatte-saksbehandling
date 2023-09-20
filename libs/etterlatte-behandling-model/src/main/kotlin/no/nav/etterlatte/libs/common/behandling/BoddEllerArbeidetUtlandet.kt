package no.nav.etterlatte.libs.common.behandling

import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning

data class BoddEllerArbeidetUtlandet(
    val boddEllerArbeidetUtlandet: Boolean,
    val kilde: Grunnlagsopplysning.Saksbehandler,
    val begrunnelse: String,
)
