package beregning.regler.barnepensjon

import no.nav.etterlatte.beregning.grunnlag.Prosent
import no.nav.etterlatte.beregning.regler.barnepensjon.BP_1967_DATO
import no.nav.etterlatte.beregning.regler.barnepensjon.BarnepensjonGrunnlag
import no.nav.etterlatte.beregning.regler.barnepensjon.sats.grunnbeloep
import no.nav.etterlatte.libs.regler.Regel
import no.nav.etterlatte.libs.regler.RegelMeta
import no.nav.etterlatte.libs.regler.RegelReferanse
import no.nav.etterlatte.libs.regler.benytter
import no.nav.etterlatte.libs.regler.finnFaktumIGrunnlag
import no.nav.etterlatte.libs.regler.med
import no.nav.etterlatte.libs.regler.og
import no.nav.etterlatte.regler.Beregningstall

val institusjonsoppholdRegel: Regel<BarnepensjonGrunnlag, Prosent> =
    finnFaktumIGrunnlag(
        gjelderFra = BP_1967_DATO,
        beskrivelse = "Finner % av G mottaker skal ha for denne institusjonsoppholdytelsen",
        finnFaktum = BarnepensjonGrunnlag::institusjonsopphold,
    ) { it?.prosentEtterReduksjon() ?: Prosent.hundre }

val erBrukerIInstitusjon: Regel<BarnepensjonGrunnlag, Boolean> =
    finnFaktumIGrunnlag(
        gjelderFra = BP_1967_DATO,
        beskrivelse = "Finner om bruker har et institusjonsopphold",
        finnFaktum = BarnepensjonGrunnlag::institusjonsopphold,
    ) {
        it != null
    }

val institusjonsoppholdSatsRegel =
    RegelMeta(
        gjelderFra = BP_1967_DATO,
        beskrivelse = "Finner satsen for institusjonsoppholdberegning",
        regelReferanse = RegelReferanse(id = "BP-BEREGNING-1967-REDUKSJON-INSTITUSJON"),
    ) benytter grunnbeloep og institusjonsoppholdRegel med { grunnbeloep, prosent ->
        Beregningstall.somBroek(prosent).multiply(grunnbeloep.grunnbeloepPerMaaned)
    }
