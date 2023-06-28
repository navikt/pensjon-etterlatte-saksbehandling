package no.nav.etterlatte.brev.model

import no.nav.etterlatte.brev.behandling.Behandling
import no.nav.etterlatte.brev.behandling.Utbetalingsinfo
import no.nav.etterlatte.libs.common.behandling.BarnepensjonSoeskenjusteringGrunn
import no.nav.etterlatte.libs.common.behandling.RevurderingAarsak
import no.nav.etterlatte.libs.common.behandling.RevurderingInfo

abstract class EndringBrevData : BrevData()

data class SoeskenjusteringRevurderingBrevdata(
    val utbetalingsinfo: Utbetalingsinfo,
    val grunnForJustering: BarnepensjonSoeskenjusteringGrunn
) : EndringBrevData() {

    companion object {
        fun fra(behandling: Behandling): SoeskenjusteringRevurderingBrevdata {
            if (behandling.revurderingsaarsak != RevurderingAarsak.SOESKENJUSTERING) {
                throw IllegalArgumentException(
                    "Kan ikke opprette et revurderingsbrev for søskenjustering når " +
                        "revurderingsårsak er ${behandling.revurderingsaarsak}"
                )
            }
            if (behandling.revurderingInfo !is RevurderingInfo.Soeskenjustering) {
                throw IllegalArgumentException(
                    "Kan ikke opprette et revurderingsbrev for søskenjustering når " +
                        "revurderingsinfo ikke er Søskenjustering"
                )
            }

            return SoeskenjusteringRevurderingBrevdata(
                utbetalingsinfo = requireNotNull(behandling.utbetalingsinfo) {
                    "Kan ikke opprette et revurderingsbrev for søksenjustering uten utbetalingsinfo"
                },
                grunnForJustering = behandling.revurderingInfo.grunnForSoeskenjustering
            )
        }
    }
}