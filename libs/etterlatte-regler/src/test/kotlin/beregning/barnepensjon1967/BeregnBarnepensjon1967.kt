package no.nav.etterlatte.libs.regler.beregning.barnepensjon1967

import no.nav.etterlatte.libs.regler.RegelMeta
import no.nav.etterlatte.libs.regler.RegelReferanse
import no.nav.etterlatte.libs.regler.beregning.BarnepensjonGrunnlag
import no.nav.etterlatte.libs.regler.beregning.barnepensjon1967.barnekull.barnekullRegel
import no.nav.etterlatte.libs.regler.beregning.barnepensjon1967.trygdetidsfaktor.trygdetidsFaktor
import no.nav.etterlatte.libs.regler.definerKonstant
import no.nav.etterlatte.libs.regler.kombinerer
import no.nav.etterlatte.libs.regler.med
import no.nav.etterlatte.libs.regler.og
import java.math.RoundingMode
import java.time.LocalDate

val BP_1967_DATO: LocalDate = LocalDate.of(1967, 1, 1)

val reduksjonMotFolketrygdRegel = RegelMeta(
    gjelderFra = BP_1967_DATO,
    beskrivelse = "Reduserer ytelsen mot opptjening i folketrygden",
    regelReferanse = RegelReferanse(id = "BP-BEREGNING-1967-REDUSERMOTTRYGDETID")
) kombinerer barnekullRegel og trygdetidsFaktor med { sats, trygdetidsfaktor ->
    (sats * trygdetidsfaktor)
}

val kroneavrundingKonstant = definerKonstant<BarnepensjonGrunnlag, RoundingMode>(
    gjelderFra = BP_1967_DATO,
    beskrivelse = "Formel for avrunding til nærmeste krone. Dersom det er like langt, rund opp.",
    regelReferanse = RegelReferanse(id = "REGEL-KRONEAVRUNDING"),
    verdi = RoundingMode.HALF_UP
)

val beregnBarnepensjon1967Regel = RegelMeta(
    gjelderFra = BP_1967_DATO,
    beskrivelse = "Beregner barnepensjon med regelverk fra 1967 med kroneavrunding",
    regelReferanse = RegelReferanse(id = "REGEL-KRONEAVRUNDING")
) kombinerer reduksjonMotFolketrygdRegel og kroneavrundingKonstant med { sum, avrunding ->
    sum.setScale(0, avrunding).toInt()
}