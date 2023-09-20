package no.nav.etterlatte.beregning.regler.barnepensjon.trygdetidsfaktor

import no.nav.etterlatte.beregning.regler.barnepensjon.BP_1967_DATO
import no.nav.etterlatte.beregning.regler.barnepensjon.BarnepensjonGrunnlag
import no.nav.etterlatte.libs.regler.Regel
import no.nav.etterlatte.libs.regler.RegelMeta
import no.nav.etterlatte.libs.regler.RegelReferanse
import no.nav.etterlatte.libs.regler.benytter
import no.nav.etterlatte.libs.regler.definerKonstant
import no.nav.etterlatte.libs.regler.finnFaktumIGrunnlag
import no.nav.etterlatte.libs.regler.med
import no.nav.etterlatte.libs.regler.og
import no.nav.etterlatte.regler.Beregningstall

val trygdetidRegel: Regel<BarnepensjonGrunnlag, Beregningstall> =
    finnFaktumIGrunnlag(
        gjelderFra = BP_1967_DATO,
        beskrivelse = "Finner avdødes trygdetid",
        finnFaktum = BarnepensjonGrunnlag::avdoedesTrygdetid,
        finnFelt = { it }
    )

val maksTrygdetid = definerKonstant<BarnepensjonGrunnlag, Beregningstall>(
    gjelderFra = BP_1967_DATO,
    beskrivelse = "Full trygdetidsopptjening er 40 år",
    regelReferanse = RegelReferanse("BP-BEREGNING-1967-TRYGDETIDSFAKTOR"),
    verdi = Beregningstall(40)
)

val trygdetidsFaktor = RegelMeta(
    gjelderFra = BP_1967_DATO,
    beskrivelse = "Finn trygdetidsfaktor",
    regelReferanse = RegelReferanse(id = "BP-BEREGNING-1967-TRYGDETIDSFAKTOR")
) benytter maksTrygdetid og trygdetidRegel med { maksTrygdetid, trygdetid ->
    minOf(trygdetid, maksTrygdetid).divide(maksTrygdetid)
}