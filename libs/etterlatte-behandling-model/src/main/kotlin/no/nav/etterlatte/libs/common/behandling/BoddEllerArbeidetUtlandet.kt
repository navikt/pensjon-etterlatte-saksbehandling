package no.nav.etterlatte.libs.common.behandling

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning

@JsonIgnoreProperties(ignoreUnknown = true)
data class BoddEllerArbeidetUtlandet(
    val boddEllerArbeidetUtlandet: Boolean,
    val kilde: Grunnlagsopplysning.Kilde,
    val begrunnelse: String,
    val boddArbeidetIkkeEosEllerAvtaleland: Boolean? = false,
    val boddArbeidetEosNordiskKonvensjon: Boolean? = false,
    val boddArbeidetAvtaleland: Boolean? = false,
    val vurdereAvoededsTrygdeavtale: Boolean? = false,
    val skalSendeKravpakke: Boolean? = false,
)
