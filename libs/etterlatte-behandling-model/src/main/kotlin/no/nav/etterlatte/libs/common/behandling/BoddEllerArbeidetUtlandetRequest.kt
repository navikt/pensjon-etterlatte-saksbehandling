package no.nav.etterlatte.libs.common.behandling

data class BoddEllerArbeidetUtlandetRequest(
    val boddEllerArbeidetUtlandet: Boolean,
    val begrunnelse: String,
    val boddArbeidetIkkeEosEllerAvtaleland: Boolean = false,
    val boddArbeidetEosNordiskKonvensjon: Boolean = false,
    val boddArbeidetAvtaleland: Boolean = false,
    val vurdereAvoededsTrygdeavtale: Boolean = false,
    val skalSendeKravpakke: Boolean = false,
)
