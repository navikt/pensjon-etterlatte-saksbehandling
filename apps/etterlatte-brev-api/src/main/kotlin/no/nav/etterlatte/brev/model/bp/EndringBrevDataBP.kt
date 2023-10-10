package no.nav.etterlatte.brev.model.bp

import no.nav.etterlatte.brev.behandling.Behandling
import no.nav.etterlatte.brev.behandling.Utbetalingsinfo
import no.nav.etterlatte.brev.model.AvslagBrevData
import no.nav.etterlatte.brev.model.BrevData
import no.nav.etterlatte.brev.model.EndringBrevData
import no.nav.etterlatte.brev.model.EtterbetalingDTO
import no.nav.etterlatte.brev.model.InnholdMedVedlegg
import no.nav.etterlatte.brev.model.Slate
import no.nav.etterlatte.libs.common.behandling.BarnepensjonSoeskenjusteringGrunn
import no.nav.etterlatte.libs.common.behandling.RevurderingAarsak
import no.nav.etterlatte.libs.common.behandling.RevurderingInfo

data class EndringHovedmalBrevData(
    val erEndret: Boolean,
    val etterbetaling: EtterbetalingDTO?,
    val utbetalingsinfo: Utbetalingsinfo,
    val innhold: List<Slate.Element>,
) : EndringBrevData() {
    companion object {
        fun fra(
            behandling: Behandling,
            innhold: InnholdMedVedlegg,
        ): BrevData =
            EndringHovedmalBrevData(
                erEndret = true, // TODO når resten av fengselsopphold implementerast
                etterbetaling = behandling.etterbetalingDTO,
                utbetalingsinfo = behandling.utbetalingsinfo!!,
                innhold = innhold.innhold(),
            )
    }
}

data class SoeskenjusteringRevurderingBrevdata(
    val utbetalingsinfo: Utbetalingsinfo,
    val grunnForJustering: BarnepensjonSoeskenjusteringGrunn,
) : EndringBrevData() {
    companion object {
        fun fra(behandling: Behandling): SoeskenjusteringRevurderingBrevdata {
            val revurderingsinfo =
                AvslagBrevData.valider<RevurderingInfo.Soeskenjustering>(
                    behandling,
                    RevurderingAarsak.SOESKENJUSTERING,
                )

            return SoeskenjusteringRevurderingBrevdata(
                utbetalingsinfo = behandling.utbetalingsinfo!!,
                grunnForJustering = revurderingsinfo.grunnForSoeskenjustering,
            )
        }
    }
}
