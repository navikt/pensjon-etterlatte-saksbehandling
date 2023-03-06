package no.nav.etterlatte.beregning.regler.omstillingstoenad.trygdetidsfaktor

import no.nav.etterlatte.beregning.regler.Beregningstall
import no.nav.etterlatte.beregning.regler.omstillingstoenad.Avdoed
import no.nav.etterlatte.beregning.regler.omstillingstoenad.OMS_GYLDIG_FROM_TEST
import no.nav.etterlatte.beregning.regler.omstillingstoenad.OmstillingstoenadGrunnlag
import no.nav.etterlatte.libs.regler.Regel
import no.nav.etterlatte.libs.regler.RegelMeta
import no.nav.etterlatte.libs.regler.RegelReferanse
import no.nav.etterlatte.libs.regler.benytter
import no.nav.etterlatte.libs.regler.definerKonstant
import no.nav.etterlatte.libs.regler.finnFaktumIGrunnlag
import no.nav.etterlatte.libs.regler.med
import no.nav.etterlatte.libs.regler.og

val trygdetidRegel: Regel<OmstillingstoenadGrunnlag, Beregningstall> =
    finnFaktumIGrunnlag(
        gjelderFra = OMS_GYLDIG_FROM_TEST,
        beskrivelse = "Finner avdødes trygdetid",
        finnFaktum = OmstillingstoenadGrunnlag::avdoed,
        finnFelt = Avdoed::trygdetid
    )

val maksTrygdetid = definerKonstant<OmstillingstoenadGrunnlag, Beregningstall>(
    gjelderFra = OMS_GYLDIG_FROM_TEST,
    beskrivelse = "Full trygdetidsopptjening er 40 år",
    regelReferanse = RegelReferanse("TODO"),
    verdi = Beregningstall(40)
)

val trygdetidsFaktor = RegelMeta(
    gjelderFra = OMS_GYLDIG_FROM_TEST,
    beskrivelse = "Finn trygdetidsfaktor",
    regelReferanse = RegelReferanse(id = "TODO")
) benytter maksTrygdetid og trygdetidRegel med { maksTrygdetid, trygdetid ->
    minOf(trygdetid, maksTrygdetid).divide(maksTrygdetid)
}