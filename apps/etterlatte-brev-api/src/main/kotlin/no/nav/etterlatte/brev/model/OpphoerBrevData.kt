package no.nav.etterlatte.brev.model

import no.nav.etterlatte.brev.behandling.Behandling
import no.nav.etterlatte.libs.common.behandling.Navn
import no.nav.etterlatte.libs.common.behandling.RevurderingAarsak
import no.nav.etterlatte.libs.common.behandling.RevurderingInfo
import java.time.LocalDate

abstract class OpphoerBrevData : BrevData()

data class AdopsjonRevurderingBrevdata(val virkningsdato: LocalDate, val adoptertAv: Navn) :
    OpphoerBrevData() {
    companion object {
        fun fra(behandling: Behandling): AdopsjonRevurderingBrevdata {
            if (behandling.revurderingsaarsak != RevurderingAarsak.ADOPSJON) {
                throw IllegalArgumentException(
                    "Kan ikke opprette et revurderingsbrev for adopsjon når " +
                        "revurderingsårsak er ${behandling.revurderingsaarsak}"
                )
            }
            if (behandling.revurderingInfo !is RevurderingInfo.Adopsjon) {
                throw IllegalArgumentException(
                    "Kan ikke opprette et revurderingsbrev for adopsjon når " +
                        "revurderingsinfo ikke er adopsjon"
                )
            }

            return AdopsjonRevurderingBrevdata(
                virkningsdato = behandling.virkningsdato!!.atDay(1),
                adoptertAv = behandling.revurderingInfo.adoptertAv
            )
        }
    }
}