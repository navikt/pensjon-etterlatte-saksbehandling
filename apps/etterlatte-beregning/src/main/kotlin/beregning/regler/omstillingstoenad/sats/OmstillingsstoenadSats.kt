package no.nav.etterlatte.beregning.regler.omstillingstoenad.sats

import no.nav.etterlatte.beregning.grunnbeloep.Grunnbeloep
import no.nav.etterlatte.beregning.grunnbeloep.GrunnbeloepRepository
import no.nav.etterlatte.beregning.regler.Beregningstall
import no.nav.etterlatte.beregning.regler.omstillingstoenad.OMS_GYLDIG_FROM_TEST
import no.nav.etterlatte.beregning.regler.omstillingstoenad.OmstillingstoenadGrunnlag
import no.nav.etterlatte.libs.regler.Regel
import no.nav.etterlatte.libs.regler.RegelMeta
import no.nav.etterlatte.libs.regler.RegelReferanse
import no.nav.etterlatte.libs.regler.benytter
import no.nav.etterlatte.libs.regler.definerKonstant
import no.nav.etterlatte.libs.regler.med
import no.nav.etterlatte.libs.regler.og
import no.nav.etterlatte.libs.regler.velgNyesteGyldige

val historiskeGrunnbeloep = GrunnbeloepRepository.historiskeGrunnbeloep.map { grunnbeloep ->
    val grunnbeloepGyldigFra = grunnbeloep.dato.atDay(1)
    definerKonstant<OmstillingstoenadGrunnlag, Grunnbeloep>(
        gjelderFra = grunnbeloepGyldigFra,
        beskrivelse = "Grunnbeløp gyldig fra $grunnbeloepGyldigFra",
        regelReferanse = RegelReferanse(id = "REGEL-GRUNNBELOEP"),
        verdi = grunnbeloep
    )
}

val grunnbeloep: Regel<OmstillingstoenadGrunnlag, Grunnbeloep> = RegelMeta(
    gjelderFra = OMS_GYLDIG_FROM_TEST,
    beskrivelse = "Finner grunnbeløp",
    regelReferanse = RegelReferanse(id = "REGEL-GRUNNBELOEP")
) velgNyesteGyldige historiskeGrunnbeloep

val faktorKonstant = definerKonstant<OmstillingstoenadGrunnlag, Beregningstall>(
    gjelderFra = OMS_GYLDIG_FROM_TEST,
    beskrivelse = "Faktor for omstillingsstønad",
    regelReferanse = RegelReferanse(id = "TODO"),
    verdi = Beregningstall(2.25)
)

val omstillingsstoenadSatsRegel = RegelMeta(
    gjelderFra = OMS_GYLDIG_FROM_TEST,
    beskrivelse = "Beregn uavkortet omstillingsstønad",
    regelReferanse = RegelReferanse(id = "TODO")
) benytter faktorKonstant og grunnbeloep med { faktor, grunnbeloep ->
    faktor
        .multiply(grunnbeloep.grunnbeloep)
        .divide(12)
}