package no.nav.etterlatte.brev.model

import no.nav.etterlatte.brev.behandling.Trygdetidsperiode
import no.nav.pensjon.brevbaker.api.model.Kroner

data class Beregningsinfo(
    val innhold: List<Slate.Element>,
    val grunnbeloep: Kroner,
    val beregningsperioder: List<NyBeregningsperiode>,
    val trygdetidsperioder: List<Trygdetidsperiode>,
)

data class NyBeregningsperiode(
    val inntekt: Kroner,
    val trygdetid: Int,
    val stoenadFoerReduksjon: Kroner,
    var utbetaltBeloep: Kroner,
)
