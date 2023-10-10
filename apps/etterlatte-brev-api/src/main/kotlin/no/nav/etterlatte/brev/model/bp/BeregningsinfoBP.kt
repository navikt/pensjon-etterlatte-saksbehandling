package no.nav.etterlatte.brev.model.bp

import no.nav.etterlatte.brev.behandling.Behandling
import no.nav.etterlatte.brev.behandling.Beregningsperiode
import no.nav.etterlatte.brev.behandling.Trygdetidsperiode
import no.nav.etterlatte.brev.model.BrevVedleggKey
import no.nav.etterlatte.brev.model.InnholdMedVedlegg
import no.nav.etterlatte.brev.model.Slate
import no.nav.pensjon.brevbaker.api.model.Kroner

data class BeregningsinfoBP(
    val innhold: List<Slate.Element>,
    val grunnbeloep: Kroner,
    val beregningsperioder: List<Beregningsperiode>,
    val antallBarn: Int,
    val aarTrygdetid: Int,
    val maanederTrygdetid: Int,
    val trygdetidsperioder: List<Trygdetidsperiode>,
) {
    companion object {
        fun fra(
            behandling: Behandling,
            innhold: InnholdMedVedlegg,
        ) = BeregningsinfoBP(
            innhold = innhold.finnVedlegg(BrevVedleggKey.BP_BEREGNING_TRYGDETID),
            grunnbeloep = Kroner(behandling.grunnbeloep.grunnbeloep),
            beregningsperioder = behandling.utbetalingsinfo!!.beregningsperioder,
            antallBarn = behandling.utbetalingsinfo.antallBarn,
            aarTrygdetid = behandling.trygdetid!!.aarTrygdetid,
            maanederTrygdetid = behandling.trygdetid.maanederTrygdetid,
            trygdetidsperioder = behandling.trygdetid.perioder,
        )
    }
}
