package no.nav.etterlatte.beregning.regler.barnepensjon.sats

import no.nav.etterlatte.beregning.regler.barnepensjon.BP_1967_DATO
import no.nav.etterlatte.beregning.regler.barnepensjon.BarnepensjonGrunnlag
import no.nav.etterlatte.grunnbeloep.Grunnbeloep
import no.nav.etterlatte.grunnbeloep.GrunnbeloepRepository
import no.nav.etterlatte.libs.common.person.Folkeregisteridentifikator
import no.nav.etterlatte.libs.regler.Regel
import no.nav.etterlatte.libs.regler.RegelMeta
import no.nav.etterlatte.libs.regler.RegelReferanse
import no.nav.etterlatte.libs.regler.benytter
import no.nav.etterlatte.libs.regler.definerKonstant
import no.nav.etterlatte.libs.regler.finnFaktumIGrunnlag
import no.nav.etterlatte.libs.regler.med
import no.nav.etterlatte.libs.regler.og
import no.nav.etterlatte.libs.regler.velgNyesteGyldige
import no.nav.etterlatte.regler.Beregningstall

val historiskeGrunnbeloep = GrunnbeloepRepository.historiskeGrunnbeloep.map { grunnbeloep ->
    val grunnbeloepGyldigFra = grunnbeloep.dato.atDay(1)
    definerKonstant<BarnepensjonGrunnlag, Grunnbeloep>(
        gjelderFra = grunnbeloepGyldigFra,
        beskrivelse = "Grunnbeløp gyldig fra $grunnbeloepGyldigFra",
        regelReferanse = RegelReferanse(id = "REGEL-GRUNNBELOEP"),
        verdi = grunnbeloep
    )
}

val grunnbeloep: Regel<BarnepensjonGrunnlag, Grunnbeloep> = RegelMeta(
    gjelderFra = BP_1967_DATO,
    beskrivelse = "Finner grunnbeløp",
    regelReferanse = RegelReferanse(id = "REGEL-GRUNNBELOEP")
) velgNyesteGyldige historiskeGrunnbeloep

val soeskenIKullet: Regel<BarnepensjonGrunnlag, List<Folkeregisteridentifikator>> = finnFaktumIGrunnlag(
    gjelderFra = BP_1967_DATO,
    beskrivelse = "Søskenkull fra grunnlaget",
    finnFaktum = BarnepensjonGrunnlag::soeskenKull,
    finnFelt = { it }
)

val antallSoeskenIKullet = RegelMeta(
    gjelderFra = BP_1967_DATO,
    beskrivelse = "Finner antall søsken i kullet",
    regelReferanse = RegelReferanse(id = "BP-BEREGNING-1967-ANTALL-SOESKEN")
) benytter soeskenIKullet med { soesken -> soesken.size }

val prosentsatsFoersteBarnKonstant = definerKonstant<BarnepensjonGrunnlag, Beregningstall>(
    gjelderFra = BP_1967_DATO,
    beskrivelse = "Prosentsats benyttet for første barn",
    regelReferanse = RegelReferanse(id = "BP-BEREGNING-1967-ETTBARN"),
    verdi = Beregningstall(0.40)
)

val belopForFoersteBarn = RegelMeta(
    gjelderFra = BP_1967_DATO,
    beskrivelse = "Satser i kr av for første barn",
    regelReferanse = RegelReferanse(id = "BP-BEREGNING-1967-ETTBARN")
) benytter prosentsatsFoersteBarnKonstant og grunnbeloep med { prosentsatsFoersteBarn, grunnbeloep ->
    prosentsatsFoersteBarn.multiply(grunnbeloep.grunnbeloepPerMaaned)
}

val prosentsatsEtterfoelgendeBarnKonstant = definerKonstant<BarnepensjonGrunnlag, Beregningstall>(
    gjelderFra = BP_1967_DATO,
    beskrivelse = "Prosentsats benyttet for etterfølgende barn",
    regelReferanse = RegelReferanse(id = "BP-BEREGNING-1967-FLERBARN"),
    verdi = Beregningstall(0.25)
)

val belopForEtterfoelgendeBarn = RegelMeta(
    gjelderFra = BP_1967_DATO,
    beskrivelse = "Satser i kr av for etterfølgende barn",
    regelReferanse = RegelReferanse(id = "BP-BEREGNING-1967-FLERBARN")
) benytter prosentsatsEtterfoelgendeBarnKonstant og grunnbeloep med { prosentsatsEtterfoelgendeBarn, grunnbeloep ->
    prosentsatsEtterfoelgendeBarn.multiply(grunnbeloep.grunnbeloepPerMaaned)
}

val barnepensjonSatsRegel = RegelMeta(
    gjelderFra = BP_1967_DATO,
    beskrivelse = "Beregn uavkortet barnepensjon basert på størrelsen på barnekullet",
    regelReferanse = RegelReferanse(id = "BP-BEREGNING-1967-UAVKORTET")
) benytter belopForFoersteBarn og belopForEtterfoelgendeBarn og antallSoeskenIKullet med {
        foerstebarnSats, etterfoelgendeBarnSats, antallSoesken ->
    foerstebarnSats
        .plus(etterfoelgendeBarnSats.multiply(antallSoesken))
        .divide(antallSoesken.plus(1))
}