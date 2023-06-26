package no.nav.etterlatte.brev.model

import no.nav.etterlatte.brev.behandling.Behandling
import no.nav.etterlatte.libs.common.behandling.RevurderingAarsak
import java.time.LocalDate

abstract class OpphoerBrevData : BrevData()

data class AdopsjonRevurderingBrevdata(val virkningsdato: LocalDate, val adoptertAv: AdoptertAv) : OpphoerBrevData() {
    companion object {
        fun fra(behandling: Behandling): AdopsjonRevurderingBrevdata {
            if (behandling.revurderingsaarsak != RevurderingAarsak.ADOPSJON) {
                throw IllegalArgumentException(
                    "Kan ikke opprette et revurderingsbrev for adopsjon når " +
                        "revurderingsårsak er ${behandling.revurderingsaarsak}"
                )
            }

            return AdopsjonRevurderingBrevdata(
                virkningsdato = LocalDate.now(), // TODO hent frå behandlingsobjektet
                adoptertAv = AdoptertAv("Navn Navnesen") // TODO hent frå behandlingsobjektet
            )
        }
    }
}

data class AdoptertAv(val navn: String)