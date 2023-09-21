package no.nav.etterlatte.libs.common.behandling

import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning

data class BoddEllerArbeidetUtlandet(
    val boddEllerArbeidetUtlandet: Boolean,
    val kilde: Grunnlagsopplysning.Saksbehandler,
    val begrunnelse: String,
    val boddArbeidetIkkeEosEllerAvtaleland: Boolean? = false,
    val boddArbeidetEosNordiskKonvensjon: Boolean? = false,
    val boddArbeidetAvtaleland: Boolean? = false,
    val vurdereAvoededsTrygdeavtale: Boolean? = false,
    val norgeErBehandlendeland: Boolean? = false,
    val skalSendeKravpakke: Boolean? = false,
)
