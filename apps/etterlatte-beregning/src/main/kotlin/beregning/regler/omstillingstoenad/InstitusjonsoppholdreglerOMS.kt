package no.nav.etterlatte.beregning.regler.omstillingstoenad

import no.nav.etterlatte.beregning.grunnlag.Prosent
import no.nav.etterlatte.beregning.grunnlag.Reduksjon
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
        gjelderFra = OMS_GYLDIG_FRA,
        beskrivelse = "Finner % av G mottaker skal ha for denne institusjonsoppholdytelsen",
        finnFaktum = OmstillingstoenadGrunnlag::institusjonsopphold,
    ) { it?.prosentEtterReduksjon() ?: Prosent.hundre }

val brukerHarTellendeInstitusjonsopphold: Regel<OmstillingstoenadGrunnlag, Boolean> =
    finnFaktumIGrunnlag(
        gjelderFra = OMS_GYLDIG_FRA,
        beskrivelse = "Finner om bruker har et institusjonsopphold",
        finnFaktum = OmstillingstoenadGrunnlag::institusjonsopphold,
    ) {
        it != null && it.reduksjon != Reduksjon.NEI_KORT_OPPHOLD
    }

val brukerHarSanksjon: Regel<OmstillingstoenadGrunnlag, Boolean> =
    finnFaktumIGrunnlag(
        OMS_GYLDIG_FRA,
        beskrivelse = "Finner om bruker har sanksjon",
        finnFaktum = OmstillingstoenadGrunnlag::sanksjon,
    ) {
        it != null
    }

val institusjonsoppholdSatsRegelOMS =
    RegelMeta(
        gjelderFra = OMS_GYLDIG_FRA,
        beskrivelse = "Finner satsen for institusjonsoppholdberegning",
        regelReferanse = RegelReferanse(id = "OMS-BEREGNING-2024-INSTITUSJONSOPPHOLD-SATS"),
    ) benytter grunnbeloep og institusjonsoppholdRegelOMS med { grunnbeloep, prosent ->
        Beregningstall.somBroek(prosent).multiply(grunnbeloep.grunnbeloepPerMaaned)
    }
