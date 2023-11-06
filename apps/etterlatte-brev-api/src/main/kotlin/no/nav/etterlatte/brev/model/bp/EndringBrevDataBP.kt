package no.nav.etterlatte.brev.model.bp

import no.nav.etterlatte.brev.behandling.GenerellBrevData
import no.nav.etterlatte.brev.behandling.Utbetalingsinfo
import no.nav.etterlatte.brev.model.AvslagBrevData
import no.nav.etterlatte.brev.model.BrevData
import no.nav.etterlatte.brev.model.EndringBrevData
import no.nav.etterlatte.brev.model.EtterbetalingBrev
import no.nav.etterlatte.brev.model.EtterbetalingDTO
import no.nav.etterlatte.brev.model.InnholdMedVedlegg
import no.nav.etterlatte.brev.model.Slate
import no.nav.etterlatte.libs.common.behandling.BarnepensjonSoeskenjusteringGrunn
import no.nav.etterlatte.libs.common.behandling.RevurderingInfo
import no.nav.etterlatte.libs.common.behandling.Revurderingaarsak

data class EndringHovedmalBrevData(
    val erEndret: Boolean,
    val etterbetaling: EtterbetalingBrev?,
    val utbetalingsinfo: Utbetalingsinfo,
    val innhold: List<Slate.Element>,
) : EndringBrevData() {
    companion object {
        fun fra(
            utbetalingsinfo: Utbetalingsinfo,
            etterbetaling: EtterbetalingDTO?,
            innhold: InnholdMedVedlegg,
        ): BrevData =
            EndringHovedmalBrevData(
                erEndret = true, // TODO når resten av fengselsopphold implementerast
                etterbetaling = EtterbetalingBrev.fra(etterbetaling, utbetalingsinfo.beregningsperioder),
                utbetalingsinfo = Utbetalingsinfo.kopier(utbetalingsinfo, etterbetaling),
                innhold = innhold.innhold(),
            )
    }
}

data class SoeskenjusteringRevurderingBrevdata(
    val utbetalingsinfo: Utbetalingsinfo,
    val grunnForJustering: BarnepensjonSoeskenjusteringGrunn,
) : EndringBrevData() {
    companion object {
        fun fra(
            generellBrevData: GenerellBrevData,
            utbetalingsinfo: Utbetalingsinfo,
        ): SoeskenjusteringRevurderingBrevdata {
            val revurderingsinfo =
                AvslagBrevData.valider<RevurderingInfo.Soeskenjustering>(
                    generellBrevData.revurderingsaarsak,
                    generellBrevData.forenkletVedtak.revurderingInfo,
                    Revurderingaarsak.SOESKENJUSTERING,
                )

            return SoeskenjusteringRevurderingBrevdata(
                utbetalingsinfo = utbetalingsinfo,
                grunnForJustering = revurderingsinfo.grunnForSoeskenjustering,
            )
        }
    }
}
