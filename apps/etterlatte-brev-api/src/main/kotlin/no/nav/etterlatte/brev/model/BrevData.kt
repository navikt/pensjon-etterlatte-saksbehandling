package no.nav.etterlatte.brev.model

import no.nav.etterlatte.libs.common.behandling.RevurderingInfo
import no.nav.etterlatte.libs.common.behandling.Revurderingaarsak

abstract class BrevData {
    inline fun <reified T : RevurderingInfo> valider(
        revurderingsaarsakVedtak: Revurderingaarsak?,
        revurderingInfo: RevurderingInfo?,
        revurderingAarsak: Revurderingaarsak,
    ): T {
        val lesbartnavn = revurderingAarsak.name.lowercase()
        if (revurderingsaarsakVedtak != revurderingAarsak) {
            throw IllegalArgumentException(
                "Kan ikke opprette et revurderingsbrev for $lesbartnavn når " +
                    "revurderingsårsak er $revurderingsaarsakVedtak",
            )
        }
        if (revurderingInfo !is T) {
            throw IllegalArgumentException(
                "Kan ikke opprette et revurderingsbrev for $lesbartnavn når " +
                    "revurderingsinfo ikke er $lesbartnavn",
            )
        }
        return revurderingInfo
    }
}

object AvslagBrevData : BrevData() {
    fun fra(): AvslagBrevData = AvslagBrevData
}

abstract class EndringBrevData : BrevData()

abstract class OpphoerBrevData : BrevData()

data class ManueltBrevData(val innhold: List<Slate.Element>) : BrevData()
