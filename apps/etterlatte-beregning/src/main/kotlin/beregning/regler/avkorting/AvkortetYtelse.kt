package no.nav.etterlatte.beregning.regler.avkorting

import no.nav.etterlatte.beregning.regler.Beregningstall
import no.nav.etterlatte.beregning.regler.omstillingstoenad.OMS_GYLDIG_FROM_TEST
import no.nav.etterlatte.libs.regler.FaktumNode
import no.nav.etterlatte.libs.regler.Regel
import no.nav.etterlatte.libs.regler.RegelMeta
import no.nav.etterlatte.libs.regler.RegelPeriode
import no.nav.etterlatte.libs.regler.RegelReferanse
import no.nav.etterlatte.libs.regler.benytter
import no.nav.etterlatte.libs.regler.finnFaktumIGrunnlag
import no.nav.etterlatte.libs.regler.med
import no.nav.etterlatte.libs.regler.og

data class AvkortetYtelseGrunnlag(
    val periode: RegelPeriode,
    val bruttoYtelse: FaktumNode<Int>,
    val avkorting: FaktumNode<Int>
)

val bruttoYtelse: Regel<AvkortetYtelseGrunnlag, Int> = finnFaktumIGrunnlag(
    gjelderFra = OMS_GYLDIG_FROM_TEST,
    beskrivelse = "Finner bruttoytelse",
    finnFaktum = AvkortetYtelseGrunnlag::bruttoYtelse,
    finnFelt = { it }
)

val avkortingsbeloep: Regel<AvkortetYtelseGrunnlag, Int> = finnFaktumIGrunnlag(
    gjelderFra = OMS_GYLDIG_FROM_TEST,
    beskrivelse = "Finner avkortignsbeløp",
    finnFaktum = AvkortetYtelseGrunnlag::avkorting,
    finnFelt = { it }
)

val avkorteYtelse = RegelMeta(
    gjelderFra = OMS_GYLDIG_FROM_TEST,
    beskrivelse = "Finner endelig ytelse ved å trekke avkortingsbeløp fra bruttoytelse",
    regelReferanse = RegelReferanse(id = "TODO")
) benytter bruttoYtelse og avkortingsbeloep med { bruttoYtelse, avkortingsbeloep ->
    Beregningstall(bruttoYtelse).minus(Beregningstall(avkortingsbeloep)).toInteger()
}