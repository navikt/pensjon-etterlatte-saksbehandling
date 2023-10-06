package no.nav.etterlatte.brev.model

import no.nav.etterlatte.brev.behandling.Behandling
import no.nav.etterlatte.libs.common.behandling.RevurderingAarsak
import no.nav.etterlatte.libs.common.behandling.RevurderingInfo

abstract class BrevData {
    inline fun <reified T : RevurderingInfo> valider(
        behandling: Behandling,
        revurderingAarsak: RevurderingAarsak,
    ): T {
        val lesbartnavn = revurderingAarsak.name.lowercase()
        if (behandling.revurderingsaarsak != revurderingAarsak) {
            throw IllegalArgumentException(
                "Kan ikke opprette et revurderingsbrev for $lesbartnavn når " +
                    "revurderingsårsak er ${behandling.revurderingsaarsak}",
            )
        }
        if (behandling.revurderingInfo !is T) {
            throw IllegalArgumentException(
                "Kan ikke opprette et revurderingsbrev for $lesbartnavn når " +
                    "revurderingsinfo ikke er $lesbartnavn",
            )
        }
        return behandling.revurderingInfo
    }
}

object AvslagBrevData : BrevData() {
    // TODO: denne skal ikke ha hele behandlingen inn
    fun fra(behandling: Behandling): AvslagBrevData = AvslagBrevData
}

abstract class EndringBrevData : BrevData()

abstract class OpphoerBrevData : BrevData()

data class ManueltBrevData(val innhold: List<Slate.Element>) : BrevData()
