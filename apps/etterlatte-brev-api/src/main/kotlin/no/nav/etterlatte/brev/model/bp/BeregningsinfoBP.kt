package no.nav.etterlatte.brev.model.bp

import no.nav.etterlatte.brev.behandling.Beregningsperiode
import no.nav.etterlatte.brev.behandling.Trygdetid
import no.nav.etterlatte.brev.behandling.Trygdetidsperiode
import no.nav.etterlatte.brev.behandling.Utbetalingsinfo
import no.nav.etterlatte.brev.model.BrevVedleggKey
import no.nav.etterlatte.brev.model.InnholdMedVedlegg
import no.nav.etterlatte.brev.model.Slate
import no.nav.etterlatte.grunnbeloep.Grunnbeloep
import no.nav.etterlatte.libs.common.IntBroek
import no.nav.etterlatte.libs.common.beregning.BeregningsMetode
import no.nav.pensjon.brevbaker.api.model.Kroner

data class BeregningsinfoBP(
    val innhold: List<Slate.Element>,
    val grunnbeloep: Kroner,
    val beregningsperioder: List<Beregningsperiode>,
    val antallBarn: Int,
    val aarTrygdetid: Int,
    val maanederTrygdetid: Int,
    val trygdetidsperioder: List<Trygdetidsperiode>,
    val prorataBroek: IntBroek?,
    val beregningsMetodeAnvendt: BeregningsMetode,
    val beregningsMetodeFraGrunnlag: BeregningsMetode,
    val mindreEnnFireFemtedelerAvOpptjeningstiden: Boolean,
) {
    companion object {
        fun fra(
            utbetalingsinfo: Utbetalingsinfo,
            trygdetid: Trygdetid,
            grunnbeloep: Grunnbeloep,
            innhold: InnholdMedVedlegg,
        ) = BeregningsinfoBP(
            innhold = innhold.finnVedlegg(BrevVedleggKey.BP_BEREGNING_TRYGDETID),
            grunnbeloep = Kroner(grunnbeloep.grunnbeloep),
            beregningsperioder = utbetalingsinfo.beregningsperioder,
            antallBarn = utbetalingsinfo.antallBarn,
            aarTrygdetid = trygdetid.aarTrygdetid,
            maanederTrygdetid = trygdetid.maanederTrygdetid,
            trygdetidsperioder = trygdetid.perioder,
            prorataBroek = trygdetid.prorataBroek,
            beregningsMetodeAnvendt = utbetalingsinfo.beregningsperioder.first().beregningsMetodeAnvendt,
            beregningsMetodeFraGrunnlag = utbetalingsinfo.beregningsperioder.first().beregningsMetodeFraGrunnlag,
            mindreEnnFireFemtedelerAvOpptjeningstiden = trygdetid.mindreEnnFireFemtedelerAvOpptjeningstiden,
        )
    }
}
