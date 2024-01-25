package no.nav.etterlatte.brev.model.bp

import no.nav.etterlatte.brev.behandling.Trygdetid
import no.nav.etterlatte.brev.behandling.Utbetalingsinfo
import no.nav.etterlatte.brev.model.BrevData
import no.nav.etterlatte.brev.model.EndringBrevData
import no.nav.etterlatte.brev.model.EtterbetalingBrev
import no.nav.etterlatte.brev.model.EtterbetalingDTO
import no.nav.etterlatte.brev.model.InnholdMedVedlegg
import no.nav.etterlatte.brev.model.Slate
import no.nav.etterlatte.grunnbeloep.Grunnbeloep

data class EndringHovedmalBrevData(
    val erEndret: Boolean,
    val etterbetaling: EtterbetalingBrev? = null,
    val beregningsinfo: BeregningsinfoBP,
    val utbetalingsinfo: Utbetalingsinfo,
    val innhold: List<Slate.Element>,
) : EndringBrevData() {
    companion object {
        fun fra(
            utbetalingsinfo: Utbetalingsinfo,
            forrigeUtbetalingsinfo: Utbetalingsinfo?,
            etterbetaling: EtterbetalingDTO?,
            trygdetid: Trygdetid,
            grunnbeloep: Grunnbeloep,
            innhold: InnholdMedVedlegg,
        ): BrevData =
            EndringHovedmalBrevData(
                erEndret = forrigeUtbetalingsinfo == null || forrigeUtbetalingsinfo.beloep != utbetalingsinfo.beloep,
                etterbetaling = EtterbetalingBrev.fra(etterbetaling, utbetalingsinfo.beregningsperioder),
                beregningsinfo = BeregningsinfoBP.fra(utbetalingsinfo, trygdetid, grunnbeloep, innhold),
                utbetalingsinfo = Utbetalingsinfo.kopier(utbetalingsinfo, etterbetaling),
                innhold = innhold.innhold(),
            )
    }
}
