package no.nav.etterlatte.libs.regler.beregning.barnepensjon1967.trygdetidsfaktor

import no.nav.etterlatte.libs.regler.Regel
import no.nav.etterlatte.libs.regler.RegelMeta
import no.nav.etterlatte.libs.regler.beregning.AvdoedForelder
import no.nav.etterlatte.libs.regler.beregning.BarnepensjonGrunnlag
import no.nav.etterlatte.libs.regler.beregning.barnepensjon1967.BP_1967_DATO
import no.nav.etterlatte.libs.regler.definerKonstant
import no.nav.etterlatte.libs.regler.finnFaktumIGrunnlag
import no.nav.etterlatte.libs.regler.kombinerer
import no.nav.etterlatte.libs.regler.med
import no.nav.etterlatte.libs.regler.og
import java.math.BigDecimal

private val trygdetidRegel: Regel<BarnepensjonGrunnlag, BigDecimal> =
    finnFaktumIGrunnlag(
        gjelderFra = BP_1967_DATO,
        beskrivelse = "Finner avdødes trygdetid",
        regelReferanse = LesInputReferanse(beskrivelse = "Henter faktisk trygdetid fra input til beregningen"),
        finnFaktum = BarnepensjonGrunnlag::avdoedForelder,
        finnFelt = AvdoedForelder::trygdetid
    )

private val maksTrygdetid = definerKonstant<BarnepensjonGrunnlag, BigDecimal>(
    gjelderFra = BP_1967_DATO,
    beskrivelse = "Full trygdetidsopptjening er 40 år",
    regelReferanse = GenerellRegel("BEREGNING-G-TTF", "Full trygdetidsopptjening er 40 år"),
    verdi = BigDecimal(40)
)

val trygdetidsFaktor = RegelMeta(
    gjelderFra = BP_1967_DATO,
    beskrivelse = "Finn trygdetidsfaktor",
    regelReferanse = BarnepensjonGammeltRegelverk(id = "BEREGNING-G-TTF", beskrivelse = "Finn trygdetidsfaktor")
) kombinerer maksTrygdetid og trygdetidRegel med { maksTrygdetid, trygdetid ->
    minOf(trygdetid, maksTrygdetid) / maksTrygdetid
}