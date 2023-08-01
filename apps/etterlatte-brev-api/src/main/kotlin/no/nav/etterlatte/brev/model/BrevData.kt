package no.nav.etterlatte.brev.model

import no.nav.etterlatte.brev.behandling.Behandling
import no.nav.etterlatte.libs.common.behandling.RevurderingAarsak
import no.nav.etterlatte.libs.common.behandling.RevurderingInfo
import no.nav.pensjon.brevbaker.api.model.Telefonnummer

abstract class BrevData {
    protected inline fun <reified T : RevurderingInfo> valider(
        behandling: Behandling,
        revurderingAarsak: RevurderingAarsak
    ): T {
        val lesbartnavn = revurderingAarsak.name.lowercase()
        if (behandling.revurderingsaarsak != revurderingAarsak) {
            throw IllegalArgumentException(
                "Kan ikke opprette et revurderingsbrev for $lesbartnavn når " +
                    "revurderingsårsak er ${behandling.revurderingsaarsak}"
            )
        }
        if (behandling.revurderingInfo !is T) {
            throw IllegalArgumentException(
                "Kan ikke opprette et revurderingsbrev for $lesbartnavn når " +
                    "revurderingsinfo ikke er $lesbartnavn"
            )
        }
        return behandling.revurderingInfo
    }
}

data class Avsender(
    val kontor: String,
    val adresse: String,
    val postnummer: String,
    val telefonnummer: Telefonnummer,
    val saksbehandler: String,
    val attestant: String?
)