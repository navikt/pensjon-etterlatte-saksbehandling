package no.nav.etterlatte.beregning.regler.omstillingstoenad

import no.nav.etterlatte.beregning.regler.Beregningstall
import no.nav.etterlatte.libs.regler.FaktumNode
import no.nav.etterlatte.libs.regler.RegelMeta
import no.nav.etterlatte.libs.regler.RegelReferanse
import no.nav.etterlatte.libs.regler.benytter
import no.nav.etterlatte.libs.regler.definerKonstant
import no.nav.etterlatte.libs.regler.med

data class Avdoed(val trygdetid: Beregningstall)
data class OmstillingstoenadGrunnlag(
    val avdoed: FaktumNode<Avdoed>
)

val hardkodetOmstillingstoenadSum = definerKonstant<OmstillingstoenadGrunnlag, Beregningstall>(
    gjelderFra = OMS_2024_DATO,
    beskrivelse = "Hardkodet sum omstillingstønad",
    regelReferanse = RegelReferanse(id = "TODO"),
    verdi = Beregningstall(100.00)
)

val kroneavrundetOmstillingstoenadRegel = RegelMeta(
    gjelderFra = OMS_2024_DATO,
    beskrivelse = "Gjør en kroneavrunding av omstillingstønad",
    regelReferanse = RegelReferanse(id = "REGEL-KRONEAVRUNDING")
) benytter hardkodetOmstillingstoenadSum med { beregnetOmstillingstoenad ->
    beregnetOmstillingstoenad.round(decimals = 0).toInteger()
}