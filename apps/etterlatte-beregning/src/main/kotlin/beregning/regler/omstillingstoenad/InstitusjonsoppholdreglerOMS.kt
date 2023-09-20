package no.nav.etterlatte.beregning.regler.omstillingstoenad

import no.nav.etterlatte.beregning.grunnlag.Prosent
import no.nav.etterlatte.beregning.regler.omstillingstoenad.sats.faktorKonstant
import no.nav.etterlatte.beregning.regler.omstillingstoenad.sats.grunnbeloep
import no.nav.etterlatte.libs.regler.Regel
import no.nav.etterlatte.libs.regler.RegelMeta
import no.nav.etterlatte.libs.regler.RegelReferanse
import no.nav.etterlatte.libs.regler.benytter
import no.nav.etterlatte.libs.regler.finnFaktumIGrunnlag
import no.nav.etterlatte.libs.regler.med
import no.nav.etterlatte.libs.regler.og
import no.nav.etterlatte.regler.Beregningstall

val institusjonsoppholdRegelOMS: Regel<OmstillingstoenadGrunnlag, Prosent> =
    finnFaktumIGrunnlag(
        gjelderFra = OMS_GYLDIG_FROM_TEST,
        beskrivelse = "Finner % av G mottaker skal ha for denne institusjonsoppholdytelsen",
        finnFaktum = OmstillingstoenadGrunnlag::institusjonsopphold,
    ) { it?.prosentEtterReduksjon() ?: Prosent.hundre }

val erBrukerIInstitusjonOMS: Regel<OmstillingstoenadGrunnlag, Boolean> =
    finnFaktumIGrunnlag(
        gjelderFra = OMS_GYLDIG_FROM_TEST,
        beskrivelse = "Finner om bruker har et institusjonsopphold",
        finnFaktum = OmstillingstoenadGrunnlag::institusjonsopphold,
    ) {
        it != null
    }

val institusjonsoppholdSatsRegelOMS =
    RegelMeta(
        gjelderFra = OMS_GYLDIG_FROM_TEST,
        beskrivelse = "Finner satsen for institusjonsoppholdberegning",
        regelReferanse = RegelReferanse(id = "Finner sats for bruker, gitt at de skal ha institusjonsoppholdsats"),
    ) benytter grunnbeloep og institusjonsoppholdRegelOMS og faktorKonstant med { grunnbeloep, prosent, faktor ->
        Beregningstall.somBroek(prosent).multiply(grunnbeloep.grunnbeloepPerMaaned).multiply(faktor)
    }
