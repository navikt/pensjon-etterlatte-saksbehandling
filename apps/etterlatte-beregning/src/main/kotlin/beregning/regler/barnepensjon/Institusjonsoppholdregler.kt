package beregning.regler.barnepensjon

import no.nav.etterlatte.beregning.grunnlag.Prosent
import no.nav.etterlatte.beregning.regler.barnepensjon.BP_1967_DATO
import no.nav.etterlatte.beregning.regler.barnepensjon.BarnepensjonGrunnlag
import no.nav.etterlatte.libs.regler.Regel
import no.nav.etterlatte.libs.regler.finnFaktumIGrunnlag

val institusjonsoppholdRegel: Regel<BarnepensjonGrunnlag, Prosent> =
    finnFaktumIGrunnlag(
        gjelderFra = BP_1967_DATO,
        beskrivelse = "Finner søkers institusjonsopphold",
        finnFaktum = BarnepensjonGrunnlag::institusjonsopphold
    ) { it?.prosentEtterReduksjon() ?: Prosent.hundre }