package no.nav.etterlatte.libs.regler.beregning.barnepensjon1967

import no.nav.etterlatte.libs.regler.RegelMeta
import no.nav.etterlatte.libs.regler.RegelReferanse
import no.nav.etterlatte.libs.regler.benytter
import no.nav.etterlatte.libs.regler.beregning.barnepensjon1967.barnekull.barnekullRegel
import no.nav.etterlatte.libs.regler.beregning.barnepensjon1967.trygdetidsfaktor.trygdetidsFaktor
import no.nav.etterlatte.libs.regler.med
import no.nav.etterlatte.libs.regler.og
import java.time.LocalDate

val BP_1967_DATO: LocalDate = LocalDate.of(1967, 1, 1)

val beregnBarnepensjon1967Regel = RegelMeta(
    gjelderFra = BP_1967_DATO,
    beskrivelse = "Reduserer ytelsen mot opptjening i folketrygden",
    regelReferanse = RegelReferanse(id = "BP-BEREGNING-1967-REDUSERMOTTRYGDETID")
) benytter barnekullRegel og trygdetidsFaktor med { sats, trygdetidsfaktor ->
    (sats * trygdetidsfaktor)
}