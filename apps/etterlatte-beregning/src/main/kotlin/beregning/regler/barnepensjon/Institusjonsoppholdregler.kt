package beregning.regler.barnepensjon

import no.nav.etterlatte.beregning.grunnlag.Prosent
import no.nav.etterlatte.beregning.regler.barnepensjon.BP_1967_DATO
import no.nav.etterlatte.beregning.regler.barnepensjon.BarnepensjonGrunnlag
import no.nav.etterlatte.libs.regler.Regel
import no.nav.etterlatte.libs.regler.RegelReferanse
import no.nav.etterlatte.libs.regler.definerKonstant
import no.nav.etterlatte.libs.regler.finnFaktumIGrunnlag
import no.nav.etterlatte.regler.Beregningstall

val institusjonsoppholdFaktor = definerKonstant<BarnepensjonGrunnlag, Beregningstall>(
    gjelderFra = BP_1967_DATO,
    beskrivelse = "Institusjonsopphold har i utgangspunktet 10% av full sats",
    regelReferanse = RegelReferanse("BP-BEREGNING-1967-INSTITUSJONSOPPHOLD-FAKTOR"),
    verdi = Beregningstall(0.1)
)

val institusjonsoppholdRegel: Regel<BarnepensjonGrunnlag, Prosent> =
    finnFaktumIGrunnlag(
        gjelderFra = BP_1967_DATO,
        beskrivelse = "Finner søkers institusjonsopphold",
        finnFaktum = BarnepensjonGrunnlag::institusjonsopphold
    ) { it?.prosentEtterReduksjon() ?: Prosent.hundre }