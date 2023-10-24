package no.nav.etterlatte.beregning.regler

import no.nav.etterlatte.libs.common.beregning.BeregningsMetode
import no.nav.etterlatte.libs.common.beregning.SamletTrygdetidMedBeregningsMetode
import no.nav.etterlatte.libs.common.trygdetid.DetaljertBeregnetTrygdetidResultat
import no.nav.etterlatte.regler.Beregningstall

fun DetaljertBeregnetTrygdetidResultat.toSamlet(
    beregningsMetode: BeregningsMetode,
    avdoed: String?,
): SamletTrygdetidMedBeregningsMetode {
    return SamletTrygdetidMedBeregningsMetode(
        beregningsMetode = beregningsMetode,
        samletTrygdetidNorge = this.samletTrygdetidNorge?.let { Beregningstall(it.toDouble()) },
        samletTrygdetidTeoretisk = this.samletTrygdetidTeoretisk?.let { Beregningstall(it.toDouble()) },
        prorataBroek = this.prorataBroek,
        avdoed = avdoed,
    )
}

data class AnvendtTrygdetid(
    val beregningsMetode: BeregningsMetode,
    val trygdetid: Beregningstall,
    val avdoed: String? = null,
)
