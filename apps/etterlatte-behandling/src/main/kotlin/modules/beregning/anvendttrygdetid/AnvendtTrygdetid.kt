package no.nav.etterlatte.modules.beregning.anvendttrygdetid

import no.nav.etterlatte.libs.common.beregning.BeregningsMetode
import no.nav.etterlatte.regler.Beregningstall

data class AnvendtTrygdetid(
    val beregningsMetode: BeregningsMetode,
    val trygdetid: Beregningstall,
    val ident: String,
)
