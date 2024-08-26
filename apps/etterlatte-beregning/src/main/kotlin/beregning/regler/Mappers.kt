package no.nav.etterlatte.beregning.regler

import no.nav.etterlatte.libs.common.beregning.BeregningsMetode
import no.nav.etterlatte.libs.common.beregning.SamletTrygdetidMedBeregningsMetode
import no.nav.etterlatte.libs.common.trygdetid.TrygdetidDto
import no.nav.etterlatte.regler.Beregningstall

fun TrygdetidDto.toSamlet(beregningsMetode: BeregningsMetode): SamletTrygdetidMedBeregningsMetode? {
    val resultat = this.beregnetTrygdetid?.resultat ?: return null

    return SamletTrygdetidMedBeregningsMetode(
        beregningsMetode = beregningsMetode,
        samletTrygdetidNorge = resultat.samletTrygdetidNorge?.let { Beregningstall(it.toDouble()) },
        samletTrygdetidTeoretisk = resultat.samletTrygdetidTeoretisk?.let { Beregningstall(it.toDouble()) },
        prorataBroek = resultat.prorataBroek,
        ident = this.ident,
    )
}

data class AnvendtTrygdetid(
    val beregningsMetode: BeregningsMetode,
    val trygdetid: Beregningstall,
    val ident: String,
)
